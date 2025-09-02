package com.example.achoggmusicplayer;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OGG 내부 정보와 햅틱(ACH) 파형 추출기
 * - inspect(): 오디오 메타(채널, 샘플레이트, 길이, ANDROID_HAPTIC 태그 유무)
 * - parseAchFromOgg(): 3ch 이상이면 3번째 채널을 "햅틱 전용 채널"로 간주하여
 *   MediaCodec으로 PCM 디코드 → 10ms 윈도우 RMS → 0~255 진폭 파형으로 변환.
 *   3ch 아니거나 디코드 불가면: ANDROID_HAPTIC 태그가 있으면 간단 펄스, 아니면 null.
 */
public class OggHapticInspector {

    // ==== 오디오 메타 결과 ====
    public static class Result {
        public String mime;
        public int channelCount;
        public int sampleRate;
        public long durationMs;
        public boolean hasHapticTag; // 파일 내 "ANDROID_HAPTIC" 문자열 존재 여부
        public String notes;

        public boolean hasHaptic() {
            return hasHapticTag || channelCount >= 3;
        }
    }

    // ==== 햅틱 트랙(10ms bin 기반 진폭 테이블) 모델 ====
    public static class HapticTrackInfo {
        public final long totalDurationMs;
        public final int binMs;    // 10ms 고정
        public final int[] amps;   // 각 bin의 0~255 진폭

        public HapticTrackInfo(long totalDurationMs, int binMs, int[] amps) {
            this.totalDurationMs = totalDurationMs;
            this.binMs = binMs;
            this.amps = amps;
        }

        /** 임의 시각 ms에서의 진폭(가장 가까운 bin) */
        public int getAmplitudeAt(long ms) {
            if (amps == null || amps.length == 0) return 0;
            if (ms < 0) return amps[0];
            int idx = (int) (ms / binMs);
            if (idx >= amps.length) return amps[amps.length - 1];
            return amps[idx];
        }
    }

    private static final String TAG = "OggHapticInspector";
    private static final int HAPTIC_BIN_MS = 10; // 10ms bin

    // ---------- 오디오 메타/포맷 ----------
    public static Result inspect(Context ctx, Uri uri) throws Exception {
        Result r = new Result();

        // 1) duration/mime
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(ctx, uri);
        String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try { r.durationMs = dur == null ? -1 : Long.parseLong(dur); } catch (Throwable ignore) {}
        r.mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

        // 2) audio track 포맷(채널/샘플레이트)
        MediaExtractor ex = new MediaExtractor();
        ex.setDataSource(ctx, uri, null);
        int trackCount = ex.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat fmt = ex.getTrackFormat(i);
            String m = fmt.getString(MediaFormat.KEY_MIME);
            if (m != null && m.toLowerCase(Locale.US).startsWith("audio/")) {
                if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    r.channelCount = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                }
                if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    r.sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                }
                break;
            }
        }
        ex.release();

        // 3) 간단 문자열 스캔: "ANDROID_HAPTIC"
        r.hasHapticTag = scanForString(ctx, uri, "ANDROID_HAPTIC", 1024 * 1024);

        // 4) 메모
        if (r.channelCount >= 3 && !r.hasHapticTag) {
            r.notes = "3채널 이상이지만 ANDROID_HAPTIC 문자열은 없음(전용 채널로 추정하여 사용).";
        } else if (r.channelCount <= 2 && r.hasHapticTag) {
            r.notes = "태그는 있으나 채널 수는 1~2ch(인코더/Extractor 차이 가능).";
        }

        return r;
    }

    private static boolean scanForString(Context ctx, Uri uri, String needle, int maxBytes) {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            byte[] buf = new byte[64 * 1024];
            int total = 0;
            byte[] needleBytes = needle.getBytes(StandardCharsets.US_ASCII);
            int nLen = needleBytes.length;

            int read;
            byte[] window = new byte[Math.max(buf.length + nLen, 128)];
            int wSize = 0;

            while ((read = is.read(buf)) > 0 && total < maxBytes) {
                if (wSize + read > window.length) wSize = 0;
                System.arraycopy(buf, 0, window, wSize, read);
                wSize += read;
                total += read;

                if (indexOf(window, wSize, needleBytes) >= 0) return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "scanForString fail: " + e.getMessage());
        }
        return false;
    }

    private static int indexOf(byte[] hay, int hayLen, byte[] needle) {
        outer:
        for (int i = 0; i <= hayLen - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    // ---------- 햅틱 파형 추출 ----------
    public static HapticTrackInfo parseAchFromOgg(Context ctx, Uri uri) {
        try {
            Result meta = inspect(ctx, uri);
            long totalMs = meta.durationMs > 0 ? meta.durationMs : 0;

            if (meta.channelCount >= 3 && meta.sampleRate > 0 && totalMs > 0) {
                // 3ch 이상 → ch2(0-based) = 3번째 채널에서 10ms RMS → 0~255
                int sr = meta.sampleRate;
                int binSamples = (int) (sr * (HAPTIC_BIN_MS / 1000.0));
                if (binSamples <= 0) binSamples = Math.max(1, sr / 100); // 안전
                int[] amps = decodeThirdChannelRmsToAmps(ctx, uri, 2, sr, binSamples, totalMs);
                if (amps != null && amps.length > 0) {
                    return new HapticTrackInfo(totalMs, HAPTIC_BIN_MS, amps);
                }
            }

            // 우회: 태그만 있으면 간단 펄스 2초 생성
            if (meta.hasHapticTag) {
                long total = Math.min(totalMs > 0 ? totalMs : 2000, 4000);
                int bins = (int) (total / HAPTIC_BIN_MS);
                if (bins <= 0) bins = 1;
                int[] amps = new int[bins];
                // 60ms on/off 패턴
                int onBins = 6, offBins = 6;
                int p = 0;
                while (p < bins) {
                    for (int i = 0; i < onBins && p < bins; i++) amps[p++] = 180;
                    for (int i = 0; i < offBins && p < bins; i++) amps[p++] = 0;
                }
                return new HapticTrackInfo(bins * HAPTIC_BIN_MS, HAPTIC_BIN_MS, amps);
            }

        } catch (Exception e) {
            Log.e(TAG, "parseAchFromOgg error", e);
        }
        return null;
    }

    /**
     * MediaCodec으로 디코드하여 3번째 채널(short PCM) RMS→amp(0~255)로 변환
     */
    private static int[] decodeThirdChannelRmsToAmps(Context ctx, Uri uri, int hapticChIndex,
                                                     int sampleRate, int binSamples, long totalMs) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(ctx, uri, null);
        int audioTrack = -1;
        MediaFormat format = null;

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat fmt = extractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.toLowerCase(Locale.US).startsWith("audio/")) {
                audioTrack = i;
                format = fmt;
                break;
            }
        }
        if (audioTrack < 0 || format == null) {
            extractor.release();
            return null;
        }

        extractor.selectTrack(audioTrack);
        String mime = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        // output은 PCM 16bit로 나오는 것이 일반적
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false, outputDone = false;

        int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 2;
        if (hapticChIndex >= channels) { // 채널 수가 기대보다 적으면 실패
            try { codec.stop(); codec.release(); } catch (Exception ignore) {}
            extractor.release();
            return null;
        }

        // 10ms bin 누적용
        ArrayList<Integer> amps = new ArrayList<>();
        long samplesAccum = 0;
        double sumSquares = 0.0;
        long totalSamplesTarget = (long) ((totalMs / 1000.0) * sampleRate);

        while (!outputDone) {
            // 입력
            if (!inputDone) {
                int inIndex = codec.dequeueInputBuffer(10_000);
                if (inIndex >= 0) {
                    ByteBuffer ibuf = codec.getInputBuffer(inIndex);
                    int sampleSize = extractor.readSampleData(ibuf, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long pts = extractor.getSampleTime();
                        codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                        extractor.advance();
                    }
                }
            }

            // 출력
            int outIndex = codec.dequeueOutputBuffer(info, 10_000);
            if (outIndex >= 0) {
                ByteBuffer obuf = codec.getOutputBuffer(outIndex);
                if (obuf != null && info.size > 0) {
                    obuf.order(ByteOrder.LITTLE_ENDIAN);
                    // 16bit PCM interleaved
                    int samples = info.size / 2; // shorts
                    short[] tmp = new short[samples];
                    obuf.asShortBuffer().get(tmp);
                    // hapticChIndex 채널만 추출
                    for (int i = hapticChIndex; i < samples; i += channels) {
                        int s = tmp[i];
                        sumSquares += (s * s);
                        samplesAccum++;
                        if (samplesAccum >= binSamples) {
                            double rms = Math.sqrt(sumSquares / samplesAccum);
                            // short max 32767 → 0~255 스케일
                            int amp = (int) Math.round((rms / 32767.0) * 255.0);
                            if (amp < 0) amp = 0; if (amp > 255) amp = 255;
                            amps.add(amp);
                            sumSquares = 0.0;
                            samplesAccum = 0;
                        }
                    }
                }
                codec.releaseOutputBuffer(outIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ||
                       outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // ignore
            }
        }

        try { codec.stop(); codec.release(); } catch (Exception ignore) {}
        extractor.release();

        // 남은 샘플 처리
        if (samplesAccum > 0) {
            double rms = Math.sqrt(sumSquares / samplesAccum);
            int amp = (int) Math.round((rms / 32767.0) * 255.0);
            if (amp < 0) amp = 0; if (amp > 255) amp = 255;
            amps.add(amp);
        }

        // 총 길이에 맞춰 bin 개수 보정
        int expectedBins = Math.max(1, (int) (totalMs / HAPTIC_BIN_MS));
        if (amps.size() < expectedBins) {
            // 부족하면 0으로 패딩
            while (amps.size() < expectedBins) amps.add(0);
        } else if (amps.size() > expectedBins) {
            // 많으면 잘라냄
            while (amps.size() > expectedBins) amps.remove(amps.size() - 1);
        }

        // 배열 변환
        int[] out = new int[amps.size()];
        for (int i = 0; i < amps.size(); i++) out[i] = amps.get(i);
        return out;
    }
}

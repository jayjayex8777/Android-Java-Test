package com.example.achoggmusicplayer;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OGG 내부 정보와 햅틱(ACH) 트랙 추정/파싱 스켈레톤.
 * - inspect(): 오디오 메타/채널/태그 등 확인
 * - parseAchFromOgg(): 실제 햅틱 트랙 정보를 리턴(없으면 null).
 *   현재는 데모 형태로 2초 길이의 펄스 세그먼트를 생성.
 *   실제 파싱 코드를 보유하시면 이 메서드 내부만 교체하세요(인터페이스 고정).
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
            // 경험상 3채널 이상이면 Haptic 포함 가능성이 높음, 또는 ANDROID_HAPTIC 태그가 있으면 '있음'으로 간주
            return hasHapticTag || channelCount >= 3;
        }
    }

    // ==== 햅틱 트랙(세그먼트 리스트) 모델 ====
    public static class HapticSegment {
        public final long startMs;     // 세그먼트 시작(트랙 기준)
        public final long durationMs;  // 세그먼트 길이
        public final int amplitude;    // 0~255

        public HapticSegment(long startMs, long durationMs, int amplitude) {
            this.startMs = startMs;
            this.durationMs = durationMs;
            this.amplitude = amplitude;
        }
    }

    public static class HapticTrackInfo {
        public final long totalDurationMs;
        public final List<HapticSegment> segments;

        public HapticTrackInfo(long totalDurationMs, List<HapticSegment> segments) {
            this.totalDurationMs = totalDurationMs;
            this.segments = segments;
        }
    }

    private static final String TAG = "OggHapticInspector";

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
            r.notes = "3채널 이상이지만 ANDROID_HAPTIC 문자열은 발견되지 않음(디바이스/인코딩에 따라 정상 동작 가능).";
        } else if (r.channelCount <= 2 && r.hasHapticTag) {
            r.notes = "태그는 있으나 채널 수는 1~2ch로 보고됨(인코더/Extractor 차이 가능).";
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
                if (wSize + read > window.length) {
                    wSize = 0;
                }
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

    // ---------- (데모) ACH 트랙 파싱 ----------
    public static HapticTrackInfo parseAchFromOgg(Context ctx, Uri uri) {
        // TODO: 실제 파서를 보유하고 계시면 이 부분에 구현을 넣으세요.
        // 현재는 데모로 2.5초 길이에 60ms ON / 60ms OFF 펄스 시퀀스를 생성합니다.
        long total = 2500;
        ArrayList<HapticSegment> list = new ArrayList<>();
        long t = 0;
        while (t < total) {
            long on = Math.min(60, total - t);
            list.add(new HapticSegment(t, on, 180)); // amplitude 180/255
            t += 120; // on 60ms, off 60ms
        }
        return new HapticTrackInfo(total, list);
    }
}

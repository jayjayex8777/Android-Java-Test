package com.example.achoggmusicplayer;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class OggHapticInspector {

    public static class Result {
        public String mime;
        public int channelCount;
        public int sampleRate;
        public long durationMs;
        public boolean hasHapticTag; // 파일 내 문자열에 ANDROID_HAPTIC 존재 여부(단순 스캔)
        public String notes;

        public boolean hasHaptic() {
            // 경험상 3채널 이상이면 Haptic 포함 가능성이 높음.
            // 또는 ANDROID_HAPTIC 태그가 있으면 '있음'으로 간주.
            return hasHapticTag || channelCount >= 3;
        }
    }

    private static final String TAG = "OggHapticInspector";

    public static Result inspect(Context ctx, Uri uri) throws Exception {
        Result r = new Result();

        // 1) MediaMetadataRetriever로 duration, mime
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(ctx, uri);
        String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        try { r.durationMs = dur == null ? -1 : Long.parseLong(dur); } catch (Throwable ignore) {}
        r.mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

        // 2) MediaExtractor로 audio track 포맷 파기 (채널/샘플레이트)
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
                // 첫 audio 트랙만 채택
                break;
            }
        }
        ex.release();

        // 3) 간단 스캔: 파일에 "ANDROID_HAPTIC" 문자열이 있는지 확인(OGG Vorbis comment 대비)
        //    너무 큰 파일 방지 위해 최대 1MB만 검사
        r.hasHapticTag = scanForString(ctx, uri, "ANDROID_HAPTIC", 1024 * 1024);

        // 4) 메모
        if (r.channelCount >= 3 && !r.hasHapticTag) {
            r.notes = "3채널 이상이지만 ANDROID_HAPTIC 문자열은 발견되지 않음(디바이스에 따라 정상 동작 가능).";
        } else if (r.channelCount <= 2 && r.hasHapticTag) {
            r.notes = "태그는 있으나 채널 수는 1~2ch로 보고됨(인코딩/Extractor 차이 가능).";
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
            // 슬라이딩 윈도우 간단 검색
            byte[] window = new byte[Math.max(buf.length + nLen, 128)];
            int wSize = 0;

            while ((read = is.read(buf)) > 0 && total < maxBytes) {
                if (wSize + read > window.length) {
                    wSize = 0; // overflow 방지(간단화)
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
}

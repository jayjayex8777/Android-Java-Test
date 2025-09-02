package com.example.achoggmusicplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * 같은 A.ogg에서 "오디오"와 "내장 햅틱 채널(3ch 이상 시 3번째)"을 분리 제어
 * - Audio: MediaPlayer (시스템 ACH는 음소거로 끔)
 * - Haptic: OGG에서 직접 추출한 10ms 간격 진폭 파형을 Vibrator로 재생
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_LAST_URI = "last_uri";
    private static final int UI_INTERVAL_MS = 20; // 10ms 표기를 위한 충분한 갱신 주기

    // UI
    private Button btnPick, btnPlay, btnPause, btnStop;
    private TextView tvPath, tvInfo, tvAudioTime, tvHapticTime;
    private SeekBar seekAudio, seekHaptic;
    private Button btnHapticPlay, btnHapticPause, btnHapticStop, btnHapticResync, btnDesyncMinus, btnDesyncPlus;

    // 오디오
    private Uri selectedUri = null;
    private MediaPlayer mediaPlayer = null;
    private long audioDurationMs = 0L;

    // 햅틱
    private Vibrator vibrator;
    private HapticPlaybackEngine hapticEngine;
    private OggHapticInspector.HapticTrackInfo hapticInfo; // null 가능

    // UI 주기 업데이트용
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiTicker = new Runnable() {
        @Override public void run() {
            updateTimesAndSeek();
            uiHandler.postDelayed(this, UI_INTERVAL_MS);
        }
    };

    private final ActivityResultLauncher<String[]> pickOggLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    selectedUri = uri;
                    tvPath.setText(String.valueOf(uri));
                    saveLastUri(uri);

                    // 파일 분석(오디오 메타 + 햅틱 파형 추출)
                    showMetadataAndPrepare(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI 바인딩
        btnPick = findViewById(R.id.btnPick);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        tvPath = findViewById(R.id.tvPath);
        tvInfo = findViewById(R.id.tvInfo);

        tvAudioTime = findViewById(R.id.tvAudioTime);
        tvHapticTime = findViewById(R.id.tvHapticTime);
        seekAudio = findViewById(R.id.seekAudio);
        seekHaptic = findViewById(R.id.seekHaptic);

        btnHapticPlay = findViewById(R.id.btnHapticPlay);
        btnHapticPause = findViewById(R.id.btnHapticPause);
        btnHapticStop = findViewById(R.id.btnHapticStop);
        btnHapticResync = findViewById(R.id.btnHapticResync);
        btnDesyncMinus = findViewById(R.id.btnDesyncMinus);
        btnDesyncPlus = findViewById(R.id.btnDesyncPlus);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        hapticEngine = new HapticPlaybackEngine(vibrator);

        // 리스너 설정
        btnPick.setOnClickListener(v -> openPicker());
        btnPlay.setOnClickListener(v -> startAudio());
        btnPause.setOnClickListener(v -> pauseAudio());
        btnStop.setOnClickListener(v -> stopAudio());

        // 오디오 시킹
        seekAudio.setMax(1000);
        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser;
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { fromUser = u; }
            @Override public void onStartTrackingTouch(SeekBar sb) { fromUser = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (!fromUser || mediaPlayer == null || audioDurationMs <= 0) return;
                fromUser = false;
                int target = (int) (audioDurationMs * (sb.getProgress() / (float) sb.getMax()));
                try { mediaPlayer.seekTo(target); } catch (Exception ignore) {}
                updateTimesAndSeek();
            }
        });

        // 햅틱 제어
        btnHapticPlay.setOnClickListener(v -> {
            if (hapticInfo == null) {
                Toast.makeText(this, "햅틱 트랙이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            hapticEngine.playFrom(hapticEngine.getCurrentPositionMs());
        });
        btnHapticPause.setOnClickListener(v -> hapticEngine.pause());
        btnHapticStop.setOnClickListener(v -> hapticEngine.stop());
        btnHapticResync.setOnClickListener(v -> {
            long aPos = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0L;
            hapticEngine.seekTo(aPos, true);
            Toast.makeText(this, "햅틱을 오디오 현재 시점으로 재동기화", Toast.LENGTH_SHORT).show();
        });

        // 의도적 디싱크(±100ms)
        btnDesyncMinus.setOnClickListener(v -> {
            long pos = hapticEngine.getCurrentPositionMs() - 100;
            hapticEngine.seekTo(Math.max(0, pos), hapticEngine.isPlaying());
        });
        btnDesyncPlus.setOnClickListener(v -> {
            long pos = hapticEngine.getCurrentPositionMs() + 100;
            long max = (hapticInfo != null) ? hapticInfo.totalDurationMs : audioDurationMs;
            hapticEngine.seekTo(Math.min(max, pos), hapticEngine.isPlaying());
        });

        // 햅틱 시킹
        seekHaptic.setMax(1000);
        seekHaptic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser;
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { fromUser = u; }
            @Override public void onStartTrackingTouch(SeekBar sb) { fromUser = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (!fromUser) return;
                fromUser = false;
                long hDur = (hapticInfo != null) ? hapticInfo.totalDurationMs : audioDurationMs;
                if (hDur <= 0) return;
                long target = (long) (hDur * (sb.getProgress() / (float) sb.getMax()));
                hapticEngine.seekTo(target, hapticEngine.isPlaying());
                updateTimesAndSeek();
            }
        });

        // UI Ticker 시작
        uiHandler.post(uiTicker);

        restoreLastUriIfPossible();
    }

    // ---------- 파일 선택 ----------
    private void openPicker() {
        pickOggLauncher.launch(new String[]{"audio/ogg", "application/ogg", "audio/x-ogg"});
    }

    private void showMetadataAndPrepare(Uri uri) {
        try {
            // 오디오 메타
            OggHapticInspector.Result r = OggHapticInspector.inspect(this, uri);
            audioDurationMs = (r.durationMs > 0) ? r.durationMs : 0L;

            // 햅틱 파형 추출(동기 X, 완전 독립)
            // 1) 3채널 이상이면 3번째 채널을 햅틱으로 간주해서 10ms RMS → 0~255 진폭 파형 생성
            // 2) 아니고 ANDROID_HAPTIC 태그만 있으면 간단 펄스(우회)
            // 3) 둘 다 아니면 null
            hapticInfo = OggHapticInspector.parseAchFromOgg(this, uri);
            if (hapticInfo != null) hapticEngine.setTrack(hapticInfo);
            else hapticEngine.clear();

            String chText;
            if (r.channelCount <= 0) chText = "Unknown";
            else if (r.channelCount == 1) chText = "Mono";
            else if (r.channelCount == 2) chText = "Stereo";
            else chText = r.channelCount + "ch (≥3: haptic ch3 assumed)";

            String info = ""
                    + "Container/MIME : " + safe(r.mime) + "\n"
                    + "Channels       : " + chText + "\n"
                    + "Haptic Tag     : " + (r.hasHapticTag ? "ANDROID_HAPTIC 발견" : "미확인") + "\n"
                    + "Haptic 추정    : " + (r.hasHaptic() ? "있음(추정)" : "없음(추정)") + "\n"
                    + "Sample Rate    : " + (r.sampleRate > 0 ? r.sampleRate + " Hz" : "Unknown") + "\n"
                    + "Audio Duration : " + formatMs10(audioDurationMs) + "\n"
                    + "Haptic Duration: " + (hapticInfo != null ? formatMs10(hapticInfo.totalDurationMs) : "Unknown") + "\n";

            tvInfo.setText(info);
            updateTimesAndSeek();

        } catch (Exception e) {
            tvInfo.setText("메타/파싱 실패: " + e.getMessage());
            Log.e(TAG, "showMetadata error", e);
        }
    }

    // ---------- 오디오 ----------
    private void startAudio() {
        if (selectedUri == null) {
            Toast.makeText(this, "먼저 OGG 파일을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        stopAudio();
        try {
            mediaPlayer = new MediaPlayer();

            AudioAttributes.Builder ab = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);

            // 시스템 ACH는 음소거 → 오디오는 소리만 재생(햅틱은 우리 엔진이 별도로)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ab.setHapticChannelsMuted(true);
            } else {
                try {
                    Method m = AudioAttributes.Builder.class
                            .getMethod("setHapticChannelsMuted", boolean.class);
                    m.invoke(ab, true);
                } catch (Throwable ignore) {}
            }

            mediaPlayer.setAudioAttributes(ab.build());
            mediaPlayer.setDataSource(this, selectedUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                audioDurationMs = mp.getDuration();
                mp.start();
                Toast.makeText(this, "오디오 재생 시작(시스템 ACH 끔)", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                Toast.makeText(this, "오디오 재생 완료", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "오디오 오류: " + what + "/" + extra, Toast.LENGTH_LONG).show();
                return true;
            });
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Toast.makeText(this, "오디오 재생 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "startAudio error", e);
        }
    }

    private void pauseAudio() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Toast.makeText(this, "오디오 일시정지", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "pauseAudio error", e);
        }
    }

    private void stopAudio() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                Toast.makeText(this, "오디오 정지", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopAudio error", e);
        }
    }

    // ---------- 공통 UI ----------
    private void updateTimesAndSeek() {
        // 오디오
        int aPos = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        int aDur = (mediaPlayer != null && mediaPlayer.getDuration() > 0)
                ? mediaPlayer.getDuration() : (int) audioDurationMs;

        tvAudioTime.setText(getString(R.string.label_audio_time)
                + "  " + formatClock10(aPos) + getString(R.string.time_sep) + formatClock10(aDur));

        if (aDur > 0) {
            int progress = Math.round((aPos / (float) aDur) * seekAudio.getMax());
            seekAudio.setProgress(progress);
        } else {
            seekAudio.setProgress(0);
        }

        // 햅틱
        long hPos = (hapticEngine != null) ? hapticEngine.getCurrentPositionMs() : 0L;
        long hDur = (hapticInfo != null) ? hapticInfo.totalDurationMs : 0L;

        tvHapticTime.setText(getString(R.string.label_haptic_time)
                + "  " + formatClock10((int) hPos) + getString(R.string.time_sep) + formatClock10((int) hDur));

        if (hDur > 0) {
            int progressH = Math.round((hPos / (float) hDur) * seekHaptic.getMax());
            seekHaptic.setProgress(progressH);
        } else {
            seekHaptic.setProgress(0);
        }
    }

    private String formatClock10(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        int cs = (ms % 1000) / 10; // 10ms 단위
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d.%02d", h, m, s, cs);
        return String.format(Locale.US, "%02d:%02d.%02d", m, s, cs);
    }

    private String formatMs10(long ms) {
        if (ms <= 0) return "Unknown";
        int totalSec = (int)(ms / 1000);
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        int cs = (int)((ms % 1000) / 10);
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d.%02d (%d ms)", h, m, s, cs, ms);
        return String.format(Locale.US, "%02d:%02d.%02d (%d ms)", m, s, cs, ms);
    }

    private String safe(String s) { return s == null ? "Unknown" : s; }

    private void saveLastUri(Uri uri) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_URI, uri.toString()).apply();
    }

    private void restoreLastUriIfPossible() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String last = sp.getString(KEY_LAST_URI, null);
        if (last == null) return;

        try {
            Uri uri = Uri.parse(last);
            boolean stillHave = false;
            for (UriPermission p : getContentResolver().getPersistedUriPermissions()) {
                if (p.getUri().equals(uri) && p.isReadPermission()) { stillHave = true; break; }
            }
            if (stillHave) {
                selectedUri = uri;
                tvPath.setText(String.valueOf(uri));
                showMetadataAndPrepare(uri);
            }
        } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(uiTicker);
        stopAudio();
        if (hapticEngine != null) hapticEngine.stop();
    }

    // ---------- 독립 햅틱 재생 엔진 ----------
    private static class HapticPlaybackEngine {
        private final Vibrator vibrator;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private OggHapticInspector.HapticTrackInfo track;

        private boolean playing = false;
        private long baseTrackMs = 0;      // 트랙 기준 시작 오프셋
        private long baseRealtimeMs = 0;   // 실제 시작 시간 (elapsedRealtime)

        HapticPlaybackEngine(Vibrator vibrator) { this.vibrator = vibrator; }

        void setTrack(OggHapticInspector.HapticTrackInfo info) {
            stop();
            this.track = info;
            this.baseTrackMs = 0;
        }

        void clear() { stop(); this.track = null; this.baseTrackMs = 0; }

        boolean isPlaying() { return playing; }

        long getCurrentPositionMs() {
            if (!playing) return baseTrackMs;
            long now = SystemClock.elapsedRealtime();
            long delta = now - baseRealtimeMs;
            long pos = baseTrackMs + delta;
            if (track != null && pos > track.totalDurationMs) pos = track.totalDurationMs;
            return Math.max(0, pos);
        }

        void playFrom(long trackMs) {
            if (track == null) return;
            stopInternal();
            playing = true;
            baseTrackMs = clamp(trackMs, 0, track.totalDurationMs);
            baseRealtimeMs = SystemClock.elapsedRealtime();

            // 10ms bins → 연속 원샷으로 보내면 gaps 생길 수 있어, 40~60ms로 묶어서 예약
            final int BIN = 10; // ms
            long t = baseTrackMs;
            while (t < track.totalDurationMs) {
                int amp = track.getAmplitudeAt(t);
                if (amp <= 0) { t += BIN; continue; }
                // 같은 amp가 이어지는 구간을 하나로 묶기
                long segStart = t;
                long segEnd = Math.min(track.totalDurationMs, t + BIN);
                long cursor = segEnd;
                while (cursor < track.totalDurationMs && track.getAmplitudeAt(cursor) == amp) {
                    cursor += BIN;
                }
                long dur = cursor - segStart;

                long delay = (baseRealtimeMs + (segStart - baseTrackMs)) - SystemClock.elapsedRealtime();
                if (delay < 0) delay = 0;
                final int fAmp = amp;
                final long fDur = dur;

                handler.postDelayed(() -> {
                    if (!playing) return;
                    try {
                        if (Build.VERSION.SDK_INT >= 26) {
                            vibrator.vibrate(VibrationEffect.createOneShot(fDur, fAmp));
                        } else {
                            vibrator.vibrate(fDur);
                        }
                    } catch (Exception ignore) {}
                }, delay);

                t = cursor;
            }

            // 끝에서 자동 정지
            long tailDelay = (baseRealtimeMs + (track.totalDurationMs - baseTrackMs)) - SystemClock.elapsedRealtime();
            if (tailDelay < 0) tailDelay = 0;
            handler.postDelayed(() -> playing = false, tailDelay);
        }

        void pause() {
            if (!playing) return;
            long pos = getCurrentPositionMs();
            stopInternal();
            baseTrackMs = pos;
        }

        void stop() {
            stopInternal();
            baseTrackMs = 0;
        }

        void seekTo(long trackMs, boolean resumeIfPlaying) {
            boolean wasPlaying = playing;
            stopInternal();
            baseTrackMs = clamp(trackMs, 0, (track != null ? track.totalDurationMs : 0));
            if (track != null && (resumeIfPlaying || wasPlaying)) {
                playFrom(baseTrackMs);
            }
        }

        private void stopInternal() {
            handler.removeCallbacksAndMessages(null);
            try { vibrator.cancel(); } catch (Exception ignore) {}
            playing = false;
        }

        private static long clamp(long v, long lo, long hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}

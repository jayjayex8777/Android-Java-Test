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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * - 오디오와 햅틱을 완전히 분리 제어
 *   * 오디오: Play/Pause/Stop + 개별 SeekBar
 *   * 햅틱: Play/Pause/Stop + 개별 SeekBar + 의도적 디싱크(+/-) + 오디오로 재동기화
 * - 두 타임라인 모두 10ms 단위 표기(HH:MM:SS.cc)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_LAST_URI = "last_uri";
    private static final int UI_INTERVAL_MS = 20; // 10ms 단위 표기에 충분한 UI 갱신 간격

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
    private OggHapticInspector.HapticTrackInfo hapticInfo; // null일 수 있음(없으면 오디오 길이에 맞춰 0 표시)

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

                    // 파일 분석(오디오 메타 + 햅틱 트랙)
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
            // 현재 햅틱 위치에서 재생
            hapticEngine.playFrom(hapticEngine.getCurrentPositionMs());
        });
        btnHapticPause.setOnClickListener(v -> hapticEngine.pause());
        btnHapticStop.setOnClickListener(v -> hapticEngine.stop());
        btnHapticResync.setOnClickListener(v -> {
            // 오디오 현재 위치로 즉시 동기화
            long aPos = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0L;
            hapticEngine.seekTo(aPos, true);
            Toast.makeText(this, "햅틱 타임라인을 오디오에 재동기화", Toast.LENGTH_SHORT).show();
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

            // 햅틱 트랙 파싱(데모 세그먼트라도 생성)
            hapticInfo = OggHapticInspector.parseAchFromOgg(this, uri);
            if (hapticInfo != null) hapticEngine.setTrack(hapticInfo);
            else hapticEngine.clear();

            String chText;
            if (r.channelCount <= 0) chText = "Unknown";
            else if (r.channelCount == 1) chText = "Mono";
            else if (r.channelCount == 2) chText = "Stereo";
            else chText = r.channelCount + "ch (3ch 이상: Haptic 포함 가능)";

            String info = ""
                    + "Container/MIME : " + safe(r.mime) + "\n"
                    + "Channels       : " + chText + "\n"
                    + "Haptic Tag     : " + (r.hasHapticTag ? "존재 가능(ANDROID_HAPTIC 발견)" : "미확인") + "\n"
                    + "Haptic 추정    : " + (r.hasHaptic() ? "있음(추정)" : "없음(추정)") + "\n"
                    + "Sample Rate    : " + (r.sampleRate > 0 ? r.sampleRate + " Hz" : "Unknown") + "\n"
                    + "Audio Duration : " + formatMs10(audioDurationMs) + "\n"
                    + "Haptic Duration: " + (hapticInfo != null ? formatMs10(hapticInfo.totalDurationMs) : "Unknown") + "\n"
                    + (r.notes == null ? "" : ("Notes          : " + r.notes + "\n"));

            tvInfo.setText(info);
            updateTimesAndSeek();

        } catch (Exception e) {
            tvInfo.setText("메타데이터/파싱 실패: " + e.getMessage());
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

            // Haptic 채널 음소거 해제(Framework 쪽 ACH 사용 시 의미). 독립 엔진과 별개로 설정 유지.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ab.setHapticChannelsMuted(false);
            } else {
                try {
                    Method m = AudioAttributes.Builder.class
                            .getMethod("setHapticChannelsMuted", boolean.class);
                    m.invoke(ab, false);
                } catch (Throwable ignore) {}
            }

            mediaPlayer.setAudioAttributes(ab.build());
            mediaPlayer.setDataSource(this, selectedUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                audioDurationMs = mp.getDuration();
                mp.start();
                Toast.makeText(this, "오디오 재생 시작", Toast.LENGTH_SHORT).show();
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
        int cs = (ms % 1000) / 10; // 10ms 단위(0~99)
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
            // 여전히 권한이 있는지 확인
            boolean stillHave = false;
            List<UriPermission> perms = getContentResolver().getPersistedUriPermissions();
            for (UriPermission p : perms) {
                if (p.getUri().equals(uri) && p.isReadPermission()) {
                    stillHave = true; break;
                }
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
        private final List<Runnable> scheduled = new ArrayList<>();

        HapticPlaybackEngine(Vibrator vibrator) {
            this.vibrator = vibrator;
        }

        void setTrack(OggHapticInspector.HapticTrackInfo info) {
            stopInternal(true);
            this.track = info;
            this.baseTrackMs = 0;
        }

        void clear() {
            stopInternal(true);
            this.track = null;
            this.baseTrackMs = 0;
        }

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
            stopInternal(false);
            playing = true;
            baseTrackMs = clamp(trackMs, 0, track.totalDurationMs);
            baseRealtimeMs = SystemClock.elapsedRealtime();
            scheduleFrom(baseTrackMs);
        }

        void pause() {
            if (!playing) return;
            long pos = getCurrentPositionMs();
            stopInternal(true);
            baseTrackMs = pos;
        }

        void stop() {
            stopInternal(true);
            baseTrackMs = 0;
        }

        void seekTo(long trackMs, boolean resumeIfPlaying) {
            boolean wasPlaying = playing;
            stopInternal(false);
            baseTrackMs = clamp(trackMs, 0, (track != null ? track.totalDurationMs : 0));
            if (track != null && (resumeIfPlaying || wasPlaying)) {
                playing = true;
                baseRealtimeMs = SystemClock.elapsedRealtime();
                scheduleFrom(baseTrackMs);
            }
        }

        private void stopInternal(boolean stopPlaybackFlag) {
            for (Runnable r : scheduled) handler.removeCallbacks(r);
            scheduled.clear();
            try { vibrator.cancel(); } catch (Exception ignore) {}
            if (stopPlaybackFlag) playing = false;
        }

        private void scheduleFrom(long startTrackMs) {
            if (track == null) return;

            long start = startTrackMs;
            for (OggHapticInspector.HapticSegment seg : track.segments) {
                long segStart = seg.startMs;
                long segEnd = seg.startMs + seg.durationMs;
                if (segEnd <= start) continue;
                long effectiveStart = Math.max(segStart, start);

                long delayFromNow = (baseRealtimeMs + (effectiveStart - baseTrackMs)) - SystemClock.elapsedRealtime();
                if (delayFromNow < 0) delayFromNow = 0;

                long remaining = segEnd - effectiveStart;
                if (remaining <= 0) continue;

                final int amplitude = seg.amplitude;
                final long vibrateDuration = remaining;

                Runnable r = () -> {
                    if (!playing) return;
                    try {
                        if (Build.VERSION.SDK_INT >= 26) {
                            VibrationEffect effect = VibrationEffect.createOneShot(vibrateDuration, amplitude);
                            vibrator.vibrate(effect);
                        } else {
                            vibrator.vibrate(vibrateDuration);
                        }
                    } catch (Exception ignore) {}
                };
                scheduled.add(r);
                handler.postDelayed(r, delayFromNow);
            }

            long tailDelay = (baseRealtimeMs + (track.totalDurationMs - baseTrackMs)) - SystemClock.elapsedRealtime();
            if (tailDelay < 0) tailDelay = 0;
            Runnable tail = () -> {
                playing = false;
                try { vibrator.cancel(); } catch (Exception ignore) {}
            };
            scheduled.add(tail);
            handler.postDelayed(tail, tailDelay);
        }

        private static long clamp(long v, long lo, long hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}

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
 * A.ogg → 오디오 전용, B.ogg → 햅틱 전용 (파일을 각각 선택)
 * - mpAudio: 오디오만 재생(ACH 진동 mute)
 * - mpHaptic: 오디오는 0볼륨, ACH 진동만 출력
 * 기존 독립 제어(Play/Pause/Stop/Seek/디싱크/재동기화)와 10ms 단위 표시는 유지.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_AUDIO_URI = "audio_uri";
    private static final String KEY_HAPTIC_URI = "haptic_uri";
    private static final int UI_INTERVAL_MS = 20;

    // UI
    private Button btnPickAudio, btnPickHaptic;
    private TextView tvAudioPath, tvHapticPath, tvInfo;
    private Button btnPlay, btnPause, btnStop;
    private TextView tvAudioTime, tvHapticTime;
    private SeekBar seekAudio, seekHaptic;
    private Button btnHapticPlay, btnHapticPause, btnHapticStop, btnHapticResync, btnDesyncMinus, btnDesyncPlus;

    // 파일
    private Uri audioUri = null;
    private Uri hapticUri = null;

    // 플레이어
    private MediaPlayer mpAudio = null;   // 오디오 전용
    private MediaPlayer mpHaptic = null;  // 햅틱 전용(무음)

    private long audioDurationMs = 0L;
    private long hapticDurationMs = 0L;

    // UI 주기 업데이트용
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiTicker = new Runnable() {
        @Override public void run() {
            updateTimesAndSeek();
            uiHandler.postDelayed(this, UI_INTERVAL_MS);
        }
    };

    // 파일 선택 런처 (오디오/햅틱 분리)
    private final ActivityResultLauncher<String[]> pickAudioLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    grantPersist(uri);
                    audioUri = uri;
                    tvAudioPath.setText("오디오 파일: " + uri);
                    saveUris();
                    prepareAudio(); // 오디오만 준비
                }
            });

    private final ActivityResultLauncher<String[]> pickHapticLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    grantPersist(uri);
                    hapticUri = uri;
                    tvHapticPath.setText("햅틱 파일: " + uri);
                    saveUris();
                    prepareHaptic(); // 햅틱만 준비
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
        btnPickAudio = findViewById(R.id.btnPickAudio);
        btnPickHaptic = findViewById(R.id.btnPickHaptic);
        tvAudioPath = findViewById(R.id.tvAudioPath);
        tvHapticPath = findViewById(R.id.tvHapticPath);
        tvInfo = findViewById(R.id.tvInfo);

        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);

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

        // 리스너
        btnPickAudio.setOnClickListener(v -> pickAudioLauncher.launch(MIME()));
        btnPickHaptic.setOnClickListener(v -> pickHapticLauncher.launch(MIME()));

        btnPlay.setOnClickListener(v -> { if (ensureAudioReady()) try { mpAudio.start(); } catch (Exception ignore) {} });
        btnPause.setOnClickListener(v -> { if (mpAudio != null && mpAudio.isPlaying()) try { mpAudio.pause(); } catch (Exception ignore) {} });
        btnStop.setOnClickListener(v -> stopAudio());

        // 오디오 시킹
        seekAudio.setMax(1000);
        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser;
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { fromUser = u; }
            @Override public void onStartTrackingTouch(SeekBar sb) { fromUser = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (!fromUser || mpAudio == null || audioDurationMs <= 0) return;
                fromUser = false;
                int target = (int) (audioDurationMs * (sb.getProgress() / (float) sb.getMax()));
                try { mpAudio.seekTo(target); } catch (Exception ignore) {}
                updateTimesAndSeek();
            }
        });

        // 햅틱 제어
        btnHapticPlay.setOnClickListener(v -> {
            if (ensureHapticReady()) {
                try { mpHaptic.setVolume(0f, 0f); mpHaptic.start(); } catch (Exception ignore) {}
            }
        });
        btnHapticPause.setOnClickListener(v -> { if (mpHaptic != null && mpHaptic.isPlaying()) try { mpHaptic.pause(); } catch (Exception ignore) {} });
        btnHapticStop.setOnClickListener(v -> stopHaptic());

        // 오디오 시점으로 재동기화
        btnHapticResync.setOnClickListener(v -> {
            if (mpHaptic == null || mpAudio == null) return;
            int aPos = 0;
            try { aPos = mpAudio.getCurrentPosition(); } catch (Exception ignore) {}
            boolean resume = mpHaptic.isPlaying();
            try {
                int max = (int) (hapticDurationMs > 0 ? hapticDurationMs : mpHaptic.getDuration());
                if (aPos > max) aPos = max; // 범위 제한
                mpHaptic.seekTo(aPos);
                if (resume) mpHaptic.start();
            } catch (Exception ignore) {}
            Toast.makeText(this, "햅틱을 오디오 위치로 재동기화", Toast.LENGTH_SHORT).show();
        });

        // 의도적 디싱크(±100ms)
        btnDesyncMinus.setOnClickListener(v -> nudgeHaptic(-100));
        btnDesyncPlus.setOnClickListener(v -> nudgeHaptic(+100));

        // 햅틱 시킹
        seekHaptic.setMax(1000);
        seekHaptic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser;
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) { fromUser = u; }
            @Override public void onStartTrackingTouch(SeekBar sb) { fromUser = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (!fromUser || mpHaptic == null || hapticDurationMs <= 0) return;
                fromUser = false;
                int target = (int) (hapticDurationMs * (sb.getProgress() / (float) sb.getMax()));
                boolean resume = mpHaptic.isPlaying();
                try {
                    mpHaptic.seekTo(target);
                    if (resume) mpHaptic.start();
                } catch (Exception ignore) {}
                updateTimesAndSeek();
            }
        });

        // UI ticker
        uiHandler.post(uiTicker);

        // 이전 선택 복원
        restoreUris();
    }

    private String[] MIME() {
        return new String[]{"audio/ogg", "application/ogg", "audio/x-ogg"};
    }

    // ===== 오디오 준비 =====
    private void prepareAudio() {
        releaseAudio();
        if (audioUri == null) return;

        mpAudio = new MediaPlayer();
        mpAudio.setAudioAttributes(buildAudioAttrs(/*hapticMuted=*/true));
        try {
            mpAudio.setDataSource(this, audioUri);
        } catch (Exception e) {
            Toast.makeText(this, "오디오 dataSource 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "mpAudio.setDataSource", e);
            releaseAudio();
            return;
        }
        mpAudio.setOnPreparedListener(mp -> {
            audioDurationMs = mp.getDuration();
            updateInfo();
        });
        mpAudio.setOnCompletionListener(mp -> updateTimesAndSeek());
        mpAudio.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "오디오 오류: " + what + "/" + extra, Toast.LENGTH_LONG).show();
            return true;
        });
        mpAudio.prepareAsync();
    }

    // ===== 햅틱 준비 =====
    private void prepareHaptic() {
        releaseHaptic();
        if (hapticUri == null) return;

        mpHaptic = new MediaPlayer();
        mpHaptic.setAudioAttributes(buildAudioAttrs(/*hapticMuted=*/false));
        mpHaptic.setVolume(0f, 0f); // 소리는 항상 무음
        try {
            mpHaptic.setDataSource(this, hapticUri);
        } catch (Exception e) {
            Toast.makeText(this, "햅틱 dataSource 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "mpHaptic.setDataSource", e);
            releaseHaptic();
            return;
        }
        mpHaptic.setOnPreparedListener(mp -> {
            hapticDurationMs = mp.getDuration();
            updateInfo();
        });
        mpHaptic.setOnCompletionListener(mp -> updateTimesAndSeek());
        mpHaptic.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "햅틱 오류: " + what + "/" + extra, Toast.LENGTH_LONG).show();
            return true;
        });
        mpHaptic.prepareAsync();
    }

    // ===== 공통 =====
    private AudioAttributes buildAudioAttrs(boolean hapticMuted) {
        AudioAttributes.Builder ab = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ab.setHapticChannelsMuted(hapticMuted);
        } else {
            try {
                Method m = AudioAttributes.Builder.class
                        .getMethod("setHapticChannelsMuted", boolean.class);
                m.invoke(ab, hapticMuted);
            } catch (Throwable ignore) {}
        }
        return ab.build();
    }

    private boolean ensureAudioReady() {
        if (audioUri == null) {
            Toast.makeText(this, "오디오 파일을 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mpAudio == null) {
            prepareAudio();
            Toast.makeText(this, "오디오 준비 중...", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean ensureHapticReady() {
        if (hapticUri == null) {
            Toast.makeText(this, "햅틱 파일을 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mpHaptic == null) {
            prepareHaptic();
            Toast.makeText(this, "햅틱 준비 중...", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void stopAudio() {
        try {
            if (mpAudio != null) {
                if (mpAudio.isPlaying()) mpAudio.stop();
                mpAudio.seekTo(0);
                updateTimesAndSeek();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopAudio", e);
        }
    }

    private void stopHaptic() {
        try {
            if (mpHaptic != null) {
                if (mpHaptic.isPlaying()) mpHaptic.stop();
                mpHaptic.seekTo(0);
                updateTimesAndSeek();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopHaptic", e);
        }
    }

    private void nudgeHaptic(int deltaMs) {
        if (mpHaptic == null) return;
        try {
            int cur = mpHaptic.getCurrentPosition();
            int dur = hapticDurationMs > 0 ? (int) hapticDurationMs : mpHaptic.getDuration();
            int next = Math.max(0, Math.min(dur, cur + deltaMs));
            boolean resume = mpHaptic.isPlaying();
            mpHaptic.seekTo(next);
            if (resume) mpHaptic.start();
        } catch (Exception e) {
            Log.e(TAG, "nudgeHaptic", e);
        }
    }

    private void updateTimesAndSeek() {
        // 오디오
        int aPos = 0, aDur = (int) audioDurationMs;
        try {
            if (mpAudio != null) {
                aPos = mpAudio.getCurrentPosition();
                aDur = mpAudio.getDuration() > 0 ? mpAudio.getDuration() : (int) audioDurationMs;
            }
        } catch (Exception ignore) {}

        tvAudioTime.setText(getString(R.string.label_audio_time)
                + "  " + formatClock10(aPos) + getString(R.string.time_sep) + formatClock10(aDur));
        seekAudio.setProgress(aDur > 0 ? Math.round((aPos / (float) aDur) * seekAudio.getMax()) : 0);

        // 햅틱
        int hPos = 0, hDur = (int) hapticDurationMs;
        try {
            if (mpHaptic != null) {
                hPos = mpHaptic.getCurrentPosition();
                hDur = mpHaptic.getDuration() > 0 ? mpHaptic.getDuration() : (int) hapticDurationMs;
            }
        } catch (Exception ignore) {}

        tvHapticTime.setText(getString(R.string.label_haptic_time)
                + "  " + formatClock10(hPos) + getString(R.string.time_sep) + formatClock10(hDur));
        seekHaptic.setProgress(hDur > 0 ? Math.round((hPos / (float) hDur) * seekHaptic.getMax()) : 0);
    }

    private void updateInfo() {
        String info = "Audio Duration : " + formatMs10(audioDurationMs) + "\n"
                    + "Haptic Duration: " + (hapticDurationMs > 0 ? formatMs10(hapticDurationMs) : "Unknown") + "\n";
        tvInfo.setText(info);
    }

    /** 10ms 단위(HH:MM:SS.cc) */
    private String formatClock10(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        int cs = (ms % 1000) / 10;
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

    // 저장/복원/권한
    private void saveUris() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
          .putString(KEY_AUDIO_URI, audioUri != null ? audioUri.toString() : null)
          .putString(KEY_HAPTIC_URI, hapticUri != null ? hapticUri.toString() : null)
          .apply();
    }

    private void restoreUris() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String a = sp.getString(KEY_AUDIO_URI, null);
        String h = sp.getString(KEY_HAPTIC_URI, null);

        if (a != null) {
            Uri uri = Uri.parse(a);
            if (hasPersist(uri)) {
                audioUri = uri;
                tvAudioPath.setText("오디오 파일: " + uri);
                prepareAudio();
            }
        }
        if (h != null) {
            Uri uri = Uri.parse(h);
            if (hasPersist(uri)) {
                hapticUri = uri;
                tvHapticPath.setText("햅틱 파일: " + uri);
                prepareHaptic();
            }
        }
    }

    private void grantPersist(Uri uri) {
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignore) {}
    }

    private boolean hasPersist(Uri uri) {
        List<UriPermission> perms = getContentResolver().getPersistedUriPermissions();
        for (UriPermission p : perms) {
            if (p.getUri().equals(uri) && p.isReadPermission()) return true;
        }
        return false;
    }

    private void releaseAudio() {
        try { if (mpAudio != null) { mpAudio.release(); } } catch (Exception ignore) {}
        mpAudio = null; audioDurationMs = 0L;
    }

    private void releaseHaptic() {
        try { if (mpHaptic != null) { mpHaptic.release(); } } catch (Exception ignore) {}
        mpHaptic = null; hapticDurationMs = 0L;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(uiTicker);
        releaseAudio();
        releaseHaptic();
    }
}

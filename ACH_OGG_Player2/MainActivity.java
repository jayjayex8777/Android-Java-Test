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
 * 같은 OGG 파일에서
 *  - mpAudio: 오디오만 재생 (시스템 ACH 하프틱은 mute)
 *  - mpHaptic: 오디오는 볼륨 0, 햅틱 채널만 활성화 → 원본에 인코딩된 햅틱을 그대로 재생
 * 둘을 완전히 독립 제어(Play/Pause/Stop/Seek/디싱크/재동기화)합니다.
 * 시간 표시는 10ms 단위(hh:mm:ss.cc).
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_LAST_URI = "last_uri";
    private static final int UI_INTERVAL_MS = 20; // 10ms 표기에 충분한 주기

    // UI
    private Button btnPick, btnPlay, btnPause, btnStop;
    private TextView tvPath, tvInfo, tvAudioTime, tvHapticTime;
    private SeekBar seekAudio, seekHaptic;
    private Button btnHapticPlay, btnHapticPause, btnHapticStop, btnHapticResync, btnDesyncMinus, btnDesyncPlus;

    // 파일
    private Uri selectedUri = null;

    // 플레이어 2개
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

    private final ActivityResultLauncher<String[]> pickOggLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    selectedUri = uri;
                    tvPath.setText(String.valueOf(uri));
                    saveLastUri(uri);
                    preparePlayers(); // 선택 즉시 두 플레이어 준비
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

        btnHapticPlay  = findViewById(R.id.btnHapticPlay);
        btnHapticPause = findViewById(R.id.btnHapticPause);
        btnHapticStop  = findViewById(R.id.btnHapticStop);
        btnHapticResync= findViewById(R.id.btnHapticResync);
        btnDesyncMinus = findViewById(R.id.btnDesyncMinus);
        btnDesyncPlus  = findViewById(R.id.btnDesyncPlus);

        // 리스너
        btnPick.setOnClickListener(v -> openPicker());

        btnPlay.setOnClickListener(v -> {
            if (ensureAudioReady()) {
                try { mpAudio.start(); } catch (Exception ignore) {}
            }
        });
        btnPause.setOnClickListener(v -> {
            if (mpAudio != null && mpAudio.isPlaying()) {
                try { mpAudio.pause(); } catch (Exception ignore) {}
            }
        });
        btnStop.setOnClickListener(v -> stopAudio());

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

        // 햅틱 컨트롤(원본 그대로)
        btnHapticPlay.setOnClickListener(v -> {
            if (ensureHapticReady()) {
                try {
                    mpHaptic.setVolume(0f, 0f); // 항상 무음
                    mpHaptic.start();
                } catch (Exception ignore) {}
            }
        });
        btnHapticPause.setOnClickListener(v -> {
            if (mpHaptic != null && mpHaptic.isPlaying()) {
                try { mpHaptic.pause(); } catch (Exception ignore) {}
            }
        });
        btnHapticStop.setOnClickListener(v -> stopHaptic());

        btnHapticResync.setOnClickListener(v -> {
            if (mpHaptic == null || mpAudio == null) return;
            int aPos = 0;
            try { aPos = mpAudio.getCurrentPosition(); } catch (Exception ignore) {}
            boolean resume = mpHaptic.isPlaying();
            try {
                mpHaptic.seekTo(aPos);
                if (resume) mpHaptic.start();
            } catch (Exception ignore) {}
            Toast.makeText(this, "햅틱을 오디오 위치로 재동기화", Toast.LENGTH_SHORT).show();
        });

        // 의도적 디싱크 ±100ms
        btnDesyncMinus.setOnClickListener(v -> nudgeHaptic(-100));
        btnDesyncPlus.setOnClickListener(v -> nudgeHaptic(+100));

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

        // 이전 파일 복원
        restoreLastUriIfPossible();
    }

    // ===== 파일 선택 =====
    private void openPicker() {
        pickOggLauncher.launch(new String[]{"audio/ogg", "application/ogg", "audio/x-ogg"});
    }

    // ===== 플레이어 준비 =====
    private void preparePlayers() {
        releasePlayers();

        if (selectedUri == null) return;

        // 오디오 전용 플레이어 (햅틱 MUTE)
        mpAudio = new MediaPlayer();
        mpAudio.setAudioAttributes(buildAudioAttrs(/*hapticMuted=*/true));
        try {
            mpAudio.setDataSource(this, selectedUri);
        } catch (Exception e) {
            Toast.makeText(this, "오디오 dataSource 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "mpAudio.setDataSource", e);
            releasePlayers();
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

        // 햅틱 전용 플레이어 (오디오는 무음, 햅틱 UNMUTE)
        mpHaptic = new MediaPlayer();
        mpHaptic.setAudioAttributes(buildAudioAttrs(/*hapticMuted=*/false));
        mpHaptic.setVolume(0f, 0f); // 원본 햅틱만 나오게 무음
        try {
            mpHaptic.setDataSource(this, selectedUri);
        } catch (Exception e) {
            Toast.makeText(this, "햅틱 dataSource 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "mpHaptic.setDataSource", e);
            // 햅틱만 실패해도 오디오는 사용할 수 있게 유지
            try { mpAudio.stop(); mpAudio.release(); } catch (Exception ignore) {}
            mpAudio = null;
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

    private AudioAttributes buildAudioAttrs(boolean hapticMuted) {
        AudioAttributes.Builder ab = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
        // Android 12+ 정식, 그 이하는 리플렉션 시도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ab.setHapticChannelsMuted(hapticMuted);
        } else {
            try {
                Method m = AudioAttributes.Builder.class
                        .getMethod("setHapticChannelsMuted", boolean.class);
                m.invoke(ab, hapticMuted);
            } catch (Throwable ignore) {
                // 구버전은 시스템 ACH 미지원일 수 있음(햅틱 분리 불가). 이 경우 오디오만 정상 동작.
            }
        }
        return ab.build();
    }

    private boolean ensureAudioReady() {
        if (selectedUri == null) {
            Toast.makeText(this, "먼저 OGG 파일을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mpAudio == null) {
            preparePlayers();
            Toast.makeText(this, "플레이어 준비 중...", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean ensureHapticReady() {
        if (selectedUri == null) {
            Toast.makeText(this, "먼저 OGG 파일을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mpHaptic == null) {
            preparePlayers();
            Toast.makeText(this, "플레이어 준비 중...", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ===== 제어 =====
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

    // ===== UI =====
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

        if (aDur > 0) {
            int progress = Math.round((aPos / (float) aDur) * seekAudio.getMax());
            seekAudio.setProgress(progress);
        } else {
            seekAudio.setProgress(0);
        }

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

        if (hDur > 0) {
            int progressH = Math.round((hPos / (float) hDur) * seekHaptic.getMax());
            seekHaptic.setProgress(progressH);
        } else {
            seekHaptic.setProgress(0);
        }
    }

    private void updateInfo() {
        try {
            // 간단 메타: 채널/샘플레이트 등은 기존 OggHapticInspector.inspect()를 쓰셔도 됩니다.
            // 여기서는 길이만 표기.
            String info = "Audio Duration : " + formatMs10(audioDurationMs) + "\n"
                        + "Haptic Duration: " + (hapticDurationMs > 0 ? formatMs10(hapticDurationMs) : "Unknown") + "\n";
            tvInfo.setText(info);
        } catch (Exception ignore) {}
    }

    /** 10ms 단위(HH:MM:SS.cc) */
    private String formatClock10(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        int cs = (ms % 1000) / 10; // 0~99
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

    // ===== 저장/복원/해제 =====
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
            List<UriPermission> perms = getContentResolver().getPersistedUriPermissions();
            for (UriPermission p : perms) {
                if (p.getUri().equals(uri) && p.isReadPermission()) { stillHave = true; break; }
            }
            if (stillHave) {
                selectedUri = uri;
                tvPath.setText(String.valueOf(uri));
                preparePlayers();
            }
        } catch (Exception ignore) {}
    }

    private void releasePlayers() {
        try { if (mpAudio != null) { mpAudio.release(); } } catch (Exception ignore) {}
        try { if (mpHaptic != null) { mpHaptic.release(); } } catch (Exception ignore) {}
        mpAudio = null;
        mpHaptic = null;
        audioDurationMs = 0L;
        hapticDurationMs = 0L;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(uiTicker);
        releasePlayers();
    }
}

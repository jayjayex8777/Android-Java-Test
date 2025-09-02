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
 * - OGG 파일 선택/재생(오디오)
 * - 시스템 ACH(오디오 결합 햅틱) 동작 시 오디오 타임라인과 동기 재생
 * - 오디오/햅틱 경과·총시간 실시간 표시
 * - SeekBar 시킹(오디오/햅틱 동기 이동), 일시정지/정지
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_LAST_URI = "last_uri";

    private Button btnPick, btnPlay, btnPause, btnStop;
    private TextView tvPath, tvInfo, tvAudioTime, tvHapticTime;
    private SeekBar seekBar;

    private Uri selectedUri = null;
    private MediaPlayer mediaPlayer = null;

    // 메타 데이터(오디오 기준)
    private long audioDurationMs = 0L;

    // UI 주기 업데이트용
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable uiTicker = new Runnable() {
        @Override public void run() {
            updateTimesAndSeek();
            uiHandler.postDelayed(this, 100);
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
                    showMetadataAndPrepareDuration(uri);
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

        btnPick = findViewById(R.id.btnPick);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        tvPath = findViewById(R.id.tvPath);
        tvInfo = findViewById(R.id.tvInfo);
        tvAudioTime = findViewById(R.id.tvAudioTime);
        tvHapticTime = findViewById(R.id.tvHapticTime);
        seekBar = findViewById(R.id.seekBar);

        btnPick.setOnClickListener(v -> openPicker());
        btnPlay.setOnClickListener(v -> startPlayback());
        btnPause.setOnClickListener(v -> pausePlayback());
        btnStop.setOnClickListener(v -> stopPlayback());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean user) { fromUser = user; }
            @Override public void onStartTrackingTouch(SeekBar sb) { fromUser = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (!fromUser) return;
                fromUser = false;
                if (mediaPlayer == null || audioDurationMs <= 0) return;
                float ratio = sb.getProgress() / (float) sb.getMax();
                int target = (int) (audioDurationMs * ratio);
                try {
                    mediaPlayer.seekTo(target);
                    updateTimesAndSeek();
                } catch (Exception ignore) {}
            }
        });

        // UI ticker 시작
        uiHandler.post(uiTicker);

        restoreLastUriIfPossible();
    }

    private void openPicker() {
        // audio/ogg 가 주 MIME 이지만, 일부 기기/앱은 application/ogg, audio/x-ogg 로 노출되므로 모두 허용
        pickOggLauncher.launch(new String[]{"audio/ogg", "application/ogg", "audio/x-ogg"});
    }

    private void startPlayback() {
        if (selectedUri == null) {
            Toast.makeText(this, "먼저 OGG 파일을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        stopPlayback(); // 기존 인스턴스 정리
        try {
            mediaPlayer = new MediaPlayer();

            AudioAttributes.Builder ab = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);

            // ACH: Haptic 채널 음소거 해제 (API 31+). 하위 버전은 리플렉션으로 시도.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ab.setHapticChannelsMuted(false);
            } else {
                try {
                    Method m = AudioAttributes.Builder.class
                            .getMethod("setHapticChannelsMuted", boolean.class);
                    m.invoke(ab, false);
                } catch (Throwable ignore) {
                    Log.i(TAG, "setHapticChannelsMuted not available on this API level.");
                }
            }

            mediaPlayer.setAudioAttributes(ab.build());
            mediaPlayer.setDataSource(this, selectedUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                // duration 확보
                audioDurationMs = mp.getDuration();
                mp.start();
                Toast.makeText(this, "재생 시작", Toast.LENGTH_SHORT).show();
                updateTimesAndSeek();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                Toast.makeText(this, "재생 완료", Toast.LENGTH_SHORT).show();
                updateTimesAndSeek();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "재생 오류: " + what + " / " + extra, Toast.LENGTH_LONG).show();
                return true;
            });
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Toast.makeText(this, "재생 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "startPlayback error", e);
        }
    }

    private void pausePlayback() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Toast.makeText(this, "일시정지", Toast.LENGTH_SHORT).show();
                updateTimesAndSeek();
            }
        } catch (Exception e) {
            Log.e(TAG, "pausePlayback error", e);
        }
    }

    private void stopPlayback() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                Toast.makeText(this, "정지", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopPlayback error", e);
        }
        updateTimesAndSeek();
    }

    private void showMetadataAndPrepareDuration(Uri uri) {
        try {
            OggHapticInspector.Result r = OggHapticInspector.inspect(this, uri);

            // 오디오 총 길이(없으면 -1) → UI에 표시용으로 활용
            audioDurationMs = (r.durationMs > 0) ? r.durationMs : 0L;

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
                    + "Duration       : " + formatMs(r.durationMs) + "\n"
                    + (r.notes == null ? "" : ("Notes          : " + r.notes + "\n"));

            tvInfo.setText(info);
            updateTimesAndSeek();

        } catch (Exception e) {
            tvInfo.setText("메타데이터 읽기 실패: " + e.getMessage());
            Log.e(TAG, "showMetadata error", e);
        }
    }

    private void updateTimesAndSeek() {
        int aPos = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        int aDur = (mediaPlayer != null && mediaPlayer.getDuration() > 0)
                ? mediaPlayer.getDuration() : (int) audioDurationMs;

        // 시스템 ACH는 오디오 타임라인과 동기 → 현 단계에서는 동일 값으로 표기
        long hPos = aPos;
        long hDur = aDur;

        tvAudioTime.setText(getString(R.string.label_audio_time)
                + "  " + formatClock(aPos) + getString(R.string.time_sep) + formatClock(aDur));
        tvHapticTime.setText(getString(R.string.label_haptic_time)
                + "  " + formatClock((int) hPos) + getString(R.string.time_sep) + formatClock((int) hDur));

        if (aDur > 0) {
            int progress = Math.round((aPos / (float) aDur) * seekBar.getMax());
            seekBar.setProgress(progress);
        } else {
            seekBar.setProgress(0);
        }
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "Unknown";
        long totalSec = ms / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.US, "%d:%02d (%d ms)", m, s, ms);
    }

    private String formatClock(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%02d:%02d", m, s);
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
                showMetadataAndPrepareDuration(uri);
            }
        } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(uiTicker);
        stopPlayback();
    }
}

package com.example.achoggmusicplayer;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ACHPlayer";
    private static final String PREFS = "ach_prefs";
    private static final String KEY_LAST_URI = "last_uri";

    private Button btnPick, btnPlay, btnStop;
    private TextView tvPath, tvInfo;

    private Uri selectedUri = null;
    private MediaPlayer mediaPlayer = null;

    private final ActivityResultLauncher<String[]> pickOggLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    selectedUri = uri;
                    tvPath.setText(String.valueOf(uri));
                    saveLastUri(uri);
                    showMetadata(uri);
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
        btnStop = findViewById(R.id.btnStop);
        tvPath = findViewById(R.id.tvPath);
        tvInfo = findViewById(R.id.tvInfo);

        btnPick.setOnClickListener(v -> openPicker());
        btnPlay.setOnClickListener(v -> startPlayback());
        btnStop.setOnClickListener(v -> stopPlayback());

        restoreLastUriIfPossible();
    }

    private void openPicker() {
        // audio/ogg 가 주 MIME 이지만, 일부 기기/앱은 application/ogg 로 노출되므로 둘 다 허용
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

            // ACH: Haptic 채널 음소거 해제 (API 31+). 하위 버전은 리플렉션 시도.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ab.setHapticChannelsMuted(false);
            } else {
                try {
                    Method m = AudioAttributes.Builder.class
                            .getMethod("setHapticChannelsMuted", boolean.class);
                    m.invoke(ab, false); // 있으면 사용
                } catch (Throwable ignore) {
                    Log.i(TAG, "setHapticChannelsMuted not available on this API level.");
                }
            }

            mediaPlayer.setAudioAttributes(ab.build());
            mediaPlayer.setDataSource(this, selectedUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(this, "재생 시작", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.setOnCompletionListener(mp ->
                    Toast.makeText(this, "재생 완료", Toast.LENGTH_SHORT).show()
            );
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
    }

    private void showMetadata(Uri uri) {
        try {
            OggHapticInspector.Result r = OggHapticInspector.inspect(this, uri);

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

        } catch (Exception e) {
            tvInfo.setText("메타데이터 읽기 실패: " + e.getMessage());
            Log.e(TAG, "showMetadata error", e);
        }
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "Unknown";
        long totalSec = ms / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%d:%02d (%d ms)", m, s, ms);
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
                showMetadata(uri);
            }
        } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }
}

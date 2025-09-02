package com.example.achoggmusicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private Uri lastPickedUri;

    private TextView tvTime;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long UI_UPDATE_INTERVAL_MS = 10L; // 10ms 단위 업데이트

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                long pos = Math.max(0, player.getCurrentPosition());
                long dur = player.getDuration();
                String left = formatTime(pos);
                String right = (dur != C.TIME_UNSET && dur > 0) ? formatTime(dur) : "--:--.--";
                tvTime.setText(left + " / " + right);

                uiHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
            }
        }
    };

    private final ActivityResultLauncher<Intent> openDoc =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                (ActivityResult result) -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            final int flags = (result.getData().getFlags()
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                            try {
                                getContentResolver().takePersistableUriPermission(uri, flags);
                            } catch (SecurityException ignored) {
                                // 일부 기기에서 WRITE 권한은 거부될 수 있음. READ만으로도 재생 가능.
                            }
                            lastPickedUri = uri;
                            playUri(uri);
                        }
                    }
                });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        tvTime = findViewById(R.id.tvTime);

        // ExoPlayer 생성 + 오디오 속성
        player = new ExoPlayer.Builder(this).build();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();
        player.setAudioAttributes(attrs, true);
        playerView.setPlayer(player);

        // XML 커스텀 속성 대신 코드로 컨트롤러 설정 (빌드 에러 회피)
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerShowTimeoutMs(3000);

        // 플레이어 상태 리스너: READY 시 총 길이 표시 초기화
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    String right = (dur != C.TIME_UNSET && dur > 0) ? formatTime(dur) : "--:--.--";
                    tvTime.setText("00:00.00 / " + right);
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                tvTime.setText("--:--.-- / --:--.--");
            }
        });

        Button btnPick = findViewById(R.id.btnPick);
        btnPick.setOnClickListener(v -> pickOgg());

        Button btnHaptic = findViewById(R.id.btnHaptic);
        btnHaptic.setOnClickListener(v -> {
            if (lastPickedUri != null) {
                // 프로젝트에 이미 있는 HapticController 사용 (없다면 주석 처리 가능)
                HapticController.playFromMetadata(this, lastPickedUri);
            }
        });
    }

    private void pickOgg() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/ogg");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        openDoc.launch(i);
    }

    private void playUri(Uri uri) {
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        uiHandler.removeCallbacks(timeUpdater);
        uiHandler.postDelayed(timeUpdater, UI_UPDATE_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHandler.removeCallbacks(timeUpdater);
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    /** mm:ss.SS (10ms 단위) 포맷 */
    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long totalSeconds = millis / 1000;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long centiseconds = (millis % 1000) / 10; // 10ms 단위 → 00~99

        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds);
    }
}

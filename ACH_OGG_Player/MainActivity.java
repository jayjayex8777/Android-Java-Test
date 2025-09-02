package com.example.achoggmusicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private Uri lastPickedUri;

    private TextView tvTime;
    private SeekBar seekBar;
    private Button btnPick, btnPlayPause;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long UI_UPDATE_INTERVAL_MS = 10L; // 10ms 단위 업데이트
    private boolean userSeeking = false;

    private long[] currentHapticPattern = null; // OGG 메타에서 추출(없으면 폴백)
    private boolean hapticEnabled = true;       // 기본 ON

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                long pos = Math.max(0, player.getCurrentPosition());
                long dur = player.getDuration();

                String left = formatTime(pos);
                String right = (dur != C.TIME_UNSET && dur > 0) ? formatTime(dur) : "--:--.--";
                tvTime.setText(left + " / " + right);

                if (!userSeeking && dur > 0 && dur != C.TIME_UNSET) {
                    // SeekBar는 ms 단위로 동기화
                    seekBar.setProgress((int) Math.min(Integer.MAX_VALUE, pos));
                }

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
                            } catch (SecurityException ignored) { }
                            lastPickedUri = uri;
                            prepareAndMaybePlay(uri, /*autoplay=*/true);
                        }
                    }
                });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView   = findViewById(R.id.playerView);
        tvTime       = findViewById(R.id.tvTime);
        seekBar      = findViewById(R.id.seekBar);
        btnPick      = findViewById(R.id.btnPick);
        btnPlayPause = findViewById(R.id.btnPlayPause);

        // ExoPlayer 2.x 생성 + 오디오 속성
        player = new ExoPlayer.Builder(this).build();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        player.setAudioAttributes(attrs, /* handleAudioFocus= */ true);
        playerView.setPlayer(player);

        // 플레이어 상태 콜백
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    String right = (dur != C.TIME_UNSET && dur > 0) ? formatTime(dur) : "--:--.--";
                    tvTime.setText("00:00.00 / " + right);

                    if (dur > 0 && dur != C.TIME_UNSET) {
                        int max = (int) Math.min(Integer.MAX_VALUE, dur);
                        seekBar.setMax(max);
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setText(isPlaying ? "Pause" : "Play");
                if (hapticEnabled && currentHapticPattern != null) {
                    if (isPlaying) {
                        HapticController.playAtOffset(MainActivity.this, currentHapticPattern, player.getCurrentPosition());
                    } else {
                        HapticController.pause(MainActivity.this);
                    }
                }
            }
        });

        // 파일 선택
        btnPick.setOnClickListener(v -> pickOgg());

        // 재생/일시정지
        btnPlayPause.setOnClickListener(v -> {
            if (player.getPlaybackState() == Player.STATE_IDLE) {
                if (lastPickedUri != null) {
                    prepareAndMaybePlay(lastPickedUri, true);
                }
                return;
            }
            boolean play = !player.isPlaying();
            player.setPlayWhenReady(play);
            if (play && hapticEnabled && currentHapticPattern != null) {
                HapticController.playAtOffset(this, currentHapticPattern, player.getCurrentPosition());
            } else {
                HapticController.pause(this);
            }
        });

        // SeekBar 사용자 시킹
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            long pendingSeek = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) pendingSeek = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                player.seekTo(pendingSeek);
                // Haptic도 동일 오프셋으로 재시작
                if (hapticEnabled && currentHapticPattern != null && player.isPlaying()) {
                    HapticController.playAtOffset(MainActivity.this, currentHapticPattern, player.getCurrentPosition());
                }
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

    private void prepareAndMaybePlay(Uri uri, boolean autoplay) {
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
        player.setPlayWhenReady(autoplay);

        // Haptic 패턴 추출(OGG Vorbis Comment에서, 없으면 폴백)
        currentHapticPattern = HapticController.extractPatternOrFallback(this, uri);
        if (hapticEnabled && currentHapticPattern != null && autoplay) {
            HapticController.playAtOffset(this, currentHapticPattern, /*offsetMs=*/0L);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        uiHandler.removeCallbacks(timeUpdater);
        uiHandler.postDelayed(timeUpdater, UI_UPDATE_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHandler.removeCallbacks(timeUpdater);
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        HapticController.pause(this);
    }

    @Override
    protected void onDestroy() {
        HapticController.stop(this);
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
        long centiseconds = (millis % 1000) / 10; // 10ms → 00~99
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds);
    }
}

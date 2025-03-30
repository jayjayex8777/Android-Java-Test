package com.example.videoplayertest1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private boolean isFullScreen = false;
    private Button fullScreenButton;

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Log.d("VideoDebug", "Callback triggered");

                if (result.getResultCode() == RESULT_OK) {
                    Log.d("VideoDebug", "Result OK");

                    Intent data = result.getData();
                    if (data != null) {
                        Uri videoUri = data.getData();
                        Log.d("VideoDebug", "Selected URI: " + videoUri);

                        if (videoUri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Log.d("VideoDebug", "Permission granted");

                                playVideo(videoUri);
                            } catch (SecurityException e) {
                                Log.e("VideoDebug", "Permission error", e);
                            }
                        } else {
                            Log.e("VideoDebug", "videoUri is null");
                        }
                    } else {
                        Log.e("VideoDebug", "getData() is null");
                    }
                } else {
                    Log.e("VideoDebug", "Result NOT OK. Code: " + result.getResultCode());
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.playerView);
        fullScreenButton = findViewById(R.id.fullScreenButton);

        // 더블탭 제스처
        GestureDetector gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        float x = e.getX();
                        int width = playerView.getWidth();
                        long pos = player.getCurrentPosition();

                        if (x < width / 2) {
                            player.seekTo(Math.max(pos - 5000, 0));
                        } else {
                            player.seekTo(Math.min(pos + 5000, player.getDuration()));
                        }
                        return true;
                    }
                });

        playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        fullScreenButton.setOnClickListener(v -> toggleFullScreen());

        // 파일 선택 시작
        pickVideoFile();
    }

    private void pickVideoFile() {
        Log.d("VideoDebug", "Launching file picker...");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        videoPickerLauncher.launch(intent);
    }

    private void playVideo(Uri uri) {
        Log.d("VideoDebug", "playVideo() called with URI: " + uri);

        try {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Log.e("VideoDebug", "Error playing video", e);
        }
    }

    private void toggleFullScreen() {
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) findViewById(R.id.videoContainer).getLayoutParams();

        if (isFullScreen) {
            params.weight = 1;
            fullScreenButton.setText("전체 보기");
        } else {
            params.weight = 3;
            fullScreenButton.setText("축소");
        }

        findViewById(R.id.videoContainer).setLayoutParams(params);
        isFullScreen = !isFullScreen;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}

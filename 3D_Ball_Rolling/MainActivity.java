package com.example.apptest2;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private GestureDetector gestureDetector;
    private BallRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // OpenGL ES View
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0 사용
        renderer = new BallRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(glSurfaceView);

        // Gesture Detector
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 터치 이벤트 리스너 설정
        glSurfaceView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 사용자의 손가락 이동 방향에 따라 공을 굴린다.
            renderer.updateBallMovement(-distanceX / 100, distanceY / 100);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // 빠른 스와이프로 플링(Fling) 효과 적용
            renderer.applyFling(velocityX / 5000, -velocityY / 5000);
            return true;
        }
    }
}

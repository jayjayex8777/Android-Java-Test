package com.example.apptest2;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private GestureDetector gestureDetector;
    private BallRenderer renderer;
    private float screenWidth, screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new BallRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(glSurfaceView);

        gestureDetector = new GestureDetector(this, new GestureListener());

        glSurfaceView.setOnTouchListener((v, event) -> {
            screenWidth = v.getWidth();
            screenHeight = v.getHeight();
            return gestureDetector.onTouchEvent(event);
        });
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float glX = (e2.getX() / screenWidth) * 2 - 1;  // 🔥 OpenGL X 좌표 변환
            float glY = 1 - (e2.getY() / screenHeight) * 2;  // 🔥 OpenGL Y 좌표 변환
            renderer.updateBallPosition(glX, glY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            renderer.applyFling(velocityX / 5000, -velocityY / 5000);
            return true;
        }
    }
}

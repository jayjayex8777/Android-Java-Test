package com.example.objectposturetest1;

import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private PrismRenderer renderer;
    private float previousX;
    private float previousY;
    private static final float TOUCH_SCALE_FACTOR = 180.0f / 320; // 회전 민감도

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // GLSurfaceView 초기화
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0 사용
        renderer = new PrismRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // 터치 시에만 렌더링

        // 터치 이벤트 처리
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - previousX;
                        float dy = y - previousY;

                        // 터치 이동에 따라 회전
                        renderer.setRotationY(renderer.getRotationY() + (dx * TOUCH_SCALE_FACTOR));
                        renderer.setRotationX(renderer.getRotationX() + (dy * TOUCH_SCALE_FACTOR));
                        glSurfaceView.requestRender();
                        break;
                }

                previousX = x;
                previousY = y;
                return true;
            }
        });

        setContentView(glSurfaceView);

        // Edge-to-edge 처리를 위한 인셋 설정
        ViewCompat.setOnApplyWindowInsetsListener(glSurfaceView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }
}

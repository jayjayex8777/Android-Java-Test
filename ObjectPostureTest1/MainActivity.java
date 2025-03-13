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
    private static final float TOUCH_SCALE_FACTOR = 180.0f / 320; // Sensitivity for rotation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // Use OpenGL ES 2.0
        renderer = new PrismRenderer();
        glSurfaceView.setRenderer(renderer);

        // Handle touch events
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - previousX;
                        float dy = y - previousY;

                        // Rotate based on touch movement
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

        // Handle window insets for edge-to-edge
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

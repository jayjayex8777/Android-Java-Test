package com.example.objectposturetest1;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private CubeRenderer renderer;
    private float previousX, previousY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new CubeRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        setContentView(glSurfaceView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;

                if (renderer != null) {
                    renderer.setRotation(dx, dy);
                    glSurfaceView.requestRender();
                }
                break;
        }

        previousX = x;
        previousY = y;
        return true;
    }
}

package com.example.objectposturetest1;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private CubeRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate: GLSurfaceView initializing");

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // 🚀 OpenGL ES 2.0 사용
        renderer = new CubeRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); // 🚀 지속적으로 렌더링

        setContentView(glSurfaceView); // 🚨 GLSurfaceView를 화면에 추가

        Log.d("MainActivity", "onCreate: GLSurfaceView set and running");
    }
}

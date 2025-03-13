package com.example.objectposturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class PrismRenderer implements GLSurfaceView.Renderer {

    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int mProgram;
    private float rotationX = 0f;
    private float rotationY = 0f;

    private final float[] vertices = {
        -0.5f, -1.5f,  0.5f,  0.5f, -1.5f,  0.5f,
         0.5f,  1.5f,  0.5f, -0.5f,  1.5f,  0.5f,
        -0.5f, -1.5f, -0.5f,  0.5f, -1.5f, -0.5f,
         0.5f,  1.5f, -0.5f, -0.5f,  1.5f, -0.5f
    };

    private final short[] indices = {
        0, 1, 2, 2, 3, 0, 1, 5, 6, 6, 2, 1, 5, 4, 7, 7, 6, 5,
        4, 0, 3, 3, 7, 4, 3, 2, 6, 6, 7, 3, 4, 5, 1, 1, 0, 4
    };

    private final float[] prismColor = {0.0f, 0.5f, 1.0f, 1.0f};

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mProgram == -1) return;
        
        // 카메라 및 변환 적용
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 1.0f, 0.0f);
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);

        float[] tempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }
}

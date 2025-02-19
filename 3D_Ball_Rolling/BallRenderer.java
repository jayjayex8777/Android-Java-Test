package com.example.apptest2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BallRenderer implements GLSurfaceView.Renderer {

    private Ball ball;
    private float ballX = 0f, ballY = 0f;
    private float velocityX = 0f, velocityY = 0f;
    private static final float FRICTION = 0.95f; // 마찰 계수
    private float[] projectionMatrix = new float[16];
    private float rotationAngle = 0f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        ball = new Ball();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        ballX += velocityX;
        ballY += velocityY;
        velocityX *= FRICTION;
        velocityY *= FRICTION;

        rotationAngle += (Math.abs(velocityX) + Math.abs(velocityY)) * 20;

        ball.draw(ballX, ballY, rotationAngle, projectionMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
    }

    public void updateBallMovement(float dx, float dy) {
        velocityX = dx * 2;
        velocityY = dy * 2;
    }

    public void applyFling(float vX, float vY) {
        velocityX = vX;
        velocityY = vY;
    }
}

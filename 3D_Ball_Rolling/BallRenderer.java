package com.example.apptest2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BallRenderer implements GLSurfaceView.Renderer {

    private Ball ball;
    private float ballX = 0f, ballY = 0f, ballZ = -3f;
    private float velocityX = 0f, velocityY = 0f;
    private static final float FRICTION = 0.99f;  
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

        // 🔥 속도 적용 (자연스러운 이동)
        ballX += velocityX;
        ballY += velocityY;
        velocityX *= FRICTION;
        velocityY *= FRICTION;

        // 🔥 속도에 따라 회전 적용
        rotationAngle += (Math.abs(velocityX) + Math.abs(velocityY)) * 200;

        ball.draw(ballX, ballY, ballZ, rotationAngle, projectionMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 1, 10);  // 🔥 원근법 적용
    }

    public void updateBallMovement(float x, float y) {
        velocityX = (x - ballX) * 0.1f;  // 🔥 부드러운 이동 적용
        velocityY = (y - ballY) * 0.1f;
    }

    public void applyFling(float vX, float vY) {
        velocityX = vX;
        velocityY = vY;
    }
}

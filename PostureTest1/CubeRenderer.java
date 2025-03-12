package com.example.posturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {

    private Cube cube;
    private final float[] rotationMatrix = new float[16];
    private float yaw = 0, pitch = 0, roll = 0, posX = 0, posY = 0, posZ = 0;

    public CubeRenderer() {
        cube = new Cube();
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    public void setTransform(float yaw, float pitch, float roll, float x, float y, float z) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1);
        cube.init();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.translateM(rotationMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(rotationMatrix, 0, yaw, 0, 0, 1);
        Matrix.rotateM(rotationMatrix, 0, pitch, 1, 0, 0);
        Matrix.rotateM(rotationMatrix, 0, roll, 0, 1, 0);

        cube.draw(rotationMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }
}

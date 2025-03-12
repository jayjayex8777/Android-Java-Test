package com.example.posturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {

    private Cube cube;
    private final float[] rotationMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private float yaw = 0, pitch = 0, roll = 0;
    private float posX = 0, posY = 0, posZ = 0;

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
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // 🔹 깊이 테스트 활성화
        GLES20.glClearColor(0, 0, 0, 1); // 배경색 검정색
        cube.init();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT); // 🔹 깊이 버퍼 클리어

        // 🔹 카메라 위치 설정 (원근법 적용)
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 0f, 3f, // 카메라 위치 (z=3)
                0f, 0f, 0f, // 바라볼 위치 (원점)
                0f, 1f, 0f); // 위쪽 방향

        // 🔹 모델 변환 행렬 설정
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.translateM(rotationMatrix, 0, posX, posY, posZ); // 위치 이동
        Matrix.rotateM(rotationMatrix, 0, yaw, 0, 0, 1); // Z축 회전
        Matrix.rotateM(rotationMatrix, 0, pitch, 1, 0, 0); // X축 회전
        Matrix.rotateM(rotationMatrix, 0, roll, 0, 1, 0); // Y축 회전

        // 🔹 최종 MVP 행렬 계산
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 🔹 3D 큐브 그리기
        cube.draw(mvpMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height); // 🔹 OpenGL 뷰포트 설정
        float ratio = (float) width / height;
        
        // 🔹 원근 투영 행렬 설정 (3D 효과 적용)
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
    }
}

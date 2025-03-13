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

    // 직육면체 정점 (폭 1, 높이 3, 깊이 1)
    private final floatvertices = {
        // 앞면
        -0.5f, -1.5f,  0.5f,  // 0
         0.5f, -1.5f,  0.5f,  // 1
         0.5f,  1.5f,  0.5f,  // 2
        -0.5f,  1.5f,  0.5f,  // 3
        // 뒷면
        -0.5f, -1.5f, -0.5f,  // 4
         0.5f, -1.5f, -0.5f,  // 5
         0.5f,  1.5f,  0.5f,  // 6
        -0.5f,  1.5f, -0.5f   // 7
    };

    // 인덱스 (삼각형으로 면 정의)
    private final shortindices = {
        0, 1, 2, 2, 3, 0,  // 앞면
        1, 5, 6, 6, 2, 1,  // 오른쪽 면
        5, 4, 7, 7, 6, 5,  // 뒷면
        4, 0, 3, 3, 7, 4,  // 왼쪽 면
        3, 2, 6, 6, 7, 3,  // 윗면
        4, 5, 1, 1, 0, 4   // 아랫면
    };

    private final floatprismColor = {0.0f, 0.5f, 1.0f, 1.0f}; // 연한 파란색

    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    public PrismRenderer() {
        // 정점 버퍼
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // 인덱스 버퍼
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // 셰이더 컴파일 및 프로그램 생성
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == -1 || fragmentShader == -1) {
            // 셰이더 컴파일 실패 시 처리
            Log.e("PrismRenderer", "Shader compilation failed. Program will not be created.");
            mProgram = -1; // 프로그램 생성을 막기 위해 -1로 설정
            return;
        }

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        // 셰이더 링크 오류 확인
        intlinkStatus = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("PrismRenderer", "Program link error: " + GLES20.glGetProgramInfoLog(mProgram));
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1; // 프로그램 생성을 막기 위해 -1로 설정
        }
    }

    private final floatprojectionMatrix = new float[16];
    private final floatviewMatrix = new float[16];
    private final floatmvpMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f); // 연한 회색 배경
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // 깊이 테스트 활성화
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7); // 근거리 3, 원거리 7
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 프로그램 생성 실패 시 렌더링하지 않음
        if (mProgram == -1) {
            return;
        }

        // 카메라 위치 (객체가 보이도록 충분히 뒤로)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 1.0f, 0.0f);

        // 모델 행렬 (회전 적용)
        floatmodelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f); // X축 회전
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f); // Y축 회전

        // MVP 행렬 계산
        floattempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        // 셰이더 프로그램 사용
        GLES20.glUseProgram(mProgram);

        // 정점 데이터 전달
        int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        // 색상 전달
        int colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, prismColor, 0);

        // MVP 행렬 전달
        int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // 프리즘 그리기
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);

        // OpenGL 오류 확인
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("PrismRenderer", "OpenGL Error: " + error);
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // 컴파일 오류 확인
        intcompileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e("PrismRenderer", "Shader compile error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return -1;
        }
        return shader;
    }

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
    }
}

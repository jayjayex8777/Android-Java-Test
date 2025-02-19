package com.example.apptest2;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Ball {

    private FloatBuffer vertexBuffer;
    private int shaderProgram;
    private float[] modelMatrix = new float[16];
    private int positionHandle, colorHandle, matrixHandle;

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

    private float[] color = {1.0f, 0.0f, 0.0f, 1.0f}; // 빨간색 공

    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_COUNT = 36; // 세밀한 원
    private float[] ballCoords = new float[VERTEX_COUNT * 3];

    public Ball() {
        generateBallVertices();

        // 버텍스 데이터를 저장할 버퍼 생성
        ByteBuffer bb = ByteBuffer.allocateDirect(ballCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(ballCoords);
        vertexBuffer.position(0);

        // 셰이더 컴파일 및 프로그램 생성
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);
    }

    /**
     * 공을 원 형태로 생성하는 메서드
     */
    private void generateBallVertices() {
        for (int i = 0; i < VERTEX_COUNT; i++) {
            double angle = (i * 2 * Math.PI) / VERTEX_COUNT;
            ballCoords[i * 3] = (float) Math.cos(angle) * 0.2f;
            ballCoords[i * 3 + 1] = (float) Math.sin(angle) * 0.2f;
            ballCoords[i * 3 + 2] = 0f; // 2D 평면 상의 공
        }
    }

    /**
     * 공을 화면에 그리는 메서드
     * @param x 공의 x 위치
     * @param y 공의 y 위치
     * @param projectionMatrix 프로젝션 행렬
     */
    public void draw(float x, float y, float[] projectionMatrix) {
        GLES20.glUseProgram(shaderProgram);

        // 변환 행렬 설정 (공의 위치 적용)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, -2f);
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        // MVP 행렬을 셰이더에 전달
        matrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0);

        // 버텍스 속성 설정
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // 색상 설정
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        // 공을 삼각형 팬으로 그림
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_COUNT);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * 셰이더를 로드하고 컴파일하는 메서드
     * @param type 셰이더 타입 (GLES20.GL_VERTEX_SHADER 또는 GLES20.GL_FRAGMENT_SHADER)
     * @param shaderCode 셰이더 코드
     * @return 컴파일된 셰이더 ID
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // 컴파일 상태 확인
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + error);
        }

        return shader;
    }
}

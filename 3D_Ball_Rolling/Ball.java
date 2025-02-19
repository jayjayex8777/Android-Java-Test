package com.example.apptest2;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Ball {

    private FloatBuffer vertexBuffer;
    private final int COORDS_PER_VERTEX = 3;
    private float[] ballCoords;
    private int vertexCount;
    private int shaderProgram;

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private int positionHandle;
    private int colorHandle;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    // 빨간색 공 색상
    private float[] color = {1.0f, 0.0f, 0.0f, 1.0f};

    public Ball() {
        generateBallVertices(30); // 세밀한 원 형태의 공 만들기

        // 버텍스 버퍼 초기화
        ByteBuffer bb = ByteBuffer.allocateDirect(ballCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(ballCoords);
        vertexBuffer.position(0);

        // 셰이더 컴파일
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);
    }

    private void generateBallVertices(int segments) {
        ballCoords = new float[(segments + 2) * 3];
        ballCoords[0] = 0; // 중심
        ballCoords[1] = 0;
        ballCoords[2] = 0;
        for (int i = 1; i <= segments + 1; i++) {
            double angle = (i - 1) * (2 * Math.PI / segments);
            ballCoords[i * 3] = (float) Math.cos(angle) * 0.2f;
            ballCoords[i * 3 + 1] = (float) Math.sin(angle) * 0.2f;
            ballCoords[i * 3 + 2] = 0;
        }
        vertexCount = ballCoords.length / COORDS_PER_VERTEX;
    }

    public void draw(float x, float y) {
        GLES20.glUseProgram(shaderProgram);
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}

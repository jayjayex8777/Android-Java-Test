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

    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_COUNT = 36;
    private float[] ballCoords = new float[VERTEX_COUNT * 3];
    private float[] color = {1.0f, 0.0f, 0.0f, 1.0f};  // 🔥 빨간색 유지

    public Ball() {
        generateBallVertices();

        ByteBuffer bb = ByteBuffer.allocateDirect(ballCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(ballCoords);
        vertexBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);
    }

    private void generateBallVertices() {
        for (int i = 0; i < VERTEX_COUNT; i++) {
            double angle = (i * 2 * Math.PI) / VERTEX_COUNT;
            ballCoords[i * 3] = (float) Math.cos(angle) * 0.2f;
            ballCoords[i * 3 + 1] = (float) Math.sin(angle) * 0.2f;
            ballCoords[i * 3 + 2] = 0f;
        }
    }

    public void draw(float x, float y, float angle, float[] projectionMatrix) {
        GLES20.glUseProgram(shaderProgram);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, -2f);
        Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f);

        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_COUNT);
    }
}

package com.example.apptest2;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Ball {

    private FloatBuffer vertexBuffer, normalBuffer;
    private int shaderProgram;
    private float[] modelMatrix = new float[16];
    private float[] rotationMatrix = new float[16];
    private int positionHandle, colorHandle, matrixHandle, normalHandle, lightPosHandle;

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "uniform mat4 uRotationMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec3 vNormal;" +
            "uniform vec3 uLightPos;" +
            "varying vec4 vColor;" +
            "void main() {" +
            "  vec3 transformedNormal = normalize(vec3(uRotationMatrix * vec4(vNormal, 0.0)));" +
            "  float lightIntensity = max(dot(transformedNormal, normalize(uLightPos)), 0.3);" +
            "  vColor = vec4(lightIntensity, lightIntensity, lightIntensity, 1.0);" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_COUNT = 36;
    private float[] ballCoords = new float[VERTEX_COUNT * 3];
    private float[] normals = new float[VERTEX_COUNT * 3];

    public Ball() {
        generateBallVertices();

        ByteBuffer bb = ByteBuffer.allocateDirect(ballCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(ballCoords);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(normals.length * 4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(normals);
        normalBuffer.position(0);

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

            normals[i * 3] = ballCoords[i * 3];
            normals[i * 3 + 1] = ballCoords[i * 3 + 1];
            normals[i * 3 + 2] = 1.0f;
        }
    }

    public void draw(float x, float y, float angle, float[] projectionMatrix) {
        GLES20.glUseProgram(shaderProgram);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, -2f);
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, 1f);

        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        matrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0);

        normalHandle = GLES20.glGetAttribLocation(shaderProgram, "vNormal");
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, normalBuffer);

        lightPosHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightPos");
        GLES20.glUniform3f(lightPosHandle, 1.0f, 1.0f, 1.0f);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_COUNT);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

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

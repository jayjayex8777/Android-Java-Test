package com.example.posturetest1;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube {

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

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int program;

    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;
    private static final float[] cubeCoords = {
            -0.5f,  0.5f,  0.5f,  // 앞면 좌상
            -0.5f, -0.5f,  0.5f,  // 앞면 좌하
             0.5f, -0.5f,  0.5f,  // 앞면 우하
             0.5f,  0.5f,  0.5f,  // 앞면 우상
            -0.5f,  0.5f, -0.5f,  // 뒷면 좌상
            -0.5f, -0.5f, -0.5f,  // 뒷면 좌하
             0.5f, -0.5f, -0.5f,  // 뒷면 우하
             0.5f,  0.5f, -0.5f   // 뒷면 우상
    };

    private static final short[] drawOrder = {
            0, 1, 2,  0, 2, 3,  // 앞면
            4, 5, 6,  4, 6, 7,  // 뒷면
            0, 1, 5,  0, 5, 4,  // 왼쪽면
            2, 3, 7,  2, 7, 6,  // 오른쪽면
            0, 3, 7,  0, 7, 4,  // 윗면
            1, 2, 6,  1, 6, 5   // 아랫면
    };

    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private final float[] color = {0.0f, 0.0f, 1.0f, 1.0f}; // 파란색

    public Cube() {
        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    public void init() {
        GLES20.glUseProgram(program);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}

package com.example.objectposturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private final String vertexShaderCode =
        "attribute vec4 vPosition;" +
        "uniform mat4 uMVPMatrix;" +
        "uniform vec4 vColor;" +
        "varying vec4 fColor;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  fColor = vColor;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec4 fColor;" +
        "void main() {" +
        "  gl_FragColor = fColor;" +
        "}";

    private int program;
    private int positionHandle, colorHandle, mvpMatrixHandle;
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private float rotationX = 0;
    private float rotationY = 0;

    private final float[][] faceColors = {
        {1.0f, 0.0f, 0.0f, 1.0f},  // 앞면 (빨강)
        {0.0f, 1.0f, 0.0f, 1.0f},  // 뒷면 (초록)
        {0.0f, 0.0f, 1.0f, 1.0f},  // 윗면 (파랑)
        {1.0f, 1.0f, 0.0f, 1.0f},  // 아랫면 (노랑)
        {1.0f, 0.0f, 1.0f, 1.0f},  // 왼쪽면 (자홍)
        {0.0f, 1.0f, 1.0f, 1.0f}   // 오른쪽면 (청록)
    };

    private final float[] cubeCoords = {
        -0.2f,  1.0f,  0.2f,  
         0.2f,  1.0f,  0.2f,  
        -0.2f,  0.0f,  0.2f,  
         0.2f,  0.0f,  0.2f,  
        -0.2f,  1.0f, -0.2f,  
         0.2f,  1.0f, -0.2f,  
        -0.2f,  0.0f, -0.2f,  
         0.2f,  0.0f, -0.2f   
    };

    private final short[] drawOrder = {
        0, 1, 2, 2, 1, 3,
        4, 5, 6, 6, 5, 7,
        0, 4, 2, 2, 4, 6,
        1, 5, 3, 3, 5, 7,
        0, 1, 4, 4, 1, 5,
        2, 3, 6, 6, 3, 7
    };

    public CubeRenderer() {
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
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        for (int i = 0; i < 6; i++) {
            GLES20.glUniform4fv(colorHandle, 1, faceColors[i], 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, drawListBuffer.position(i * 6));
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}

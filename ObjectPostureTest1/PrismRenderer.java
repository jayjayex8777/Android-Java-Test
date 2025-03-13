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

    private final float[] vertices = {
        -0.5f, -1.5f,  0.5f,  0.5f, -1.5f,  0.5f,
         0.5f,  1.5f,  0.5f, -0.5f,  1.5f,  0.5f,
        -0.5f, -1.5f, -0.5f,  0.5f, -1.5f, -0.5f,
         0.5f,  1.5f, -0.5f, -0.5f,  1.5f, -0.5f
    };

    private final short[] indices = {
        0, 1, 2, 2, 3, 0, 1, 5, 6, 6, 2, 1, 5, 4, 7, 7, 6, 5,
        4, 0, 3, 3, 7, 4, 3, 2, 6, 6, 7, 3, 4, 5, 1, 1, 0, 4
    };

    private final float[] prismColor = {0.0f, 0.5f, 1.0f, 1.0f};

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public PrismRenderer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == -1 || fragmentShader == -1) {
            Log.e("PrismRenderer", "Shader compilation failed.");
            mProgram = -1;
            return;
        }

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("PrismRenderer", "Program link error: " + GLES20.glGetProgramInfoLog(mProgram));
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }

    public float getRotationX() { return rotationX; }
    public void setRotationX(float rotationX) { this.rotationX = rotationX; }
    public float getRotationY() { return rotationY; }
    public void setRotationY(float rotationY) { this.rotationY = rotationY; }
}

package com.example.objectposturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PrismRenderer implements GLSurfaceView.Renderer {

    private FloatBuffer vertexBuffer;
    private final int mProgram;
    private float rotationX = 0f;
    private float rotationY = 0f;

    // Vertices for a vertically elongated rectangular prism (cuboid)
    private final float[] vertices = {
        // Front face
        -0.5f, -1.5f,  0.5f,  // Bottom-left
         0.5f, -1.5f,  0.5f,  // Bottom-right
         0.5f,  1.5f,  0.5f,  // Top-right
        -0.5f,  1.5f,  0.5f,  // Top-left
        // Back face
        -0.5f, -1.5f, -0.5f,  // Bottom-left
         0.5f, -1.5f, -0.5f,  // Bottom-right
         0.5f,  1.5f, -0.5f,  // Top-right
        -0.5f,  1.5f, -0.5f   // Top-left
    };

    private final short[] indices = {
        0, 1, 2, 2, 3, 0,  // Front
        1, 5, 6, 6, 2, 1,  // Right
        5, 4, 7, 7, 6, 5,  // Back
        4, 0, 3, 3, 7, 4,  // Left
        3, 2, 6, 6, 7, 3,  // Top
        4, 5, 1, 1, 0, 4   // Bottom
    };

    private final float[] color = {0.0f, 0.5f, 1.0f, 1.0f}; // Light blue color

    // Vertex and fragment shaders
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
        // Initialize vertex buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Compile shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Create and link program
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f); // Light gray background
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Enable depth for 3D
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set up camera
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Apply rotation
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f); // X-axis rotation
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f); // Y-axis rotation

        // Combine matrices
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // Use shader program
        GLES20.glUseProgram(mProgram);

        // Pass vertex data
        int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        // Pass color
        int colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        // Pass MVP matrix
        int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the prism
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT,
                ByteBuffer.allocateDirect(indices.length * 2).order(ByteOrder.nativeOrder())
                        .asShortBuffer().put(indices).position(0));

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public float getRotationX() { return rotationX; }
    public void setRotationX(float rotationX) { this.rotationX = rotationX; }
    public float getRotationY() { return rotationY; }
    public void setRotationY(float rotationY) { this.rotationY = rotationY; }
}

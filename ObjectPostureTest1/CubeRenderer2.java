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
    private FloatBuffer vertexBuffer, colorBuffer;
    private ShortBuffer drawListBuffer;

    private final String vertexShaderCode =
        "attribute vec4 vPosition;" +
        "attribute vec4 vColor;" + 
        "varying vec4 fColor;" +   
        "uniform mat4 uMVPMatrix;" +
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

    private final float[] cubeColors = {
        1.0f, 0.0f, 0.0f, 1.0f,  
        1.0f, 0.0f, 0.0f, 1.0f,  
        0.0f, 1.0f, 0.0f, 1.0f,  
        0.0f, 1.0f, 0.0f, 1.0f,  
        0.0f, 0.0f, 1.0f, 1.0f,  
        0.0f, 0.0f, 1.0f, 1.0f,  
        1.0f, 1.0f, 0.0f, 1.0f,  
        1.0f, 1.0f, 0.0f, 1.0f   
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

        ByteBuffer cbb = ByteBuffer.allocateDirect(cubeColors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();
        colorBuffer.put(cubeColors);
        colorBuffer.position(0);
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
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        Matrix.perspectiveM(projectionMatrix, 0, 45, ratio, 1, 10);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0, 0, 0, 0, 1, 0);
    }

    public void setRotation(float dx, float dy) {
        rotationX += dy * 0.5f;
        rotationY += dx * 0.5f;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}

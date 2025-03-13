package com.example.objectposturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

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
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private int program;
    private int positionHandle, colorHandle;
    private float[] modelMatrix = new float[16];

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

    public CubeRenderer() {
        Log.d("GLRenderer", "Initializing CubeRenderer");

        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);

        // ShortBuffer 생성 및 초기화
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("GLRenderer", "onSurfaceCreated called");

        GLES20.glClearColor(1f, 1f, 1f, 1f); // 🚀 배경 흰색 설정
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e("GLRenderer", "Error: Shader compilation failed!");
            return;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e("GLRenderer", "Error: Shader program linking failed!");
            GLES20.glDeleteProgram(program);
            program = 0;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d("GLRenderer", "onDrawFrame called");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        if (drawListBuffer == null) {
            Log.e("GLRenderer", "Error: drawListBuffer is null!");
            return;
        }

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glUniform4fv(colorHandle, 1, new float[]{0.6f, 0.8f, 1.0f, 1.0f}, 0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, rotationY, 1, 0, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void setRotation(float dx, float dy) {
        rotationX += dx * 0.5f;
        rotationY += dy * 0.5f;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile shader " + type + ":");
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}

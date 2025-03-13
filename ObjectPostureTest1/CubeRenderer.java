package objectposturetest1;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {
    private FloatBuffer vertexBuffer;
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
    private int positionHandle, colorHandle, mvpMatrixHandle;
    private float[] mvpMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    private float rotationX = 0;
    private float rotationY = 0;

    private final float[] cubeCoords = {
            -0.2f,  1.0f,  0.2f,  // Top Left Front
             0.2f,  1.0f,  0.2f,  // Top Right Front
            -0.2f,  0.0f,  0.2f,  // Bottom Left Front
             0.2f,  0.0f,  0.2f,  // Bottom Right Front
            -0.2f,  1.0f, -0.2f,  // Top Left Back
             0.2f,  1.0f, -0.2f,  // Top Right Back
            -0.2f,  0.0f, -0.2f,  // Bottom Left Back
             0.2f,  0.0f, -0.2f   // Bottom Right Back
    };

    private final short[] drawOrder = {
            0, 1, 2, 2, 1, 3,
            4, 5, 6, 6, 5, 7,
            0, 4, 2, 2, 4, 6,
            1, 5, 3, 3, 5, 7,
            0, 1, 4, 4, 1, 5,
            2, 3, 6, 6, 3, 7
    };

    private final float[] color = {0.6f, 0.8f, 1.0f, 1.0f}; // 연한 파랑색

    public CubeRenderer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        program = GLES20.glCreateProgram();
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotationX, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, rotationY, 1, 0, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, ByteBuffer.allocateDirect(drawOrder.length * 2).putShort(drawOrder).position(0));
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void setRotation(float dx, float dy) {
        rotationX += dx * 0.5f;
        rotationY += dy * 0.5f;
    }
}

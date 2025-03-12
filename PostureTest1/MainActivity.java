package com.example.posturetest1;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private GLSurfaceView glSurfaceView;
    private CubeRenderer cubeRenderer;
    private TextView sensorTextView;

    private float yaw = 0, pitch = 0, roll = 0;  // 회전값
    private float posX = 0, posY = 0, posZ = 0;  // 위치값

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        sensorTextView = findViewById(R.id.sensorTextView);
        glSurfaceView.setEGLContextClientVersion(2);
        
        cubeRenderer = new CubeRenderer();
        glSurfaceView.setRenderer(cubeRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            yaw += event.values[2] * 0.1f;  // Z 축 회전
            pitch += event.values[1] * 0.1f;  // Y 축 회전
            roll += event.values[0] * 0.1f;  // X 축 회전
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            posX = event.values[0] * 0.1f;
            posY = event.values[1] * 0.1f;
            posZ = event.values[2] * 0.1f;
        }

        cubeRenderer.setTransform(yaw, pitch, roll, posX, posY, posZ);
        sensorTextView.setText(String.format("Yaw: %+.2f, Pitch: %+.2f, Roll: %+.2f\nAccel X: %+.2f, Y: %+.2f, Z: %+.2f",
                yaw, pitch, roll, posX, posY, posZ));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}

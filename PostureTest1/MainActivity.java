package com.example.posturetest1;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private SmartphoneView smartphoneView;
    private TextView gyroTextView, accelTextView;
    private GraphView gyroGraph, accelGraph;
    private LineGraphSeries<DataPoint> gyroYawSeries, gyroPitchSeries, gyroRollSeries;
    private LineGraphSeries<DataPoint> accelXSeries, accelYSeries, accelZSeries;
    private int graphXIndex = 0;

    private float currentYaw = 0f, currentPitch = 0f, currentRoll = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI 요소 연결
        smartphoneView = findViewById(R.id.smartphoneView);
        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

        // 그래프 초기화 및 뷰포트 설정 (자동 스크롤 활성화)
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();
        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);
        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(100);
        gyroGraph.getViewport().setScrollable(true);

        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();
        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);
        accelGraph.getViewport().setXAxisBoundsManual(true);
        accelGraph.getViewport().setMinX(0);
        accelGraph.getViewport().setMaxX(100);
        accelGraph.getViewport().setScrollable(true);

        // 센서 매니저 설정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        runOnUiThread(() -> {
            graphXIndex++;

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float yaw = event.values[0];
                float pitch = event.values[1];
                float roll = event.values[2];

                currentYaw += yaw;
                currentPitch += pitch;
                currentRoll += roll;

                smartphoneView.updateRotation(currentYaw, currentPitch, currentRoll);

                gyroYawSeries.appendData(new DataPoint(graphXIndex, yaw), true, 100);
                gyroPitchSeries.appendData(new DataPoint(graphXIndex, pitch), true, 100);
                gyroRollSeries.appendData(new DataPoint(graphXIndex, roll), true, 100);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}

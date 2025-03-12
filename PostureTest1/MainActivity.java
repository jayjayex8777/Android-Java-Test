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

        // 그래프 초기화
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();
        gyroYawSeries.setColor(Color.RED);
        gyroPitchSeries.setColor(Color.GREEN);
        gyroRollSeries.setColor(Color.BLUE);
        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);

        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();
        accelXSeries.setColor(Color.RED);
        accelYSeries.setColor(Color.GREEN);
        accelZSeries.setColor(Color.BLUE);
        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);

        // 센서 매니저 설정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
            }
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        runOnUiThread(() -> {
            graphXIndex++;

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float pitch = event.values[1];
                float roll = event.values[2];

                smartphoneView.updateRotation(pitch, roll);
                gyroTextView.setText(String.format("Pitch: %+06.2f, Roll: %+06.2f", pitch, roll));
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float accelX = event.values[0];
                float accelY = event.values[1];
                float accelZ = event.values[2];

                accelTextView.setText(String.format("Accel X: %+06.2f, Y: %+06.2f, Z: %+06.2f", accelX, accelY, accelZ));
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}

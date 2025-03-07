package com.example.objectselect2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private TextView gyroTextView, accelTextView;
    
    private GraphView gyroGraph, accelGraph;
    private LineGraphSeries<DataPoint> gyroYawSeries, gyroPitchSeries, gyroRollSeries;
    private LineGraphSeries<DataPoint> accelXSeries, accelYSeries, accelZSeries;
    
    private int graphXIndex = 0;
    private long lastUpdateTimeGyro = 0;
    private long lastUpdateTimeAccel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // RecyclerView 설정
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 데이터 생성
        List<String> dataList = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            dataList.add(String.valueOf(i));
        }

        // 어댑터 설정
        RectangleAdapter adapter = new RectangleAdapter(dataList);
        recyclerView.setAdapter(adapter);

        // 센서 매니저 설정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // UI 요소 연결
        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

        // 그래프 초기화
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();
        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);

        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();
        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        graphXIndex++;

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];
            float pitch = event.values[1];
            float roll = event.values[2];

            gyroYawSeries.appendData(new DataPoint(graphXIndex, yaw), true, 100);
            gyroPitchSeries.appendData(new DataPoint(graphXIndex, pitch), true, 100);
            gyroRollSeries.appendData(new DataPoint(graphXIndex, roll), true, 100);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float accelX = event.values[0];
            float accelY = event.values[1];
            float accelZ = event.values[2];

            accelXSeries.appendData(new DataPoint(graphXIndex, accelX), true, 100);
            accelYSeries.appendData(new DataPoint(graphXIndex, accelY), true, 100);
            accelZSeries.appendData(new DataPoint(graphXIndex, accelZ), true, 100);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}

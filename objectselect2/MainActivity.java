package com.example.objectselect2;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // UI 요소 연결
        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

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

        // 그래프 초기화 (자이로 데이터)
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();

        gyroYawSeries.setColor(Color.RED);    // Yaw - 빨간색
        gyroPitchSeries.setColor(Color.GREEN); // Pitch - 초록색
        gyroRollSeries.setColor(Color.BLUE);   // Roll - 파란색

        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);

        // 그래프 초기화 (가속도 데이터)
        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();

        accelXSeries.setColor(Color.RED);    // Accel X - 빨간색
        accelYSeries.setColor(Color.GREEN); // Accel Y - 초록색
        accelZSeries.setColor(Color.BLUE);   // Accel Z - 파란색

        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);

        // Gyro 그래프 설정
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMinY(-7);
        gyroGraph.getViewport().setMaxY(7);
        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(100);
        gyroGraph.getViewport().setScrollable(true);

        // Accel 그래프 설정
        accelGraph.getViewport().setXAxisBoundsManual(true);
        accelGraph.getViewport().setMinX(0);
        accelGraph.getViewport().setMaxX(100);
        accelGraph.getViewport().setScrollable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
                Log.d("SENSOR_REGISTER", "Gyroscope registered successfully");
            }
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                Log.d("SENSOR_REGISTER", "Accelerometer registered successfully");
            }
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

                gyroTextView.setText(String.format("Yaw: %+06.2f, Pitch: %+06.2f, Roll: %+06.2f", yaw, pitch, roll));
                gyroYawSeries.appendData(new DataPoint(graphXIndex, yaw), true, 100);
                gyroPitchSeries.appendData(new DataPoint(graphXIndex, pitch), true, 100);
                gyroRollSeries.appendData(new DataPoint(graphXIndex, roll), true, 100);
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float accelX = event.values[0];
                float accelY = event.values[1];
                float accelZ = event.values[2];

                accelTextView.setText(String.format("Accel X: %+06.2f, Y: %+06.2f, Z: %+06.2f", accelX, accelY, accelZ));
                accelXSeries.appendData(new DataPoint(graphXIndex, accelX), true, 100);
                accelYSeries.appendData(new DataPoint(graphXIndex, accelY), true, 100);
                accelZSeries.appendData(new DataPoint(graphXIndex, accelZ), true, 100);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}

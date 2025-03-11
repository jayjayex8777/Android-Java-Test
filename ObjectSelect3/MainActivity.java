package com.example.objectselect3;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
        setContentView(R.layout.activity_main);

        // 센서 및 그래프 UI 연결 (기존 코드 그대로)
        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

        // RecyclerView (30×30 그리드) 설정
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        // GridLayoutManager로 열(span)을 30개로 설정 → 그리드 형태로 표시됨
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 30);
        recyclerView.setLayoutManager(gridLayoutManager);

        // 30×30 데이터 생성 (각 사각형에 X, Y 좌표 표시)
        List<String> dataList = new ArrayList<>();
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                dataList.add("X: " + x + ", Y: " + y);
            }
        }

        // 어댑터 설정 (RectangleAdapter 수정하여 아이템 터치 시 DetailActivity 전환)
        RectangleAdapter adapter = new RectangleAdapter(dataList);
        recyclerView.setAdapter(adapter);

        // 센서 매니저 및 센서 초기화 (기존 코드 그대로)
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // 그래프 초기화 (자이로 데이터)
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();
        gyroYawSeries.setColor(Color.RED);
        gyroPitchSeries.setColor(Color.GREEN);
        gyroRollSeries.setColor(Color.BLUE);
        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);

        // 그래프 초기화 (가속도 데이터)
        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();
        accelXSeries.setColor(Color.RED);
        accelYSeries.setColor(Color.GREEN);
        accelZSeries.setColor(Color.BLUE);
        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);

        // 자이로 그래프 설정
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMinY(-7);
        gyroGraph.getViewport().setMaxY(7);
        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(100);
        gyroGraph.getViewport().setScrollable(true);

        // 가속도 그래프 설정
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

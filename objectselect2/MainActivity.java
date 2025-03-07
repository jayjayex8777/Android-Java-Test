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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private TextView gyroTextView, accelTextView;
    
    private long lastUpdateTimeGyro = 0;
    private long lastUpdateTimeAccel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // RecyclerView 설정 (상단 배치)
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 데이터 생성 (1번부터 30번까지)
        List<String> dataList = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            dataList.add(String.valueOf(i));  // 숫자만 표시
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
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];   // Yaw (Z 축 회전)
            float pitch = event.values[1]; // Pitch (X 축 회전)
            float roll = event.values[2];  // Roll (Y 축 회전)

            long intervalGyro = (lastUpdateTimeGyro == 0) ? 0 : (currentTime - lastUpdateTimeGyro);
            lastUpdateTimeGyro = currentTime;

            // 텍스트 길이 고정된 형식
            String gyroText = String.format("Yaw: %+06.2f, Pitch: %+06.2f, Roll: %+06.2f | %03d ms", 
                                            yaw, pitch, roll, intervalGyro);
            gyroTextView.setText(gyroText);
            Log.d("SENSOR_UPDATE", "Gyro: " + gyroText);
        } 

        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float accelX = event.values[0];
            float accelY = event.values[1];
            float accelZ = event.values[2];

            long intervalAccel = (lastUpdateTimeAccel == 0) ? 0 : (currentTime - lastUpdateTimeAccel);
            lastUpdateTimeAccel = currentTime;

            // 텍스트 길이 고정된 형식
            String accelText = String.format("Accel X: %+06.2f, Y: %+06.2f, Z: %+06.2f | %03d ms", 
                                             accelX, accelY, accelZ, intervalAccel);
            accelTextView.setText(accelText);
            Log.d("SENSOR_UPDATE", "Accelerometer: " + accelText);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 이벤트는 여기서는 사용하지 않음
    }
}

package com.example.objectselect1;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private TextView sensorValues;
    private LineChart chart;
    private LineData lineData;
    private int timeIndex = 0;

    private CustomRecyclerView recyclerView;
    private float accumulatedPitch = 0f; // 📌 Pitch 변화량 누적
    private static final float PITCH_THRESHOLD = 0.03f; // 📌 민감도 조정 (기존 0.1 → 0.03)
    private static final int SCROLL_SPEED = 200; // 📌 스크롤 속도 증가 (기존 50 → 200)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 🚀 RecyclerView 설정
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 숫자 리스트 (1~10)
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            numbers.add(i);
        }
        RectangleAdapter adapter = new RectangleAdapter(this, numbers);
        recyclerView.setAdapter(adapter);

        // SnapHelper 적용
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // 📌 센서 값 표시 TextView
        sensorValues = findViewById(R.id.sensorValues);

        // 📊 그래프 초기화
        chart = findViewById(R.id.chart);
        setupChart();

        // 📡 센서 매니저 설정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscopeSensor != null) {
            // 📌 업데이트 주기를 더 빠르게 (SENSOR_DELAY_UI → SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // 📡 센서 데이터 업데이트
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];
            float pitch = event.values[1]; // PITCH (X축)
            float roll = event.values[2];

            // 📝 센서 값 업데이트
            sensorValues.setText(String.format("Yaw: %.2f | Pitch: %.2f | Roll: %.2f", yaw, pitch, roll));

            // 📊 그래프에 데이터 추가
            addEntry(yaw, pitch, roll);

            // 🚀 PITCH 값으로 RecyclerView 스크롤
            handleGyroScroll(pitch);
        }
    }

    // 📊 그래프 데이터 추가
    private void addEntry(float yaw, float pitch, float roll) {
        LineData data = chart.getData();
        if (data != null) {
            data.getDataSetByIndex(0).addEntry(new Entry(timeIndex, yaw));
            data.getDataSetByIndex(1).addEntry(new Entry(timeIndex, pitch));
            data.getDataSetByIndex(2).addEntry(new Entry(timeIndex, roll));

            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.moveViewToX(timeIndex++);
        }
    }

    // 🚀 PITCH 값에 따라 RecyclerView 스크롤 조절 (누적 방식 적용)
    private void handleGyroScroll(float pitch) {
        accumulatedPitch += pitch; // 📌 Pitch 변화량을 누적하여 반영

        if (Math.abs(accumulatedPitch) > PITCH_THRESHOLD) {
            int scrollAmount = (int) (SCROLL_SPEED * accumulatedPitch);
            recyclerView.smoothScrollBy(scrollAmount, 0); // 🚀 좌우 스크롤 적용
            accumulatedPitch = 0; // 📌 적용 후 Pitch 변화량 초기화
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // 📊 그래프 초기 설정
    private void setupChart() {
        lineData = new LineData();
        lineData.addDataSet(createDataSet("Yaw", 0xFFAA0000));  // 🔴 빨간색
        lineData.addDataSet(createDataSet("Pitch", 0xFF00AA00)); // 🟢 초록색
        lineData.addDataSet(createDataSet("Roll", 0xFF0000AA"));  // 🔵 파란색

        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        return dataSet;
    }
}

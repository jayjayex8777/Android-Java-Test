package com.example.objectselect1;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private TextView sensorValues;
    private LineChart chart;
    private LineData lineData;
    private int timeIndex = 0;

    private CustomRecyclerView recyclerView;
    private final Handler handler = new Handler();
    private static final int TIME_WINDOW_MS = 10;
    private final ArrayList<Float> yawValues = new ArrayList<>();
    private final ArrayList<Float> pitchValues = new ArrayList<>();
    private final ArrayList<Float> rollValues = new ArrayList<>();

    private final Runnable timeWindowRunnable = new Runnable() {
        @Override
        public void run() {
            processTimeWindow();
            handler.postDelayed(this, TIME_WINDOW_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 🚀 RecyclerView 설정 (Rectangle Object 리스트)
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);

            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= 30; i++) {  // ✅ 30개로 변경
                numbers.add(i);
            }
            RectangleAdapter adapter = new RectangleAdapter(this, numbers);
            recyclerView.setAdapter(adapter);

            // 스냅 도우미 추가 (플링 시 자연스럽게 정렬)
            SnapHelper snapHelper = new LinearSnapHelper();
            snapHelper.attachToRecyclerView(recyclerView);
        }

        // 📌 센서 값 표시 TextView
        sensorValues = findViewById(R.id.sensorValues);

        // 📊 그래프 초기화
        chart = findViewById(R.id.chart);
        setupChart();

        // 📡 센서 매니저 설정
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscopeSensor != null) {
                sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            } else {
                sensorValues.setText("Gyroscope Sensor Not Available");
            }
        }

        // 📌 Time Window 시작
        handler.post(timeWindowRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];
            float pitch = event.values[1];
            float roll = event.values[2];

            // 📌 센서 값 표시
            sensorValues.setText(String.format("Yaw: %.2f | Pitch: %.2f | Roll: %.2f", yaw, pitch, roll));

            // 📌 Time Window 내 데이터 수집
            yawValues.add(yaw);
            pitchValues.add(pitch);
            rollValues.add(roll);
        }
    }

    private void processTimeWindow() {
        if (!yawValues.isEmpty() && !pitchValues.isEmpty() && !rollValues.isEmpty()) {
            float avgYaw = calculateAverage(yawValues);
            float avgPitch = calculateAverage(pitchValues);
            float avgRoll = calculateAverage(rollValues);

            addEntry(avgYaw, avgPitch, avgRoll);

            yawValues.clear();
            pitchValues.clear();
            rollValues.clear();
        }
    }

    private float calculateAverage(List<Float> values) {
        float sum = 0f;
        for (float v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private void addEntry(float yaw, float pitch, float roll) {
        if (chart.getData() == null) {
            return;
        }
        LineData data = chart.getData();
        if (data.getDataSetCount() > 2) {
            data.getDataSetByIndex(0).addEntry(new Entry(timeIndex, yaw));
            data.getDataSetByIndex(1).addEntry(new Entry(timeIndex, pitch));
            data.getDataSetByIndex(2).addEntry(new Entry(timeIndex, roll));

            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.moveViewToX(timeIndex++);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacks(timeWindowRunnable);
    }

    // 📊 그래프 초기 설정
    private void setupChart() {
        lineData = new LineData();
        lineData.addDataSet(createDataSet("Yaw", 0xFFAA0000));  // 🔴 빨간색
        lineData.addDataSet(createDataSet("Pitch", 0xFF00AA00)); // 🟢 초록색
        lineData.addDataSet(createDataSet("Roll", 0xFF0000AA));  // 🔵 파란색

        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(-5f);
        leftAxis.setAxisMaximum(5f);
        chart.getAxisRight().setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
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

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
            CustomFlingLinearLayoutManager layoutManager = new CustomFlingLinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setOverScrollMode(CustomRecyclerView.OVER_SCROLL_NEVER); // ✅ 오버스크롤 제거

            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= 30; i++) {  
                numbers.add(i);
            }
            RectangleAdapter adapter = new RectangleAdapter(this, numbers);
            recyclerView.setAdapter(adapter);

            // ✅ SnapHelper 유지 (너무 급격하게 멈추지 않도록 함)
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
}

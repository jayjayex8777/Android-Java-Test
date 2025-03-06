package com.example.objectselect1;

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
    private float accumulatedPitch = 0f;
    private static final float PITCH_THRESHOLD = 0.02f;
    private static final int SCROLL_SPEED = 300;
    private static final float PITCH_DECAY = 0.98f;

    private final Handler handler = new Handler();
    private final Runnable decayRunnable = new Runnable() {
        @Override
        public void run() {
            if (Math.abs(accumulatedPitch) > 0.01f) {
                accumulatedPitch *= PITCH_DECAY;
                handleGyroScroll(accumulatedPitch);
                handler.postDelayed(this, 50);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 🚀 RecyclerView 설정
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);

            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                numbers.add(i);
            }
            RectangleAdapter adapter = new RectangleAdapter(this, numbers);
            recyclerView.setAdapter(adapter);

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
                sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                sensorValues.setText("Gyroscope Sensor Not Available");
            }
        }

        handler.post(decayRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];
            float pitch = event.values[1];
            float roll = event.values[2];

            sensorValues.setText(String.format("Yaw: %.2f | Pitch: %.2f | Roll: %.2f", yaw, pitch, roll));

            addEntry(yaw, pitch, roll);

            accumulatedPitch += pitch;
            handleGyroScroll(accumulatedPitch);
        }
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

    private void handleGyroScroll(float pitch) {
        if (recyclerView != null && Math.abs(pitch) > PITCH_THRESHOLD) {
            int scrollAmount = (int) (SCROLL_SPEED * pitch);
            recyclerView.smoothScrollBy(scrollAmount, 0);
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
        handler.removeCallbacks(decayRunnable);
    }
}

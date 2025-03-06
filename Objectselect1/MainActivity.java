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
    private TextView sensorValues;
    private LineChart chart;
    private LineData lineData;
    private int timeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 🚀 RecyclerView 설정
        CustomRecyclerView recyclerView = findViewById(R.id.recyclerView);
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
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    // 📊 그래프 초기 설정
    private void setupChart() {
        lineData = new LineData();

        // 3개의 데이터셋 (Yaw, Pitch, Roll)
        lineData.addDataSet(createDataSet("Yaw", 0xFFAA0000));  // 빨간색
        lineData.addDataSet(createDataSet("Pitch", 0xFF00AA00)); // 초록색
        lineData.addDataSet(createDataSet("Roll", 0xFF0000AA));  // 파란색

        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        // X축 설정 (시간 흐름)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        // Y축 설정
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(-5f);
        leftAxis.setAxisMaximum(5f);
        chart.getAxisRight().setEnabled(false);

        // 범례 설정
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
    }

    // 📊 데이터셋 생성
    private LineDataSet createDataSet(String label, int color) {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        return dataSet;
    }

    // 📡 센서 데이터 업데이트
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yaw = event.values[0];
            float pitch = event.values[1];
            float roll = event.values[2];

            // 📝 센서 값 업데이트
            sensorValues.setText(String.format("Yaw: %.2f | Pitch: %.2f | Roll: %.2f", yaw, pitch, roll));

            // 📊 그래프에 데이터 추가
            addEntry(yaw, pitch, roll);
        }
    }

    // 📊 그래프에 데이터 추가
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

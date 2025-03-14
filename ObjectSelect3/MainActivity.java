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

        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

        OrientationAwareRecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 30, GridLayoutManager.HORIZONTAL, false));

        List<String> dataList = new ArrayList<>();
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                dataList.add("X: " + x + ", Y: " + y);
            }
        }

        recyclerView.setAdapter(new RectangleAdapter(dataList));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        runOnUiThread(() -> {
            graphXIndex++;
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelXSeries.appendData(new DataPoint(graphXIndex, event.values[0]), true, 100);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}

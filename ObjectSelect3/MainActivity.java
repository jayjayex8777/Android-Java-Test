package com.example.objectselect3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.recyclerview.widget.GridLayoutManager;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private TextView gyroTextView, accelTextView;
    private OrientationAwareRecyclerView recyclerView;
    private GraphView gyroGraph, accelGraph;

    private LineGraphSeries<DataPoint> gyroYawSeries, gyroPitchSeries, gyroRollSeries;
    private LineGraphSeries<DataPoint> accelXSeries, accelYSeries, accelZSeries;
    private int graphXIndex = 0;
    private static final int grid_size = 20;

    private BufferedWriter csvWriter;
    private File csvFile;
    private boolean isCsvRecording = false;

    private static final int REQUEST_MANAGE_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 외부 저장소 권한 확인
        checkStoragePermission();

        // UI 연결
        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);

        Button startCsvButton = findViewById(R.id.startCsvButton);
        Button stopCsvButton = findViewById(R.id.stopCsvButton);
        Button deleteCsvButton = findViewById(R.id.deleteCsvButton);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, grid_size));

        List<String> dataList = new ArrayList<>();
        for (int y = 0; y < grid_size; y++) {
            for (int x = 0; x < grid_size; x++) {
                dataList.add("X: " + x + ", Y: " + y);
            }
        }

        recyclerView.setAdapter(new RectangleAdapter(dataList));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // 그래프 초기화
        gyroYawSeries = new LineGraphSeries<>();
        gyroPitchSeries = new LineGraphSeries<>();
        gyroRollSeries = new LineGraphSeries<>();
        gyroYawSeries.setColor(Color.RED);
        gyroPitchSeries.setColor(Color.GREEN);
        gyroRollSeries.setColor(Color.BLUE);
        gyroGraph.addSeries(gyroYawSeries);
        gyroGraph.addSeries(gyroPitchSeries);
        gyroGraph.addSeries(gyroRollSeries);
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMinY(-7);
        gyroGraph.getViewport().setMaxY(7);
        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(100);
        gyroGraph.getViewport().setScrollable(true);

        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();
        accelZSeries = new LineGraphSeries<>();
        accelXSeries.setColor(Color.RED);
        accelYSeries.setColor(Color.GREEN);
        accelZSeries.setColor(Color.BLUE);
        accelGraph.addSeries(accelXSeries);
        accelGraph.addSeries(accelYSeries);
        accelGraph.addSeries(accelZSeries);
        accelGraph.getViewport().setXAxisBoundsManual(true);
        accelGraph.getViewport().setMinX(0);
        accelGraph.getViewport().setMaxX(100);
        accelGraph.getViewport().setScrollable(true);

        // ▶ CSV 저장 시작
        startCsvButton.setOnClickListener(v -> {
            if (!isCsvRecording) {
                try {
                    String fileName = "sensor_data_" + System.currentTimeMillis() + ".csv";
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadDir.exists()) downloadDir.mkdirs();
                    csvFile = new File(downloadDir, fileName);
                    csvWriter = new BufferedWriter(new FileWriter(csvFile));
                    csvWriter.write("Timestamp,TimeString,SensorType,X,Y,Z\n");
                    isCsvRecording = true;
                    Toast.makeText(this, "CSV 저장 시작됨", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "파일 생성 실패", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ⏹ 저장 중지
        stopCsvButton.setOnClickListener(v -> {
            if (isCsvRecording && csvWriter != null) {
                try {
                    csvWriter.close();
                    csvWriter = null;
                    isCsvRecording = false;
                    Toast.makeText(this, "CSV 저장 중지됨", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // ❌ 파일 삭제
        deleteCsvButton.setOnClickListener(v -> {
            if (csvFile != null && csvFile.exists()) {
                boolean deleted = csvFile.delete();
                Toast.makeText(this, deleted ? "CSV 삭제됨" : "삭제 실패", Toast.LENGTH_SHORT).show();
                if (deleted) {
                    csvWriter = null;
                    csvFile = null;
                    isCsvRecording = false;
                }
            } else {
                Toast.makeText(this, "삭제할 파일 없음", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
            }
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        runOnUiThread(() -> {
            graphXIndex++;
            long timestamp = System.currentTimeMillis();
            String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
            String line = "";

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float yaw = event.values[0];
                float pitch = event.values[1];
                float roll = event.values[2];
                gyroTextView.setText(String.format("Yaw: %+06.2f, Pitch: %+06.2f, Roll: %+06.2f", yaw, pitch, roll));
                gyroYawSeries.appendData(new DataPoint(graphXIndex, yaw), true, 100);
                gyroPitchSeries.appendData(new DataPoint(graphXIndex, pitch), true, 100);
                gyroRollSeries.appendData(new DataPoint(graphXIndex, roll), true, 100);
                line = String.format("%d,%s,GYROSCOPE,%.4f,%.4f,%.4f\n", timestamp, timeString, yaw, pitch, roll);
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float ax = event.values[0];
                float ay = event.values[1];
                float az = event.values[2];
                accelTextView.setText(String.format("Accel X: %+06.2f, Y: %+06.2f, Z: %+06.2f", ax, ay, az));
                accelXSeries.appendData(new DataPoint(graphXIndex, ax), true, 100);
                accelYSeries.appendData(new DataPoint(graphXIndex, ay), true, 100);
                accelZSeries.appendData(new DataPoint(graphXIndex, az), true, 100);
                line = String.format("%d,%s,ACCELEROMETER,%.4f,%.4f,%.4f\n", timestamp, timeString, ax, ay, az);
            }

            if (isCsvRecording && csvWriter != null) {
                try {
                    csvWriter.write(line);
                    csvWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (csvWriter != null) {
            try {
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}

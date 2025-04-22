package com.example.objectselect3;

import android.Manifest; import android.content.Intent; import android.content.pm.PackageManager; import android.graphics.Color; import android.net.Uri; import android.os.Build; import android.os.Bundle; import android.os.Environment; import android.provider.Settings; import android.view.MotionEvent; import android.view.View; import android.widget.Button; import android.widget.EditText; import android.widget.TextView; import android.widget.Toast;

import androidx.activity.EdgeToEdge; import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat; import androidx.recyclerview.widget.GridLayoutManager;

import com.jjoe64.graphview.GraphView; import com.jjoe64.graphview.series.DataPoint; import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter; import java.io.File; import java.io.FileWriter; import java.io.IOException; import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.LinkedList; import java.util.List;

public class MainActivity extends AppCompatActivity implements android.hardware.SensorEventListener {

private android.hardware.SensorManager sensorManager;
private android.hardware.Sensor accelerometer, gyroscope;
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
private boolean isTouching = false;
private boolean shouldInsertBlank = false;

private long lastGyroTimestamp = -1;
private long lastAccelTimestamp = -1;

private static final int REQUEST_MANAGE_STORAGE = 1001;

private final LinkedList<SensorData> sensorDataBuffer = new LinkedList<>();
private long BUFFER_DURATION_MS = 1500;
private long touchPreDurationMs = 100;
private long touchPostDurationMs = 100;
private long postTouchEndTime = 0;

private View touchOverlay;
private EditText preDurationInput, postDurationInput;

private static class SensorData {
    long timestamp;
    String line;
    boolean isTouchStart = false;
    boolean isTouchEnd = false;

    SensorData(long timestamp, String line) {
        this.timestamp = timestamp;
        this.line = line;
    }
}

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    checkStoragePermission();

    gyroTextView = findViewById(R.id.gyroTextView);
    accelTextView = findViewById(R.id.accelTextView);
    gyroGraph = findViewById(R.id.gyroGraph);
    accelGraph = findViewById(R.id.accelGraph);

    Button startCsvButton = findViewById(R.id.startCsvButton);
    Button stopCsvButton = findViewById(R.id.stopCsvButton);
    Button deleteCsvButton = findViewById(R.id.deleteCsvButton);
    preDurationInput = findViewById(R.id.preDurationInput);
    postDurationInput = findViewById(R.id.postDurationInput);
    touchOverlay = findViewById(R.id.touchOverlay);

    recyclerView = findViewById(R.id.recyclerView);
    recyclerView.setLayoutManager(new GridLayoutManager(this, grid_size));

    List<String> dataList = new ArrayList<>();
    for (int y = 0; y < grid_size; y++) {
        for (int x = 0; x < grid_size; x++) {
            dataList.add("X: " + x + ", Y: " + y);
        }
    }
    recyclerView.setAdapter(new RectangleAdapter(dataList));

    sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
    if (sensorManager != null) {
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
    }

    // 그래프 초기화 (생략 - 기존 그대로 유지)

    startCsvButton.setOnClickListener(v -> {
        try {
            touchPreDurationMs = Long.parseLong(preDurationInput.getText().toString());
            touchPostDurationMs = Long.parseLong(postDurationInput.getText().toString());
        } catch (Exception e) {
            touchPreDurationMs = 100;
            touchPostDurationMs = 100;
        }

        if (!isCsvRecording) {
            try {
                String fileName = "sensor_data_" + System.currentTimeMillis() + ".csv";
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                csvFile = new File(downloadDir, fileName);
                csvWriter = new BufferedWriter(new FileWriter(csvFile));
                csvWriter.write("Timestamp,TimeString,SensorType,X,Y,Z,Interval(ms)\n");
                isCsvRecording = true;
                Toast.makeText(this, "CSV 저장 준비됨 (터치 시 기록)", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "파일 생성 실패", Toast.LENGTH_SHORT).show();
            }
        }
    });

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
public boolean dispatchTouchEvent(MotionEvent event) {
    long now = System.currentTimeMillis();
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            isTouching = true;
            shouldInsertBlank = true;
            touchOverlay.setBackgroundColor(Color.parseColor("#66FF0000"));
            touchOverlay.setVisibility(View.VISIBLE);
            if (csvWriter != null) {
                try {
                    for (SensorData data : sensorDataBuffer) {
                        if (now - data.timestamp <= touchPreDurationMs) {
                            data.isTouchStart = true;
                            csvWriter.write("*START*" + data.line);
                        }
                    }
                    csvWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            isTouching = false;
            postTouchEndTime = now + touchPostDurationMs;
            touchOverlay.setBackgroundColor(Color.parseColor("#660000FF"));
            break;
    }
    return super.dispatchTouchEvent(event);
}

@Override
public void onSensorChanged(android.hardware.SensorEvent event) {
    runOnUiThread(() -> {
        graphXIndex++;
        long timestamp = System.currentTimeMillis();
        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
        String line;
        long interval;

        if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
            interval = (lastGyroTimestamp > 0) ? (timestamp - lastGyroTimestamp) : 0;
            lastGyroTimestamp = timestamp;

            float yaw = event.values[0];
            float pitch = event.values[1];
            float roll = event.values[2];
            gyroTextView.setText(String.format("Yaw: %+06.2f, Pitch: %+06.2f, Roll: %+06.2f", yaw, pitch, roll));
            gyroYawSeries.appendData(new DataPoint(graphXIndex, yaw), true, 100);
            gyroPitchSeries.appendData(new DataPoint(graphXIndex, pitch), true, 100);
            gyroRollSeries.appendData(new DataPoint(graphXIndex, roll), true, 100);
            line = String.format("%d,%s,GYROSCOPE,%.4f,%.4f,%.4f,%d\n",
                    timestamp, timeString, yaw, pitch, roll, interval);
        } else {
            interval = (lastAccelTimestamp > 0) ? (timestamp - lastAccelTimestamp) : 0;
            lastAccelTimestamp = timestamp;

            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            accelTextView.setText(String.format("Accel X: %+06.2f, Y: %+06.2f, Z: %+06.2f", ax, ay, az));
            accelXSeries.appendData(new DataPoint(graphXIndex, ax), true, 100);
            accelYSeries.appendData(new DataPoint(graphXIndex, ay), true, 100);
            accelZSeries.appendData(new DataPoint(graphXIndex, az), true, 100);
            line = String.format("%d,%s,ACCELEROMETER,%.4f,%.4f,%.4f,%d\n",
                    timestamp, timeString, ax, ay, az, interval);
        }

        SensorData sensorData = new SensorData(timestamp, line);
        sensorDataBuffer.addLast(sensorData);
        while (!sensorDataBuffer.isEmpty() && timestamp - sensorDataBuffer.getFirst().timestamp > BUFFER_DURATION_MS) {
            sensorDataBuffer.removeFirst();
        }

        if (isCsvRecording && csvWriter != null) {
            boolean saveNow = isTouching || (timestamp <= postTouchEndTime);
            if (saveNow) {
                try {
                    if (shouldInsertBlank) {
                        csvWriter.write("\n");
                        shouldInsertBlank = false;
                    }
                    if (timestamp == postTouchEndTime) {
                        csvWriter.write("*END*" + line);
                    } else {
                        csvWriter.write(line);
                    }
                    csvWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                touchOverlay.setVisibility(View.GONE);
            }
        }
    });
}

@Override
public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}

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


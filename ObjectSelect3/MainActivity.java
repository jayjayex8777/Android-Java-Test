package com.example.objectselect3;

import android.Manifest; import android.content.Intent; import android.content.pm.PackageManager; import android.net.Uri; import android.os.Build; import android.os.Bundle; import android.os.Environment; import android.provider.Settings; import android.util.Log; import android.view.MotionEvent; import android.widget.Button; import android.widget.TextView; import android.widget.Toast;

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

private long lastGyroTimestamp = -1;
private long lastAccelTimestamp = -1;

private static final int REQUEST_MANAGE_STORAGE = 1001;

private static final long BUFFER_DURATION_MS = 1500;
private static final long TOUCH_PRE_MS = 100;
private static final long TOUCH_POST_MS = 100;

private final LinkedList<String> sensorDataBuffer = new LinkedList<>();
private boolean isTouchActive = false;
private long touchStartTime = 0;
private long touchEndTime = 0;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    Log.d("jit_test", "onCreate called");

    checkStoragePermission();

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

    sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
    if (sensorManager != null) {
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
    }

    startCsvButton.setOnClickListener(v -> {
        Log.d("jit_test", "Start CSV button clicked");
        if (!isCsvRecording) {
            try {
                String fileName = "sensor_data_" + System.currentTimeMillis() + ".csv";
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                csvFile = new File(downloadDir, fileName);
                csvWriter = new BufferedWriter(new FileWriter(csvFile));
                csvWriter.write("Timestamp,TimeString,SensorType,X,Y,Z,Interval(ms)\n");
                isCsvRecording = true;
                Toast.makeText(this, "CSV 저장 준비됨 (터치 시 저장)", Toast.LENGTH_SHORT).show();
                Log.d("jit_test", "CSV writer initialized");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    stopCsvButton.setOnClickListener(v -> {
        Log.d("jit_test", "Stop CSV button clicked");
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
        Log.d("jit_test", "Delete CSV button clicked");
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
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            Log.d("jit_test", "Touch DOWN event");
            if (isCsvRecording) {
                isTouchActive = true;
                touchStartTime = System.currentTimeMillis();
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            Log.d("jit_test", "Touch UP/CANCEL event");
            if (isCsvRecording && isTouchActive) {
                touchEndTime = System.currentTimeMillis();
                isTouchActive = false;
                flushTouchBufferToCsv();
            }
            break;
    }
    return super.dispatchTouchEvent(event);
}

private void flushTouchBufferToCsv() {
    long windowStart = touchStartTime - TOUCH_PRE_MS;
    long windowEnd = touchEndTime + TOUCH_POST_MS;
    Log.d("jit_test", "Flushing buffer to CSV: " + windowStart + " ~ " + windowEnd);
    try {
        for (String line : sensorDataBuffer) {
            String[] parts = line.split(",", 2);
            if (parts.length >= 2) {
                long ts = Long.parseLong(parts[0]);
                if (ts >= windowStart && ts <= windowEnd) {
                    csvWriter.write(line);
                }
            }
        }
        csvWriter.write("\n");
        csvWriter.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

@Override
public void onSensorChanged(android.hardware.SensorEvent event) {
    runOnUiThread(() -> {
        long timestamp = System.currentTimeMillis();
        Log.d("jit_test", "Sensor event at: " + timestamp);

        graphXIndex++;
        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
        String line = "";
        long interval = 0;

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
        } else if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
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

        sensorDataBuffer.addLast(line);
        while (!sensorDataBuffer.isEmpty()) {
            String first = sensorDataBuffer.getFirst();
            String[] parts = first.split(",", 2);
            if (parts.length >= 2) {
                long ts = Long.parseLong(parts[0]);
                if (timestamp - ts > BUFFER_DURATION_MS) {
                    sensorDataBuffer.removeFirst();
                } else {
                    break;
                }
            } else {
                sensorDataBuffer.removeFirst();
            }
        }
    });
}

@Override
public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
    Log.d("jit_test", "Accuracy changed for sensor: " + sensor.getType());
}

@Override
protected void onDestroy() {
    super.onDestroy();
    Log.d("jit_test", "onDestroy called");
    if (csvWriter != null) {
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

private void checkStoragePermission() {
    Log.d("jit_test", "Checking storage permission");
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


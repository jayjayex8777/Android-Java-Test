package com.example.ois_gyroaccel_get;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private TextView gyroTextView, accelTextView;
    private GraphView gyroGraph, accelGraph;
    private LineGraphSeries<DataPoint> gyroYaw, gyroPitch, gyroRoll;
    private LineGraphSeries<DataPoint> accelX, accelY, accelZ;

    private BufferedWriter csvWriter;
    private boolean isRecording = false;
    private boolean isTouching = false;
    private boolean shouldInsertBlank = false;
    private int graphIndex = 0;
    private File csvFile;

    private int recordDelay = 0;
    private static final int MARGIN_TIME = 10;
    private float[][] gyroQueue = new float[MARGIN_TIME][3];
    private long[][] gyroTimeQueue = new long[MARGIN_TIME][2];
    private float[][] accelQueue = new float[MARGIN_TIME][3];
    private long[][] accelTimeQueue = new long[MARGIN_TIME][2];
    private int gIndex, aIndex;
    private long lastGyroTimestamp = -1;
    private long lastAccelTimestamp = -1;
    private static final int REQUEST_MANAGE_STORAGE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkPermission();

        gyroTextView = findViewById(R.id.gyroTextView);
        accelTextView = findViewById(R.id.accelTextView);
        gyroGraph = findViewById(R.id.gyroGraph);
        accelGraph = findViewById(R.id.accelGraph);
        Button startBtn = findViewById(R.id.startCsvButton);
        Button stopBtn = findViewById(R.id.stopCsvButton);
        Button deleteBtn = findViewById(R.id.deleteCsvButton);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // 자이로 그래프 초기화 및 색상 지정
        gyroYaw = new LineGraphSeries<>();
        gyroPitch = new LineGraphSeries<>();
        gyroRoll = new LineGraphSeries<>();
        gyroYaw.setColor(android.graphics.Color.RED);    // Yaw
        gyroPitch.setColor(android.graphics.Color.GREEN); // Pitch
        gyroRoll.setColor(android.graphics.Color.BLUE);   // Roll
        gyroGraph.addSeries(gyroYaw);
        gyroGraph.addSeries(gyroPitch);
        gyroGraph.addSeries(gyroRoll);
        gyroGraph.getViewport().setYAxisBoundsManual(true);
        gyroGraph.getViewport().setMinY(-7);
        gyroGraph.getViewport().setMaxY(7);
        gyroGraph.getViewport().setXAxisBoundsManual(true);
        gyroGraph.getViewport().setMinX(0);
        gyroGraph.getViewport().setMaxX(100);
        gyroGraph.getViewport().setScrollable(true);
        gyroGraph.getViewport().setScalable(true);

        // 가속도 그래프 초기화 및 색상 지정
        accelX = new LineGraphSeries<>();
        accelY = new LineGraphSeries<>();
        accelZ = new LineGraphSeries<>();
        accelX.setColor(android.graphics.Color.RED);     // X
        accelY.setColor(android.graphics.Color.GREEN);   // Y
        accelZ.setColor(android.graphics.Color.BLUE);    // Z
        accelGraph.addSeries(accelX);
        accelGraph.addSeries(accelY);
        accelGraph.addSeries(accelZ);
        accelGraph.getViewport().setXAxisBoundsManual(true);
        accelGraph.getViewport().setMinX(0);
        accelGraph.getViewport().setMaxX(100);
        accelGraph.getViewport().setScrollable(true);
        accelGraph.getViewport().setScalable(true);

        startBtn.setOnClickListener(v -> startRecording());
        stopBtn.setOnClickListener(v -> stopRecording());
        deleteBtn.setOnClickListener(v -> deleteCsvFile());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isTouching = true;
            shouldInsertBlank = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            isTouching = false;
            recordDelay = MARGIN_TIME;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroscope != null)
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        graphIndex++;
        long timestamp = System.currentTimeMillis();
        long interval = 0;
        String line = "";

        if (graphIndex > 100) {
            gyroGraph.getViewport().setMinX(graphIndex - 100);
            gyroGraph.getViewport().setMaxX(graphIndex);
            accelGraph.getViewport().setMinX(graphIndex - 100);
            accelGraph.getViewport().setMaxX(graphIndex);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            int nextIndex = (gIndex + 1) % MARGIN_TIME;
            gyroQueue[nextIndex][0] = event.values[0];
            gyroQueue[nextIndex][1] = event.values[1];
            gyroQueue[nextIndex][2] = event.values[2];
            gyroTimeQueue[nextIndex][0] = timestamp;
            gyroTimeQueue[nextIndex][1] = (lastGyroTimestamp > 0) ? (timestamp - lastGyroTimestamp) : 0;
            lastGyroTimestamp = timestamp;

            float yaw = gyroQueue[gIndex][0];
            float pitch = gyroQueue[gIndex][1];
            float roll = gyroQueue[gIndex][2];
            timestamp = gyroTimeQueue[gIndex][0];
            interval = gyroTimeQueue[gIndex][1];
            String timeStr = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timestamp));

            gIndex = nextIndex;

            gyroTextView.setText(String.format("Yaw: %.2f, Pitch: %.2f, Roll: %.2f", yaw, pitch, roll));
            gyroYaw.appendData(new DataPoint(graphIndex, yaw), true, 300);
            gyroPitch.appendData(new DataPoint(graphIndex, pitch), true, 300);
            gyroRoll.appendData(new DataPoint(graphIndex, roll), true, 300);
            line = String.format("%d,%s,GYRO,%.4f,%.4f,%.4f,%d\n", timestamp, timeStr, yaw, pitch, roll, interval);

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int nextIndex = (aIndex + 1) % MARGIN_TIME;
            accelQueue[nextIndex][0] = event.values[0];
            accelQueue[nextIndex][1] = event.values[1];
            accelQueue[nextIndex][2] = event.values[2];
            accelTimeQueue[nextIndex][0] = timestamp;
            accelTimeQueue[nextIndex][1] = (lastAccelTimestamp > 0) ? (timestamp - lastAccelTimestamp) : 0;
            lastAccelTimestamp = timestamp;

            float ax = accelQueue[aIndex][0];
            float ay = accelQueue[aIndex][1];
            float az = accelQueue[aIndex][2];
            timestamp = accelTimeQueue[aIndex][0];
            interval = accelTimeQueue[aIndex][1];
            String timeStr = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timestamp));

            aIndex = nextIndex;

            accelTextView.setText(String.format("X: %.2f, Y: %.2f, Z: %.2f", ax, ay, az));
            accelX.appendData(new DataPoint(graphIndex, ax), true, 300);
            accelY.appendData(new DataPoint(graphIndex, ay), true, 300);
            accelZ.appendData(new DataPoint(graphIndex, az), true, 300);
            line = String.format("%d,%s,ACCEL,%.4f,%.4f,%.4f,%d\n", timestamp, timeStr, ax, ay, az, interval);
        }

        if (isRecording && (isTouching || recordDelay > 0) && csvWriter != null) {
            try {
                recordDelay--;
                if (shouldInsertBlank) {
                    csvWriter.write("\n");
                    shouldInsertBlank = false;
                }
                csvWriter.write(line);
                csvWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startRecording() {
        try {
            String fileName = "sensor_log_" + System.currentTimeMillis() + ".csv";
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            csvFile = new File(dir, fileName);
            csvWriter = new BufferedWriter(new FileWriter(csvFile));
            csvWriter.write("Timestamp,Time,Sensor,X,Y,Z,Interval\n");
            isRecording = true;
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            if (csvWriter != null) {
                csvWriter.close();
                isRecording = false;
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteCsvFile() {
        if (csvFile != null && csvFile.exists()) {
            boolean deleted = csvFile.delete();
            Toast.makeText(this, deleted ? "CSV 삭제됨" : "삭제 실패", Toast.LENGTH_SHORT).show();
            if (deleted) {
                csvWriter = null;
                csvFile = null;
                isRecording = false;
            }
        } else {
            Toast.makeText(this, "삭제할 파일 없음", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}

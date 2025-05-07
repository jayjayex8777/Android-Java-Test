package com.example.ois_gyroaccel_get;

import android.Manifest; import android.content.pm.PackageManager; import android.hardware.Sensor; import android.hardware.SensorEvent; import android.hardware.SensorEventListener; import android.hardware.SensorManager; import android.os.Build; import android.os.Bundle; import android.os.Environment; import android.provider.Settings; import android.view.MotionEvent; import android.widget.Button; import android.widget.TextView; import android.widget.Toast;

import androidx.activity.EdgeToEdge; import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat; import androidx.core.graphics.Insets; import androidx.core.view.ViewCompat; import androidx.core.view.WindowInsetsCompat;

import com.jjoe64.graphview.GraphView; import com.jjoe64.graphview.series.DataPoint; import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter; import java.io.File; import java.io.FileWriter; import java.io.IOException; import java.text.SimpleDateFormat; import java.util.Date;

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

    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    if (sensorManager != null) {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    gyroYaw = new LineGraphSeries<>();
    gyroPitch = new LineGraphSeries<>();
    gyroRoll = new LineGraphSeries<>();
    gyroGraph.addSeries(gyroYaw);
    gyroGraph.addSeries(gyroPitch);
    gyroGraph.addSeries(gyroRoll);

    accelX = new LineGraphSeries<>();
    accelY = new LineGraphSeries<>();
    accelZ = new LineGraphSeries<>();
    accelGraph.addSeries(accelX);
    accelGraph.addSeries(accelY);
    accelGraph.addSeries(accelZ);

    startBtn.setOnClickListener(v -> startRecording());
    stopBtn.setOnClickListener(v -> stopRecording());
}

private void startRecording() {
    try {
        String fileName = "sensor_log_" + System.currentTimeMillis() + ".csv";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) dir.mkdirs();
        csvFile = new File(dir, fileName);
        csvWriter = new BufferedWriter(new FileWriter(csvFile));
        csvWriter.write("Timestamp,Time,Sensor,X,Y,Z\n");
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
    String timeStr = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timestamp));

    String type = event.sensor.getType() == Sensor.TYPE_GYROSCOPE ? "GYRO" : "ACCEL";
    float x = event.values[0];
    float y = event.values[1];
    float z = event.values[2];

    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
        gyroTextView.setText(String.format("Yaw: %.2f, Pitch: %.2f, Roll: %.2f", x, y, z));
        gyroYaw.appendData(new DataPoint(graphIndex, x), true, 100);
        gyroPitch.appendData(new DataPoint(graphIndex, y), true, 100);
        gyroRoll.appendData(new DataPoint(graphIndex, z), true, 100);
    } else {
        accelTextView.setText(String.format("X: %.2f, Y: %.2f, Z: %.2f", x, y, z));
        accelX.appendData(new DataPoint(graphIndex, x), true, 100);
        accelY.appendData(new DataPoint(graphIndex, y), true, 100);
        accelZ.appendData(new DataPoint(graphIndex, z), true, 100);
    }

    if (isRecording && isTouching && csvWriter != null) {
        try {
            if (shouldInsertBlank) {
                csvWriter.write("\n");
                shouldInsertBlank = false;
            }
            csvWriter.write(String.format("%d,%s,%s,%.4f,%.4f,%.4f\n",
                    timestamp, timeStr, type, x, y, z));
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

@Override
public void onAccuracyChanged(Sensor sensor, int accuracy) {}

@Override
public boolean dispatchTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
        isTouching = true;
        shouldInsertBlank = true;
    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
        isTouching = false;
    }
    return super.dispatchTouchEvent(event);
}

private void checkPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            startActivity(new android.content.Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    } else {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}

}


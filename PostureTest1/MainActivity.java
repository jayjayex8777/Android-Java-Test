package com.example.posturetest1;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private float[] rotationMatrix = new float[9];
    private float[] orientationValues = new float[3];

    private float pitch = 0, roll = 0;

    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Paint paint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        holder = surfaceView.getHolder();
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                draw();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                draw();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] gravity = event.values.clone();
            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, new float[]{0, 0, 1});
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            pitch = (float) Math.toDegrees(orientationValues[1]);  // X 축 회전
            roll = (float) Math.toDegrees(orientationValues[2]);   // Y 축 회전
        }
        draw();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    private void draw() {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            canvas.save();

            float centerX = surfaceView.getWidth() / 2.0f;
            float centerY = surfaceView.getHeight() / 2.0f;
            float rectWidth = 300;
            float rectHeight = 600;

            canvas.translate(centerX, centerY);
            canvas.rotate(roll);

            canvas.drawRect(-rectWidth / 2, -rectHeight / 2, rectWidth / 2, rectHeight / 2, paint);
            canvas.restore();

            holder.unlockCanvasAndPost(canvas);
        }
    }
}

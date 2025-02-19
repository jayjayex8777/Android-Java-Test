package com.example.apptest3;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private BallView ballView;
    private GestureDetector gestureDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;

    private float accelX = 0, accelY = 0; // 기울기 데이터
    private float gyroX = 0, gyroY = 0;   // 자이로 데이터

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        FrameLayout mainLayout = findViewById(R.id.main);
        ballView = new BallView(this);
        mainLayout.addView(ballView);

        // 센서 매니저 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Weight 버튼 (기존 팝업 기능 유지)
        Button btnWeight = findViewById(R.id.btn_weight);
        btnWeight.setOnClickListener(v -> showWeightDialog());

        // GestureDetector 설정 (터치 이벤트는 기존대로 유지)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                ballView.moveBall(-distanceX, -distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                ballView.flingBall(velocityX, velocityY);
                return true;
            }
        });

        // 터치 이벤트 리스너 설정
        mainLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    // 센서 값 변경 감지
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0]; // X축 기울기
            accelY = event.values[1]; // Y축 기울기
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
        }

        // 공의 가속도 적용 (폰 기울기에 따라 이동)
        ballView.applySensorMovement(-accelX, accelY);
    }

    // 센서 등록 및 해제
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

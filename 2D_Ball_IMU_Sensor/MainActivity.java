package com.example.apptest3;

import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private BallView ballView;
    private GestureDetector gestureDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;

    private float accelX = 0, accelY = 0; // 가속도 센서 값
    private float gyroX = 0, gyroY = 0;   // 자이로 센서 값

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

        // Weight 버튼 (무게 설정 팝업)
        Button btnWeight = findViewById(R.id.btn_weight);
        btnWeight.setOnClickListener(v -> showWeightDialog());

        // GestureDetector 설정 (터치 제어)
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

    // Weight 버튼을 눌렀을 때 팝업 창 띄우기
    private void showWeightDialog() {
        Dialog weightDialog = new Dialog(this);
        weightDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        weightDialog.setContentView(R.layout.dialog_weight);
        weightDialog.setCancelable(true);

        EditText etMass = weightDialog.findViewById(R.id.et_mass);
        Button btnSave = weightDialog.findViewById(R.id.btn_save_mass);

        btnSave.setOnClickListener(v -> {
            String massInput = etMass.getText().toString();
            if (!massInput.isEmpty()) {
                try {
                    float newMass = Float.parseFloat(massInput);
                    if (newMass > 0) {
                        ballView.setBallMass(newMass);
                        Toast.makeText(this, "Mass set to: " + newMass, Toast.LENGTH_SHORT).show();
                        weightDialog.dismiss(); // 팝업 닫기
                    } else {
                        Toast.makeText(this, "Enter a positive number!", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        weightDialog.show();
    }

    // 센서 값 변경 감지 (가속도 센서 및 자이로 센서)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0]; // X축 기울기
            accelY = event.values[1]; // Y축 기울기
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
        }

        // 공의 움직임을 센서 기반으로 업데이트
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

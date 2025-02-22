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
import android.widget.ToggleButton;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private BallView ballView;
    private GestureDetector gestureDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean sensorMode = false; // 기본 모드는 터치 모드
    private final float[] gravity = new float[3]; // 중력 가속도 저장 배열

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
        }

        // Weight 버튼 (무게 설정 팝업)
        Button btnWeight = findViewById(R.id.btn_weight);
        btnWeight.setOnClickListener(v -> showWeightDialog());

        // Toggle 버튼 (모드 전환)
        ToggleButton toggleMode = findViewById(R.id.toggle_mode);
        toggleMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sensorMode = isChecked;
            if (sensorMode) {
                Toast.makeText(this, "Sensor Mode Activated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Touch Mode Activated", Toast.LENGTH_SHORT).show();
            }
        });

        // GestureDetector 설정 (터치 제어)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return !sensorMode; // Sensor Mode일 때 터치 비활성화
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!sensorMode) ballView.moveBall(-distanceX, -distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!sensorMode) ballView.flingBall(velocityX, velocityY);
                return true;
            }
        });

        mainLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

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
                        weightDialog.dismiss();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensorMode && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float alpha = 0.8f; // 중력 필터링 계수

            // 원본 가속도 값
            float rawX = event.values[0];
            float rawY = event.values[1];
            float rawZ = event.values[2];

            // 저역 통과 필터 적용 (중력 성분 분리)
            gravity[0] = alpha * gravity[0] + (1 - alpha) * rawX;
            gravity[1] = alpha * gravity[1] + (1 - alpha) * rawY;
            gravity[2] = alpha * gravity[2] + (1 - alpha) * rawZ;

            // 고역 통과 필터 적용 (중력 제거)
            float accelX = rawX - gravity[0];
            float accelY = rawY - gravity[1];

            // 공의 움직임 적용 (중력 제거된 가속도 사용)
            ballView.applySensorMovement(-accelX, accelY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
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

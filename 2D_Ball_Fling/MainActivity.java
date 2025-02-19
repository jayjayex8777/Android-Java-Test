package com.example.apptest3;

import android.app.Dialog;
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

public class MainActivity extends AppCompatActivity {
    private BallView ballView;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        FrameLayout mainLayout = findViewById(R.id.main);
        ballView = new BallView(this);
        mainLayout.addView(ballView);

        // Weight 버튼
        Button btnWeight = findViewById(R.id.btn_weight);
        btnWeight.setOnClickListener(v -> showWeightDialog());

        // GestureDetector 설정
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

    // 팝업 창 띄우기
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
}

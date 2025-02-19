package com.example.apptest3;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
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

        // 무게 조절 버튼 설정
        Button btnLight = findViewById(R.id.btn_light);
        Button btnMedium = findViewById(R.id.btn_medium);
        Button btnHeavy = findViewById(R.id.btn_heavy);

        btnLight.setOnClickListener(v -> ballView.setBallMass(0.5f));   // 가벼움
        btnMedium.setOnClickListener(v -> ballView.setBallMass(1.0f));  // 보통
        btnHeavy.setOnClickListener(v -> ballView.setBallMass(2.0f));   // 무거움

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
}

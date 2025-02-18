package com.example.apptest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView; // 손가락 터치로 그리는 뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);  // 전체 화면 활성화

        // 프레임 레이아웃 생성 (전체 화면 컨테이너)
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(R.id.main);
        frameLayout.setBackgroundColor(Color.WHITE); // 배경을 흰색으로 설정

        // 격자 뷰 추가
        GridView gridView = new GridView(this);
        frameLayout.addView(gridView);

        // 터치 드로잉 뷰 추가
        drawingView = new DrawingView(this);
        frameLayout.addView(drawingView);

        // Reset 버튼 추가
        Button resetButton = new Button(this);
        resetButton.setText("Reset");
        resetButton.setBackgroundColor(Color.LTGRAY);
        resetButton.setTextColor(Color.BLACK);

        // 버튼의 위치 설정 (좌측 하단)
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.leftMargin = 50;
        buttonParams.bottomMargin = 50;
        buttonParams.width = 200;
        buttonParams.height = 100;
        buttonParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
        frameLayout.addView(resetButton, buttonParams);

        // Reset 버튼 클릭 시 화면 초기화
        resetButton.setOnClickListener(v -> drawingView.clear());

        setContentView(frameLayout);

        // 시스템 바 영역을 고려하여 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(frameLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 내부 클래스: 10x10 격자를 그리는 커스텀 뷰
    private static class GridView extends View {
        private final Paint paint;
        private final int numColumns = 10; // 가로 10개
        private final int numRows = 10;    // 세로 10개

        public GridView(Context context) {
            super(context);
            paint = new Paint();
            paint.setColor(Color.BLACK); // 선 색상 검은색
            paint.setStrokeWidth(2); // 선 두께
        }

        public GridView(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(2);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();

            int cellWidth = width / numColumns;
            int cellHeight = height / numRows;

            // 세로선 그리기
            for (int i = 0; i <= numColumns; i++) {
                float x = i * cellWidth;
                canvas.drawLine(x, 0, x, height, paint);
            }

            // 가로선 그리기
            for (int i = 0; i <= numRows; i++) {
                float y = i * cellHeight;
                canvas.drawLine(0, y, width, y, paint);
            }
        }
    }

    // 내부 클래스: 손가락 터치로 그리는 커스텀 뷰
    private static class DrawingView extends View {
        private final Paint paint;
        private final Path path;

        public DrawingView(Context context) {
            super(context);
            path = new Path();
            paint = new Paint();
            paint.setColor(Color.RED); // 자취 색상: 빨강
            paint.setStrokeWidth(5); // 선 두께
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(path, paint); // 터치 경로를 그림
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }

            invalidate(); // 화면 갱신
            return true;
        }

        // 터치 경로 초기화 (Reset 버튼 클릭 시 호출)
        public void clear() {
            path.reset();
            invalidate();
        }
    }
}

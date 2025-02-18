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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(R.id.main);
        frameLayout.setBackgroundColor(Color.WHITE);

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

        ViewCompat.setOnApplyWindowInsetsListener(frameLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 내부 클래스: 10x10 격자를 그리는 커스텀 뷰
    private static class GridView extends View {
        private final Paint paint;
        private final int numColumns = 10;
        private final int numRows = 10;

        public GridView(Context context) {
            super(context);
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(2);
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

            for (int i = 0; i <= numColumns; i++) {
                float x = i * cellWidth;
                canvas.drawLine(x, 0, x, height, paint);
            }

            for (int i = 0; i <= numRows; i++) {
                float y = i * cellHeight;
                canvas.drawLine(0, y, width, y, paint);
            }
        }
    }

    // 내부 클래스: 터치 속도가 빨라지는 순간 새로운 색으로 변경하는 뷰
    private static class DrawingView extends View {
        private final List<Path> paths = new ArrayList<>();
        private final List<Paint> paints = new ArrayList<>();

        private Path currentPath;
        private Paint currentPaint;
        private boolean speedIncreased = false; // 속도가 증가했는지 체크

        private float lastX, lastY;
        private long lastTime;

        public DrawingView(Context context) {
            super(context);
            initNewPath(Color.BLUE); // 초기 파란색
        }

        private void initNewPath(int color) {
            currentPath = new Path();
            currentPaint = new Paint();
            currentPaint.setStrokeWidth(5);
            currentPaint.setStyle(Paint.Style.STROKE);
            currentPaint.setStrokeJoin(Paint.Join.ROUND);
            currentPaint.setStrokeCap(Paint.Cap.ROUND);
            currentPaint.setColor(color);
            speedIncreased = false; // 새로운 선 시작 시 속도 초기화
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < paths.size(); i++) {
                canvas.drawPath(paths.get(i), paints.get(i));
            }
            if (currentPath != null) {
                canvas.drawPath(currentPath, currentPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            long currentTime = System.currentTimeMillis();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initNewPath(Color.BLUE);
                    currentPath.moveTo(x, y);
                    lastX = x;
                    lastY = y;
                    lastTime = currentTime;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastX;
                    float dy = y - lastY;
                    long dt = currentTime - lastTime;

                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float speed = (dt > 0) ? (distance / dt) * 1000 : 0; // px/sec 단위로 속도 측정

                    // 속도가 빨라지는 순간 새로운 Path 생성하여 색상 유지
                    if (!speedIncreased && speed > 10000) { // 특정 속도 이상이면 색 변경
                        paths.add(currentPath);
                        Paint savedPaint = new Paint(currentPaint);
                        paints.add(savedPaint);

                        // 새로운 Path를 빨간색으로 시작
                        initNewPath(Color.RED);
                        currentPath.moveTo(lastX, lastY);
                        speedIncreased = true;
                    }

                    currentPath.lineTo(x, y);
                    lastX = x;
                    lastY = y;
                    lastTime = currentTime;
                    break;

                case MotionEvent.ACTION_UP:
                    paths.add(currentPath);
                    Paint savedPaintFinal = new Paint(currentPaint);
                    paints.add(savedPaintFinal);
                    initNewPath(Color.BLUE);
                    break;
            }

            invalidate();
            return true;
        }

        // 모든 자취 초기화 (Reset 버튼 클릭 시 호출)
        public void clear() {
            paths.clear();
            paints.clear();
            initNewPath(Color.BLUE);
            invalidate();
        }
    }
}

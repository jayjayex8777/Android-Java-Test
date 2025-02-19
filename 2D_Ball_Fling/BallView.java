package com.example.apptest3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

public class BallView extends View {
    private float ballX, ballY; // 공의 좌표
    private final float ballRadius = 50f; // 공의 반지름
    private final Paint paint; // 공을 그릴 Paint 객체
    private float velocityX = 0, velocityY = 0; // 공의 속도
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int FRAME_RATE = 16; // 60fps를 위한 약 16ms 갱신 속도
    private final float friction = 0.98f; // 공의 감속 효과

    public BallView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        ballX = 300; // 초기 위치 설정
        ballY = 300;
        startBallMovement();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);
    }

    // 사용자가 터치해서 이동하는 경우
    public void moveBall(float dx, float dy) {
        ballX += dx;
        ballY += dy;
        invalidate(); // 화면 갱신
    }

    // 사용자가 빠르게 Fling 했을 때
    public void flingBall(float velocityX, float velocityY) {
        this.velocityX = velocityX / 40; // 속도 조절
        this.velocityY = velocityY / 40;
    }

    // 공의 물리적 이동 처리
    private void startBallMovement() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ballX += velocityX;
                ballY += velocityY;

                // 감속 효과 적용
                velocityX *= friction;
                velocityY *= friction;

                // 너무 작은 속도는 0으로 설정하여 멈춤
                if (Math.abs(velocityX) < 0.1) velocityX = 0;
                if (Math.abs(velocityY) < 0.1) velocityY = 0;

                invalidate(); // 화면 갱신

                // 계속해서 반복 실행
                handler.postDelayed(this, FRAME_RATE);
            }
        }, FRAME_RATE);
    }
}

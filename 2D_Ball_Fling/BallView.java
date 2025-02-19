package com.example.apptest3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.View;
import android.view.Choreographer;

public class BallView extends View {
    private float ballX, ballY; // 공 위치
    private final float ballRadius = 50f; // 공 크기
    private final Paint paint;
    private float velocityX = 0, velocityY = 0; // 공 속도
    private final float friction = 0.98f; // 마찰 계수 (0.98 = 서서히 감속)
    private final float gravity = 0.5f; // 중력 가속도
    private final float bounceFactor = 0.7f; // 탄성 계수 (벽 충돌 시 속도 유지율)
    private int screenWidth, screenHeight; // 화면 크기

    public BallView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        ballX = 300; // 초기 위치
        ballY = 300;
        startAnimationLoop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);
    }

    // 터치 드래그 이동
    public void moveBall(float dx, float dy) {
        ballX += dx;
        ballY += dy;
        velocityX = 0; // 이동 중에는 기존 속도를 초기화
        velocityY = 0;
        invalidate();
    }

    // Fling 동작 시 빠르게 이동
    public void flingBall(float velocityX, float velocityY) {
        this.velocityX = velocityX / 30; // 속도를 조절하여 너무 빠르지 않도록 함
        this.velocityY = velocityY / 30;
    }

    // 공의 움직임 업데이트 (애니메이션 루프)
    private void startAnimationLoop() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                updatePhysics();
                invalidate();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    // 물리 효과 적용
    private void updatePhysics() {
        ballX += velocityX;
        ballY += velocityY;
        velocityY += gravity; // 중력 적용

        // 좌우 벽 충돌 처리
        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            velocityX = -velocityX * bounceFactor; // 반사 효과
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            velocityX = -velocityX * bounceFactor;
        }

        // 상하 벽 충돌 처리 (바닥 포함)
        if (ballY - ballRadius < 0) {
            ballY = ballRadius;
            velocityY = -velocityY * bounceFactor;
        } else if (ballY + ballRadius > screenHeight) {
            ballY = screenHeight - ballRadius;
            velocityY = -velocityY * bounceFactor;

            // 바닥에 닿으면 마찰력 추가 (서서히 멈추도록)
            velocityX *= 0.9f;
        }

        // 마찰 적용 (점점 느려짐)
        velocityX *= friction;
        velocityY *= friction;

        // 너무 느려지면 멈춤
        if (Math.abs(velocityX) < 0.1) velocityX = 0;
        if (Math.abs(velocityY) < 0.1) velocityY = 0;
    }
}

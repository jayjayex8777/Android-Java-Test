package com.example.apptest3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.Choreographer;

public class BallView extends View {
    private float ballX, ballY; // 공 위치
    private final float ballRadius = 50f; // 공 반지름
    private final Paint paint;
    private float velocityX = 0, velocityY = 0; // 공 속도
    private final float friction = 0.98f;
    private final float gravity = 0.5f;
    private final float bounceFactor = 0.7f;
    private int screenWidth = 0, screenHeight = 0; // 화면 크기 저장
    private boolean isTouched = false; // 사용자가 터치했는지 여부

    public BallView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        startAnimationLoop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // 공을 화면 정중앙에 배치
        ballX = screenWidth / 2f;
        ballY = screenHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);
    }

    public void moveBall(float dx, float dy) {
        isTouched = true; // 사용자가 터치했으므로 중력 적용 시작
        ballX += dx;
        ballY += dy;
        velocityX = 0;
        velocityY = 0;
        invalidate();
    }

    public void flingBall(float velocityX, float velocityY) {
        isTouched = true; // Fling 시에도 중력 적용 시작
        this.velocityX = velocityX / 30;
        this.velocityY = velocityY / 30;
    }

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

    private void updatePhysics() {
        if (isTouched) {
            velocityY += gravity; // 중력은 터치 후부터 적용
        }

        ballX += velocityX;
        ballY += velocityY;

        // 좌우 벽 충돌 처리
        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            velocityX = -velocityX * bounceFactor;
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

            // 바닥에서 감속 효과 적용
            velocityX *= 0.9f;
        }

        // 마찰력 적용 (속도 점차 감소)
        velocityX *= friction;
        velocityY *= friction;

        // 너무 작은 속도는 0으로 설정하여 멈춤
        if (Math.abs(velocityX) < 0.1) velocityX = 0;
        if (Math.abs(velocityY) < 0.1) velocityY = 0;
    }
}

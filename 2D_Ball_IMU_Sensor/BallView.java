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
    private final float friction = 0.98f; // 마찰 계수 (서서히 멈추도록)
    private final float bounceFactor = 0.7f; // 벽 충돌 후 반사 비율
    private final float bounceFriction = 0.85f; // 벽 충돌 후 감속 비율
    private int screenWidth = 0, screenHeight = 0; // 화면 크기
    private float mass = 1.0f; // 공의 무게
    private final float sensorSensitivity = 1.5f; // 센서 감도 조절
    private final float maxSpeed = 10f; // 공의 최대 속도 제한

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

        // 공을 화면 중앙에 배치
        ballX = screenWidth / 2f;
        ballY = screenHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);
    }

    public void setBallMass(float newMass) {
        this.mass = newMass;
    }

    public void moveBall(float dx, float dy) {
        ballX += dx / mass;
        ballY += dy / mass;
        velocityX = 0;
        velocityY = 0;
        invalidate();
    }

    public void flingBall(float velocityX, float velocityY) {
        this.velocityX = velocityX / (30 * mass);
        this.velocityY = velocityY / (30 * mass);
    }

    public void applySensorMovement(float dx, float dy) {
        float threshold = 0.2f; // 노이즈 제거를 위한 최소 임계값

        // 작은 값은 노이즈로 간주하고 0 처리
        if (Math.abs(dx) < threshold) dx = 0;
        if (Math.abs(dy) < threshold) dy = 0;

        velocityX += dx * sensorSensitivity;
        velocityY += dy * sensorSensitivity;

        // 최대 속도 제한
        velocityX = Math.max(-maxSpeed, Math.min(maxSpeed, velocityX));
        velocityY = Math.max(-maxSpeed, Math.min(maxSpeed, velocityY));
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
        ballX += velocityX;
        ballY += velocityY;

        // 좌우 벽 충돌 처리
        if (ballX - ballRadius < 0) {
            ballX = ballRadius;
            velocityX = -velocityX * bounceFactor; // 반사
            velocityX *= bounceFriction; // 감속
        } else if (ballX + ballRadius > screenWidth) {
            ballX = screenWidth - ballRadius;
            velocityX = -velocityX * bounceFactor;
            velocityX *= bounceFriction;
        }

        // 상하 벽 충돌 처리
        if (ballY - ballRadius < 0) {
            ballY = ballRadius;
            velocityY = -velocityY * bounceFactor;
            velocityY *= bounceFriction;
        } else if (ballY + ballRadius > screenHeight) {
            ballY = screenHeight - ballRadius;
            velocityY = -velocityY * bounceFactor;
            velocityY *= bounceFriction;
        }

        // 마찰력 적용 (서서히 멈춤)
        velocityX *= friction;
        velocityY *= friction;

        // 너무 작은 속도는 자동으로 0 처리하여 멈춤
        if (Math.abs(velocityX) < 0.1) velocityX = 0;
        if (Math.abs(velocityY) < 0.1) velocityY = 0;
    }
}

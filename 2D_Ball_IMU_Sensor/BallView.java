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
    private final float bounceFactor = 0.7f;
    private int screenWidth = 0, screenHeight = 0;
    private float mass = 1.0f; // 공의 무게 (기본값)
    private boolean isTouched = false;
    private final float sensorSensitivity = 1.5f; // 센서 이동 감도
    private final float maxSpeed = 10f; // 센서 최대 속도

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
        isTouched = true;
        ballX += dx / mass;
        ballY += dy / mass;
        velocityX = 0;
        velocityY = 0;
        invalidate();
    }

    public void flingBall(float velocityX, float velocityY) {
        isTouched = true;
        this.velocityX = velocityX / (30 * mass);
        this.velocityY = velocityY / (30 * mass);
    }

    // 센서 데이터 기반 이동
    public void applySensorMovement(float dx, float dy) {
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

        // 벽 충돌 처리
        if (ballX - ballRadius < 0 || ballX + ballRadius > screenWidth) velocityX = -velocityX * bounceFactor;
        if (ballY - ballRadius < 0 || ballY + ballRadius > screenHeight) velocityY = -velocityY * bounceFactor;

        // 마찰력 적용
        velocityX *= friction;
        velocityY *= friction;
    }
}

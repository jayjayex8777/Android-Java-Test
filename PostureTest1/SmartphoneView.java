package com.example.posturetest1;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SmartphoneView extends View {

    private Paint paint;
    private Camera camera;
    private Matrix matrix;
    private float rotationYaw = 0f;
    private float rotationPitch = 0f;
    private float rotationRoll = 0f;
    private float translateX = 0f;
    private float translateY = 0f;

    public SmartphoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        camera = new Camera();
        matrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 캔버스 이동 (기울기에 따라 위치 변화 적용)
        canvas.save();
        canvas.translate(width / 2 + translateX, height / 2 + translateY);

        // Camera를 사용하여 X, Y, Z 축 회전 적용
        camera.save();
        camera.rotateX(rotationPitch);
        camera.rotateY(rotationYaw);
        camera.rotateZ(rotationRoll);
        camera.getMatrix(matrix);
        camera.restore();

        // 변환 적용
        matrix.preTranslate(-width / 2, -height / 2);
        matrix.postTranslate(width / 2, height / 2);
        canvas.concat(matrix);

        // 스마트폰 직사각형 그리기
        float rectWidth = 200;
        float rectHeight = 400;
        canvas.drawRect(-rectWidth / 2, -rectHeight / 2, rectWidth / 2, rectHeight / 2, paint);

        canvas.restore();
    }

    public void updateRotation(float yaw, float pitch, float roll) {
        this.rotationYaw = yaw * 10;   // 감도 조정
        this.rotationPitch = pitch * 10;
        this.rotationRoll = roll * 10;

        // 위치 이동 (기울기에 따라 화면 위치 조정)
        this.translateX = roll * 5;
        this.translateY = pitch * 5;

        invalidate();
    }
}

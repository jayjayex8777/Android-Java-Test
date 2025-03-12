package com.example.posturetest1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SmartphoneView extends View {

    private Paint paint;
    private float rotationX = 0f;
    private float rotationY = 0f;

    public SmartphoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 가운데 정렬을 위해 캔버스 이동
        canvas.save();
        canvas.translate(width / 2, height / 2);
        canvas.rotate(rotationX, 1, 0, 0);
        canvas.rotate(rotationY, 0, 1, 0);

        // 스마트폰 직사각형 그리기
        float rectWidth = 200;
        float rectHeight = 400;
        canvas.drawRect(-rectWidth / 2, -rectHeight / 2, rectWidth / 2, rectHeight / 2, paint);

        canvas.restore();
    }

    public void updateRotation(float pitch, float roll) {
        this.rotationX = pitch * 5; // 감도 조정
        this.rotationY = roll * 5;
        invalidate(); // 화면 다시 그리기
    }
}

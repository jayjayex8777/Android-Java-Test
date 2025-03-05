package com.example.objectselect1;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

public class CustomRecyclerView extends RecyclerView {
    private static final float FLING_SPEED_FACTOR = 3.0f; // 🚀 플링 속도를 3배 증가

    public CustomRecyclerView(Context context) {
        super(context);
    }

    public CustomRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        velocityX *= FLING_SPEED_FACTOR; // 🚀 속도 증가
        velocityY *= FLING_SPEED_FACTOR;
        return super.fling(velocityX, velocityY);
    }
}

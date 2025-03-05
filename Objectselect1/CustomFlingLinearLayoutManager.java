package com.example.objectselect1;

import android.content.Context;
import android.util.DisplayMetrics;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomFlingLinearLayoutManager extends LinearLayoutManager {

    private static final float FLING_SPEED_FACTOR = 3.0f; // 🚀 플링 속도를 3배 증가

    public CustomFlingLinearLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        velocityX *= FLING_SPEED_FACTOR; // 🚀 속도 3배 증가
        return super.fling(velocityX, velocityY);
    }

    @Override
    protected float computeScrollSpeedFactor(DisplayMetrics displayMetrics) {
        return super.computeScrollSpeedFactor(displayMetrics) / FLING_SPEED_FACTOR;
    }
}

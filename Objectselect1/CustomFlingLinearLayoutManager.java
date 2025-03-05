package com.example.objectselect1;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomFlingLinearLayoutManager extends LinearLayoutManager {

    private static final float FLING_SPEED_FACTOR = 3.0f; // 🚀 플링 속도를 3배 증가

    public CustomFlingLinearLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        velocityX *= FLING_SPEED_FACTOR; // 🚀 속도 증가
        velocityY *= FLING_SPEED_FACTOR; 
        return super.fling(velocityX, velocityY);
    }
}

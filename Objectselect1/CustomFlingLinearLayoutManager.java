package com.example.objectselect1;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomFlingLinearLayoutManager extends LinearLayoutManager {
    private static final float FLING_SPEED_FACTOR = 2.0f; // ✅ 플링 속도 증가

    public CustomFlingLinearLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return super.scrollHorizontallyBy((int) (dx * FLING_SPEED_FACTOR), recycler, state);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }
}

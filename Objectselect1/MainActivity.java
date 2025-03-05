package com.example.objectselect1;

import android.content.Context;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomSpeedLinearLayoutManager extends LinearLayoutManager {
    private Scroller scroller;

    public CustomSpeedLinearLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
        scroller = new Scroller(context, new DecelerateInterpolator());
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        RecyclerView.SmoothScroller smoothScroller = new RecyclerView.SmoothScroller(recyclerView.getContext()) {
            @Override
            protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                int time = Math.max(100, Math.abs(dx) * 3); // 🚀 이동 속도 증가 (100 ~ 300ms)
                action.update(dx, dy, time, scroller);
            }
        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}

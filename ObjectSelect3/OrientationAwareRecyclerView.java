package com.example.objectselect3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class OrientationAwareRecyclerView extends RecyclerView {

    private float lastX = 0.0f;
    private float lastY = 0.0f;
    private boolean scrolling = false;

    public OrientationAwareRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public OrientationAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OrientationAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        final LayoutManager lm = getLayoutManager();
        if (lm == null) {
            return super.onInterceptTouchEvent(e);
        }

        boolean allowScroll = true;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                if (scrolling) {
                    MotionEvent newEvent = MotionEvent.obtain(e);
                    newEvent.setAction(MotionEvent.ACTION_UP);
                    return super.onInterceptTouchEvent(newEvent);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(e.getX() - lastX);
                float dy = Math.abs(e.getY() - lastY);
                allowScroll = dy > dx ? lm.canScrollVertically() : lm.canScrollHorizontally();
                break;
        }

        return allowScroll && super.onInterceptTouchEvent(e);
    }
}

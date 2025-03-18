package com.example.objectselect3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.OverScroller;
import android.view.View;

public class Custom2DScrollView extends View {
    private float lastX, lastY;
    private OverScroller scroller;
    private boolean isScrollingHorizontal = false;
    private boolean isScrollingVertical = false;

    public Custom2DScrollView(Context context) {
        super(context);
        init();
    }

    public Custom2DScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scroller = new OverScroller(getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                isScrollingHorizontal = false;
                isScrollingVertical = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastX;
                float dy = event.getY() - lastY;

                if (Math.abs(dx) > Math.abs(dy)) {
                    isScrollingHorizontal = true;
                } else {
                    isScrollingVertical = true;
                }

                if (isScrollingHorizontal) {
                    scrollBy((int) -dx, 0);
                } else if (isScrollingVertical) {
                    scrollBy(0, (int) -dy);
                }

                lastX = event.getX();
                lastY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
        }
        return super.onTouchEvent(event);
    }
}

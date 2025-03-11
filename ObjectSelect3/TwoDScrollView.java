package com.example.objectselect3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.OverScroller;

public class TwoDScrollView extends FrameLayout {
    private OverScroller scroller;
    private VelocityTracker velocityTracker;
    private int lastX;
    private int lastY;
    private int touchSlop;
    private int minimumVelocity;
    private int maximumVelocity;

    public TwoDScrollView(Context context) {
        super(context);
        init(context);
    }

    public TwoDScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TwoDScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 모든 터치 이벤트를 가로채서 처리합니다.
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                lastX = x;
                lastY = y;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                int dx = lastX - x;
                int dy = lastY - y;
                if (Math.abs(dx) < touchSlop && Math.abs(dy) < touchSlop) {
                    break;
                }
                scrollBy(dx, dy);
                lastX = x;
                lastY = y;
                velocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                int vx = (int) velocityTracker.getXVelocity();
                int vy = (int) velocityTracker.getYVelocity();
                if (Math.abs(vx) > minimumVelocity || Math.abs(vy) > minimumVelocity) {
                    int maxX = getChildAt(0).getWidth() - getWidth();
                    int maxY = getChildAt(0).getHeight() - getHeight();
                    scroller.fling(getScrollX(), getScrollY(), -vx, -vy, 0, Math.max(0, maxX), 0, Math.max(0, maxY));
                    invalidate();
                }
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        }
    }
}

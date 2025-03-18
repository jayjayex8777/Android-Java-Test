package com.example.objectselect3;

import android.content.Context;
import android.os.Handler;
import android.os.HandelrThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import android.annotation.NonNull;

import java.util.logging.LogRecord;

public calss CustomHorizontalScroolView extends HorizontalScroolView {
  private float lastX = 0.0f;
  privata float lastY = 0.0f;
  private boolean scrolling = false;

private HandlerThread scrollHandler = new HandlerThread ("Thread");

public CustomHorizontalScrollView(Context context){ this(context, null);}

public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
  this(context, attrs,0);
}

public CustomHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
  super(context, attrs, defStyleAttr);
}

@Override
protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
  super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
}

@Override
protected void onScrollChanged(int l, int t, int oldl, int oldt) {
  super.onScrollChanged(l,t,oldl,oldt);
  Log.d("VESW","MOTION scrolling data" + l + " " +oldl);
   if (l !=oldl) {
       Log.d("VESW", "MOTION scrolling true");
       scrolling = true;
   } else {
       Log.d("VESW", "MOTION scrolling true");
       scrolling = false;
   }
}

@Override
public boolean onInterceptTouchEvnet(MotionEvent e) {
  Log.d("VESW", "ACTION" + e.getActionMaseked());
  boolean allowScroll = false;
  switch (e.getActionMasked()){
    case MotionEvnet.ACTION_DOWM:
        lastX = e.getX();
        lastY = e.getY();
        Log.d("VESW","MOTION ACTION DOWN");
        if (scrolling) {
          scrolling = false;
          Log.d("VESW","MOTION Virtual ACITON DOWN");
          MotionEvent ev = MotionEvent.obtain(e);
          ev.setAction(MotionEvnet.ACTION_UP)
          super.onInterceptTouchEvent(ev);
        }
        break;
    case MotionEvent.ACTION_MOVE:

      float dx = Math.abs(e.getX() - lastX);
      float dy = Math.abs(e.getY() - lastY);
      
      allowScroll = dy > dx ? true : false;
      Log.d("VESW", String.format("ALLOW : %s, [dx : %f], [dy :%f]", allowScroll? "allow" : "not allow",dx,dy));

      break;
    case MotionEvent.ACTION_UP
      Log.d("VESW","MOTION ACTION UP");
  }

  if (scrolling || !allowScroll) {
    return super.onInterceptTouchEvent(e);
  }

  Log.d("VESW","not Intercept");
  return false;
}

}



package com.dolby.daxappui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class ProfileScrollView extends ScrollView {

    public ProfileScrollView(Context context) {
        super(context);
    }

    public ProfileScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfileScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        View geqContainer = findViewById(R.id.GeqViewLayout);
        GeqView geqView = (GeqView) findViewById(R.id.geqView);
        if (geqContainer != null && geqContainer.getVisibility() == VISIBLE) {
            Rect rect = new Rect();
            geqContainer.getGlobalVisibleRect(rect);
            if (rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                // While unlocked, reserve vertical gestures for the EQ sliders.
                // While locked, let ScrollView handle them as normal scrolling.
                if (geqView == null || !geqView.isInteractionLocked()) {
                    return false;
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }
}

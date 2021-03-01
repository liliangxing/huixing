package com.hx.huixing.webview;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * 主要解决viewPager嵌套webView横向滚动问题
 */

public class ExtendedWebView extends WebView {

    public ExtendedWebView(Context context) {
        super(context);
    }

    public ExtendedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (clampedX) {
            ViewParent viewParent = findViewParentIfNeeds(this);
            viewParent.requestDisallowInterceptTouchEvent(false);
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ViewParent viewParent = findViewParentIfNeeds(this);
            if (viewParent != null)
                viewParent.requestDisallowInterceptTouchEvent(true);
        }
        return super.onTouchEvent(event);
    }

    private ViewParent findViewParentIfNeeds(View tag) {
        ViewParent parent = tag.getParent();
        if (parent == null) {
            return null;
        }
        if (parent instanceof ViewPager || parent instanceof AbsListView || parent instanceof ScrollView || parent instanceof HorizontalScrollView) {
            return parent;
        } else {
            if (parent instanceof View) {
                findViewParentIfNeeds((View) parent);
            } else {
                return parent;
            }
        }
        return parent;
    }
}
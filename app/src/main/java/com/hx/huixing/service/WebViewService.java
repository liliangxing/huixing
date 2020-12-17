package com.hx.huixing.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.webkit.WebView;

public class WebViewService extends Service {
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        public WebViewService getService() {
            return WebViewService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public static WebView mWebView;
    public WebViewService() {
        this(null);
    }
    public WebViewService(WebView view) {
        mWebView = view;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

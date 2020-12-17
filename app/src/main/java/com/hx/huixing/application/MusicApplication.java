package com.hx.huixing.application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.mihua.thirdplatform.sharesdk.ShareManager;

import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.service.LocalService;

/**
 * 自定义Application
 * Created by wcy on 2015/11/27.
 */
public class MusicApplication extends Application {

    private static MusicActivity mainActivity = null;

    public static MusicActivity getMainActivity() {
        return mainActivity;
    }

    public static void setMainActivity(MusicActivity activity) {
        mainActivity = activity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCache.get().init(this);
        ForegroundObserver.init(this);
        if (isMainProcess(getApplicationContext())) {
            startService(new Intent(this, LocalService.class));
        } else {
            return;
        }
        ShareManager.initSDK(this);
    }

    /**
     * 获取当前进程名
     */
    public String getCurrentProcessName(Context context) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) context.getApplicationContext().getSystemService
                (Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

    public boolean isMainProcess(Context context) {
        /**
         * 是否为主进程
         */
        boolean isMainProcess;
        isMainProcess = context.getApplicationContext().getPackageName().equals
                (getCurrentProcessName(context));
        return isMainProcess;
    }
}

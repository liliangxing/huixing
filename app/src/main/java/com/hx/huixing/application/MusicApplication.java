package com.hx.huixing.application;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.hx.huixing.receiver.Receiver1;
import com.hx.huixing.receiver.Receiver2;
import com.hx.huixing.service.Service1;
import com.hx.huixing.service.Service2;
import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.mihua.thirdplatform.sharesdk.ShareManager;

import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.service.LocalService;

/**
 * 自定义Application
 * Created by wcy on 2015/11/27.
 */
public class MusicApplication extends DaemonApplication {

    private static MusicActivity mainActivity = null;

    public static MusicActivity getMainActivity() {
        return mainActivity;
    }

    public static void setMainActivity(MusicActivity activity) {
        mainActivity = activity;
    }

    @SuppressLint("SimpleDateFormat")
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
        context = getApplicationContext();
        startService(new Intent(this, Service1.class));
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

    private volatile static Context context;

    public static Context getContext() { return context; }
    /**
     * you can override this method instead of {@link android.app.Application attachBaseContext}
     * @param base
     */
    @Override
    public void attachBaseContextByDaemon(Context base) {
        super.attachBaseContextByDaemon(base);
    }


    /**
     * give the configuration to lib in this callback
     * @return
     */
    @Override
    protected DaemonConfigurations getDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                "com.ordolabs.clipit:process1",
                Service1.class.getCanonicalName(),
                Receiver1.class.getCanonicalName());

        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                "com.ordolabs.clipit:process2",
                Service2.class.getCanonicalName(),
                Receiver2.class.getCanonicalName());

        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        //return new DaemonConfigurations(configuration1, configuration2);//listener can be null
        return new DaemonConfigurations(configuration1, configuration2, listener);
    }


    class MyDaemonListener implements DaemonConfigurations.DaemonListener{
        @Override
        public void onPersistentStart(Context context) {
        }

        @Override
        public void onDaemonAssistantStart(Context context) {
        }

        @Override
        public void onWatchDaemonDaed() {
        }
    }
}

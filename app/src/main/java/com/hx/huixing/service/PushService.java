package com.hx.huixing.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hx.huixing.receiver.AlarmReceive;
import com.hx.huixing.storage.db.SQLiteDatabase;

public class PushService extends Service {
    // 守护进程 Service ID
    private final static int DAEMON_SERVICE_ID = -5121;
    private final static String TAG = PushService.class.getSimpleName();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PushService.PlayBinder();
    }
    public class PlayBinder extends Binder {
        public PushService getService() {
            return PushService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: " + getClass().getSimpleName());
        // 利用 Android 漏洞提高进程优先级，
        startForeground(DAEMON_SERVICE_ID, new Notification());
        // 当 SDk 版本大于18时，需要通过内部 Service 类启动同样 id 的 Service
        if (Build.VERSION.SDK_INT >= 18) {
            Intent innerIntent = new Intent(this, DaemonInnerService.class);
            startService(innerIntent);
        }
        ar(getApplicationContext());
        aq(getApplicationContext());
        return START_STICKY;
    }

    private static final int ONE_Miniute=30*1000;
    public static void aq(Context context) {
        //通过AlarmManager定时启动广播
        AlarmManager alarmManager= (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent i=new Intent(context, AlarmReceive.class);
        PendingIntent pIntent= PendingIntent.getBroadcast(context,0,i, SQLiteDatabase.CREATE_IF_NECESSARY);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ((long) ONE_Miniute), (long) ONE_Miniute, pIntent);
    }

    public static void ar(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceive.class), SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
        if (broadcast != null) {
            alarmManager.cancel(broadcast);
            broadcast.cancel();
        }
    }

    /**
     * 实现一个内部的 Service，实现让后台服务的优先级提高到前台服务，这里利用了 android 系统的漏洞，
     * 不保证所有系统可用，测试在7.1.1 之前大部分系统都是可以的，不排除个别厂商优化限制
     */
    public static class DaemonInnerService extends Service {

        @Override public void onCreate() {
            Log.i(TAG, "DaemonInnerService -> onCreate");
            super.onCreate();
        }

        @Override public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(TAG, "DaemonInnerService -> onStartCommand");
            startForeground(DAEMON_SERVICE_ID, new Notification());
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override public IBinder onBind(Intent intent) {
            // TODO: Return the communication channel to the service.
            throw new UnsupportedOperationException("onBind 未实现");
        }

        @Override public void onDestroy() {
            Log.i(TAG, "DaemonInnerService -> onDestroy");
            super.onDestroy();
        }
    }
}
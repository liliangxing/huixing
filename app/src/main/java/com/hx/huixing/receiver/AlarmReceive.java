package com.hx.huixing.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hx.huixing.utils.SystemUtils;
import com.hx.huixing.utils.ViewUtils;

public class AlarmReceive extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //判断app进程是否存活
        if(SystemUtils.isAppALive(context, "com.hx.huixing")){
        }else {
            ViewUtils.openWithPackageName("com.hx.huixing",context);
        }
    }
}
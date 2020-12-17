package com.hx.huixing.supervisor;

import android.content.Intent;
import android.support.annotation.Nullable;
import com.hx.huixing.service.ForegroundService;

public class SupervisorForegroundService extends ForegroundService {

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        SupervisorManager.inst(this).startKeepAlive();
        return super.onStartCommand(intent, flags, startId);
    }
}

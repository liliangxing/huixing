package com.hx.huixing.supervisor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * native进程保活，原理是:
 * 1、通过 exec 直接执行 so 文件, 开启一个native进程；
 * 2、native进程监听客户端的一个noPushFile 文件；
 * 3、客户端进程存活的时候，会删除这个文件, 客户端进程死亡的时候，会创建这个文件；
 * 4、当native进程发现这个文件存在了，说明客户端进程死亡了，则通过 am startservice 唤起 {@link SupervisorForegroundService}
 */
public class SupervisorManager {

    private static final String TAG = "NativeManager";
    private static final String SUPERVISOR_FILE = "supervisor";

    private Context mContext;
    private AtomicBoolean mIsInited = new AtomicBoolean(false);
    private String mSupervisorFilePath;
    private volatile java.lang.Process mNativeProcess;

    private volatile static SupervisorManager sInstance;

    private SupervisorManager(Context context){
        mContext = context.getApplicationContext();
    }

    public static SupervisorManager inst(Context context){
        if(sInstance == null){
            synchronized (SupervisorManager.class){
                if(sInstance == null){
                    sInstance = new SupervisorManager(context);
                }
            }
        }
        return sInstance;
    }

    public void startKeepAlive() {
        try {
            if(mIsInited.getAndSet(true)){
                return;
            }
            copyFileFormAssets();
            // 开始5.0 以下的 c进程保活
            startNativeSupervisorProcess();
        } catch (Throwable e) {
            e.printStackTrace();
            mIsInited.set(false);
        }
    }

    public void stopKeepAlive() {
        try {
            mNativeProcess.destroy();
            mIsInited.set(false);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void startNativeSupervisorProcess(){
        if(TextUtils.isEmpty(mSupervisorFilePath)){
            return;
        }
        //提高权限
        exec("chmod 777 " + mSupervisorFilePath);

        String supervisorFile = mSupervisorFilePath;
        String packageName = mContext.getPackageName();
        String serviceName = SupervisorForegroundService.class.getCanonicalName();
        String processName = getCurProcessName(mContext);
        String appDataPath = mContext.getApplicationInfo().dataDir;
        StringBuilder sb = new StringBuilder();
        sb.append(supervisorFile).append(" ")
                .append(packageName).append(" ")
                .append(serviceName).append(" ")
                .append(processName).append(" ")
                .append(appDataPath).append(" ");
        String userSerial = getUserSerial();
        if (userSerial != null) {
            sb.append(userSerial);
        }
        exec(sb.toString());
    }

    private String getCurProcessName(Context context) {
        try{
            int pid = Process.myPid();
            ActivityManager am =(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                if (process.pid == pid) {
                    return process.processName;
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void copyFileFormAssets() throws IOException {
        File destDir = mContext.getDir("bin", Context.MODE_PRIVATE);
        File supervisorFile = new File(destDir, SUPERVISOR_FILE);
        if(supervisorFile.exists()){
            Log.d(TAG, "supervisorFile has existed");
            mSupervisorFilePath = supervisorFile.getAbsolutePath();
            return;
        }
        InputStream is = null;
        OutputStream os = null;
        try{
            AssetManager assetManager = mContext.getAssets();
            String supervisorFilePathInAssets = Build.CPU_ABI + File.separator + SUPERVISOR_FILE;
            is = assetManager.open(supervisorFilePathInAssets);
            os = new FileOutputStream(supervisorFile);
            byte[] buff = new byte[1024];
            while (is.read(buff) > 0){
                os.write(buff);
            }
            mSupervisorFilePath = supervisorFile.getAbsolutePath();
            Log.d(TAG, "copy supervisorFile from assets");
        }finally {
            if(is != null){
                is.close();
            }
            if(os != null){
                os.close();
            }
        }
    }

    /**
     * Executes UNIX command.
     * @throws IOException
     */
    private void exec(final String command){
        Log.d(TAG, "cmd = " + command);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int ret = -1;
                try {
                    mNativeProcess = Runtime.getRuntime().exec(command, null, null);
                    ret = mNativeProcess.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                    mIsInited.set(false);
                }
                Log.d(TAG, "exec, ret = " + ret);
            }
        });
    }

    /**
     * 获取当前用户在linux系统的唯一序号
     */
    public String getUserSerial() {
        if (mContext == null) {
            return null;
        }
        Object userManager = mContext.getSystemService("user");
        if (userManager == null) {
            return null;
        }
        try {
            Method myUserHandleMethod = Process.class.getMethod("myUserHandle", (Class<?>[]) null);
            Object myUserHandle = myUserHandleMethod.invoke(Process.class, (Object[]) null);
            Method getSerialNumberForUser = userManager.getClass().getMethod("getSerialNumberForUser",
                    myUserHandle.getClass());
            long userSerial = (Long) getSerialNumberForUser.invoke(userManager, myUserHandle);
            return String.valueOf(userSerial);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}

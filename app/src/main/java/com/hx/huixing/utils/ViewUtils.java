package com.hx.huixing.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.hx.huixing.activity.SubscribeMessageActivity;
import com.hx.huixing.enums.LoadStateEnum;
import com.hx.huixing.model.Music;
import com.hx.huixing.service.PasteCopyService;

/**
 * 视图工具类
 * Created by hzwangchenyan on 2016/1/14.
 */
public class ViewUtils {
    public static void changeViewState(View success, View loading, View fail, LoadStateEnum state) {
        switch (state) {
            case LOADING:
                success.setVisibility(View.GONE);
                loading.setVisibility(View.VISIBLE);
                fail.setVisibility(View.GONE);
                break;
            case LOAD_SUCCESS:
                success.setVisibility(View.VISIBLE);
                loading.setVisibility(View.GONE);
                fail.setVisibility(View.GONE);
                break;
            case LOAD_FAIL:
                success.setVisibility(View.GONE);
                loading.setVisibility(View.GONE);
                fail.setVisibility(View.VISIBLE);
                break;
        }
    }
    public static void openWith(Music music, Context context){
        String html = music.getFileName();
        if (!TextUtils.isEmpty(html)) {
            if(!html.contains("douyin.com")){
                String packageName = "com.ss.android.ugc.aweme";
                if(html.contains("v.kuaishou.com")){
                    packageName = "com.smile.gifmaker";
                }else if((html.contains("kuai") && html.contains(".com"))){
                    packageName = "com.kuaishou.nebula";
                }else if(html.contains("weishi.qq.com")){
                    packageName = "com.tencent.weishi";
                }else {
                    ToastUtils.show("无法识别链接：" + JSONObject.toJSONString(html));
                    return;
                }
                PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label", html+"#openWith"));
                openWithPackageName(packageName,context);
            }else {
                SubscribeMessageActivity.createChooser(html, context);
            }
        } else {
            ToastUtils.show("无抖音链接：" + JSONObject.toJSONString(music));
        }
    }

    public static void openWithPackageName(String packageName,Context context){
        try {
            Intent launchIntent = context.getPackageManager().
                    getLaunchIntentForPackage(packageName);
            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

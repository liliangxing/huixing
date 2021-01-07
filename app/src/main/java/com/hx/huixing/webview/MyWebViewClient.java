package com.hx.huixing.webview;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hx.huixing.activity.MainActivity;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.service.PasteCopyService;

/**
 * Created by hzwangchenyan on 2017/2/8.
 */
public class MyWebViewClient extends WebViewClient {

    private ProgressDialog progressDialog;//加载界面的菊花
    private Context context;//加载界面的菊花
    public MyWebViewClient(Context context,ProgressDialog progressDialog){
        this.context = context;
        this.progressDialog = progressDialog;
    }
    //重写页面打开和结束的监听。添加友好，弹出菊花
    /**
     * 界面打开的回调
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        System.out.println("onPageStarted");
        if(progressDialog!=null&&progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        //弹出菊花
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("提示");
        progressDialog.setMessage("正在努力加载……");
        progressDialog.show();

    }
    /**
     * 界面打开完毕的回调
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        System.out.println("onPageFinished:"+url);
        //隐藏菊花:不为空，正在显示。才隐藏
        if(progressDialog!=null&&progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        if(url.endsWith(LocalMusicFragment.FILE_NAME)||url.startsWith("https://www.iesdouyin.com/web/api/v2/aweme/post/")
        ) {
            view.loadUrl("javascript:window.java_obj.getSource('<head>'+" +
                    "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
        }
        if(url.contains("weishi.qq.com")
        ||url.contains("douyin.com")
        ||url.contains("huoshan.com")
        ||url.contains("fw/photo/")){
            view.loadUrl("javascript:setTimeout(function () {window.java_obj.getSource('<head>'+" +
                    "document.getElementsByTagName('html')[0].innerHTML+'</head>');},2000);");
        }

        if(MainActivity.fromPush && !PasteCopyService.hashSetIterator.hasNext()){
            view.loadUrl("about:blank");
            MainActivity.sendHttpRequest(url);
        }
        super.onPageFinished(view, url);
    }
}

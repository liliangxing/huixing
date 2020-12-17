package com.hx.huixing.webview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Created by hzwangchenyan on 2017/2/8.
 */
public class MyWebChromeClient extends WebChromeClient {

    private ProgressDialog progressDialog;//加载界面的菊花
    private Context context;//加载界面的菊花
    private View customView ;
    private Activity activity;
    public MyWebChromeClient(Context context, Activity activity,  ProgressDialog progressDialog){
        this.context = context;
        this.progressDialog = progressDialog;
        this.activity = activity;
    }
    /**
     * 当打开超链接的时候，回调的方法
     * WebView：自己本身mWebView
     * url：即将打开的url
     */
    /**
     * 进度改变的回调
     * WebView：就是本身
     * newProgress：即将要显示的进度
     */
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if(progressDialog!=null&&progressDialog.isShowing())
            progressDialog.setMessage("正在努力加载……"+newProgress+"%");
    }
    /**
     * 重写alert、confirm和prompt的回调
     */
    /**
     * Webview加载html中有alert()执行的时候，回调
     * url:当前Webview显示的url
     * message：alert的参数值
     * JsResult：java将结果回传到js中
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示");
        builder.setMessage(message);//这个message就是alert传递过来的值
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //处理确定按钮了
                result.confirm();//通过jsresult传递，告诉js点击的是确定按钮
            }
        });
        builder.show();

        return true;//自己处理
    }
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示");
        builder.setMessage(message);//这个message就是alert传递过来的值
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //处理确定按钮了
                result.confirm();//通过jsresult传递，告诉js点击的是确定按钮
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //处理取消按钮
                //告诉js点击的是取消按钮
                result.cancel();

            }
        });
        builder.show();
        return true;//自己处理
    }
    /**
     * defaultValue就是prompt的第二个参数值，输入框的默认值
     * JsPromptResult：向js回传数据
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                              final JsPromptResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示");
        builder.setMessage(message);//这个message就是alert传递过来的值
        //添加一个EditText
        final EditText editText = new EditText(context);
        editText.setText(defaultValue);//这个就是prompt 输入框的默认值
        //添加到对话框
        builder.setView(editText);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //获取edittext的新输入的值
                String newValue = editText.getText().toString().trim();
                //处理确定按钮了
                result.confirm(newValue);//通过jsresult传递，告诉js点击的是确定按钮(参数就是输入框新输入的值，我们需要回传到js中)
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //处理取消按钮
                //告诉js点击的是取消按钮
                result.cancel();

            }
        });
        builder.show();
        return true;//自己处理
    }
}

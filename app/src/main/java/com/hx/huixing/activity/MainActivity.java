package com.hx.huixing.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;

import com.example.ijkplayer.controller.BaseVideoController;
import com.example.ijkplayer.player.IjkVideoView;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.zhy.http.okhttp.OkHttpUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hx.huixing.R;
import com.hx.huixing.adapter.VideoRecyclerViewAdapter;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.bean.VideoBean;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.fragment.WebviewFragment;
import com.hx.huixing.model.Music;
import com.hx.huixing.service.PasteCopyService;
import okhttp3.Response;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class MainActivity extends AppCompatActivity {

    public static IjkVideoView ijkVideoView;
    private RecyclerView recyclerView;
    private VideoRecyclerViewAdapter videoRecyclerViewAdapter;
    private  static List<VideoBean> videoList;
    private final static String pushURL = "http://www.time24.cn/test/index_push.php";
    public static boolean fromPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setStatusBarTransparent();
    }

    /**
     * 把状态栏设成透明
     */
    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = MainActivity.this.getWindow().getDecorView();
            decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                WindowInsets defaultInsets = v.onApplyWindowInsets(insets);
                return defaultInsets.replaceSystemWindowInsets(
                        defaultInsets.getSystemWindowInsetLeft(),
                        0,
                        defaultInsets.getSystemWindowInsetRight(),
                        defaultInsets.getSystemWindowInsetBottom());
            });
            ViewCompat.requestApplyInsets(decorView);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    private void initView() {
        recyclerView = findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
//        recyclerView.setNestedScrollingEnabled(false);
        final PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        List<Music> musicList = AppCache.get().getLocalMusicList();
        videoList = new ArrayList<>();
        for(Music music:musicList){
            if(TextUtils.isEmpty(music.getArtist())){ continue;}
            videoList.add(new VideoBean(music.getTitle(),
                    music.getCoverPath(),
                    music.getArtist()));
        }
         videoRecyclerViewAdapter =   new VideoRecyclerViewAdapter(videoList, this);
        recyclerView.setAdapter(videoRecyclerViewAdapter);
        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                ijkVideoView = view.findViewById(R.id.video_view);
                if (ijkVideoView != null) {
                    ijkVideoView.stopPlayback();
                }
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case SCROLL_STATE_IDLE: //滚动停止
                         ijkVideoView = snapHelper.findSnapView(layoutManager).findViewById(R.id.video_view);
                        if (ijkVideoView != null) {
                            ijkVideoView.start();
                        }
                        httpRequestVideo(ijkVideoView.mCurrentUrl);
                        break;
                }
            }
        });
        startPosition();
    }

    public static void httpRequestVideo(String url){
        fromPush = true;
        if(url.contains("aweme.snssdk.com")) {
            LocalMusicFragment.mWebView.loadUrl(url);
        }else {
            sendHttpRequest(url);
        }
    }

    private static  String getCover(){
        if(null !=ijkVideoView && !PasteCopyService.fromClip ) {
            for (VideoBean videoBean : videoList) {
                if (videoBean.getUrl().equals(ijkVideoView.mCurrentUrl)) {
                    return videoBean.getThumb();
                }
            }
        }else {
            return WebviewFragment.currentMusic.getCoverPath();
        }
        return null;
    }

    private static String getVideoTitle(){
        String title;
        if(null !=ijkVideoView  && !PasteCopyService.fromClip ) {
            title = ijkVideoView.mCurrentTitle;
        }else {
            title =  WebviewFragment.currentMusic.getTitle();
        }
        if(PasteCopyService.fromClip){
            PasteCopyService.fromClip  = false;
        }
        return title;
    }

    public static void  sendHttpRequest(String url){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response =OkHttpUtils.get().url(pushURL)
                            .addParams("cover_path", getCover())
                            .addParams("video", url)
                            .addParams("title", getVideoTitle())
                            .build()
                            .execute();
                    System.out.println("发送内容：" + response.body().string());
                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }).start();
        fromPush = false;
    }

    private  void getRedirect(String url){
        new HttpUtils().configUserAgent("Mozilla/5.0 (Windows NT 6.3; WOW64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
                + "Safari/537.36 OPR/30.0.1835.59").send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> objectResponseInfo) {
                String html = objectResponseInfo.result;
                Document document = Jsoup.parse(html);
                System.out.println("html：" + html);
            }
            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    private void startPosition(){
        recyclerView.scrollToPosition(MusicActivity.position);
        recyclerView.post(() -> {
            //自动播放第一个
            View view = recyclerView.getChildAt(0);
            ijkVideoView = view.findViewById(R.id.video_view);
            ijkVideoView.start();
            httpRequestVideo(ijkVideoView.mCurrentUrl);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        ijkVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ijkVideoView != null) {
            ijkVideoView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ijkVideoView.release();
    }
}

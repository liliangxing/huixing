package com.hx.huixing.fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.alibaba.fastjson.JSONObject;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hx.huixing.R;
import com.hx.huixing.activity.FullScreenActivity;
import com.hx.huixing.activity.MainActivity;
import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.adapter.PlaylistAdapter;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.application.CrashHandler;
import com.hx.huixing.constants.Keys;
import com.hx.huixing.http.HttpUtils;
import com.hx.huixing.model.AwemeVO;
import com.hx.huixing.model.Music;
import com.hx.huixing.model.ResponseVO;
import com.hx.huixing.model.UriVO;
import com.hx.huixing.model.VideoVO;
import com.hx.huixing.service.AudioPlayer;
import com.hx.huixing.service.PasteCopyService;
import com.hx.huixing.storage.db.DBManager;
import com.hx.huixing.utils.FileUtils;
import com.hx.huixing.utils.MusicUtils;
import com.hx.huixing.utils.ToastUtils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.webview.MyWebViewClient;
import okhttp3.Call;
import okhttp3.Request;

/**
 * 在线音乐
 * Created by wcy on 2015/11/26.
 */
public class WebviewFragment extends BaseFragment {
    @Bind(R.id.lv_webview)
    public WebView mWebView;
    private WebSettings webSettings;
    private ProgressDialog progressDialog;//加载界面的菊花
    private PlaylistAdapter adapter;
    private Handler handler1;
    private String LAST_OPEN_URL;
    public static Music currentMusic;

    private Integer loopCount;
    private View xCustomView;
    private WebChromeClient.CustomViewCallback   xCustomViewCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new PlaylistAdapter(AppCache.get().getLocalMusicList());
        init();
        handler1 = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String data =  msg.getData().getString("data");
                String signc =  msg.getData().getString("signc");
                String coverPath =  msg.getData().getString("coverPath");
                if(!TextUtils.isEmpty(coverPath) && TextUtils.isEmpty(currentMusic.getCoverPath())){
                    currentMusic.setCoverPath(coverPath);
                    LocalMusicFragment.adapter.notifyDataSetChanged();
                }
                if(PasteCopyService.fromClip){
                    //发送到http请求
                    MainActivity.httpRequestVideo(data);
                }
                if(!TextUtils.isEmpty(data)){
                    if(MusicActivity.fromClicked) {
                        mWebView.loadUrl(currentMusic.getArtist());
                    }
                    if(LocalMusicFragment.downloadFirst != null){
                        LocalMusicFragment.downloadFirst.openWithBrowser(currentMusic);
                        LocalMusicFragment.downloadFirst = null;
                    }
                    DBManager.get().getMusicDao().save(currentMusic);
                    if(MusicActivity.autoDownload || MusicActivity.forceDownload) {
                        MusicActivity.forceDownload = false;
                        downloadAndPlay(data);
                    }
                    if(PasteCopyService.hashSetIterator.hasNext()){
                        String url = PasteCopyService.hashSetIterator.next();
                        MusicActivity.instance.playService2.dealWithUrl(url);
                    }
                    return;
                }
                if(!TextUtils.isEmpty(signc)){
                    String ajaxData = "https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id="+currentMusic.getSongId()+"&sec_uid=&count=300&max_cursor=0&app_id=1128&_signature="+signc;
                    currentMusic.setArtist(ajaxData);
                    mWebView.loadUrl(ajaxData);
                    return;
                }
                String url = LAST_OPEN_URL;
                if (url!=null) {
                    downloadAndPlay(url);
                }
            }
        };
       // PasteCopyService.startCommand(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Keys.WEBVIEW_URL,  mWebView.getUrl());

    }

    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        mWebView.post(() -> {
            String position = savedInstanceState.getString(Keys.WEBVIEW_URL);
        });
    }

    private void init(){
        webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient(this.getContext(),progressDialog){
            /**
             * 当打开超链接的时候，回调的方法
             * WebView：自己本身mWebView
             * url：即将打开的url
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(HttpUtils.IsVideoUrl(url)){
                    Intent intent = new Intent(getActivity(), FullScreenActivity.class);
                    intent.putExtra("url",url);
                    startActivity(intent);
                    return true;
                }
                //自己处理新的url
                LAST_OPEN_URL = url;
                mWebView.loadUrl(url);
                return true;//true就是自己处理
            }
        });

        mWebView.addJavascriptInterface(
                new JSInterface()
                , "itcast");
        mWebView.addJavascriptInterface(
                new InJavaScriptLocalObj()
                , "java_obj");
        mWebView.loadUrl(Keys.HOME_PAGE);
    }
    /**
     * 逻辑处理
     * @author linzewu
     */
    private final class InJavaScriptLocalObj {
        @JavascriptInterface
        public void getSource(String html2) {
            Document document = Jsoup.parse(html2);
            String html = document.text();
            if(html.contains("_signature")){
                System.out.println("_signature..."+html);
                Matcher m =Pattern.compile("_signature=([\\S-]+)").matcher(html);
                String signc = null;
                if(m.find()){
                    signc = m.group(1);
                }
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("signc", signc);
                message.setData(bundle);
                handler1.sendMessage(message);
            }
            if(html2.contains("v.weishi.qq.com")
                ||html2.contains("q.weishi.qq.com")
                ||html2.contains("h5.weishi.qq.com")){
                Elements elements1 =Jsoup.parse(html2).select(".desc");
                final Elements elements2 =Jsoup.parse(html2).select("video[src],video");
                //Elements elements5 =Jsoup.parse(html2).select(".figure-img");
                Elements elementsAuthor =Jsoup.parse(html2).select(".user-name");
                if(!elements1.isEmpty()){
                    currentMusic.setTitle(elements1.get(0).text());
                }
                if(!elementsAuthor.isEmpty()){
                    String url="";
                    Matcher m =Pattern.compile("spid=([\\S-][^&]+)").matcher(html2);
                    if(m.find()){
                        url = m.group(1);
                    }else {
                        Elements elements5 =Jsoup.parse(html2).select(".figure-img");
                        if(!elements5.isEmpty()){
                            elements5 = elements5.get(0).children();
                            if(!elements5.isEmpty()) {
                                url = elements5.get(0).attr("src");
                            }
                            m =Pattern.compile("\\/([\\S-][^\\/]+)\\.jpg").matcher(url);
                            if(m.find()){
                                url = m.group(1);
                            }
                        }
                    }
                    String author = elementsAuthor.get(0).text();
                    currentMusic.setAlbum(StringUtil.join(Arrays.asList(url.trim(),author)," "));
                }
                //部分封面图和视频地址拿不到？已跳转到title为腾讯微视，weishi://feed?feed_id=76sQaLbUp1KHtLMSf
                //url含有collectionid=导致的
                if(elements2.isEmpty()) {
                    CrashHandler.getInstance().saveCrashInfo(html2);
                }
                Matcher m =Pattern.compile("background:url\\(([\\S-]+)\\)").matcher(html2);
                doBundle(m,elements2);
                return;
            }
            if(html2.contains("aweme.snssdk.com")){
                Elements elements2 =Jsoup.parse(html2).select("video[src]");
                Elements list2 = Jsoup.parse(html2).getElementsByTag("input");
                Elements elements3 =Jsoup.parse(html2).select("img");
                for(Element element:elements3){
                    if(element.attr("class").contains("poster")){
                        elements3 = new Elements(element);
                        break;
                    }
                }
                Message message = new Message();
                Bundle bundle = new Bundle();
                String coverUrl = null;
                for(Element element2:list2) {
                    String tagName =  element2.attr("name").toLowerCase();
                    if(tagName.equals("shareAppDesc".toLowerCase())){
                        currentMusic.setTitle(element2.val());
                    }else if(tagName.equals("shareImage".toLowerCase())){
                        coverUrl = element2.val();
                        bundle.putString("coverPath", coverUrl);
                    }
                }
                if(TextUtils.isEmpty(coverUrl) && !elements3.isEmpty()){
                    coverUrl = elements3.get(0).attr("src");
                    if(coverUrl.startsWith("//")){
                        coverUrl = coverUrl.replace("//","https://");
                    }
                    bundle.putString("coverPath", coverUrl);
                    Elements elements4 =Jsoup.parse(html2).select("p");
                    for(Element element:elements4){
                        if(element.attr("class").contains("desc")){
                            elements4 = new Elements(element);
                            break;
                        }
                    }
                    if((TextUtils.isEmpty(currentMusic.getTitle()) || currentMusic.getTitle().equals("[]"))
                            && !elements4.isEmpty()){
                        currentMusic.setTitle(elements4.get(0).text());
                    }
                }
                //uid获取
                Elements elements5 =Jsoup.parse(html2).select("p");
                Elements elementsAuthor = elements5;
                for(Element element:elements5){
                    if(element.attr("class").contains("author")){
                        elementsAuthor = new Elements(element);
                    }else if(element.attr("class").contains("unique")){
                        elements5 = new Elements(element);
                        if(elementsAuthor!= elements5) {
                            break;
                        }
                    }
                }
                if(!elements5.isEmpty() && !elementsAuthor.isEmpty()){
                    String url = elements5.get(0).text().replaceAll("(.*)：","");
                    String author = elementsAuthor.get(0).text().replaceAll("(.*)@","");
                    currentMusic.setAlbum(StringUtil.join(Arrays.asList(url.trim(),author)," "));
                }
                if(!elements2.isEmpty()){
                    String url = elements2.get(0).attr("src");
                    Matcher m =Pattern.compile("video_id=([\\S-][^&]+)").matcher(url);
                    if(m.find()){
                        url = url.replaceAll("playwm\\/\\?video_id=([\\S-][^&]+)","play/?video_id=$1&media_type=4");
                        currentMusic.setArtist(url);
                        bundle.putString("data", url);
                        message.setData(bundle);
                        //LocalMusicFragment.adapter.notifyDataSetChanged();
                        handler1.sendMessage(message);
                    }
                }
              return;
            }else if(html2.contains("抖音")){
                        if(PasteCopyService.hashSetIterator.hasNext()){
                            String url = PasteCopyService.hashSetIterator.next();
                            MusicActivity.instance.playService2.dealWithUrl(url);
                        }
                        return;
            }
            if(html2.contains("<video")){
                Elements elements1 =Jsoup.parse(html2).select(".caption-container");
                Elements elements2 =Jsoup.parse(html2).select("video[src]");
                Elements elements3 =Jsoup.parse(html2).select(".video-cover,.poster-content");
                Elements elementsAuthor =Jsoup.parse(html2).select(".auth-name");
                Elements elements5 =Jsoup.parse(html2).select(".card-container");
                if(!elements1.isEmpty()){
                    currentMusic.setTitle(elements1.get(0).text());
                }
                if(!elements5.isEmpty() && !elementsAuthor.isEmpty()){
                    String url = elements5.get(0).attr("data-scheme-url");
                    Matcher m =Pattern.compile("userId=([\\S-][^&]+)").matcher(url);
                    if(m.find()){
                        url = m.group(1);
                    }
                    String author = elementsAuthor.get(0).text();
                    currentMusic.setAlbum(StringUtil.join(Arrays.asList(url.trim(),author)," "));
                }
                Matcher m =Pattern.compile("background:url\\(([\\S-]+)\\)").matcher(html2);
                if(!elements3.isEmpty()) {
                    String url = elements3.get(0).attr("style");
                    m =Pattern.compile("background-image:url\\(([\\S-]+)\\)").matcher(url);
                }
                doBundle(m,elements2);
                return;
            }
            if(!html.contains("aweme_list")){
                return;
            }
            System.out.println("aweme_list..."+html);
            ResponseVO responseVO = JSONObject.parseObject(html, ResponseVO.class);
            for(AwemeVO awemeVO:responseVO.getAweme_list()){
                if((currentMusic.getFileSize()+"").equals(awemeVO.getAweme_id()+"")) {
                    VideoVO videoVO = awemeVO.getVideo();
                    UriVO play_addr = videoVO.getPlay_addr();
                    List<String> url_list = play_addr.getUrl_list();
                    for (String url : url_list) {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("data", url);
                        message.setData(bundle);
                        handler1.sendMessage(message);
                        break;
                    }
                    break;
                }
            }
        }
    }

    private void doBundle( Matcher m, Elements elements2 ){
        Message message = new Message();
        Bundle bundle = new Bundle();
        if(m.find()){
            String coverUrl = m.group(1);
            if(coverUrl.startsWith("//")){
                coverUrl = coverUrl.replace("//","https://");
            }
            bundle.putString("coverPath", coverUrl);
        }
        if(!elements2.isEmpty()){
            String url = elements2.get(0).attr("src");
            currentMusic.setArtist(url);
            bundle.putString("data", url);
            message.setData(bundle);
            //LocalMusicFragment.adapter.notifyDataSetChanged();
            handler1.sendMessage(message);
        }
    }
    private final class JSInterface{
        @SuppressLint("JavascriptInterface")
        @JavascriptInterface
        public void showToast(String url){
            Message message = new Message();
            LAST_OPEN_URL=url;
            handler1.sendMessage(message);
        }
    }

    public void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }
    public void cancelProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    public void downloadAndPlay(String url){
        String fileName1 = HttpUtils.getFileName(url);
        if(TextUtils.isEmpty(fileName1) ||
                url.contains("weishi.qq.com")){
            Matcher m =Pattern.compile("video_id=([\\w-][^&]+)").matcher(url);
            if(m.find()){
                fileName1 = m.group(1)+".mp4";
            }else {
                 m = Pattern.compile(".*\\/(.*\\.mp4).*").matcher(url);
                if(m.find()){
                    fileName1 = m.group(1);
                }
            }
        }
        String fileName = fileName1;
        String path  = FileUtils.getMusicDir().concat(fileName);
        File file = new File(path);
        if(!file.exists()){
                    OkHttpUtils.get().url(url).build()
                    .execute(new FileCallBack(FileUtils.getMusicDir(), fileName) {
                        boolean finishScanned =false;
                        @Override
                        public void onBefore(Request request, int id) {
                            showProgress(getString(R.string.now_download,fileName));
                        }

                        @Override
                        public void inProgress(float progress, long total, int id) {
                            showProgress("正在下载……"+((float)Math.round(progress*100*100)/100)+"%");
                        }

                        @Override
                        public void onResponse(File file, int id) {

                        }

                        @Override
                        public void onError(Call call, Exception e, int id) {

                        }

                        @Override
                        public void onAfter(int id) {
                            if(!finishScanned){
                                // 刷新媒体库
                                Intent intent =
                                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.toURI().toString()));
                                getContext().sendBroadcast(intent);
                                finishScanned = true;
                            }
                            loopCount = 3;
                            cancelProgress();
                            if(fileName.endsWith(".mp4")){
                                addPathAndPaste(path);
                                return;
                            }
                            checkCounter(fileName,true);
                        }
                    });
        }else {
            if(fileName.endsWith(".mp4")){
                addPathAndPaste(path);
                return;
            }
            checkCounter(fileName,false);
        }

    }

    private void addPathAndPaste(String path){
        currentMusic.setPath(path);
        DBManager.get().getMusicDao().save(currentMusic);
        String title =  currentMusic.getTitle();
        if(!TextUtils.isEmpty(title) && !Patterns.WEB_URL.matcher(title).matches()) {
            PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label",
                    title.replaceAll("[@|#]([\\S]{1,10})", "").trim()));
        }
        MusicActivity.instance.mViewPager.setCurrentItem(1);
        LocalMusicFragment.adapter.notifyDataSetChanged();
    }

    private void  checkCounter(String fileName,boolean loop){
        String path  = FileUtils.getMusicDir().concat(fileName);
        List<Music> musicList = MusicUtils.scanMusic(getContext());
        for(Music m:musicList) {
            if(m.getPath().equals(path)) {
                AudioPlayer.get().addAndPlay(m);
                ToastUtils.show("已添加到播放列表");
                loop = false;
                MusicActivity.instance.showPlayingFragment();
                break;
            }else {
                loop = true;
            }
        }
        if (loop) {
            if(null == loopCount){
                //直接拿，又匹配不到媒体库，删除文件，重新走下载流程
                File  file=new File(path);
                if(file.delete()){
                    downloadAndPlay(LAST_OPEN_URL);
                    return;
                }
            }
            loopCount --;
            if(loopCount<0) return;
            try {
                //耗时的操作
                ToastUtils.show("下载完毕，尝试播放中...");
                Thread.sleep(500);
                //handler主要用于异步消息的处理,使用sendMessage()后，方法立即返回，Message放入消息队列，
                //等待Message出消息队列，由handlerMessage(Message msg)通知UI线程子线程已经挂起，并使用返回的msg。
                checkCounter(fileName, false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            }
        }

}

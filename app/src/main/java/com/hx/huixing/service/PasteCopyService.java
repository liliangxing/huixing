package com.hx.huixing.service;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hx.huixing.R;
import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.constants.Extras;
import com.hx.huixing.executor.NaviMenuExecutor;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.model.Music;
import com.hx.huixing.utils.DownFile;
import com.hx.huixing.utils.ToastUtils;

/**
 * 音乐播放后台服务
 * Created by wcy on 2015/11/27.
 */
public class PasteCopyService extends Service {
    private static final String TAG = "Service";
    private static  LocalMusicFragment adapter;
    public static boolean fromClip;
    public static PasteCopyService instance;

    public  static ClipboardManager clipboardManager;

    public static Iterator<String> hashSetIterator = new Iterator<String>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public String next() {
            return null;
        }
    };

    private String mPreviousText = "";
    public static Handler handler1;
    private long clipNowTime;
    private long clipPreTime;
    private final static long runMillis = 8 * 1000;
    public class PlayBinder extends Binder {
        public PasteCopyService getService() {
            return PasteCopyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "onCreate: " + getClass().getSimpleName());
        clipboardManager =(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clipPreTime = System.currentTimeMillis();
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                clipNowTime = System.currentTimeMillis();
                ClipData clipData = clipboardManager.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                if(null == item || null == item.getText()){
                    return;
                }
                if(mPreviousText.equals(item.getText().toString()) ||
                        mPreviousText.endsWith("#openWith")){ return;}
                else{
                    mPreviousText = item.getText().toString();
                    String html  = mPreviousText;
                    String mode = "(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";
                    String htmlText = html.replaceAll(mode,"");
                    Pattern p = Pattern.compile(mode);
                    Matcher m = p.matcher(html);
                    Matcher m2 = p.matcher(html);
                    int urlCount= 0;
                    while(m2.find()){ ++urlCount;}
                    if(!checkUrl(html)){
                        return;
                    }
                    HashSet<String> hashSet = new HashSet<>();
                    //没有全部遍历，如果不在该页面内，会重新新增
                    List<Music> musicList = AppCache.get().getLocalMusicList();
                    MusicActivity.moreUrl = false;
                    while(m.find()){
                        String url = m.group();
                        System.out.println("url：" + url);
                        if(!checkUrl(url)){
                            continue;
                        }
                        boolean addFlag = true;
                        for(Music music:musicList){
                            boolean startSpc = music.getTitle()==null||music.getTitle().startsWith("@")||
                                    music.getTitle().startsWith("#");
                            if(
                                (url.equals(music.getFileName())||
                                 (music.getTitle()!=null
                                 && htmlText.contains(music.getTitle()) && music.getTitle().length()>=5
                                 &&(!startSpc||startSpc && music.getTitle().length()> 9)))
                                    && urlCount<=1
                             ){
                                sendMsgVO(music,"moveTop");
                                addFlag = false;
                                break;
                            }
                        }
                        if(addFlag) {
                            hashSet.add(url);
                        }
                    }
                    //新链接加入
                    if(hashSet.size()>1){
                        MusicActivity.moreUrl = true;
                    } else if(hashSet.isEmpty()){
                        ToastUtils.show("存在重复链接");
                        return;
                    }
                    hashSetIterator = hashSet.iterator();
                    if(hashSetIterator.hasNext()) {
                        dealWithUrl(hashSetIterator.next());
                    }
                    ToastUtils.show("您有新的"+getSiteName(html)+"链接了！");
                }
            }
        });
        startForeground( 0x111, buildNotification(this));
    }

    private static String getSiteName(String html){
        String siteName = "抖音";
        if(html.contains("weishi.qq.com")){
            siteName =  "微视";
        }else if(html.contains("kuai") && html.contains(".com")){
            siteName =  "快手";
        }
        return siteName;
    }
    public static boolean checkUrl(String url){
        if((url.contains("douyin.com")
                || url.contains("huoshan.com")
                || url.contains("weishi.qq.com")
                || (url.contains("kuai") && url.contains(".com"))
                || url.contains("yxixy.com/fw/photo/"))&& !url.contains("time24.cn")){
            return true;
        }
        return false;
    }

    public void dealWithUrl(String url){
        //if(isWeiShi){
        if(null != NaviMenuExecutor.mapLinks.get(url)){
            sendMsgVO(NaviMenuExecutor.mapLinks.get(url));
            return;
        }
        url = dealWithIsee(url);
        Music videoVO = new Music();
        videoVO.setPath(url);
        videoVO.setAlbumId(1);
        //videoVO.setTitle(mPreviousText);
        videoVO.setFileName(url);
        sendMsgVO(videoVO);
        //}
        //clipUrlCrawler(url);
    }

    private static String dealWithIsee(String url){
        if(url.contains("isee.weishi.qq.com")){
            Matcher m =Pattern.compile("[&|?]id=([\\S-][^&]+)").matcher(url);
            Matcher m2 =Pattern.compile("spid=([\\S-][^&]+)").matcher(url);
            String feedId = null;
            String spid = null;
            if(m.find()){
                feedId = m.group(1);
            }
            if(m2.find()){
                spid = m2.group(1);
            }
            url = "https://h5.weishi.qq.com/weishi/feed/"+feedId+
                    "/wsfeed?wxplay=1&id="+ feedId+"&spid="+spid+"&qua=v1_and_weishi_8.6.0_588_312025000";
        }
        return url;
    }

    private Notification buildNotification(Context context) {
        Intent intent = new Intent(context, MusicActivity.class);
        intent.putExtra(Extras.EXTRA_NOTIFICATION, true);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification);
        return builder.build();
    }


    public final static String agent = "Mozilla/5.0 (Windows NT 6.3; WOW64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 "
            + "Safari/537.36 OPR/30.0.1835.59";
    public final static String uidAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1";
    private String mode = "(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";
    private Pattern dytkPattern = Pattern.compile("dytk: ('|\")(\\w*?)('|\")");
    private Pattern uidPattern = Pattern.compile("uid: \"(\\w*?)\"");
    private Pattern p = Pattern.compile(mode);
    private Pattern playAddrPattern = Pattern.compile("playAddr: \"(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))\"");
    private Pattern coverPattern = Pattern.compile("cover: \"(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))\"");
    private Pattern itemIdPattern = Pattern.compile("itemId: \"(\\w*?)\"");


    public static final String AWEME_LIST_API = "https://www.iesdouyin.com/web/api/v2/aweme/post/?user_id=%s&sec_uid=&count=21&max_cursor=0&app_id=1128&_signature=%s&dytk=%s";

    private void clipUrlCrawler(String url) {
            new HttpUtils().configUserAgent(uidAgent).send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
                @Override
                public void onSuccess(ResponseInfo<String> objectResponseInfo) {

                    // 拿到2级页面源码
                    String html2 = objectResponseInfo.result;
                    System.out.println("clipUrlCrawler "+url+"："+html2);
                    /***********************解析<img>图片标签**************************/
                    // 框架JSoup:GitHub
                    Document doc3 = Jsoup.parse(html2);// 解析HTML页面
                    // 获取图片
                    Elements list =doc3.getElementsByTag("script");
                    for(Element element:list) {
                        String html = element.toString();
                        if(!(html.contains("playAddr") || html.contains("uid"))){
                            continue;
                        }/*
                        if (element.childNodeSize() == 0) {
                            continue;
                        }*/
                        Music videoVO = new Music();
                        videoVO.setPath(url);
                        try {
                            String title = mPreviousText;
                            Elements list2 = doc3.getElementsByTag("input");
                            for(Element element2:list2) {
                                String tagName =  element2.attr("name").toLowerCase();
                                if(tagName.equals("shareAppDesc".toLowerCase())){
                                    title = element2.val();

                                }else if(tagName.equals("shareImage".toLowerCase())){
                                    videoVO.setCoverPath(element2.val());
                                }
                            }
                            videoVO.setTitle(title);
                        }catch (Exception e){
                            videoVO.setTitle(mPreviousText);
                            System.out.println(e.getMessage());
                        }
                        Matcher m2 = dytkPattern.matcher(html);
                        Matcher m3 =uidPattern.matcher(html);
                        Matcher m4 =playAddrPattern.matcher(html);
                        Matcher m5 = coverPattern.matcher(html);
                        Matcher m6 = itemIdPattern.matcher(html);
                        if(m2.find()) {
                            String dytk = m2.group(2);
                            videoVO.setAlbum(dytk);
                        }

                        if(m3.find()) {
                            String uid = m3.group(1);
                            videoVO.setSongId(Long.valueOf(uid));
                        }
                        if(m4.find()) {
                            String playAddr = m4.group(1);
                            //crawlerPlayAddr(playAddr);
                        }

                        if(m5.find()) {
                            String cover = m5.group(1);
                            videoVO.setCoverPath(cover);
                        }
                        if(m6.find()) {
                            String cover = m6.group(1);
                            videoVO.setFileSize(Long.valueOf(cover));
                        }
                        String videoUrl = String.format(AWEME_LIST_API, videoVO.getSongId(), "{signature}", videoVO.getAlbum());
                        videoVO.setArtist(videoUrl);
                        sendMsgVO(videoVO);
                        }
                    }



                @Override
                public void onFailure(HttpException e, String s) {

                }
            });

    }

    private void crawlerPlayAddr(String url){
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Response response =OkHttpUtils.get().url(url).build()
                            .execute();
                    String html = response.body().string();
                }catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }).start();*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                Document doc3 = DownFile.doGetDoc(url);
                Elements links = doc3.select("a[href]");
                for (Element vLink : links)
                {
                    String lin = vLink.attr("abs:href");
                    if(lin.contains("video/tos")){
                       // videoVO.setPath(lin);
                    }
                }

            }
        }).start();
    }

    public void sendMsgVO(Music videoVO){
        sendMsgVO(videoVO,"data");
        fromClip = true;
    }

    public void sendMsgVO(Music videoVO,String key){
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString(key, JSON.toJSONString(videoVO));
        message.setData(bundle);
        if(clipNowTime - clipPreTime > runMillis) {
            handler1.sendMessage(message);
            clipPreTime = clipNowTime;
        }else {
            long delayMillis;
            long exceedTime = clipPreTime - clipNowTime;
            if(exceedTime > 0){
                //时间点还没到，计算下一个延时
                delayMillis = (exceedTime> runMillis?
                        runMillis:exceedTime*2)+runMillis;
            }else {
                delayMillis = runMillis;
            }
            handler1.sendMessageDelayed(message,delayMillis);
            clipPreTime = clipNowTime + delayMillis;
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();
    }

    public static void startCommand(Context context, Handler handler) {
        Intent intent = new Intent(context, PasteCopyService.class);
        context.startService(intent);
        handler1 = handler;
    }
}

package com.hx.huixing.fragment;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.HttpProxyCacheServer;
import com.example.ijkplayer.player.VideoCacheManager;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hx.huixing.activity.LocalMusicActivity;
import com.hx.huixing.utils.ViewUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.mihua.thirdplatform.sharesdk.ShareData;
import com.mihua.thirdplatform.sharesdk.ShareManager;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import com.hx.huixing.activity.MainActivity;
import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.activity.MusicInfoActivity;
import com.hx.huixing.activity.SubscribeMessageActivity;
import com.hx.huixing.adapter.OnMoreClickListener;
import com.hx.huixing.adapter.PlaylistAdapter;
import com.hx.huixing.application.CrashHandler;
import com.hx.huixing.constants.Keys;
import com.hx.huixing.executor.NaviMenuExecutor;
import com.hx.huixing.model.Music;
import com.hx.huixing.service.PasteCopyService;
import com.hx.huixing.storage.db.DBManager;
import com.hx.huixing.storage.db.greendao.MusicDao;
import com.hx.huixing.utils.DownFile;
import com.hx.huixing.utils.FileUtils;
import com.hx.huixing.utils.HttpPostUtils;
import com.hx.huixing.utils.Modify;
import com.hx.huixing.utils.PermissionReq;
import com.hx.huixing.utils.ScreenUtils;
import com.hx.huixing.utils.ToastUtils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.R;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.constants.RequestCode;
import com.hx.huixing.constants.RxBusTags;

/**
 * 本地音乐列表
 * Created by wcy on 2015/11/26.
 */
public class LocalMusicFragment extends BaseFragment implements AdapterView.OnItemClickListener, OnMoreClickListener, AdapterView.OnItemLongClickListener {
    @Bind(R.id.lv_local_music)
    private static ListView lvLocalMusic;
    @Bind(R.id.v_searching)
    private TextView vSearching;
    public static WebView mWebView;
    private View vHeader;
    private Loader<Cursor> loader;
    public static PlaylistAdapter adapter;
    private Handler handler1;
    public static final String FILE_NAME = "test.html";
    public static LocalMusicFragment downloadFirst;
    public static LocalMusicFragment instance;
    public static List<Music> musicList = new ArrayList<>();
    public static boolean viewByGroupFlag;

    public static final int MUSIC_LIST_SIZE = 10000;
    @Bind(R.id.ll_loading)
    private LinearLayout llLoading;
    @Bind(R.id.ll_load_fail)
    private LinearLayout llLoadFail;
    private static int mOffset = 0;
    private static int uploadNum = 1;
    private static  WhereCondition cond = null;
    private static Property[] orderBy =new Property[] {MusicDao.Properties.Id};

    private static int position;
    private static int offset;
    private ShareManager shareManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_music, container, false);
    }

    public static void refresh(){
        resetOffset();
        cond = MusicDao.Properties.SongId.eq(1);
        position = lvLocalMusic.getFirstVisiblePosition();
        offset = (lvLocalMusic.getChildAt(0) == null) ? 0 : lvLocalMusic.getChildAt(0).getTop();
        resetAdapter();
    }

    public static void resetOffset(){
        mOffset = 0;
        cond = null;
        orderBy = new Property[] {MusicDao.Properties.Id};
        musicList.clear();
    }

    public static WhereCondition getAlbumCondition(Music music){
        String keyword = music.getAlbum();
        String check = "([_.0-9a-zA-Z-]{3,})";
        Pattern regex = Pattern.compile(check);
        Matcher matcher = regex.matcher(music.getAlbum());
        if(matcher.find()) {
            keyword = matcher.group(1);
        }
       return MusicDao.Properties.Album.like("%"+ keyword + "%");
    }
    public static void refreshOrder(Music music){
            position = lvLocalMusic.getFirstVisiblePosition();
            offset = (lvLocalMusic.getChildAt(0) == null) ? 0 : lvLocalMusic.getChildAt(0).getTop();
            resetOffset();
            orderBy = new Property[] {MusicDao.Properties.SongId,MusicDao.Properties.Id};
            if(!TextUtils.isEmpty(music.getAlbum())) {
                cond = getAlbumCondition(music);
            }
            resetAdapter();
            viewByGroupFlag = true;
    }

    public static void refreshAll(){
        resetOffset();
        resetAdapter();
        lvLocalMusic.setSelectionFromTop(position, offset);
    }
    private static void resetAdapter(){
        adapter.setMusicList(musicList);
        lvLocalMusic.setAdapter(adapter);
        instance.onLoad();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new PlaylistAdapter(musicList);
        adapter.setOnMoreClickListener(this);
        lvLocalMusic.setAdapter(adapter);
        instance = this;
        loadMusic();
        handler1 = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String data =  msg.getData().getString("moveTop");
                if(!TextUtils.isEmpty(data)){
                    Music music = JSONObject.parseObject(data,Music.class);
                    AppCache.get().getLocalMusicList().remove(music);
                    moveTop(music);
                    adapter.notifyDataSetChanged();
                    return;
                }
                data =  msg.getData().getString("data");
                Music music = JSONObject.parseObject(data,Music.class);
                adapter.addMusic(music);
                adapter.notifyDataSetChanged();
                if(null!=LocalMusicActivity.instance) {
                    LocalMusicActivity.instance.adapter.notifyDataSetChanged();
                    LocalMusicActivity.addNewFlag = true;
                }
                MusicActivity.fromClicked = false;
                if(music.getAlbumId() == 1){
                    WebviewFragment.currentMusic =  music;
                    mWebView.loadUrl(music.getFileName());
                    return;
                }
                downloadDouyin(music);
            }
        };
        //PasteCopyService.startCommand(getActivity(), handler1);
        PasteCopyService.handler1 = handler1;
        vHeader = LayoutInflater.from(getActivity()).inflate(R.layout.activity_local_music_list_header, null);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(150));
        vHeader.setLayoutParams(params);
        lvLocalMusic.addHeaderView(vHeader, null, false);
        onLoad();
        shareManager = ShareManager.getInstance();
    }

    public void onLoad() {
        getMusic(mOffset);
    }
    private void getMusic(final int offset) {
        QueryBuilder<Music> queryBuilder = DBManager.get().getMusicDao().queryBuilder();
        if (null != cond) {
            queryBuilder = queryBuilder.where(cond);
        }
        List<Music> songList = queryBuilder.orderDesc(orderBy)
                .offset(offset).limit(MUSIC_LIST_SIZE).build().list();
                mOffset += MUSIC_LIST_SIZE;
                musicList.addAll(songList);
                adapter.notifyDataSetChanged();

    }
    private void loadMusic() {
        lvLocalMusic.setVisibility(View.GONE);
        vSearching.setVisibility(View.VISIBLE);
        PermissionReq.with(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new PermissionReq.Result() {
                    @Override
                    public void onGranted() {
                        initLoader();
                    }

                    @Override
                    public void onDenied() {
                        ToastUtils.show(R.string.no_permission_storage);
                        lvLocalMusic.setVisibility(View.VISIBLE);
                        vSearching.setVisibility(View.GONE);
                    }
                })
                .request();
    }

    private void initLoader() {
        lvLocalMusic.setVisibility(View.VISIBLE);
        vSearching.setVisibility(View.GONE);
        /*loader = getActivity().getLoaderManager().initLoader(0, null,new MusicLoaderCallback(getContext(), value -> {
            AppCache.get().getLocalMusicList().clear();
            AppCache.get().getLocalMusicList().addAll(value);
            adapter.notifyDataSetChanged();
        }));*/
    }

    @Subscribe(tags = { @Tag(RxBusTags.SCAN_MUSIC) })
    public void scanMusic(Object object) {
        if (loader != null) {
            loader.forceLoad();
        }
    }

    @Override
    protected void setListener() {
        lvLocalMusic.setOnItemClickListener(this);
        lvLocalMusic.setOnItemLongClickListener(this);
    }

    private final static String userInfoUrl = "https://www.iesdouyin.com/share/user/";
    public final static String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36";
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent intent = new Intent(getContext(), MainActivity.class);
        MusicActivity.position = position-1;
        Music music = AppCache.get().getLocalMusicList().get(position-1);
        for(Music music2:AppCache.get().getLocalMusicList()){
            if(TextUtils.isEmpty(music2.getArtist())){ -- MusicActivity.position;}
            if(music2.equals(music)){
                break;
            }
        }
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        MusicActivity.position = position-1;
        Music music = AppCache.get().getLocalMusicList().get(position-1);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        CharSequence[] mItems1 = getResources().getTextArray(R.array.local_music_frag_long_dialog);
        int fIndex = mItems1.length-1<0?0:mItems1.length-1;
        if(NaviMenuExecutor.favoriteFlag) {
            if (0 == music.getSongId()) {
                mItems1[fIndex] = "设为喜欢";
            } else {
                mItems1[fIndex] = "查看所有喜欢";
            }
        }else {
            if (1 == music.getSongId()) {
                mItems1[fIndex] = "取消喜欢";
            } else {
                mItems1[fIndex] = "查看所有链接";
            }
        }
        dialog.setItems(mItems1, (dialog1, which) -> {
            switch (which) {
                case 0:// 查看抖音ID所有
                    refreshOrder(music);
                    ToastUtils.show("查看抖音ID");
                    break;
                case 1:// 置顶
                    if(!TextUtils.isEmpty(music.getAlbum())) {
                        List<Music> musicList2 = DBManager.get().getMusicDao().queryBuilder().where(getAlbumCondition(music)).build().list();
                        for(Music musicOther:musicList2) {
                            moveTop(musicOther);
                        }
                    }else {
                        moveTop(music);
                    }
                    adapter.notifyDataSetChanged();
                    if(viewByGroupFlag) {
                        refreshAll();
                        viewByGroupFlag = false;
                    }
                    ToastUtils.show("置顶成功");
                    break;
                case 2:// 查看所有喜欢/取消
                    NaviMenuExecutor.changeMenuItem();
                    ToastUtils.show(!NaviMenuExecutor.favoriteFlag?"查看所有喜欢":"取消查看喜欢");
                    break;
                case 3:// 缓存转本地MP4
                    doCacheSave(music);
                    break;
                case 4:// 发送文件到
                    if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())) {
                        shareMusic(music);
                    }else {
                        ToastUtils.show("文件未下载");
                    }
                    break;
                case 5:
                    if(NaviMenuExecutor.favoriteFlag) {
                        if (0 == music.getSongId()) {
                            // 设为喜欢
                            music.setSongId(1);
                            DBManager.get().getMusicDao().save(music);
                            adapter.notifyDataSetChanged();
                            ToastUtils.show("成功");
                        } else {
                            // 查看所有喜欢
                            NaviMenuExecutor.changeMenuItem();
                        }
                    }else {
                        if (1 == music.getSongId()) {
                            // 取消喜欢
                            music.setSongId(0);
                            DBManager.get().getMusicDao().save(music);
                            AppCache.get().getLocalMusicList().remove(music);
                            adapter.notifyDataSetChanged();
                            ToastUtils.show("已取消");
                        } else {
                            // 查看所有链接
                            NaviMenuExecutor.changeMenuItem();
                        }
                    }
                    break;
            }
        });
        dialog.show();
        return true;
    }


    private void doCacheSave(Music music){
        WebviewFragment.currentMusic = music;
        if (music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())) {
            refreshCache(new File(music.getPath()));
            ToastUtils.show("已有MP4");
            return;
        }
        String proxyPath = getProxyPathByUrl(music);
        File fileCache =  new File(proxyPath.replace("file://",""));
        if (!proxyPath.startsWith("file:///")) {
            ToastUtils.show("没缓存");
            return;
        }
        int slashIndex = proxyPath.lastIndexOf('/');
        String fileName = proxyPath.substring(slashIndex+1)+".mp4";
        String path  = FileUtils.getMusicDir().concat(fileName);
        File file = new File(path);
        if(fileCache.exists()) {
            if(file.exists()){
                ToastUtils.show("目标文件已存在");
                refreshCache(file);
                return;
            }
            DownFile.customBufferStreamCopy(fileCache, file);
            ToastUtils.show("缓存成功");
        }else {
            ToastUtils.show("缓存文件不存在");
        }
    }

    private static synchronized void moveTop(Music musicOther){
        AppCache.get().getLocalMusicList().remove(musicOther);
        if (null != musicOther.getId()) {
            DBManager.get().getMusicDao().delete(musicOther);
        }
        musicOther.setId(null);
        DBManager.get().getMusicDao().save(musicOther);
        adapter.addMusic(musicOther);
    }

    private void doUploadCache(Music music){
        WebviewFragment.currentMusic = music;
        String proxyPath = getProxyPathByUrl(music);
        File fileCache =  new File(proxyPath.replace("file://",""));
        if (!proxyPath.startsWith("file:///")) {
            ToastUtils.show("没缓存");
            return;
        }
        String fileName = uploadNum+".mp4";
        ToastUtils.show("准备上传"+fileName+"到\nhttp://www.time24.cn/test/upload/"+fileName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpPostUtils.httpPost(getActivity(),"http://www.time24.cn/test/index_upload.php"
                ,fileCache,fileName);
                uploadNum ++;
            }
        }).start();
    }

    private void downloadDouyin(Music music){
        WebviewFragment.currentMusic =  music;
        Long userId = music.getSongId();
        new HttpUtils().configUserAgent(userAgent).send(HttpRequest.HttpMethod.GET, userInfoUrl + userId, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> objectResponseInfo) {
                String html = objectResponseInfo.result;
                Document document = Jsoup.parse(html);

                Elements scripts = document.getElementsByTag("script");
                String tacScript = scripts.get(1).toString();
                String tac = tacScript.replace("<script>tac='", "")
                        .replace("'</script>", "");
                File file = new File(FileUtils.getMusicDir() + FILE_NAME);
                file.delete();
                Modify.modify("var tac=", "var tac='"+tac+"';",getContext(),FILE_NAME);
                Modify.modify("var user_id=","var user_id="+music.getSongId()+"",getContext(),FILE_NAME);
                mWebView.loadUrl("file:///mnt/sdcard"+FileUtils.DATA_DIR+FILE_NAME);
            }
            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    private static HttpProxyCacheServer mCacheServer;
    private static HttpProxyCacheServer getCacheServer() {
        return VideoCacheManager.getProxy(instance.getContext().getApplicationContext());
    }

    public static String getProxyPathByUrl(Music music){
        mCacheServer = getCacheServer();
        return mCacheServer.getProxyUrl(music.getArtist());
    }

    @Override
    public void onMoreClick(final int position) {
        /*if(!NaviMenuExecutor.favoriteFlag){
            NaviMenuExecutor.changeMenuItem();
            return;
        }*/
        Music music = AppCache.get().getLocalMusicList().get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(music.getTitle());
        CharSequence[] mItems1 = getResources().getTextArray(R.array.local_music_dialog);
        dialog.setItems(mItems1, (dialog1, which) -> {
            switch (which) {
                case 0:// 抖音打开
                    ViewUtils.openWith(music,getContext());
                    break;
                case 1:// 新标签页打开
                    MusicActivity.fromClicked = true;
                    MusicActivity.instance.mViewPager.setCurrentItem(1);
                    if (music.getAlbumId() == 1) {
                        WebviewFragment.currentMusic = music;
                        String url = music.getPath();
                        if (music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())) {
                            url = music.getArtist();
                        }
                        mWebView.loadUrl(url);
                    }
                    break;
                case 2:// 发送微信好友
                    showWechatShare(music,ShareManager.PlatformType.WeChat);
                   /* AppCache.get().getLocalMusicList().remove(music);
                    if(null != music.getId()) {
                        DBManager.get().getMusicDao().delete(music);
                    }
                    music.setId(null);
                    DBManager.get().getMusicDao().save(music);
                    adapter.addMusic(music);
                    adapter.notifyDataSetChanged();
                    ToastUtils.show("操作成功");*/
                    break;
                case 3:// 查看歌曲信息
                    WebviewFragment.currentMusic = music;
                    MusicInfoActivity.start(getContext(), music);
                    break;

                case 4:// 分享朋友圈并复制
                    if (null != music.getCoverPath()) {
                        showWechatShare(music,ShareManager.PlatformType.WechatMoments);
                        //openWithBrowser(music);
                    } else {
                        downloadFirst = this;
                        if (music.getAlbumId() == 1) {
                            WebviewFragment.currentMusic = music;
                            //分析并下载
                            mWebView.loadUrl(music.getArtist());
                        }
                    }
                    //requestSetRingtone(music);
                    break;
                case 5:// 下载到手机
                    if (music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())) {
                        ToastUtils.show("已下载");
                    } else {
                        MusicActivity.forceDownload = true;
                        if (music.getAlbumId() == 1) {
                            WebviewFragment.currentMusic = music;
                            mWebView.loadUrl(music.getPath());
                        }
                    }
                    break;
                case 6:// 删除
                    if (0 == music.getSongId()){
                        deleteMusic(music);
                    }else {
                        //取消喜欢
                        music.setSongId(0);
                        DBManager.get().getMusicDao().save(music);
                        ToastUtils.show("取消喜欢");
                    }
                    break;
            }
        });
        dialog.show();
    }

    public static void refreshCache(File target){
        WebviewFragment.currentMusic.setPath(target.getPath());
        DBManager.get().getMusicDao().save(WebviewFragment.currentMusic);
        // 刷新媒体库
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://".concat(WebviewFragment.currentMusic.getPath())));
        instance.getContext().sendBroadcast(intent);
        adapter.notifyDataSetChanged();
    }

    public void openWithBrowser(Music music){       
        SubscribeMessageActivity.createChooser(copyUrl(music),getContext());
    }
    
    private static String copyUrl(Music music){
        String url = getShareUrl(music);
        PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label", url));
        return url;
    }

    private static String getShareUrl(Music music){
        String title = music.getTitle()==null?"":music.getTitle().replaceAll("[@|#]([\\S]{1,10})","").trim();
        String url = "http://www.time24.cn/test/index_douyin.php?video="+ URLEncoder.encode(music.getArtist())
                +"&title="+URLEncoder.encode(title);
        if(null!=music.getCoverPath()){
            url += "&cover_path="+URLEncoder.encode(music.getCoverPath());
        }else {
            url = music.getPath();
        }
        return url;
    }

    private void showWechatShare(Music music, ShareManager.PlatformType platformType) {
        copyUrl(music);
        ShareData shareData = new ShareData();
        shareData.mPlatformType = platformType;
        Platform.ShareParams shareParams = new Platform.ShareParams();
        shareParams.setText(music.getFileName());
        shareParams.setShareType(Platform.SHARE_WEBPAGE);
        shareParams.setUrl(getShareUrl(music));
        shareParams.setTitle(music.getTitle());
        shareParams.setImageUrl(music.getCoverPath());
        shareData.mShareParams = shareParams;
        shareManager.shareData(shareData, new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                Log.e("thirdPlatform", platform.getName() + "arg2:" + hashMap.toString());
                CrashHandler.getInstance().saveCrashInfo(platform.getName() + "arg2:" + hashMap.toString());
            }

            @Override
            public void onError(Platform platform, int i, Throwable throwable) {
                String msg = throwable.toString();
                Log.e("thirdPlatform", msg);
                CrashHandler.getInstance().saveCrashInfo(msg);
            }

            @Override
            public void onCancel(Platform platform, int i) {
                Log.e("thirdPlatform", "cancel");
            }
        });
    }

    /**
     * 分享音乐
     */
    private void shareMusic(Music music) {
        File file = new File(music.getPath());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        Uri data;
        // Android  7.0
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            data = FileProvider.getUriForFile(getContext(), "com.hx.huixing.fileProvider",file);
        }else {
            data = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, data);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void requestSetRingtone(final Music music) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(getContext())) {
            ToastUtils.show(R.string.no_permission_setting);
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            startActivityForResult(intent, RequestCode.REQUEST_WRITE_SETTINGS);
        } else {
            setRingtone(music);
        }
    }

    /**
     * 设置铃声
     */
    private void setRingtone(Music music) {
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(music.getPath());
        // 查询音乐文件在媒体库是否存在
        Cursor cursor = getContext().getContentResolver()
                .query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[] { music.getPath() }, null);
        if (cursor == null) {
            return;
        }
        if (cursor.moveToFirst() && cursor.getCount() > 0) {
            String _id = cursor.getString(0);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.IS_MUSIC, true);
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            values.put(MediaStore.Audio.Media.IS_ALARM, false);
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
            values.put(MediaStore.Audio.Media.IS_PODCAST, false);

            getContext().getContentResolver()
                    .update(uri, values, MediaStore.MediaColumns.DATA + "=?", new String[] { music.getPath() });
            Uri newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
            RingtoneManager.setActualDefaultRingtoneUri(getContext(), RingtoneManager.TYPE_RINGTONE, newUri);
            ToastUtils.show(R.string.setting_ringtone_success);
        }
        cursor.close();
    }

    private void deleteMusic(final Music music) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        String title = music.getTitle();
        String msg = getString(R.string.delete_music, title);
        dialog.setMessage(msg);
        dialog.setPositiveButton(R.string.delete, (dialog1, which) -> {
            File file = new File(music.getPath());
            //删除缓存
            String cacheMsg = null;
            if(null != music.getArtist()) {
                String proxyPath = getProxyPathByUrl(music);
                File fileCache = new File(proxyPath.replace("file://", ""));
                cacheMsg = fileCache.delete() ? ",已删缓存" : "";
            }
            if (file.delete()) {
                ToastUtils.show("删除成功"+ cacheMsg);
            }else {
                ToastUtils.show("手机没有下载该文件" + cacheMsg);
            }
            AppCache.get().getLocalMusicList().remove(music);
            if(null != music.getId()) {
                DBManager.get().getMusicDao().delete(music);
            }
            adapter.notifyDataSetChanged();
        });
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCode.REQUEST_WRITE_SETTINGS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(getContext())) {
                ToastUtils.show(R.string.grant_permission_setting);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int position = lvLocalMusic.getFirstVisiblePosition();
        int offset = (lvLocalMusic.getChildAt(0) == null) ? 0 : lvLocalMusic.getChildAt(0).getTop();
        outState.putInt(Keys.LOCAL_MUSIC_POSITION, position);
        outState.putInt(Keys.LOCAL_MUSIC_OFFSET, offset);
    }

    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        lvLocalMusic.post(() -> {
            int position = savedInstanceState.getInt(Keys.LOCAL_MUSIC_POSITION);
            int offset = savedInstanceState.getInt(Keys.LOCAL_MUSIC_OFFSET);
            lvLocalMusic.setSelectionFromTop(position, offset);
        });
    }
}

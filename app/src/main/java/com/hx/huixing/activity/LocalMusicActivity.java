package com.hx.huixing.activity;

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
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.danikula.videocache.HttpProxyCacheServer;
import com.example.ijkplayer.player.VideoCacheManager;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hx.huixing.constants.Keys;
import com.hx.huixing.utils.Utils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.hx.huixing.R;
import com.hx.huixing.adapter.OnMoreClickListener;
import com.hx.huixing.adapter.PlaylistAdapter;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.constants.RequestCode;
import com.hx.huixing.constants.RxBusTags;
import com.hx.huixing.enums.LoadStateEnum;
import com.hx.huixing.executor.NaviMenuExecutor;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.fragment.WebviewFragment;
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
import com.hx.huixing.utils.ViewUtils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.widget.AutoLoadListView;

/**
 * 本地音乐列表
 * Created by wcy on 2015/11/26.
 */
public class LocalMusicActivity extends BaseActivity implements AdapterView.OnItemClickListener, OnMoreClickListener, AdapterView.OnItemLongClickListener ,
        AutoLoadListView.OnLoadListener {
    @Bind(R.id.lv_online_music_list)
    private  AutoLoadListView lvLocalMusic;
    @Bind(R.id.v_searching)
    private TextView vSearching;
    public static WebView mWebView = LocalMusicFragment.mWebView;
    private View vHeader;
    private Loader<Cursor> loader;
    public  PlaylistAdapter adapter;
    public static final String FILE_NAME = "test.html";
    public static LocalMusicActivity downloadFirst;
    public static LocalMusicActivity instance;
    public List<Music> musicList = new ArrayList<>();
    public static boolean fileNameOrder;

    public static final int MUSIC_LIST_SIZE = 10000;
    @Bind(R.id.ll_loading)
    private LinearLayout llLoading;
    @Bind(R.id.ll_load_fail)
    private LinearLayout llLoadFail;
    private int mOffset = 0;
    private static int uploadNum = 1;
    private static  WhereCondition cond = null;
    public final static Property[] ORDER_BY_ALBUM =new Property[] {MusicDao.Properties.Album , MusicDao.Properties.Id};
    private static Property[] orderBy = ORDER_BY_ALBUM;

    private static int position;
    private static int offset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);
    }

    @Override
    protected void onServiceBound() {
        init();
    }

    public static void refresh(){
        resetOffset();
        cond = MusicDao.Properties.SongId.eq(1);
        resetAdapter();
    }

    public static void resetOffset(){
        instance.mOffset = 0;
        cond = null;
        orderBy = ORDER_BY_ALBUM;
        instance.musicList.clear();
    }

    public static void refreshOrder(Music music){
        if(!fileNameOrder) {
            resetOffset();
            if(!TextUtils.isEmpty(music.getAlbum())) {
                cond = LocalMusicFragment.getAlbumCondition(music);
            }
            orderBy = ORDER_BY_ALBUM;
            resetAdapter();
        }else {
            refreshAll();
        }
        fileNameOrder = !fileNameOrder;
    }

    public static void refreshAll(){
        resetOffset();
        resetAdapter();
    }
    private static void resetAdapter(){
        instance.adapter.setMusicList(instance.musicList);
        instance.lvLocalMusic.setAdapter(instance.adapter);
        instance.onLoad();
    }

    public void init() {      
        adapter = new PlaylistAdapter(musicList);
        adapter.setOnMoreClickListener(this);
        lvLocalMusic.setAdapter(adapter);
        instance = this;
        loadMusic();
        vHeader = LayoutInflater.from(this).inflate(R.layout.activity_local_music_list_header, null);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ScreenUtils.dp2px(150));
        vHeader.setLayoutParams(params);
        lvLocalMusic.addHeaderView(vHeader, null, false);
        lvLocalMusic.setOnLoadListener(this);
        lvLocalMusic.setOnItemClickListener(this);
        lvLocalMusic.setOnItemLongClickListener(this);
        onLoad();
    }

    @Override
    public void onLoad() {
        getMusic(mOffset);
        if(position==0){
            String positionCache = Utils.readData(Keys.PREFERENCES_FILE,"positionCache");
            String offsetCache = Utils.readData(Keys.PREFERENCES_FILE,"offsetCache");
            if(!TextUtils.isEmpty(positionCache)){
                position = Integer.parseInt(positionCache);
                offset = Integer.parseInt(offsetCache);
            }
        }
        instance.lvLocalMusic.setSelectionFromTop(position, offset);
    }
    private void getMusic(final int offset) {
        QueryBuilder<Music> queryBuilder = DBManager.get().getMusicDao().queryBuilder();
        if (null != cond) {
            queryBuilder = queryBuilder.where(cond);
        }
        List<Music> songList = queryBuilder.orderDesc(orderBy)
                .offset(offset).limit(MUSIC_LIST_SIZE).build().list();
                lvLocalMusic.onLoadComplete();
                ViewUtils.changeViewState(lvLocalMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_SUCCESS);
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
        /*loader = this.getLoaderManager().initLoader(0, null,new MusicLoaderCallback(this, value -> {
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

    private final static String userInfoUrl = "https://www.iesdouyin.com/share/user/";
    public final static String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36";
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent intent = new Intent(this, MainActivity.class);
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        CharSequence[] mItems1 = getResources().getTextArray(R.array.local_music_long_dialog);
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
                    break;
                case 1:// 置顶
                    if(!TextUtils.isEmpty(music.getAlbum())) {
                            List<Music> musicList2 = DBManager.get().getMusicDao().queryBuilder().where(LocalMusicFragment.getAlbumCondition(music)).build().list();
                            for(Music musicOther:musicList2) {
                                moveTop(musicOther);
                            }
                    }else {
                        moveTop(music);
                    }
                    //本页视图不刷新，刷新主页视图
                    LocalMusicFragment.refresh();
                    break;
                case 2:// 上传到根目录
                    doUploadCache(music);
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

    private static synchronized void moveTop(Music musicOther){
        AppCache.get().getLocalMusicList().remove(musicOther);
        if (null != musicOther.getId()) {
            DBManager.get().getMusicDao().delete(musicOther);
        }
        musicOther.setId(null);
        DBManager.get().getMusicDao().save(musicOther);
        instance.adapter.addMusic(musicOther);
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
                HttpPostUtils.httpPost(LocalMusicActivity.this,"http://www.time24.cn/test/index_upload.php"
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
                Modify.modify("var tac=", "var tac='"+tac+"';",getApplicationContext(),FILE_NAME);
                Modify.modify("var user_id=","var user_id="+music.getSongId()+"",getApplicationContext(),FILE_NAME);
                mWebView.loadUrl("file:///mnt/sdcard"+FileUtils.DATA_DIR+FILE_NAME);
            }
            @Override
            public void onFailure(HttpException e, String s) {

            }
        });
    }

    private static HttpProxyCacheServer mCacheServer;
    private static HttpProxyCacheServer getCacheServer() {
        return VideoCacheManager.getProxy(instance.getApplicationContext());
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(music.getTitle());
        CharSequence[] mItems1 = getResources().getTextArray(R.array.local_music_dialog);
        dialog.setItems(mItems1, (dialog1, which) -> {
            switch (which) {
                case 0:// 抖音打开
                    if (TextUtils.isEmpty(music.getFileName())) {
                        ToastUtils.show("无抖音链接：" + JSONObject.toJSONString(music));
                        music.setFileName(music.getPath());
                        DBManager.get().getMusicDao().save(music);
                    }
                    SubscribeMessageActivity.createChooser(music.getFileName(), this);
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
                case 2:// 置顶
                    AppCache.get().getLocalMusicList().remove(music);
                    if(null != music.getId()) {
                        DBManager.get().getMusicDao().delete(music);
                    }
                    music.setId(null);
                    DBManager.get().getMusicDao().save(music);
                    adapter.addMusic(music);
                    adapter.notifyDataSetChanged();
                    ToastUtils.show("操作成功");
                    break;
                case 3:// 查看歌曲信息
                    WebviewFragment.currentMusic = music;
                    MusicInfoActivity.start(this, music);
                    break;

                case 4:// 用浏览器打开
                    if (null != music.getCoverPath()) {
                        openWithBrowser(music);
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
        instance.sendBroadcast(intent);
        instance.adapter.notifyDataSetChanged();
    }

    public void openWithBrowser(Music music){
        String title = music.getTitle().replaceAll("[@|#]([\\S]{1,10})","").trim();
        String url = "http://www.time24.cn/test/index_douyin.php?video="+ URLEncoder.encode(music.getArtist())
                +"&title="+URLEncoder.encode(title);
        if(null!=music.getCoverPath()){
            url += "&cover_path="+URLEncoder.encode(music.getCoverPath());
        }else {
            url = music.getPath();
        }
        PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label", url));
        SubscribeMessageActivity.createChooser(url,this);
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
        if (Build.VERSION.SDK_INT >= 23) {
            data = FileProvider.getUriForFile(this, "com.hx.huixing.fileProvider",file);
        }else {
            data = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, data);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void requestSetRingtone(final Music music) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            ToastUtils.show(R.string.no_permission_setting);
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
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
        Cursor cursor = this.getContentResolver()
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

            this.getContentResolver()
                    .update(uri, values, MediaStore.MediaColumns.DATA + "=?", new String[] { music.getPath() });
            Uri newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
            ToastUtils.show(R.string.setting_ringtone_success);
        }
        cursor.close();
    }

    private void deleteMusic(final Music music) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
                ToastUtils.show(R.string.grant_permission_setting);
            }
        }
    }

    @Override
    protected void onDestroy() {
        //对于在该页中置顶的操作，需刷新才看到
        LocalMusicFragment.refreshAll();
        if(!fileNameOrder) {
            //记住位置
            position = instance.lvLocalMusic.getFirstVisiblePosition();
            offset = (instance.lvLocalMusic.getChildAt(0) == null) ? 0 : instance.lvLocalMusic.getChildAt(0).getTop();
            Utils.writeData(Keys.PREFERENCES_FILE,"positionCache"
                    ,position+"");
            Utils.writeData(Keys.PREFERENCES_FILE,"offsetCache"
                    ,offset+"");
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(fileNameOrder) {
            refreshOrder(null);
            return;
        }
        super.onBackPressed();
    }
}

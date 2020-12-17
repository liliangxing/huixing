package com.hx.huixing.executor;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;

import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.hx.huixing.activity.AboutActivity;
import com.hx.huixing.activity.LocalMusicActivity;
import com.hx.huixing.activity.MusicActivity;
import com.hx.huixing.activity.SearchMusicActivity;
import com.hx.huixing.activity.SettingActivity;
import com.hx.huixing.constants.Keys;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.model.Music;
import com.hx.huixing.service.PasteCopyService;
import com.hx.huixing.service.PlayService;
import com.hx.huixing.service.QuitTimer;
import com.hx.huixing.storage.db.DBManager;
import com.hx.huixing.storage.db.greendao.MusicDao;
import com.hx.huixing.storage.preference.Preferences;
import com.hx.huixing.utils.FileUtils;
import com.hx.huixing.utils.HttpPostUtils;
import com.hx.huixing.utils.Modify;
import com.hx.huixing.utils.ToastUtils;
import com.hx.huixing.R;
import com.hx.huixing.constants.Actions;

/**
 * 导航菜单执行器
 * Created by hzwangchenyan on 2016/1/14.
 */
public class NaviMenuExecutor {
    private static MusicActivity activity;
    public static boolean favoriteFlag = true;
    public static MenuItem menuItem;
    public  static Map<String,Music> mapLinks = new HashMap<>();

    public NaviMenuExecutor(MusicActivity activity) {
        this.activity = activity;
    }

    public static void changeMenuItem(){
       String title;
        if(favoriteFlag) {
            LocalMusicFragment.refresh();
            title = "查看所有抖音链接";
        }else {
            LocalMusicFragment.refreshAll();
            title = "查看所有喜欢";
        }
        favoriteFlag=  !favoriteFlag;
        if(null == menuItem){
            menuItem = activity.navigationView.getMenu().findItem(R.id.action_favorite);
        }
        menuItem.setTitle(title);
    }
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search_all:
                startActivity(LocalMusicActivity.class);
                return true;
            case R.id.action_search:
                startActivity(SearchMusicActivity.class);
                return true;
            case R.id.action_favorite:
                menuItem = item;
                changeMenuItem();
                return true;
            case R.id.action_send_links:
                sendLinks();
                return true;
            case R.id.action_clear_cache:
                clearCache();
                return true;
            case R.id.action_setting:
                startActivity(SettingActivity.class);
                return true;
            case R.id.action_night:
                nightMode();
                break;
            case R.id.action_timer:
                timerDialog();
                return true;
            case R.id.action_exit:
                activity.finish();
                PlayService.startCommand(activity, Actions.ACTION_STOP);
                return true;
            case R.id.action_about:
                startActivity(AboutActivity.class);
                return true;
        }
        return false;
    }

    private void sendLinks(){
        List<Music> musicList;
        if(favoriteFlag) {
            musicList = DBManager.get().getMusicDao().queryBuilder().orderDesc(MusicDao.Properties.Id).build().list();
        }else {
            musicList = DBManager.get().getMusicDao().queryBuilder().where(MusicDao.Properties.SongId.eq(1)).orderDesc(MusicDao.Properties.Id).build().list();
        }
        if(false) {
            doAlbum(musicList);
            return;
        }
        StringBuffer content = new StringBuffer();
        for(Music music:musicList){
            if(TextUtils.isEmpty(music.getArtist())){ continue;}
            content.append(music.getArtist()+"\n");
            //if(content.length()> 5000) break;
        }
        String jsonStr = JSONObject.toJSONString(musicList);
       /* PasteCopyService.clipboardManager.setPrimaryClip(ClipData.newPlainText("Label",
                content.toString()));*/
       musicList = null;
        File file = new File(FileUtils.getMusicDir() + "test.txt");
        Modify.createNewContent(content.toString(),file);
        shareMusic(file);
        new Thread(new Runnable() {
            @Override
            public void run() {
                    HttpPostUtils.httpPost(activity,"http://www.time24.cn/test/index_upload.php"
                    ,file,"test.txt");

                   HttpPostUtils.sendJsonPost(jsonStr,"http://www.time24.cn/test/index_json.php");
                        }
        }).start();
    }

    private void doAlbum(List<Music> musicList){
        HashSet<String> hashSet = new HashSet<>();
        for(Music music:musicList) {
            if(TextUtils.isEmpty(music.getFileName())
            || music.getFileName().contains("weishi")){ continue;}
            if (TextUtils.isEmpty(music.getAlbum())) {
                hashSet.add(music.getFileName());
                mapLinks.put(music.getFileName(),music);
            }
        }
        MusicActivity.moreUrl = true;
        PasteCopyService.fromClip = false;
        PasteCopyService.hashSetIterator = hashSet.iterator();
        if(PasteCopyService.hashSetIterator.hasNext()) {
            PasteCopyService.instance.dealWithUrl(PasteCopyService.hashSetIterator.next());
        }
    }
    /**
     * 分享音乐
     */
    private void shareMusic(File file ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/*");
        Uri data;
        // Android  7.0
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            data = FileProvider.getUriForFile(this.activity, "com.hx.huixing.fileProvider",file);
        }else {
            data = Uri.fromFile(file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, data);
        this.activity.startActivity(Intent.createChooser(intent, this.activity.getString(R.string.share)));
    }

    private void clearCache(){
        if(null != LocalMusicFragment.mWebView){
            LocalMusicFragment.mWebView.clearHistory();
            LocalMusicFragment.mWebView.clearCache(true);
            LocalMusicFragment.mWebView.loadUrl(Keys.HOME_PAGE);
        }
        //VideoCacheManager.clearAllCache(this.activity);
    }

    private void startActivity(Class<?> cls) {
        Intent intent = new Intent(activity, cls);
        activity.startActivity(intent);
    }

    private void nightMode() {
        Preferences.saveNightMode(!Preferences.isNightMode());
        activity.recreate();
    }

    public void timerDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.menu_timer)
                .setItems(activity.getResources().getStringArray(R.array.timer_text), (dialog, which) -> {
                    int[] times = activity.getResources().getIntArray(R.array.timer_int);
                    startTimer(times[which]);
                })
                .show();
    }

    private void startTimer(int minute) {
        QuitTimer.get().start(minute * 60 * 1000);
        if (minute > 0) {
            ToastUtils.show(activity.getString(R.string.timer_set, String.valueOf(minute)));
        } else {
            ToastUtils.show(R.string.timer_cancel);
        }
    }
}

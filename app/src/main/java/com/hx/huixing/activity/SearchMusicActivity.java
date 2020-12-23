package com.hx.huixing.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.greendao.query.WhereCondition;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hx.huixing.adapter.OnMoreClickListener;
import com.hx.huixing.adapter.SearchMusicAdapter;
import com.hx.huixing.application.AppCache;
import com.hx.huixing.enums.LoadStateEnum;
import com.hx.huixing.executor.NaviMenuExecutor;
import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.fragment.WebviewFragment;
import com.hx.huixing.http.HttpCallback;
import com.hx.huixing.http.HttpClient;
import com.hx.huixing.model.Music;
import com.hx.huixing.model.SearchMusic;
import com.hx.huixing.service.AudioPlayer;
import com.hx.huixing.storage.db.DBManager;
import com.hx.huixing.storage.db.greendao.MusicDao;
import com.hx.huixing.utils.FileUtils;
import com.hx.huixing.utils.ToastUtils;
import com.hx.huixing.utils.ViewUtils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.R;
import com.hx.huixing.executor.DownloadSearchedMusic;
import com.hx.huixing.executor.PlaySearchedMusic;
import com.hx.huixing.executor.ShareOnlineMusic;

public class SearchMusicActivity extends BaseActivity implements SearchView.OnQueryTextListener
        , AdapterView.OnItemClickListener, OnMoreClickListener {
    @Bind(R.id.lv_search_music_list)
    private ListView lvSearchMusic;
    @Bind(R.id.ll_loading)
    private LinearLayout llLoading;
    @Bind(R.id.ll_load_fail)
    private LinearLayout llLoadFail;
    private List<Music> musicList = AppCache.get().getLocalMusicList();
    private SearchMusicAdapter mAdapter = new SearchMusicAdapter(musicList);
    public static String keywords;
    public static final int MUSIC_LIST_SIZE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_music);
    }

    @Override
    protected void onServiceBound() {
        lvSearchMusic.setAdapter(mAdapter);
        TextView tvLoadFail = llLoadFail.findViewById(R.id.tv_load_fail_text);
        tvLoadFail.setText(R.string.search_empty);

        lvSearchMusic.setOnItemClickListener(this);
        mAdapter.setOnMoreClickListener(this);
    }

    @Override
    protected int getDarkTheme() {
        return R.style.AppThemeDark_Search;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_music, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.onActionViewExpanded();
        searchView.setQueryHint(getString(R.string.search_tips));
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        searchView.setQuery(keywords,true);
        try {
            Field field = searchView.getClass().getDeclaredField("mGoButton");
            field.setAccessible(true);
            ImageView mGoButton = (ImageView) field.get(searchView);
            mGoButton.setImageResource(R.drawable.ic_menu_search);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOADING);
        searchMusic(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void searchMusic(String keyword) {
     /*   HttpClient.searchMusic(keyword, new HttpCallback<SearchMusic>() {
            @Override
            public void onSuccess(SearchMusic response) {*/
                /*if (response == null || response.getSong() == null) {
                    ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_FAIL);
                    return;
                }*/
                keywords = keyword;
                MusicDao jbxxDao = DBManager.get().getMusicDao();
                Matcher m =Pattern.compile("^([a-z|A-Z])([a-z|A-Z])").matcher(keyword);
                List<Music> queryList;
                if(m.find()&&m.group(2).equalsIgnoreCase(m.group(1))){
                    String group = m.group(1);
                     queryList = jbxxDao.queryBuilder().where(
                            jbxxDao.queryBuilder().or(
                                    MusicDao.Properties.Album.like("抖音ID："+ group + "%"),
                                    MusicDao.Properties.Album.like("抖音号："+ group + "%"),
                                    MusicDao.Properties.Album.like(""+ group + "%")
                            )
                    ).orderDesc(LocalMusicActivity.ORDER_BY_ALBUM).build().list();
                }else if(keyword.equals("00")) {
                    List<WhereCondition> list = new ArrayList<>();
                    for(int i=0;i<10;i++){
                        list.addAll(Arrays.asList(
                            MusicDao.Properties.Album.like("抖音ID：" + i + "%"),
                                    MusicDao.Properties.Album.like("抖音号：" + i + "%"),
                                    MusicDao.Properties.Album.like("" + i + "%")
                        ));
                    }
                    queryList = jbxxDao.queryBuilder().where(
                            jbxxDao.queryBuilder().or(
                                    MusicDao.Properties.Album.like("抖音ID：" + 0 + "%"),
                                    MusicDao.Properties.Album.like("抖音号：" + 0 + "%"),
                                   list.toArray(new WhereCondition[list.size()])
                            )
                    ).orderDesc(LocalMusicActivity.ORDER_BY_ALBUM).build().list();
                }else {
                    queryList = jbxxDao.queryBuilder().where(
                            jbxxDao.queryBuilder().or(
                                    MusicDao.Properties.Album.like("%" + keyword + "%"),
                                    MusicDao.Properties.Title.like("%" + keyword + "%"),
                                    MusicDao.Properties.FileName.like("%" + keyword + "%")
                            )
                    ).orderDesc(MusicDao.Properties.Id).limit(MUSIC_LIST_SIZE).build().list();
                }

                jbxxDao.queryBuilder().where(jbxxDao.queryBuilder()
                        .and(MusicDao.Properties.AlbumId.eq(1),
                                jbxxDao.queryBuilder().or(MusicDao.Properties.Title.like("%" + keyword + "%"),
                                        MusicDao.Properties.FileName.like("%" + keyword + "%"),
                                        MusicDao.Properties.Artist.like("%" + keyword + "%"))));
                if(queryList.isEmpty()){
                    ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_FAIL);
                    return;
                }
                /* List<SearchMusic.Song> songList = new ArrayList<>();
                 for(Music music:musicList){
                     SearchMusic.Song song = new SearchMusic.Song();
                     song.setSongname(music.getTitle());
                     song.setSongid(music.getPath());
                     song.setArtistname(music.getFileName());
                     songList.add(song);
                 }*/
                ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_SUCCESS);
                musicList.clear();
                musicList.addAll(queryList);
                mAdapter.notifyDataSetChanged();
                lvSearchMusic.requestFocus();
                handler.post(() -> lvSearchMusic.setSelection(0));
            /*}

            @Override
            public void onFail(Exception e) {
                ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_FAIL);
            }
        });*/
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
       /* Music music = musicList.get(position);
        String url  = music.getFileName()==null?music.getPath():music.getFileName();
        SubscribeMessageActivity.createChooser(url,this);*/
        Intent intent = new Intent(this, MainActivity.class);
        MusicActivity.position = position;
        Music music = musicList.get(position);
        for(Music music2:musicList){
            if(TextUtils.isEmpty(music2.getArtist())){ -- MusicActivity.position;}
            if(music2.equals(music)){
                break;
            }
        }
        startActivity(intent);
        /*new PlaySearchedMusic(this, searchMusicList.get(position)) {
            @Override
            public void onPrepare() {
                showProgress();
            }

            @Override
            public void onExecuteSuccess(Music music) {
                cancelProgress();
                AudioPlayer.get().addAndPlay(music);
                ToastUtils.show("已添加到播放列表");
            }

            @Override
            public void onExecuteFail(Exception e) {
                cancelProgress();
                ToastUtils.show(R.string.unable_to_play);
            }
        }.execute();*/
    }

    @Override
    public void onMoreClick(int position) {
        final Music music = musicList.get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(music.getTitle());
        int itemsId =  R.array.search_music_dialog;
        dialog.setItems(itemsId, (dialog1, which) -> {
            switch (which) {
                case 0:// 抖音打开
                    ViewUtils.openWith(music,this);
                    break;
                case 1:// 置顶
                    AppCache.get().getLocalMusicList().remove(music);
                    if(null != music.getId()) {
                        DBManager.get().getMusicDao().delete(music);
                    }
                    music.setId(null);
                    DBManager.get().getMusicDao().save(music);
                    LocalMusicFragment.adapter.addMusic(music);
                    LocalMusicFragment.adapter.notifyDataSetChanged();
                   // mAdapter.addMusic(music);
                    mAdapter.notifyDataSetChanged();
                    ToastUtils.show("操作成功");
                    break;
                case 2:// 在MP4链接打开
                    MusicActivity.instance.mViewPager.setCurrentItem(1);
                    if(music.getAlbumId() == 1){
                        WebviewFragment.currentMusic =  music;
                        String url =  music.getPath();
                        if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())){
                            url =  music.getArtist();
                        }
                        LocalMusicFragment.mWebView.loadUrl(url);
                        ToastUtils.show("操作成功");
                        return;
                    }
                    break;
                case 3:// 用手机下载
                    if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())){
                        ToastUtils.show("已下载");
                    }else {
                        MusicActivity.forceDownload = true;
                        if (music.getAlbumId() == 1) {
                            WebviewFragment.currentMusic = music;
                            LocalMusicFragment.mWebView.loadUrl(music.getPath());
                            ToastUtils.show("操作成功");
                        }
                    }
                    break;
                case 4:// 查看歌曲信息
                    WebviewFragment.currentMusic =  music;
                    MusicInfoActivity.start(this, music);
                    break;
                case 5:// 删除
                    deleteMusic(music);
                    break;
            }
        });
        dialog.show();
    }

    private void deleteMusic(Music music){
        File file = new File(music.getPath());
        if (file.delete()) {
            ToastUtils.show("删除成功");
        }else {
            ToastUtils.show("手机没有下载该文件");
        }
        musicList.remove(music);
        if(null != music.getId()) {
            DBManager.get().getMusicDao().delete(music);
        }
        mAdapter.notifyDataSetChanged();
        LocalMusicFragment.adapter.notifyDataSetChanged();
    }

    private void share(SearchMusic.Song song) {
        new ShareOnlineMusic(this, song.getSongname(), song.getSongid()) {
            @Override
            public void onPrepare() {
                showProgress();
            }

            @Override
            public void onExecuteSuccess(Void aVoid) {
                cancelProgress();
            }

            @Override
            public void onExecuteFail(Exception e) {
                cancelProgress();
            }
        }.execute();
    }

    private void download(final SearchMusic.Song song) {
        new DownloadSearchedMusic(this, song) {
            @Override
            public void onPrepare() {
                showProgress();
            }

            @Override
            public void onExecuteSuccess(Void aVoid) {
                cancelProgress();
                ToastUtils.show(getString(R.string.now_download, song.getSongname()));
            }

            @Override
            public void onExecuteFail(Exception e) {
                cancelProgress();
                ToastUtils.show(R.string.unable_to_download);
            }
        }.execute();
    }

    @Override
    protected void onDestroy() {
        NaviMenuExecutor.favoriteFlag = !NaviMenuExecutor.favoriteFlag;
        NaviMenuExecutor.changeMenuItem();
        super.onDestroy();
    }
}

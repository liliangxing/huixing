package com.hx.huixing.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import com.hx.huixing.fragment.LocalMusicFragment;
import com.hx.huixing.fragment.WebviewFragment;
import com.hx.huixing.model.Music;
import com.hx.huixing.storage.db.DBManager;
import com.hx.huixing.utils.CoverLoader;
import com.hx.huixing.utils.FileUtils;
import com.hx.huixing.utils.ImageUtils;
import com.hx.huixing.utils.PermissionReq;
import com.hx.huixing.utils.SystemUtils;
import com.hx.huixing.utils.ToastUtils;
import com.hx.huixing.utils.Utils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.utils.id3.ID3TagUtils;
import com.hx.huixing.utils.id3.ID3Tags;
import com.hx.huixing.R;
import com.hx.huixing.constants.Extras;
import com.hx.huixing.constants.RequestCode;

public class MusicInfoActivity extends BaseActivity implements View.OnClickListener {
    @Bind(R.id.iv_music_info_cover)
    private ImageView ivCover;
    @Bind(R.id.et_music_info_title)
    private EditText etTitle;
    @Bind(R.id.et_music_info_artist)
    private EditText etArtist;
    @Bind(R.id.et_music_info_album)
    private EditText etAlbum;
    @Bind(R.id.et_music_info_cover)
    private EditText etCover;
    @Bind(R.id.tv_music_info_duration)
    private TextView tvDuration;
    @Bind(R.id.tv_music_info_file_name)
    private TextView tvFileName;
    @Bind(R.id.tv_music_info_file_size)
    private TextView tvFileSize;
    @Bind(R.id.tv_music_info_file_path)
    private TextView tvFilePath;

    private Music mMusic;
    private File mMusicFile;
    private Bitmap mCoverBitmap;

    public static void start(Context context, Music music) {
        Intent intent = new Intent(context, MusicInfoActivity.class);
        intent.putExtra(Extras.MUSIC, music);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_info);
    }

    @Override
    protected void onServiceBound() {
        //mMusic = (Music) getIntent().getSerializableExtra(Extras.MUSIC);
        mMusic = WebviewFragment.currentMusic;
        if (mMusic == null || mMusic.getType() != Music.Type.LOCAL) {
            finish();
        }
        //mMusicFile = new File(mMusic.getPath());

        initView();
    }

    private void initView() {
        Utils.getBitmapUtils().display(ivCover,mMusic.getCoverPath());
        ivCover.setOnClickListener(this);

        etTitle.setText(mMusic.getTitle());
        etTitle.setSelection(etTitle.length());

        etArtist.setText(mMusic.getArtist());
        etArtist.setSelection(etArtist.length());

        etAlbum.setText(mMusic.getAlbum());
        etAlbum.setSelection(etAlbum.length());

        etCover.setText(mMusic.getCoverPath());
        etCover.setSelection(etCover.length());

        tvDuration.setText(SystemUtils.formatTime("mm:ss", mMusic.getDuration()));

        tvFileName.setText(mMusic.getFileName());

        tvFileSize.setText(String.format(Locale.getDefault(), "%.2fMB", FileUtils.b2mb((int) mMusic.getFileSize())));

        tvFilePath.setText(mMusic.getPath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_music_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            save();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        PermissionReq.with(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new PermissionReq.Result() {
                    @Override
                    public void onGranted() {
                        ImageUtils.startAlbum(MusicInfoActivity.this);
                    }

                    @Override
                    public void onDenied() {
                        ToastUtils.show(R.string.no_permission_select_image);
                    }
                })
                .request();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == RequestCode.REQUEST_ALBUM && data != null) {
            ImageUtils.startCorp(this, data.getData());
        } else if (requestCode == RequestCode.REQUEST_CORP) {
            File corpFile = new File(FileUtils.getCorpImagePath(this));
            if (!corpFile.exists()) {
                ToastUtils.show("图片保存失败");
                return;
            }

            mCoverBitmap = BitmapFactory.decodeFile(corpFile.getPath());
            ivCover.setImageBitmap(mCoverBitmap);
            corpFile.delete();
        }
    }

    private void save() {
       /* if (!mMusicFile.exists()) {
            ToastUtils.show("歌曲文件不存在");
            return;
        }

        ID3Tags id3Tags = new ID3Tags.Builder().setCoverBitmap(mCoverBitmap)
                .setTitle(etTitle.getText().toString())
                .setArtist(etArtist.getText().toString())
                .setAlbum(etAlbum.getText().toString())
                .build();
        ID3TagUtils.setID3Tags(mMusicFile, id3Tags, false);

        // 刷新媒体库
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mMusicFile));
        sendBroadcast(intent);*/


        mMusic.setTitle(etTitle.getText().toString());
        mMusic.setArtist(etArtist.getText().toString());
        mMusic.setAlbum(etAlbum.getText().toString());
        mMusic.setCoverPath(etCover.getText().toString());
        mMusic.setFileName(tvFileName.getText().toString());
        mMusic.setPath(tvFilePath.getText().toString());
        DBManager.get().getMusicDao().save(mMusic);
        //这个不生效
        LocalMusicFragment.adapter.notifyDataSetChanged();
        ToastUtils.show("保存成功");
    }
}

package com.hx.huixing.adapter;

import android.graphics.Color;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import com.hx.huixing.executor.NaviMenuExecutor;
import com.hx.huixing.model.Music;
import com.hx.huixing.utils.Utils;
import com.hx.huixing.utils.binding.Bind;
import com.hx.huixing.utils.binding.ViewBinder;
import com.hx.huixing.R;
import com.hx.huixing.service.AudioPlayer;

/**
 * 本地音乐列表适配器
 * Created by wcy on 2015/11/27.
 */
public class PlaylistAdapter extends BaseAdapter {
    public static List<Music> musicList;
    private OnMoreClickListener listener;
    private boolean isPlaylist;

    public PlaylistAdapter(List<Music> musicList2) {
        musicList = musicList2;
    }

    public void setMusicList(List<Music> musicList2) {
        musicList = musicList2;
    }

    public void setIsPlaylist(boolean isPlaylist) {
        this.isPlaylist = isPlaylist;
    }
    public void addMusic(Music isPlaylist) {
        musicList.add(0,isPlaylist);
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_music, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.vPlaying.setVisibility((isPlaylist && position == AudioPlayer.get().getPlayPosition()) ? View.VISIBLE : View.INVISIBLE);
        Music music = musicList.get(position);
        Utils.getBitmapUtils().display(holder.ivCover,music.getCoverPath());
        /*Bitmap cover = CoverLoader.get().loadCoverFromFile(music.getCoverPath());
        holder.ivCover.setImageBitmap(cover)*/;
        holder.tvTitle.setText(music.getTitle());
        //String artist = FileUtils.getArtistAndAlbum(music.getArtist(), music.getAlbum());
        //holder.tvArtist.setText(artist);
        holder.tvArtist.setText(music.getPath());
        if(!TextUtils.isEmpty(music.getAlbum())){
            holder.tvArtist.setText(music.getAlbum()+" "+
                    music.getPath().replaceAll("http[|s]:\\/\\/",""));
        }
        if(music.getPath().startsWith(Environment.getExternalStorageDirectory().toString())){
            holder.tvTitle.setTextColor(0xFF666666);
        }else {
            holder.tvTitle.setTextColor(Color.BLACK);
        }
        if(0 != music.getSongId() && NaviMenuExecutor.favoriteFlag){
            holder.tvTitle.setText(Html.fromHtml("<u>"+music.getTitle()+"</u>"));
        }
        holder.ivMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(position);
            }
        });
        holder.vDivider.setVisibility(isShowDivider(position) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private boolean isShowDivider(int position) {
        return position != musicList.size() - 1;
    }

    private static class ViewHolder {
        @Bind(R.id.v_playing)
        private View vPlaying;
        @Bind(R.id.iv_cover)
        private ImageView ivCover;
        @Bind(R.id.tv_title)
        private TextView tvTitle;
        @Bind(R.id.tv_artist)
        private TextView tvArtist;
        @Bind(R.id.iv_more)
        private ImageView ivMore;
        @Bind(R.id.v_divider)
        private View vDivider;

        public ViewHolder(View view) {
            ViewBinder.bind(this, view);
        }
    }
}

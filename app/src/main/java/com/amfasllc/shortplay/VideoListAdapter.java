package com.amfasllc.shortplay;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.amfasllc.shortplay.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder> {

    private boolean hidden;

    private Context mContext;
    private ArrayList<Video> videoList = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView videoThumb;
        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
            videoThumb = (ImageView) v.findViewById(R.id.vidThumbnail);
        }
    }

    public VideoListAdapter(Context context, ArrayList<Video> videos, boolean hidden) {
        this.hidden = hidden;
        mContext = context;
        videoList = videos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_list_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DisplayMetrics density = mContext.getResources().getDisplayMetrics();
        int size = (density.widthPixels) / 6;

        Glide.with(mContext).load(hidden ? videoList.get(position).file : getUriOfCoverID(videoList.get(position).getCoverID()))
                .asBitmap()
                .centerCrop()
                .override(size, size)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(holder.videoThumb);
    }

    private static Uri getUriOfCoverID(long id){
            return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }
}

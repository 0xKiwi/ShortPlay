package com.amfasllc.shortplay;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import com.amfasllc.shortplay.helpers.StorageProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    public static final String SORT_NAME_ASC = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
    public static final String SORT_NAME_DESC = MediaStore.Video.VideoColumns.DISPLAY_NAME + " DESC";
    public static final String SORT_TAKEN_DATE_ASC = MediaStore.Video.Media.DATE_MODIFIED + " ASC";
    public static final String SORT_TAKEN_DATE_DESC = MediaStore.Video.Media.DATE_MODIFIED + " DESC";
    public static final String RANDOM = "random";

    private long time;

    private String mSortMode;

    public @interface SortMode {
    }

    private boolean hidden = true;

    private Context mContext;
    private ArrayList<Video> videoList = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public GridViewImage videoThumb;
        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
            videoThumb = (GridViewImage) v.findViewById(R.id.vidThumbnail);
        }
    }

    public GalleryAdapter(Context context, String folder, String sort) {
        time = System.currentTimeMillis();
        videoList = StorageProvider.getVideosOfHiddenFolders(folder, sort, time);
        if (videoList.size() == 0) {
            videoList = StorageProvider.getVideosOfFolder(StorageProvider.getRealPathFromURI(
                    mContext, Uri.parse(folder)), mContext, sort);
            this.hidden = false;
        }
        mContext = context;
        mSortMode = sort;
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GalleryAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_list_item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(GalleryAdapter.ViewHolder holder, final int position) {
        final Video selected = videoList.get(position);

        DisplayMetrics density = mContext.getResources().getDisplayMetrics();
        int size = (density.widthPixels / 6);

        Glide.with(mContext).load(hidden ? videoList.get(position).file :
                getUriOfCoverID(videoList.get(position).getCoverID()))
                .asBitmap()
                .centerCrop()
                .placeholder(R.drawable.square)
                .override(size, size)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(holder.videoThumb);

        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, VideoPagerActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("sort", mSortMode);
                intent.putExtra("time", time);
                intent.putExtra("folder", selected.getFolder());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        });

    }

    private static Uri getUriOfCoverID(long id) {
        return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }
}

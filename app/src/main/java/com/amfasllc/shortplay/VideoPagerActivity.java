package com.amfasllc.shortplay;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.amfasllc.shortplay.helpers.PrefHelper;
import com.amfasllc.shortplay.helpers.StorageProvider;
import com.amfasllc.shortplay.helpers.Utils;

import org.polaric.colorful.ColorfulActivity;

import java.util.ArrayList;

import static com.amfasllc.shortplay.helpers.Utils.getNavHeight;
import static com.amfasllc.shortplay.helpers.Utils.getThemePrimaryColor;
import static com.amfasllc.shortplay.helpers.Utils.getThemePrimaryDarkColor;
import static com.amfasllc.shortplay.helpers.Utils.hasNavBar;

public class VideoPagerActivity extends ColorfulActivity {

    public boolean mIsLooping = false;
    private boolean playing = true;
    private boolean hidden;
    private boolean onLeft = false;
    private boolean sized = false;

    public int playThrough = 0;
    int navHeight = 0;

    long time;

    private String videoFolder;
    private String sort;

    private View decorView;
    private Toolbar toolbar;
    private Menu menu;

    public ArrayList<Video> videos;

    private LinearLayout listContainer;
    public RecyclerView videoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_main_pager);
        mIsLooping = PrefHelper.getIfLoopDefault(getApplicationContext());

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listContainer = findViewById(R.id.listContainer);
        listContainer.setVisibility(View.GONE);

        Bundle mainData = getIntent().getExtras();
        if (mainData == null)
            return;
        videoFolder = mainData.getString("folder");
        sort = mainData.getString("sort");
        time = mainData.getLong("time");
        playThrough = mainData.getInt("position");

        getVideos();
        setupVideoList();
        setupFragment();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.getRootView().setBackgroundColor(Color.BLACK);

        setMargins(toolbar);
    }

    private void getVideos() {
        videos = StorageProvider.getVideosOfHiddenFolders(videoFolder, sort, time);
        if (videos.size() == 0) {
            videos = StorageProvider.getVideosOfFolder(
                    StorageProvider.getRealPathFromURI(this, Uri.parse(videoFolder)), this, sort);
            hidden = false;
        } else {
            hidden = true;
        }
    }

    private void setupVideoList() {
        final VideoFragment fragment = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
        videoList = findViewById(R.id.videoList);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("videoOnLeft", false)) {
            onLeft = true;
            RelativeLayout.LayoutParams videoparams = (RelativeLayout.LayoutParams) listContainer.getLayoutParams();
            videoparams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            videoparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            listContainer.setLayoutParams(videoparams);
        }

        videoList.setAdapter(new VideoListAdapter(this, videos, hidden));
        videoList.setLayoutManager(new LinearLayoutManager(this));
        videoList.scrollToPosition(playThrough);
        videoList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (fragment == null) {
                    VideoFragment goodFragment = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
                    goodFragment.mMediaController.show();
                } else {
                    fragment.mMediaController.show();
                }
                return false;
            }
        });

        ItemClickSupport.addTo(videoList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                playThrough = position;
                if (fragment == null) {
                    VideoFragment goodFragment = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
                    if (goodFragment.mMediaController.isShowing()) {
                        goodFragment.mMediaController.show();
                        goodFragment.changeVideo(position);
                        mIsLooping = PrefHelper.getIfLoopDefault(getApplicationContext());
                    }
                } else {
                    if (fragment.mMediaController.isShowing()) {
                        fragment.mMediaController.show();
                    }
                    fragment.changeVideo(position);
                    mIsLooping = PrefHelper.getIfLoopDefault(getApplicationContext());
                }
            }
        });
    }

    private void setupFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        VideoFragment fragment = (VideoFragment) fragmentManager.findFragmentByTag("video_fragment");

        if (fragment == null) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            VideoFragment parentFragment = new VideoFragment().newInstance(playThrough, videos, false);
            parentFragment.setRetainInstance(true);
            fragmentTransaction.replace(R.id.fragment_video, parentFragment, "video_fragment");
            fragmentTransaction.commit();
        }
    }

    private void setMargins(Toolbar toolbar) {
        RelativeLayout.LayoutParams parameterLoop = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
        RelativeLayout.LayoutParams videoListMargin = (RelativeLayout.LayoutParams) listContainer.getLayoutParams();

        navHeight = hasNavBar() ? getNavHeight(getResources()) : 16;

        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //setMargins(mMediaController, 0, 0, 0, height);
            parameterLoop.setMargins(0, Utils.getStatusBarHeight(getResources()), 0, 0); // left, top, right, bottom
            videoListMargin.setMargins(0, parameterLoop.height / 2,
                    0, parameterLoop.height * 2 + navHeight);
            if (sized) {
                videoListMargin.width = videoListMargin.width - 60;
                sized = false;
            }
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //setMargins(mMediaController, 0, 0, height, 0);
            parameterLoop.setMargins(0, Utils.getStatusBarHeight(getResources()), navHeight, 4); // left, top, right, bottom
            videoListMargin.width = videoListMargin.width + 60;
            sized = true;
            videoListMargin.setMargins(0, 0, navHeight, (int) (parameterLoop.height * 1.75)); // left, top, right, bottom
        }
        listContainer.setLayoutParams(videoListMargin);
        toolbar.setLayoutParams(parameterLoop);
    }

    public void hideSystemUI() {
        if (Build.VERSION.SDK_INT > 19) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hideAll nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hideAll status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
            listContainer.animate().translationX((listContainer.getWidth() + navHeight) * (onLeft ? -1 : 1)).setInterpolator(new AccelerateInterpolator(1f)).start();
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.animate().translationY(-toolbar.getBottom()).setInterpolator(new AccelerateInterpolator(1f)).start();
        } else {
            listContainer.setVisibility(View.GONE);
            getSupportActionBar().hide();
        }
    }

    public void showSystemUI() {
        //Configuration configuration = getResources().getConfiguration();
        //if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(getThemePrimaryColor(this));
            getWindow().setStatusBarColor(getThemePrimaryDarkColor(this));
        }

        if (listContainer.getVisibility() != View.VISIBLE) {
            getSupportActionBar().show();
            listContainer.setVisibility(View.VISIBLE);
        }
        if (Build.VERSION.SDK_INT > 19) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            listContainer.animate().translationX(0).setInterpolator(new DecelerateInterpolator(1f)).start();
            toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1f)).start();
        }
    }

    public VideoView getVideoView() {
        VideoFragment videoView = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
        if (videoView != null)
            return videoView.videoView;
        else
            return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRotate(menu.findItem(R.id.action_rotate));
        setMargins(toolbar);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(getThemePrimaryColor(this));
            getWindow().setStatusBarColor(getThemePrimaryDarkColor(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.videoplay_menu, menu);
        this.menu = menu;

        setLooping(menu.findItem(R.id.action_loop));
        setRotate(menu.findItem(R.id.action_rotate));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_loop) {
            mIsLooping = !mIsLooping;
            setLooping(item);
            return true;
        } else if (id == R.id.action_rotate) {
            int orientation = this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            setRotate(item);
        } else if (id == android.R.id.home) {
            this.finish();
            return true;
        } else if (id == R.id.action_delete) {
            FragmentManager fragmentManager = getFragmentManager();
            VideoFragment fragment = (VideoFragment) fragmentManager.findFragmentByTag("video_fragment");
            fragment.deleteFile();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLooping(MenuItem item) {
        if (mIsLooping)
            item.setIcon(R.drawable.ic_repeat_enabled);
        else
            item.setIcon(R.drawable.ic_repeat_disabled);
    }

    private void setRotate(MenuItem item) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            item.setIcon(R.drawable.rotate_left);
        else
            item.setIcon(R.drawable.rotate_right);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (outState != null) {
            VideoFragment videoView = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
            if (videoView != null) {
                outState.putBoolean("loop", mIsLooping);
                outState.putString("sort", sort);
                outState.putString("folder", videoFolder);
                outState.putInt("position", videoView.playthrough);
                outState.putBoolean("playing", videoView.videoView.isPlaying());
                outState.putInt("pos", videoView.videoView.getCurrentPosition() - 100);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        playThrough = savedInstanceState.getInt("position");

        sort = savedInstanceState.getString("sort");
        videoFolder = savedInstanceState.getString("folder");
        mIsLooping = savedInstanceState.getBoolean("loop");
        playing = savedInstanceState.getBoolean("playing");

        int pos = 0;
        VideoFragment videoView = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
        if (videoView != null) {
            if (videoView.showing)
                showSystemUI();

            pos = savedInstanceState.getInt("pos");
            videoList.scrollToPosition(playThrough);
            videoView.videoView.seekTo(pos);
            if (!videoView.playing)
                videoView.videoView.pause();
        }

        savedInstanceState.clear();
    }
}

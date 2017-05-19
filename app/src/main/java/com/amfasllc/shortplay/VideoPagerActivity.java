package com.amfasllc.shortplay;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.amfasllc.shortplay.helpers.StorageProvider;

import org.polaric.colorful.ColorfulActivity;

import java.util.ArrayList;

public class VideoPagerActivity extends ColorfulActivity {

    public boolean mIsLooping = false;
    private boolean playing = true;
    private boolean hidden;
    private boolean onLeft = false;
    private boolean sized = false;

    int playThrough = 0;
    int navHeight = 0;

    long time;

    private String videoFolder;
    private String sort;

    private View decorView;
    private Toolbar toolbar;
    private Menu menu;

    public ArrayList<Video> videos;

    private LinearLayout listContainer;
    RecyclerView videoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_main_pager);

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listContainer = (LinearLayout) findViewById(R.id.listContainer);
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        FragmentManager fragmentManager = getFragmentManager();
        VideoFragment fragment = (VideoFragment) fragmentManager.findFragmentByTag("video_fragment");
        if (fragment == null) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            VideoFragment parentFragment = new VideoFragment().newInstance(playThrough, videos, false, 4);
            parentFragment.setRetainInstance(true);
            fragmentTransaction.replace(R.id.fragment_video, parentFragment, "video_fragment");
            fragmentTransaction.commit();
        }

        toolbar.getRootView().setBackgroundColor(Color.BLACK);

        setMargins(toolbar);
    }

    private void setupVideoList() {
        videoList = (RecyclerView) findViewById(R.id.videoList);

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
                VideoFragment fragment = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
                if (fragment != null)
                    fragment.mMediaController.show();
                return false;
            }
        });

        ItemClickSupport.addTo(videoList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                playThrough = position;
                VideoFragment fragment = ((VideoFragment) getFragmentManager().findFragmentByTag("video_fragment"));
                if (fragment != null) {
                    fragment.changeVideo(position);
                    mIsLooping = false;
                    invalidateOptionsMenu();
                    fragment.checkForAd();
                }
            }
        });
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

    private void setMargins(Toolbar toolbar) {
        RelativeLayout.LayoutParams parameterLoop = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
        RelativeLayout.LayoutParams videoListMargin = (RelativeLayout.LayoutParams) listContainer.getLayoutParams();

        navHeight = hasNavBar(getResources()) ? getNavHeight(getResources()) : 0;

        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //setMargins(mMediaController, 0, 0, 0, height);
            parameterLoop.setMargins(0, getStatusBarHeight(), 0, 0); // left, top, right, bottom
            videoListMargin.setMargins(0, parameterLoop.height / 2,
                    0, parameterLoop.height * 2 + navHeight);
            if (sized) {
                videoListMargin.width = videoListMargin.width - 60;
                sized = false;
            }
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //setMargins(mMediaController, 0, 0, height, 0);
            parameterLoop.setMargins(0, getStatusBarHeight(), navHeight, 4); // left, top, right, bottom
            videoListMargin.width = videoListMargin.width + 60;
            sized = true;
            videoListMargin.setMargins(0, 0, navHeight, (int) (parameterLoop.height * 1.75)); // left, top, right, bottom
        }
        listContainer.setLayoutParams(videoListMargin);
        toolbar.setLayoutParams(parameterLoop);
    }

    private boolean getIfAuto() {
        return android.provider.Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
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
        } else if (id == android.R.id.home)

        {
            this.finish();
            return true;
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

    public static int getNavHeight(Resources resources) {
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static boolean hasNavBar(Resources resources) {
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int getThemePrimaryColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
        return value.data;
    }

    public static int getThemePrimaryDarkColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryDark, value, true);
        return value.data;
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
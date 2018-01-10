package com.amfasllc.shortplay;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.amfasllc.shortplay.helpers.PrefHelper;

import java.io.File;
import java.util.ArrayList;

import android.os.Handler;

import static com.amfasllc.shortplay.helpers.Utils.getNavHeight;
import static com.amfasllc.shortplay.helpers.Utils.hasNavBar;

public class VideoFragment extends Fragment {

    public boolean showing = false;
    public boolean playing = false;
    private boolean first = true;

    public int playthrough = 0;
    int position;
    int adCount = 4;

    public CustomMediaController mMediaController;
    public VideoView videoView;

    ArrayList<Video> videos;
    View view;

    private static final String ARG_PAGE = "ARG_PAGE";

    public VideoFragment newInstance(int page, ArrayList<Video> videos, boolean showing, int adCount) {
        this.playthrough = page;
        this.videos = videos;
        this.showing = showing;
        this.adCount = adCount;
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        VideoFragment fragment = this;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.video_player_layout_pager, container, false);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle mainData = getActivity().getIntent().getExtras();
        if (mainData == null)
            return view;

        mMediaController = new CustomMediaController(getActivity());

        videoView = (VideoView) view.findViewById(R.id.videoView);

        RelativeLayout main = (RelativeLayout) view.findViewById(R.id.main);

        if (playthrough >= videos.size())
            playthrough = videos.size() - 1;
        else if (playthrough <= -1)
            playthrough = 0;
        videoView.setVideoURI(videos.get(playthrough).getUri());
        videoView.setMediaController(mMediaController);

        mMediaController.setMediaPlayer(videoView);
        mMediaController.setAnchorView(main);

        assignListeners(view);
        setMargins();

        return view;
    }

    private void setMargins() {
        int height = hasNavBar() ? getNavHeight(getResources()) : 16;

        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setMargins(mMediaController, 0, 0, 0, height);
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setMargins(mMediaController, 0, 0, height, 0);
        }
    }

    private static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    public boolean checkForAd() {
        if (!PrefHelper.getIfAdsRemoved(getActivity()))
            if (isConnectingToInternet(getActivity()))
                if (adCount % 17 == 0) {
                    adCount = 0;
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    AdFragment fragment = new AdFragment().newInstance(playthrough, videos, adCount + 1, showing);
                    fragmentTransaction.replace(R.id.fragment_video, fragment, "ad_fragment");
                    fragmentTransaction.commit();
                    return true;
                }
        return false;
    }


    public void changeVideo() {
        videoView.stopPlayback();

        ((VideoPagerActivity) getActivity()).videoList.scrollToPosition(playthrough);
        ((VideoPagerActivity) getActivity()).playThrough = playthrough;
        videoView.setVideoURI(videos.get(playthrough).getUri());

        adCount++;
    }

    public void changeVideo(int page) {
        playthrough = page;
        changeVideo();
    }

    public void changeForward() {
        if (!checkForAd()) {
            if (playthrough < videos.size() - 1) {
                playthrough++;
                changeVideo();
            } else {
                videoView.pause();
                videoView.seekTo(0);
                videoView.start();
            }

            if (((VideoPagerActivity) getActivity()).mIsLooping) {
                ((VideoPagerActivity) getActivity()).mIsLooping = false;
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    public void changeBackward() {
        if (!checkForAd()) {
            if (playthrough == 0) {
                videoView.pause();
                videoView.seekTo(0);
                videoView.start();
            } else {
                playthrough--;
                changeVideo();
            }

            if (((VideoPagerActivity) getActivity()).mIsLooping) {
                ((VideoPagerActivity) getActivity()).mIsLooping = false;
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    private void assignListeners(View main) {
        mMediaController.setPrevNextListeners(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeForward();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeBackward();
                    }
                });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(getActivity(),
                        "Cannot play the video",
                        Toast.LENGTH_LONG).show();
                mMediaController.hide();

                if (playthrough < videos.size() - 1) {
                    playthrough++;
                } else {
                    playthrough--;
                }
                changeVideo();

                ((VideoPagerActivity) getActivity()).mIsLooping = false;
                getActivity().invalidateOptionsMenu();
                return true;
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                ((VideoPagerActivity) getActivity()).getSupportActionBar().setTitle(videos.get(playthrough).getName());
                mp.start();
                if (showing)
                    mMediaController.show();
                else
                    mMediaController.hide();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (((VideoPagerActivity) getActivity()).mIsLooping) {
                    videoView.pause();
                    videoView.seekTo(0);
                    videoView.start();
                } else {
                    changeForward();
                }
            }
        });

        View.OnClickListener view = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSystemUI();
            }
        };
        main.findViewById(R.id.mainListener).setOnClickListener(view);
        main.findViewById(R.id.topListener).setOnClickListener(view);
        main.findViewById(R.id.botListener).setOnClickListener(view);
        main.findViewById(R.id.rightListener).setOnClickListener(view);
        main.findViewById(R.id.leftListener).setOnClickListener(view);

        if (PrefHelper.getIfGesture(getActivity())) {
            OnSwipeTouchListener swipe = new OnSwipeTouchListener(getActivity()) {
                @Override
                public void onSwipeLeft() {
                    changeForward();
                }

                @Override
                public void noSwipe() {
                    toggleSystemUI();
                }

                @Override
                public void onSwipeRight() {
                    changeBackward();
                }

                @Override
                public void onSwipeTop() {
                    mMediaController.show();
                }

                @Override
                public void onSwipeBottom() {
                    mMediaController.show();
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return super.onTouch(v, event);
                }
            };
            main.findViewById(R.id.mainListener).setOnTouchListener(swipe);
            main.findViewById(R.id.topListener).setOnTouchListener(swipe);
            main.findViewById(R.id.botListener).setOnTouchListener(swipe);
            main.findViewById(R.id.rightListener).setOnTouchListener(swipe);
            main.findViewById(R.id.leftListener).setOnTouchListener(swipe);
        }
    }

    public void deleteFile() {
        final String path = videos.get(playthrough).file.getAbsolutePath();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete File");
        builder.setMessage("Are you sure you want to remove this file?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File file = new File(path);
                boolean deleted = file.delete();
                if(videos.size() > 1 && deleted) {
                    videos.remove(playthrough);
                    ((VideoPagerActivity) getActivity()).videoList.getAdapter().notifyDataSetChanged();
                    changeVideo(playthrough);
                } else {
                    startActivity(new Intent(getActivity(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                }
                Log.d("Meme", deleted + "");
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setMargins();
        if (showing)
            mMediaController.show();
        else
            mMediaController.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!playing && !first)
                    videoView.pause();
                else {
                    videoView.start();
                }
                videoView.seekTo(position - 200);
                first = false;
            }
        }, 300);
    }

    @Override
    public void onPause() {
        super.onPause();
        playing = videoView.isPlaying();
        position = videoView.getCurrentPosition();
        videoView.pause();
    }

    private void toggleSystemUI() {
        if (showing)
            mMediaController.hide();
        else
            mMediaController.show();
    }

    public class CustomMediaController extends MediaController {
        private CustomMediaController(Context context) {
            super(new ContextThemeWrapper(context, R.style.MusicPlayer));
        }

        @Override
        public void show() {
            super.show();
            showing = true;
            if (getActivity() != null)
                ((VideoPagerActivity) getActivity()).showSystemUI();
        }

        @Override
        public void hide() {
            super.hide();
            showing = false;
            if (getActivity() != null)
                ((VideoPagerActivity) getActivity()).hideSystemUI();
        }

        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                getActivity().finish();
                return true;
            }

            return super.dispatchKeyEvent(event);
        }
    }

    public static boolean isConnectingToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}

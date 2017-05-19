package com.amfasllc.shortplay;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import static com.amfasllc.shortplay.VideoPagerActivity.getNavHeight;
import static com.amfasllc.shortplay.VideoPagerActivity.hasNavBar;

public class AdFragment extends Fragment {

    int page = 0;
    int adCount = 0;

    boolean canNext = false;
    boolean showing = false;

    View view;
    AdView mAdView;

    ArrayList<Video> videos;

    private static final String ARG_PAGE = "ARG_PAGE";

    public AdFragment newInstance(int page, ArrayList<Video> videos, int adCount, boolean showing) {
        this.page = page;
        this.videos = videos;
        this.adCount = adCount;
        this.showing = showing;
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AdFragment fragment = this;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        AdRequest adRequest = new AdRequest.Builder().build();
        view = inflater.inflate(R.layout.video_ad_layout, container, false);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mAdView = (AdView) view.findViewById(R.id.mrec);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                canNext = true;
            }

            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                canNext = true;
            }
        });
        mAdView.loadAd(adRequest);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getActivity().getWindow().setNavigationBarColor(((VideoPagerActivity) getActivity()).getThemePrimaryColor(getActivity()));
            getActivity().getWindow().setStatusBarColor(((VideoPagerActivity) getActivity()).getThemePrimaryDarkColor(getActivity()));
        }

        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1.4f)).start();
        toolbar.invalidate();
        toolbar.setTitle("Ad");

        ImageButton prev = (ImageButton) view.findViewById(R.id.prev);
        ImageButton next = (ImageButton) view.findViewById(R.id.next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canNext) {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    VideoFragment parentFragment = new VideoFragment().newInstance(page + 1, videos, showing, adCount);
                    fragmentTransaction.replace(R.id.fragment_video, parentFragment, "video_fragment");
                    fragmentTransaction.commit();
                }
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canNext) {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    VideoFragment parentFragment = new VideoFragment().newInstance(page - 1, videos, showing, adCount);
                    fragmentTransaction.replace(R.id.fragment_video, parentFragment, "video_fragment");
                    fragmentTransaction.commit();
                }
            }
        });

        setMargins();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdView.pause();
    }

    private void setMargins() {
        int height = hasNavBar(getResources()) ? getNavHeight(getResources()) : 0;

        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setMargins(view.findViewById(R.id.bar), 0, 0, 0, height);
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setMargins(view.findViewById(R.id.next), 0, 0, height, 0);
        }
    }

    private static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

}

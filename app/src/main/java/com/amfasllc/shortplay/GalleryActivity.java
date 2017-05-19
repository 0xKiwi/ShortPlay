package com.amfasllc.shortplay;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.amfasllc.shortplay.helpers.PrefHelper;
import com.amfasllc.shortplay.helpers.StringUtil;

import org.polaric.colorful.ColorfulActivity;

public class GalleryActivity extends ColorfulActivity {

    private SwipeRefreshLayout swipeContainer;
    private GalleryAdapter pictureAdapter;
    private RecyclerView recyclerView;

    private String folder = "";
    private String sort = "";

    private Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_main);

        Bundle mainData = getIntent().getExtras();
        if (mainData == null)
            return;

        folder = mainData.getString("folder");
        sort = PrefHelper.getSortMode(this);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new AdapterASyncTask().execute();
            }
        });

        swipeContainer.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(StringUtil.getFileName(folder));

        setTitle(folder.substring(folder.lastIndexOf("/", folder.length() - 2) + 1, folder.length() - 1));

        recyclerView = (RecyclerView) findViewById(R.id.folderList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT ? 4 : 5));

        context = this;
        new AdapterASyncTask().execute();

        /*new MaterialScrollBar(this, recyclerView, true).setBarThickness(8).
                setAutoHide(true).setBarColour(accentColor()).setHandleOffColour(accentColor()).
                setHandleColour(accentColor());
                */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery_menu, menu);

        switch (PrefHelper.getSortMode(this)) {
            case GalleryAdapter.SORT_TAKEN_DATE_ASC:
                menu.findItem(R.id.sortOldFirst).setChecked(true);
                break;
            case GalleryAdapter.SORT_TAKEN_DATE_DESC:
                menu.findItem(R.id.sortNewFirst).setChecked(true);
                break;
            case GalleryAdapter.SORT_NAME_ASC:
                menu.findItem(R.id.sortAtoZ).setChecked(true);
                break;
            case GalleryAdapter.SORT_NAME_DESC:
                menu.findItem(R.id.sortZtoA).setChecked(true);
                break;
            case GalleryAdapter.RANDOM:
                menu.findItem(R.id.sortRandom).setChecked(true);
                break;
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.sortAtoZ:
                item.setChecked(!item.isChecked());
                sort = GalleryAdapter.SORT_NAME_ASC;
                PrefHelper.setSortMode(this, sort);
                new AdapterASyncTask().execute();
                return true;
            case R.id.sortZtoA:
                item.setChecked(!item.isChecked());
                sort = GalleryAdapter.SORT_NAME_DESC;
                PrefHelper.setSortMode(this, sort);
                new AdapterASyncTask().execute();
                return true;
            case R.id.sortOldFirst:
                item.setChecked(!item.isChecked());
                sort = GalleryAdapter.SORT_TAKEN_DATE_ASC;
                PrefHelper.setSortMode(this, sort);
                new AdapterASyncTask().execute();
                return true;
            case R.id.sortNewFirst:
                item.setChecked(!item.isChecked());
                sort = GalleryAdapter.SORT_TAKEN_DATE_DESC;
                PrefHelper.setSortMode(this, sort);
                new AdapterASyncTask().execute();
                return true;
            case R.id.sortRandom:
                item.setChecked(!item.isChecked());
                sort = GalleryAdapter.RANDOM;
                PrefHelper.setSortMode(this, sort);
                new AdapterASyncTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class AdapterASyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeContainer.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            pictureAdapter = new GalleryAdapter(context, folder, sort);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            swipeContainer.setRefreshing(false);
            recyclerView.setAdapter(pictureAdapter);
            recyclerView.setHasFixedSize(true);
        }
    }

}

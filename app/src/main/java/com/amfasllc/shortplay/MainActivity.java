package com.amfasllc.shortplay;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.digitus.Digitus;
import com.afollestad.digitus.DigitusCallback;
import com.afollestad.digitus.DigitusErrorType;
import com.afollestad.digitus.FingerprintDialog;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amfasllc.shortplay.helpers.PrefHelper;
import com.google.android.gms.ads.MobileAds;

import org.polaric.colorful.ColorfulActivity;

import static com.amfasllc.shortplay.helpers.Utils.getNavHeight;
import static com.amfasllc.shortplay.helpers.Utils.hasNavBar;

public class MainActivity extends ColorfulActivity implements FingerprintDialog.Callback, DigitusCallback {

    private SwipeRefreshLayout swipeContainer;

    private boolean hidden = false;
    private boolean first = false;

    private RecyclerView folderList;
    private FolderAdapter adapter;

    private MainActivity callback;

    private MenuItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);
        Digitus.init(this, getString(R.string.app_name), 69, this);

        if (!PrefHelper.getIfAdsRemoved(this)) {
            MobileAds.initialize(this, getResources().getString(R.string.googlekey));
            MobileAds.setAppVolume(0.3f);
        }

        callback = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        folderList = (RecyclerView) findViewById(R.id.folderList);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startList(true);
            }
        });
        swipeContainer.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        if (PrefHelper.getIfWholeAppSecure(this)) {
            first = true;
            securityCheck(false);
        } else {
            startList(false);
        }
    }

    private class AdapterASyncTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeContainer.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            adapter = new FolderAdapter(callback, hidden, params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            swipeContainer.setRefreshing(false);
            folderList.setLayoutManager(new LinearLayoutManager(callback));
            folderList.setAdapter(adapter);

            if (adapter.getItemCount() == 0)
                Toast.makeText(callback, "There were no videos found", Toast.LENGTH_LONG).show();
        }
    }

    private void startList(boolean refresh) {
        if (Build.VERSION.SDK_INT >= 23)
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, refresh);
        else if (isExternalStorageReadable()) {
            if(hidden)
                new AdapterASyncTask().execute(refresh);
            else
                runListMainThread();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            new AdapterASyncTask().execute(true);
            Digitus.get().handleResult(requestCode, permissions, grantResults);
        } else {
            showSnack();
        }
        // Notify Digitus of the result
    }

    private void securityCheck(final boolean isHidden) {
        final String method = PrefHelper.getSecureMethod(getApplicationContext());
        if (!method.equals(getResources().getString(R.string.fingerprint))) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title("Enter " + method)
                    .positiveText("Enter")
                    .input("Enter " + method, "",
                            false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog,
                                                    CharSequence input) {
                                }
                            })
                    .inputType(method.equals(getResources().getString(R.string.pin)) ?
                            InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (dialog.getInputEditText().getText().toString().equals
                                    (PrefHelper.getHiddenPasscode(getApplicationContext()))) {
                                if (isHidden) {
                                    hidden = !hidden;
                                    item.setChecked(hidden);
                                }
                                startList(false);
                            } else {
                                Toast.makeText(MainActivity.this, "Wrong " + method, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .cancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (first) {
                                callback.finish();
                                System.exit(0);
                            }
                        }
                    })
                    .build();
            dialog.show();
        } else {
            FingerprintDialog.show(callback, getString(R.string.app_name), 69, true);
        }
    }

    private void runListMainThread(){
        adapter = new FolderAdapter(callback, false, false);
        folderList.setLayoutManager(new LinearLayoutManager(callback));
        folderList.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Digitus.deinit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hidden)
            if (folderList != null)
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        startList(false);
                    }
                } else {
                    startList(false);
                }
    }

    private void showMessageOKCancel(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 435);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showSnack();
                    }
                })
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            first = false;
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_hidden) {
            first = false;
            swipeContainer.setRefreshing(false);
            if (PrefHelper.getIfSecure(this) && !item.isChecked()) {
                if (PrefHelper.getIfHiddenSecure(this)) {
                    this.item = item;
                    securityCheck(true);
                }
            } else {
                hidden = !hidden;
                item.setChecked(hidden);
                startList(false);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDigitusReady(Digitus digitus) {

    }

    @Override
    public void onDigitusListening(boolean newFingerprint) {

    }

    @Override
    public void onDigitusAuthenticated(Digitus digitus) {
        hidden = !hidden;
        if (item != null)
            item.setChecked(hidden);

        if (!first) {
            new AdapterASyncTask().execute(false);
        }
    }

    @Override
    public void onDigitusError(Digitus digitus, DigitusErrorType type, Exception e) {

    }

    @Override
    public void onFingerprintDialogAuthenticated() {
        hidden = !hidden;
        if (item != null)
            item.setChecked(hidden);

        if (!first) {
            new AdapterASyncTask().execute(false);
        }
    }

    @Override
    public void onFingerprintDialogVerifyPassword(FingerprintDialog dialog, String password) {
        if (password.equals(PrefHelper.getHiddenPasscode(this)))
            dialog.notifyPasswordValidation(true);
        else
            dialog.notifyPasswordValidation(false);
    }

    @Override
    public void onFingerprintDialogStageUpdated(FingerprintDialog dialog, FingerprintDialog.Stage stage) {}

    @Override
    public void onFingerprintDialogCancelled() {
        if (first) {
            this.finish();
            System.exit(0);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        /*if (resultCode != RESULT_OK)
            return;
        if (Build.VERSION.SDK_INT >= 19 && requestCode == 42) {
            Uri treeUri = resultData.getData();
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PrefHelper.setSdCardPath(this, treeUri.toString());
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }*/
    }


    private void showSnack() {
        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main),
                "Storage permission is required to use ShortPlay",
                Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction("Enable", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startList(false);
            }
        });
        mySnackbar.show();
    }

    private void checkPermission(final String string, boolean refresh) {
        if (checkSelfPermission(string) != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(string)) {
                showMessageOKCancel("You need to allow access to External Storage");
                return;
            }
            requestPermissions(new String[]{string, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    435);
        } else {
            new AdapterASyncTask().execute(refresh);
        }
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

}

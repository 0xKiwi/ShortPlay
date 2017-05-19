package com.amfasllc.shortplay;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amfasllc.shortplay.helpers.StorageProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private TinyDB tinydb;

    private boolean hidden;

    private ArrayList<String> set;

    private Context mContext;

    private ArrayList<Folder> mFoldersList = new ArrayList<>();
    private ArrayList<File> mPath = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView folderTitle;
        public View v;
        public CheckBox checkBox;

        public ViewHolder(View v) {
            super(v);
            setIsRecyclable(false);

            this.v = v;
            folderTitle = (TextView) v.findViewById(R.id.folderTitle);
            checkBox = (CheckBox) v.findViewById(R.id.star);
        }
    }

    public FolderAdapter(Context context, boolean hidden, boolean refresh) {
        this.hidden = hidden;
        mContext = context;

        tinydb = new TinyDB(context);

        set = tinydb.getListString("faves");
        mPath = StorageProvider.getStorageRoots(mContext);
        scan(refresh);
    }

    private void scan(boolean refresh) {
        mFoldersList = StorageProvider.getAlbums(mContext, false);
        if (hidden) {
            ArrayList<String> saved = tinydb.getListString("saved");
            //Toast.makeText(mContext, saved.get(0), Toast.LENGTH_SHORT).show();
            ArrayList<Folder> folders;
            if (saved.isEmpty() || refresh) {
                Log.d("MEME", saved.get(1));
                saved = new ArrayList<>();
                folders = StorageProvider.getAlbums(mContext, true);
                for (Folder item : folders)
                    saved.add(item.getPath());

                tinydb.putListString("saved", saved);
                mFoldersList.addAll(folders);
            } else {
                for (String path : saved)
                    mFoldersList.add(new Folder(path, new File(path).getName(), true));
            }
        }

        Collections.sort(mFoldersList, new Comparator<Folder>() {
            @Override
            public int compare(Folder s1, Folder s2) {
                return s1.getName().compareTo(s2.getName());
            }
        });

        if (set.size() > 0)
            for (int r = 0; r < set.size(); r++)
                for (int i = 0; i < mFoldersList.size(); i++) {
                    Folder folder = mFoldersList.get(i);
                    if (folder.getPath().equals(set.get(r))) {
                        mFoldersList.remove(folder);
                        mFoldersList.add(r, folder);
                    }
                }


        for (int i = 0; i < mFoldersList.size() - 1; i++)
            if (mFoldersList.get(i).equals(mFoldersList.get(i + 1))) {
                mFoldersList.remove(i + 1);
                i--;
            }
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        final Folder folder = mFoldersList.get(position);

        viewHolder.folderTitle.setText(folder.getName());

        viewHolder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity = new Intent(mContext, GalleryActivity.class);
                activity.putExtra("folder", folder.getPath());
                mContext.startActivity(activity);
            }
        });

        /*
        viewHolder.v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (folder.getPath().contains(mPath.get(0).getAbsolutePath()))
                    if (folder.isHidden())
                        createAndShowAlertDialog("Would you like to unhide this folder?", "(This will make the folder show in other apps as well)", position);
                    else
                        createAndShowAlertDialog("Would you like to hide this folder?", "(This will make the folder not show in other apps as well)", position);
                else
                    Toast.makeText(mContext, "Sorry! Hiding external SD card folders is not supported yet!", Toast.LENGTH_SHORT).show();
                return true;
            }
        });*/

        viewHolder.checkBox.setChecked(set.contains(folder.getPath()));
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    set.add(folder.getPath());
                } else {
                    set.remove(folder.getPath());
                }
                tinydb.putListString("faves", set);

            }
        });
    }

    @Override
    public int getItemCount() {
        return mFoldersList.size();
    }

    private Folder getItem(int position) {
        return mFoldersList.get(position);
    }

    private void createAndShowAlertDialog(String title, String text, final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String path = getItem(pos).getPath();
                File nomedia = new File(path + "/.nomedia");
                hidden = mFoldersList.get(pos).isHidden();
                if (hidden) {
                    //Log.d("MEME",Uri.parse(PrefHelper.getSdCardPath(mContext)).getPath());
                    new DeleteNomediaaSync().execute(path);
                    DocumentFile.fromFile(nomedia).delete();
                } else {
                    Log.d("imagepath", path);
                    DocumentFile.fromFile(new File(path + "/")).createFile("none", ".nomedia");
                    new DeleteNomediaaSync().execute(path);
                }
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
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.main_folder_layout, viewGroup, false));
    }

    class DeleteNomediaaSync extends AsyncTask<String, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(String... params) {
            final File file = new File(params[0], ".nomedia");
            if (hidden) {
                // Set up the projection (we only need the ID)
                String[] projection = {MediaStore.Files.FileColumns._ID};

// Match on the file path
                String selection = MediaStore.Files.FileColumns.DATA + " = ?";
                String[] selectionArgs = new String[]{file.getAbsolutePath()};

                // Query for the ID of the media matching the file path
                Uri queryUri = MediaStore.Files.getContentUri("external");
                ContentResolver contentResolver = mContext.getContentResolver();
                Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
                if (c.moveToFirst()) {
                    // We found the ID. Deleting the item via the content provider will also remove the file
                    long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                    Uri deleteUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                    contentResolver.delete(deleteUri, null, null);
                    mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                } else {
                    // File not found in media store DB
                }
                c.close();
            } else {
                //Log.d("MEME", nomedia.getAbsolutePath());
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                values.put(MediaStore.Files.FileColumns.MEDIA_TYPE, 0);
                mContext.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                scanIntent.setData(contentUri);
            }

            ArrayList<String> paths = new ArrayList<>();
            paths.add("file://" + params[0] + File.separator + ".nomedia");
            paths.add("file://" + new File(params[0]).getParent());

            File[] files = new File(params[0]).listFiles();
            for (File temp : files)
                paths.add("file://" + temp.getAbsolutePath());

            MediaScannerConnection.scanFile(mContext, paths.toArray(new String[paths.size()]), null, new MediaScannerConnection.OnScanCompletedListener()

            {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    System.out.println("SCAN COMPLETED: " + path);
                }
            });
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(mContext, "Unhiding folder...",
                    "Please wait...", true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

}


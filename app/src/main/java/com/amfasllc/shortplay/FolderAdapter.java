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
    private ArrayList<String> hiddenList;

    private Context mContext;

    private ArrayList<Folder> mFoldersList = new ArrayList<>();
    private ArrayList<File> mPath = new ArrayList<>();
    ArrayList<String> saved;

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
        hiddenList = tinydb.getListString("hiddenList");
        saved = tinydb.getListString("saved");
        mPath = StorageProvider.getStorageRoots(mContext);

        scan(refresh);
    }

    private void scan(boolean refresh) {
        mFoldersList = StorageProvider.getAlbums(mContext, false);
        if (hidden) {
            //Toast.makeText(mContext, saved.get(0), Toast.LENGTH_SHORT).show();
            ArrayList<Folder> folders;
            if (saved.isEmpty() || refresh) {
                //Log.d("MEME", saved.get(0));
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
        } else {
            if (hiddenList != null)
                for (int i = 0; i < hiddenList.size(); i++) {
                    File file = new File(hiddenList.get(i));
                    Folder folder = new Folder(file.getAbsolutePath(), file.getName(), false);
                    if (mFoldersList.contains(folder)) {
                        mFoldersList.remove(folder);
                    }
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
        final File folderFile = new File(folder.getPath());

        viewHolder.folderTitle.setText(folder.getName());

        viewHolder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (folderFile.exists()) {
                    Intent activity = new Intent(mContext, GalleryActivity.class);
                    activity.putExtra("folder", folder.getPath());
                    mContext.startActivity(activity);
                } else {
                    hiddenList.remove(folder.getPath());
                    tinydb.putListString("hiddenList", hiddenList);
                    saved.remove(folder.getPath());
                    tinydb.putListString("saved", saved);
                }
            }
        });

        viewHolder.v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (folder.getPath().contains(mPath.get(0).getAbsolutePath())) {
                    if (folderFile.exists()) {
                        if (folder.isHidden() || hiddenList.contains(folder.getPath()))
                            createAndShowAlertDialog("Would you like to unhide this folder?", "(This will make the folder show in other apps as well)", position);
                        else
                            createAndShowAlertDialog("Would you like to hide this folder?", "(This will make the folder not show in other apps as well)", position);
                    }
                } else {
                        Toast.makeText(mContext, "Sorry! Hiding external SD card folders is not supported yet!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

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
                File parent = new File(path);
                hidden = mFoldersList.get(pos).isHidden();
                if (hidden) {
                    boolean deleted = nomedia.delete();
                    Log.d("meme", deleted + "");
                    if (parent.isHidden()) {
                        Log.e("rename", "" + parent.renameTo(new File(parent.getParentFile(), parent.getName().replaceFirst(".", ""))));
                        File newParent = new File(nomedia.getParent());
                        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newParent)));
                    } else if (deleted) {
                        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(nomedia)));
                        hiddenList.remove(parent.getPath());
                        tinydb.putListString("hiddenList", hiddenList);
                        saved.remove(parent.getPath());
                        tinydb.putListString("saved", saved);
                    }
                } else {
                    boolean created = false;
                    try {
                        created = nomedia.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("created", "" + created);

                    if (created) {
                        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(nomedia);
                        scanIntent.setData(contentUri);
                        mContext.sendBroadcast(scanIntent);

                        hiddenList.add(parent.getPath());
                        tinydb.putListString("hiddenList", hiddenList);
                        saved.add(parent.getPath());
                        tinydb.putListString("saved", saved);
                    }
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
}


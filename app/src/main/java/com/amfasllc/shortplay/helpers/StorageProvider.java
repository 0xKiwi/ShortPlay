package com.amfasllc.shortplay.helpers;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.amfasllc.shortplay.Folder;
import com.amfasllc.shortplay.GalleryAdapter;
import com.amfasllc.shortplay.Video;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class StorageProvider {

    public static ArrayList<Video> getVideosOfHiddenFolders(String path, String sort, long time) {
        ArrayList<Video> list = new ArrayList<>();

        path += "/";

        File folder = new File(path);
        File[] files = folder.listFiles(new VideoFilter());
        if (sort.contains(MediaStore.Video.Media.DISPLAY_NAME)) {
            Arrays.sort(files);
            if (sort.contains("DESC"))
                Collections.reverse(Arrays.asList(files));
        } else if (sort.contains(MediaStore.Video.Media.DATE_MODIFIED)) {
            if (sort.contains("ASC"))
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });
            else
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                    }
                });
        }

        if (files != null && files.length > 0) {
            for (File file : files) {
                list.add(new Video(file.getName(), path, "", Uri.fromFile(file), file));
                if (sort.equals(GalleryAdapter.RANDOM)) {
                    Collections.shuffle(list, new Random(time));
                }
            }
        }

        return list;
    }

    public static ArrayList<Video> getVideosOfFolder(String path, Context mContext, String sort) {
        ArrayList<Video> pictures = new ArrayList<>();
        if (mContext == null)
            return pictures;

        Uri uri = MediaStore.Video.Media.getContentUri("external");
        String[] projection = {MediaStore.Video.Media._ID,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.Media.DATA};

        path = path.substring(0, path.lastIndexOf("/") + 1);
        String folder = "%" + path + "%";
        String where = MediaStore.Audio.Media.DATA + " like ? ";
        String[] whereArgs = new String[]{folder};

        Cursor cursor = mContext.getContentResolver().query(uri, projection,
                where,
                whereArgs,
                sort);

        ArrayList<String> ids = new ArrayList<>();
        pictures.clear();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID));
                if (!ids.contains(id)) {
                    final Video curVideo = new Video();

                    ids.add(id);
                    curVideo.setId(id);

                    int columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns
                            .DISPLAY_NAME);
                    curVideo.setName(cursor.getString(columnIndex));

                    Integer _id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    Uri vidUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Integer.toString(_id));
                    curVideo.setUri(vidUri);

                    curVideo.setCoverID(_id);

                    curVideo.setFolder(uri.toString());

                    pictures.add(curVideo);
                }
            }
            cursor.close();
        }

        return pictures;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Video.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    private static String getRealPathFromHiddenURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Files.FileColumns.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static ArrayList<File> getStorageRoots(Context context) {
        ArrayList<File> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0)
                    Log.w("asd", "Unexpected external file dir: " + file.getAbsolutePath());
                else
                    paths.add(new File(file.getAbsolutePath().substring(0, index)));
            }
        }
        return paths;
    }

    public static ArrayList<Folder> getAlbums(Context context, boolean hidden) {
        ArrayList<Folder> list = new ArrayList<>();
        ArrayList<File> roots = getStorageRoots(context);

        if (hidden) {
            //list.addAll(hiddenFolders(context));
            for (File storage : roots)
                fetchRecursivelyHiddenFolder(storage, list);
        } else
            list = getVideoFolders(context, GalleryAdapter.SORT_NAME_ASC);
        //for (File storage : roots)
        //    fetchRecursivelyFolder(storage, list);
        return list;
    }

    private static ArrayList<Folder> getVideoFolders(Context mContext, String sort) {
        ArrayList<Folder> pictures = new ArrayList<>();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.Media._ID,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATA};

        Cursor cursor = mContext.getContentResolver().query(uri, projection,
                null,
                null,
                sort);

        ArrayList<String> ids = new ArrayList<>();
        pictures.clear();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID));
                if (!ids.contains(id)) {
                    final Folder curfolder = new Folder();

                    ids.add(id);

                    int columnIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME);
                    curfolder.setName(cursor.getString(columnIndex));

                    Integer _id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                    Uri vidUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Integer.toString(_id));
                    curfolder.setPath(new File(getRealPathFromURI(mContext, vidUri)).getParentFile().getAbsolutePath());
                    curfolder.setHidden(false);
                    pictures.add(curfolder);
                }
            }
            cursor.close();
        }

        return pictures;
    }

    private static ArrayList<Folder> newFoldersSearch(Context context, String sort) {
        final String FILE_TYPE_NO_MEDIA = ".mp4";

        ArrayList<Folder> listOfHiddenFiles = new ArrayList<>();

        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        String where = nonMediaCondition;
                //+ " AND "
                //+ MediaStore.Files.FileColumns.MEDIA_TYPE + " LIKE ?";

        //String[] params = new String[]{"%" + FILE_TYPE_NO_MEDIA + "%"};

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.DISPLAY_NAME},
                where,
                null, sort);

        // No Hidden file found
        if (cursor.getCount() == 0) {
            // showAll Nothing Found
            return listOfHiddenFiles;
        }

        ArrayList<File> ids = new ArrayList<>();

        // Add Hidden file name, path and directory in file object
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));

            Uri vidUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id);
            File file = new File(getRealPathFromHiddenURI(context, vidUri)).getParentFile();
            //Log.d("MEME", file.getAbsolutePath());

            if (!ids.contains(file)) {
                ids.add(file);

                checkAndAddFolder(file, listOfHiddenFiles, true);
            }
        }
        cursor.close();

        return listOfHiddenFiles;
    }

    /**
     * This function return list of hidden media files
     *
     * @param context
     * @return list of hidden media files
     */
    private static ArrayList<Folder> hiddenFolders(Context context) {
        final String FILE_TYPE_NO_MEDIA = ".nomedia";

        ArrayList<Folder> listOfHiddenFiles = new ArrayList<>();

        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        String where = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[]{"%" + FILE_TYPE_NO_MEDIA + "%"};

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                new String[]{MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.DISPLAY_NAME},
                where,
                params, null);

        // No Hidden file found
        if (cursor.getCount() == 0) {
            // showAll Nothing Found
            return listOfHiddenFiles;
        }

        ArrayList<File> ids = new ArrayList<>();

        // Add Hidden file name, path and directory in file object
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));

            Uri vidUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id);
            File file = new File(getRealPathFromHiddenURI(context, vidUri)).getParentFile();
            //Log.d("MEME", file.getAbsolutePath());

            if (!ids.contains(file)) {
                ids.add(file);

                checkAndAddFolder(file, listOfHiddenFiles, true);
            }
        }
        cursor.close();

        return listOfHiddenFiles;
    }

    private static void fetchRecursivelyHiddenFolder(File dir, ArrayList<Folder> albumArrayList) {
        File[] folders = dir.listFiles(new FolderFilter());
        if (folders != null) {
            for (File temp : folders) {
                if (temp.isDirectory() && temp.isHidden())
                    checkAndAddFolder(temp, albumArrayList, true);
                else if (temp.isDirectory())
                    fetchRecursivelyHiddenFolder(temp, albumArrayList);
                else
                    checkAndAddFolder(temp.getParentFile(), albumArrayList, true);
            }
        }
    }

    private static void fetchRecursivelyFolder(File dir, ArrayList<Folder> albumArrayList) {
        File[] folders = dir.listFiles(new FolderFilter());
        if (folders != null) {
            for (File temp : folders) {
                File nomedia = new File(temp, ".nomedia");
                if (!nomedia.exists() || !temp.isHidden())
                    checkAndAddFolder(temp, albumArrayList, false);

                fetchRecursivelyFolder(temp, albumArrayList);
            }
        }
    }

    private static void checkAndAddFolder(File dir, ArrayList<Folder> albumArrayList, boolean hidden) {
        if (dir.list(new VideoFilter()).length > 0) {
            albumArrayList.add(new Folder(dir.getAbsolutePath(), dir.getName(), dir, hidden));
        }
    }
}
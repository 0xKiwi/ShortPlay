package com.amfasllc.shortplay.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.File;

public class FileHelper {

    public static void deleteFile(Context context, String path) {
        createAndShowAlertDialog(context, "Delete File", "Are you sure you want to remove this file?", path);
    }

    private static void createAndShowAlertDialog(Context context, String title, String text, final String path) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File file = new File(path);
                boolean deleted = file.delete();
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
}

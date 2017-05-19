package com.amfasllc.shortplay;

import android.net.Uri;

import java.io.File;

/**
 * Created by Ivan Martinez on 9/25/2016.
 */

public class Video {
    private String name;
    private String folder;
    private String id;
    private Uri uri;
    File file;
    private long coverID;
    private long lastModified;
    private long duration;

    public Video() {

    }

    public Video(String name, String folder, String id, Uri uri, File file) {
        this.name = name;
        this.folder = folder;
        this.id = id;
        this.uri = uri;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public long getCoverID() {
        return coverID;
    }

    public void setCoverID(long coverID) {
        this.coverID = coverID;
    }
}

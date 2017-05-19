package com.amfasllc.shortplay;

import java.io.File;
import java.io.Serializable;

public class Folder implements Serializable {
    private boolean hidden;
    private String name = null;
    private String path = null;

    private File file;

    public Folder() {
    }

    public Folder(String path, String name, File file, boolean hidden) {
        this.path = path;
        this.name = name;
        this.file = file;
        this.hidden = hidden;
    }

    public Folder(String path, String name, boolean hidden) {
        this.path = path;
        this.name = name;
        this.hidden = hidden;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Folder) {
            return getPath().equals(((Folder) obj).getPath());
        }
        return super.equals(obj);
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
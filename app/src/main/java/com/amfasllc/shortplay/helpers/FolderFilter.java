package com.amfasllc.shortplay.helpers;

import java.io.File;
import java.io.FileFilter;

class FolderFilter implements FileFilter {

    public FolderFilter() {
    }

    @Override
    public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().toLowerCase().equals(".nomedia") || pathname.isHidden();
    }
}
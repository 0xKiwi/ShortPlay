package com.amfasllc.shortplay.helpers;

import java.io.File;
import java.io.FilenameFilter;

class NoMediaFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String filename) {
        return filename.toLowerCase().equals(".nomedia");
    }
}
package com.amfasllc.shortplay.helpers;

import java.io.File;
import java.io.FilenameFilter;

class VideoFilter implements FilenameFilter {

    private static final String[] extensions = new String[] { "mp4", "webm", "avi", ".3gp" };

    @Override
    public boolean accept(File dir, String filename) {
        if (new File(dir, filename).isFile()) {
            for (String extension : extensions)
                if (filename.toLowerCase().endsWith(extension))
                    return true;
        }

        return false;
    }
}
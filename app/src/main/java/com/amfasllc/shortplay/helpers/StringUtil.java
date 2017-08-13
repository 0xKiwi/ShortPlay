package com.amfasllc.shortplay.helpers;

import java.io.File;

public class StringUtil {

    public static String getFileName(String fullPath) {
        return fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getFileExtension(String file) {
        try {
            return file.substring(file.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return file;
        }
    }
}
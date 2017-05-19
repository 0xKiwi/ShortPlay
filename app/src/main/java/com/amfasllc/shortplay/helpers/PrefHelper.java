package com.amfasllc.shortplay.helpers;

import android.content.Context;
import android.preference.PreferenceManager;

import com.amfasllc.shortplay.GalleryAdapter;

public abstract class PrefHelper {
    private static final String SD_CARD_PATH = "sd_card_path";
    private static final String REMOVE_ADS = "removeads";
    private static final String SWIPE_GESTURE = "swipeGesture";

    private static final String SECURE_FUNCTIONS = "secure";
    public static final String SECURE_APPACCESS = "appaccess";
    public static final String SECURE_HIDDEN = "hidden";

    private static final String SECURE_METHOD = "secure_method";
    private static final String HIDDEN_PASSCODE = "hidden_passcode";

    private static final String SORT_MODE = "sort_mode";

    public static boolean getIfSecure(Context context) {
        return context != null && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SECURE_FUNCTIONS, false);
    }

    public static boolean getIfGesture(Context context) {
        return context != null && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SWIPE_GESTURE, true);
    }

    public static boolean getIfHiddenSecure(Context context) {
        return context != null && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SECURE_HIDDEN, false);
    }

    public static boolean getIfAdsRemoved(Context context) {
        return context != null && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(REMOVE_ADS, false);
    }

    public static void setIfAdsRemoved(Context context, boolean removed) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(REMOVE_ADS, removed).apply();
    }

    public static void setSecureMethod(Context context, String method) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString
                (SECURE_METHOD, method).apply();
    }

    public static String getSecureMethod(Context context) {
        if (context == null) {
            return "none";
        }

        return PreferenceManager.getDefaultSharedPreferences(context).getString
                (SECURE_METHOD, "none");
    }

    public static void setSdCardPath(Context context, String path) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString
                (SD_CARD_PATH, path).apply();
    }

    public static String getSdCardPath(Context context) {
        if (context == null) {
            return "none";
        }

        return PreferenceManager.getDefaultSharedPreferences(context).getString
                (SD_CARD_PATH, "none");
    }

    public static boolean getIfWholeAppSecure(Context context) {
        return context != null && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SECURE_APPACCESS, false);
    }

    public static void setIfWholeAppSecure(Context context, boolean secure) {
        //noinspection ResourceType
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SECURE_APPACCESS,
                secure).apply();
    }

    public static void setHiddenPasscode(Context context, String passcode) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString
                (HIDDEN_PASSCODE, passcode).apply();
    }

    public static String getHiddenPasscode(Context context) {
        if (context == null) {
            return "";
        }

        return PreferenceManager.getDefaultSharedPreferences(context).getString
                (HIDDEN_PASSCODE, "");
    }

    @GalleryAdapter.SortMode
    public static String getSortMode(Context context) {
        //noinspection ResourceType
        return PreferenceManager.getDefaultSharedPreferences(context).getString(SORT_MODE,
                GalleryAdapter.SORT_TAKEN_DATE_DESC);
    }

    @GalleryAdapter.SortMode
    public static void setSortMode(Context context, String sortMode) {
        //noinspection ResourceType
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(SORT_MODE,
                sortMode).apply();
    }
}

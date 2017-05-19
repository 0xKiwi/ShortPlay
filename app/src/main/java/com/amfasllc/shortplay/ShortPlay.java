package com.amfasllc.shortplay;

import android.app.Application;

import org.polaric.colorful.Colorful;

public class ShortPlay extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Colorful.defaults()
                .primaryColor(Colorful.ThemeColor.INDIGO)
                .accentColor(Colorful.ThemeColor.RED)
                .translucent(false)
                .dark(false);
        Colorful.init(this);
    }
}

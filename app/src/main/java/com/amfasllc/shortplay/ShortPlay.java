package com.amfasllc.shortplay;

import android.app.Application;

import org.polaric.colorful.Colorful;

public class ShortPlay extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Colorful.defaults()
                .primaryColor(Colorful.ThemeColor.DEEP_BLUE)
                .accentColor(Colorful.ThemeColor.PINK)
                .translucent(false)
                .dark(true);
        Colorful.init(this);
    }
}

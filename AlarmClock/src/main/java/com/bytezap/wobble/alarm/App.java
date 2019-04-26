/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.graphics.drawable.Drawable;

public class App {
    private Drawable appIcon;
    private String appLabel;
    private String appPackageName;

    public App(Drawable appIcon, String appLabel, String appPackageName) {
        this.appIcon = appIcon;
        this.appLabel = appLabel;
        this.appPackageName = appPackageName;
    }

    public Drawable getAppIcon()
    {
        return appIcon;
    }

    public String getAppLabel()
    {
        return appLabel;
    }

    public String getAppPackageName(){
        return appPackageName;
    }
}
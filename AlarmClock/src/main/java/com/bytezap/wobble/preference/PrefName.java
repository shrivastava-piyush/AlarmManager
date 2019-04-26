/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.graphics.drawable.Drawable;

class PrefName {
        private String name;
        private Drawable icon;

        PrefName(String name, Drawable icon) {
            this.name = name;
            this.icon = icon;
        }

    public Drawable getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }
}
/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm.camera;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.CommonUtils;

public class CameraActivity extends Activity {

    public static final String IMAGE_PATH = "SELECTED_IMAGE_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_picture);
        if (CommonUtils.isLOrLater()) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.cam_container, CommonUtils.isLOrLater() ? Camera2Fragment.newInstance() : CameraFragment.newInstance())
                    .commit();
        }
    }

}
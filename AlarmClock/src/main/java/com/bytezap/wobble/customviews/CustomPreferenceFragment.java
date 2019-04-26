/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytezap.wobble.R;

public class CustomPreferenceFragment extends PreferenceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try{
            return inflater.inflate(R.layout.custom_preference_fragment, container, false);
        } catch (Exception e){
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }
}

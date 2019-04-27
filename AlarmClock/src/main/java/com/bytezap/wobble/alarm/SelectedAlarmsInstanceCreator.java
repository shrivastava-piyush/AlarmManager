package com.bytezap.wobble.alarm;


import android.util.SparseBooleanArray;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class SelectedAlarmsInstanceCreator implements InstanceCreator<SparseBooleanArray> {

    public SelectedAlarmsInstanceCreator() {
    }

    @Override
    public SparseBooleanArray createInstance(Type type) {
        return new SparseBooleanArray();

    }
}

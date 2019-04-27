package com.bytezap.wobble.stopwatch;


import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

class LapsInstanceCreator implements InstanceCreator<Lap> {

    public LapsInstanceCreator() {}

    @Override
    public Lap createInstance(Type type) {
        return new Lap(1, "00:00:000");

    }
}

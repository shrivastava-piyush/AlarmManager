/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.timer;

interface PresetInterface {

    void startPreset(int position);

    void editPreset(int position);

    void deletePreset(int position);

    void setPresetTime(int position);
}

/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

public class BackupEnvoy extends BackupAgent{
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {

    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) {

    }

    @Override
    public void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime) throws IOException {

        if (destination.getName().endsWith("_preferences.xml")) {
            final String prefFileName = getPackageName() + "_preferences.xml";
            destination = new File(destination.getParentFile(), prefFileName);
        }
        super.onRestoreFile(data, size, destination, type, mode, mtime);
    }

}

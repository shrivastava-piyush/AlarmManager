/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MediaProvider {

    public static final Uri mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri internalUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

    /**
     * @param context
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return Cursor
     */
    public static Cursor queryMedia(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * @param context
     * @param projection
     * @param constraint
     * @param sortOrder
     * @return
     */
    public static Cursor queryMediaByTitle(Context context, String[] projection, String constraint, String sortOrder) {
        String selection = MediaStore.Audio.Media.TITLE  + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + constraint + "%" };
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(mediaUri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * @param context
     * @param projection
     * @param constraint
     * @param sortOrder
     * @return
     */
    public static Cursor queryRingtoneByTitle(Context context, String[] projection, String constraint, String sortOrder) {
        String selection = MediaStore.Audio.Media.TITLE  + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + constraint + "%" };
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(internalUri, projection, selection, selectionArgs, sortOrder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * @param context
     * @return
     */
    public static long [] getAllMediaId(Context context) {
        Cursor c = queryMedia(context, mediaUri,
                new String[]{BaseColumns._ID}, MediaStore.Audio.Media.IS_MUSIC + "=1",
                null, null);
        try {
            if (c == null || c.getCount() == 0) {
                return null;
            }
            int len = c.getCount();
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            return list;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static Uri  getRandomUri(Context context){
        Cursor c = null;
        try{
            c = context.getContentResolver().query(mediaUri, null, null, null, "RANDOM() LIMIT 1");
            if (c != null) {
                if (c.moveToFirst()) {
                    Uri withAppendedId = ContentUris.withAppendedId(mediaUri, c.getLong(c.getColumnIndex(BaseColumns._ID)));
                    c.close();
                    return withAppendedId;
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception ignored){}
        finally {
            if (c!=null) {
                c.close();
            }
        }
        return null;
    }

    /**
     * @param ids
     * @return
     */
    public static List<Uri> getAllMediaUri(long[] ids) {

        if (ids == null) {
            return null;
        }

        List<Uri> list = new ArrayList<>();
        for (long mediaId : ids) {
            if (mediaId > 0) {
                list.add(ContentUris.withAppendedId(mediaUri, mediaId));
            }
        }
        return list;
    }

}

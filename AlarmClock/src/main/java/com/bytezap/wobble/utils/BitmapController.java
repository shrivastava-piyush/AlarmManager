package com.bytezap.wobble.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.AlarmSettingsActivity;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;

public class BitmapController {

    private static BitmapDrawable portraitBg;
    private static BitmapDrawable landBg;
    private static BitmapDrawable detailsPortBitmap;
    private static BitmapDrawable detailsLandBitmap;
    private static Bitmap timerBitmap;
    private static Bitmap stopwatchBigBitmap;
    private static Bitmap timerBigBitmap;
    private static Bitmap stopwatchBitmap;
    private static List<Bitmap> mThemeList;
    private static boolean isAnimation;
    private static int themeNumber;

    static {
        portraitBg = null;
        landBg = null;
        timerBitmap = null;
        stopwatchBitmap = null;
        timerBigBitmap = null;
        stopwatchBigBitmap = null;
        mThemeList = null;
        isAnimation = true;
        themeNumber = 0;
    }

    public static boolean isThemeListEmpty() {
        return mThemeList == null;
    }

    public static void initList(int length) {
        mThemeList = new ArrayList<>(length);
    }

    // Drawable list for themes
    public static List<Bitmap> getThemeList() {
        return mThemeList;
    }

    public static void addObjectToList(Bitmap bitmap) {
        try {
            mThemeList.add(bitmap);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public static void setBackground(BitmapDrawable bitmap, Resources res, boolean isThemeActivity) {
        if (isThemeActivity) {
            portraitBg = null;
            landBg = null;

            if (detailsLandBitmap != null) {
                if (detailsLandBitmap.getBitmap() != null && !detailsLandBitmap.getBitmap().isRecycled()) {
                    detailsLandBitmap.getBitmap().recycle();
                }
                detailsLandBitmap = null;
            }
            if (detailsPortBitmap != null) {
                if (detailsPortBitmap.getBitmap() != null && !detailsPortBitmap.getBitmap().isRecycled()) {
                    detailsPortBitmap.getBitmap().recycle();
                }
                detailsPortBitmap = null;
            }
        }
        if (CommonUtils.isPortrait(res)) {
            if (portraitBg != null) {
                if (portraitBg.getBitmap() != null && !portraitBg.getBitmap().isRecycled()) {
                    portraitBg.getBitmap().recycle();
                }
            }
            portraitBg = bitmap;
        } else {
            if (landBg != null) {
                if (landBg.getBitmap() != null && !landBg.getBitmap().isRecycled()) {
                    landBg.getBitmap().recycle();
                }
            }
            landBg = bitmap;
        }
    }

    // Bitmap for current theme
    @Nullable
    public static BitmapDrawable getCurrentBackground(Resources res) {
        if (CommonUtils.isPortrait(res)) {
            if (portraitBg != null && !portraitBg.getBitmap().isRecycled()) {
                return portraitBg;
            }
        } else {
            if (landBg != null && !landBg.getBitmap().isRecycled()) {
                return landBg;
            }
        }
        return null;
    }

    // Let go of the static references
    public static void recycleBitmap() {
        if (portraitBg != null && !portraitBg.getBitmap().isRecycled()) {
            portraitBg.getBitmap().recycle();
        }
        portraitBg = null;

        if (landBg != null && !landBg.getBitmap().isRecycled()) {
            landBg.getBitmap().recycle();
        }
        landBg = null;

        if (timerBitmap != null && !timerBitmap.isRecycled()) {
            timerBitmap.recycle();
        }
        timerBitmap = null;

        if (stopwatchBitmap != null && !stopwatchBitmap.isRecycled()) {
            stopwatchBitmap.recycle();
        }
        stopwatchBitmap = null;

        if (detailsPortBitmap != null) {
            if (detailsPortBitmap.getBitmap() != null && !detailsPortBitmap.getBitmap().isRecycled()) {
                detailsPortBitmap.getBitmap().recycle();
            }
            detailsPortBitmap = null;
        }

        if (detailsLandBitmap != null) {
            if (detailsLandBitmap.getBitmap() != null && !detailsLandBitmap.getBitmap().isRecycled()) {
                detailsLandBitmap.getBitmap().recycle();
            }
            detailsLandBitmap = null;
        }

        if (mThemeList != null) {
            for (Bitmap themeImage :
                    mThemeList) {
                themeImage.recycle();
            }
            mThemeList.clear();
            mThemeList = null;
        }

        if (timerBigBitmap != null  && !timerBigBitmap.isRecycled()) {
            timerBigBitmap.recycle();
        }
        timerBigBitmap = null;

        if (stopwatchBigBitmap != null && !stopwatchBigBitmap.isRecycled()) {
            stopwatchBigBitmap.recycle();
        }
        stopwatchBigBitmap = null;
    }

    public static void nullifyBackground(){
        portraitBg = null;
        landBg = null;
    }

    public static BitmapDrawable setNewBackground(Context context, Resources res) {
        BitmapDrawable background = null;
        try {
            if (themeNumber < ThemeDetails.TOTAL_THEMES) {
                background = getAssetDrawable(res, themeNumber);
                if (background == null) {
                    background = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.bg0));
                    Log.e("setNewBackground", "Bitmap could not be obtained");
                }

            } else if (themeNumber == ThemeDetails.THEME_CUSTOM) {
                SharedPreferences themePrefs = context.getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE);
                String path = themePrefs.getString(Clock.THEME_BG_PATH, "Null");
                // The user might have deleted the file
                if (new File(path).exists()) {
                    try {
                        background = getCustomDrawable(path, res);
                    } catch (Exception ex) {
                        //set the default random bg if this fails
                        background = getAssetDrawable(res, themeNumber);
                    }
                } else {
                    background = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.bg0));
                }
            } /*else if (themeNumber == ThemeDetails.THEME_RANDOM) {
                int number = new Random().nextInt(ThemeDetails.TOTAL_THEMES);
                setThemeNumber(number);
                background = getAssetDrawable(res, themeNumber);
                if (background == null) {
                    background = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.bg0));
                    Log.e("setNewBackground", "Bitmap could not be obtained");
                }
            }*/
            if (background != null) {
                int blurLevel = PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingsActivity.BLUR_BG, 0);
                if (blurLevel != 0) {
                    background = new BitmapDrawable(res, Blur.apply(context, background.getBitmap(), blurLevel));
                }
            }
            setBackground(background, res, false);
        } catch (OutOfMemoryError ex) {
            // We have no memory left. Do nothing
        }

        return background;
    }

    /**
     * @param res
     * @param tNumber
     * @return
     */
    public static BitmapDrawable getAssetDrawable(Resources res, int tNumber) {

        AssetManager assets = res.getAssets();
        InputStream stream;
        DisplayMetrics metrics = res.getDisplayMetrics();
        int minDim = Math.min(metrics.widthPixels, metrics.heightPixels);
        boolean isPotrait = CommonUtils.isPortrait(res);

        String[] images = res.getStringArray(isPotrait ? R.array.backgroundMain : R.array.backgroundMainLand);
        if (tNumber >= images.length) {
            tNumber = images.length - 1;
        }

        try {
            stream = new BufferedInputStream(assets.open(images[tNumber]));
        } catch (IOException e) {
            Log.v("getAssetDrawable", e.toString());
            return null;
        }
        if (minDim < 600) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            return new BitmapDrawable(res, bitmap);
        }
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BitmapDrawable(res, bitmap);
    }

    private static BitmapDrawable getDetailsDrawable(Resources res, int tNumber){
        AssetManager assets = res.getAssets();
        InputStream stream;
        DisplayMetrics metrics = res.getDisplayMetrics();
        int minDim = Math.min(metrics.widthPixels, metrics.heightPixels);
        String name = ThemeDetails.isThemeDark(tNumber) ? "details_bg_dark" : "details_bg_light";
        String filePath = "theme/" +  (CommonUtils.isPortrait(res) ? name + ".jpg" : name + "_land.jpg");
        try {
            stream = new BufferedInputStream(assets.open(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return  new BitmapDrawable(res, BitmapFactory.decodeResource(res, ThemeDetails.isThemeDark(tNumber) ? R.drawable.details_bg_dark : R.drawable.details_bg_light));
        }
        if (minDim < 600) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            return new BitmapDrawable(res, bitmap);
        }
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BitmapDrawable(res, bitmap);
    }

    /**
     * @param res
     * @param filePath
     * @return
     */
    public static Bitmap getImageFromAssets(Resources res, String filePath){
        AssetManager assets = res.getAssets();
        InputStream stream;
        DisplayMetrics metrics = res.getDisplayMetrics();
        int minDim = Math.min(metrics.widthPixels, metrics.heightPixels);
        try {
            stream = new BufferedInputStream(assets.open(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (minDim < 600) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            return BitmapFactory.decodeStream(stream, null, options);
        }
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap getBitmapFromPath(String path){
        File imgFile = new  File(path);
        if(imgFile.exists()){
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        return null;
    }

    public static BitmapDrawable getCustomDrawable(String path, Resources res) {
        BitmapDrawable background = (BitmapDrawable) BitmapDrawable.createFromPath(path);
        boolean isTablet = res.getBoolean(R.bool.config_isTablet);
        Bitmap bitmap = background.getBitmap();
        if (isTablet) {
            return res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? background : new BitmapDrawable(res, Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth() / 2, bitmap.getHeight()));
        } else {
            return res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? background : new BitmapDrawable(res, Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight() / 2));
        }
    }

    public static Bitmap initChronoBitmap(Resources res, boolean isTimer){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        DisplayMetrics metrics = res.getDisplayMetrics();
        float screenHeight = metrics.heightPixels;
        float screenWidth = metrics.widthPixels;
        int minDim = Math.min((int) screenHeight, (int) screenWidth);
            // Sec hand is in 72.5% area(Ex: 580/800)
            if (minDim >= 1400) {
                return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial790 : R.drawable.stopwatch_dial790, options);
            } else if (minDim >= 1200) {
                    return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial700 : R.drawable.stopwatch_dial700, options);
            } else if (minDim >= 1000) {
                    return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial640 : R.drawable.stopwatch_dial640, options);
            } else if (minDim >= 720) {
                    return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial420 : R.drawable.stopwatch_dial420, options);
            } else if (minDim >= 590) {
                    return  BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial320 : R.drawable.stopwatch_dial320, options);
            } else if (minDim >= 460) {
                    return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial260 : R.drawable.stopwatch_dial260, options);
            } else if (minDim >= 300) {
                    return  BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial150 : R.drawable.stopwatch_dial150, options);
            } else {
                    return BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial110 : R.drawable.stopwatch_dial110, options);
            }
    }

    public static void setChronoBitmap(Bitmap bitmap, boolean isTimer) {
        if (isTimer) {
            timerBitmap = bitmap;
        } else {
            stopwatchBitmap = bitmap;
        }
    }

    public static void setChronoBigBitmap(Bitmap bitmap, boolean isTimer) {
        if (isTimer) {
            timerBigBitmap = bitmap;
        } else {
            stopwatchBigBitmap = bitmap;
        }
    }

    public static BitmapDrawable getDetailsBitmap(Resources res, int tNumber) {
        if (CommonUtils.isPortrait(res)) {
            if (detailsPortBitmap != null) {
                if (!detailsPortBitmap.getBitmap().isRecycled()) {
                    return detailsPortBitmap;
                } else {
                    try {
                        detailsPortBitmap = getDetailsDrawable(res, tNumber);
                    } catch (OutOfMemoryError ignored) {
                    }
                    return detailsPortBitmap;
                }
            } else {
                try {
                    detailsPortBitmap = getDetailsDrawable(res, tNumber);
                } catch (OutOfMemoryError ignored) {
                }
                return detailsPortBitmap;
            }
        } else {
            if (detailsLandBitmap != null) {
                if (!detailsLandBitmap.getBitmap().isRecycled()) {
                    return detailsLandBitmap;
                } else {
                    try {
                        detailsLandBitmap = getDetailsDrawable(res, tNumber);
                    } catch (OutOfMemoryError ignored) {
                    }
                    return detailsLandBitmap;
                }
            } else {
                try {
                    detailsLandBitmap = getDetailsDrawable(res, tNumber);
                } catch (OutOfMemoryError ignored) {
                }
                return detailsLandBitmap;
            }
        }
    }

    public static BitmapDrawable getDetailsBitmap(Configuration newConfig, Resources res, int tNumber) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (detailsPortBitmap != null) {
                if (!detailsPortBitmap.getBitmap().isRecycled()) {
                    return detailsPortBitmap;
                } else {
                    try {
                        detailsPortBitmap = getDetailsDrawable(res, tNumber);
                    } catch (OutOfMemoryError ignored) {
                    }
                    return detailsPortBitmap;
                }
            } else {
                try {
                    detailsPortBitmap = getDetailsDrawable(res, tNumber);
                } catch (OutOfMemoryError ignored) {
                }
                return detailsPortBitmap;
            }
        } else {
            if (detailsLandBitmap != null) {
                if (!detailsLandBitmap.getBitmap().isRecycled()) {
                    return detailsLandBitmap;
                } else {
                    try {
                        detailsPortBitmap = getDetailsDrawable(res, tNumber);
                    } catch (OutOfMemoryError ignored) {
                    }
                    return detailsLandBitmap;
                }
            } else {
                try {
                    detailsPortBitmap = getDetailsDrawable(res, tNumber);
                } catch (OutOfMemoryError ignored) {
                }
                return detailsLandBitmap;
            }
        }
    }

    /**
     * @param isTablet
     * @param resolver
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFromUri(boolean isTablet, ContentResolver resolver, Uri uri) {

        InputStream in;
        try {
            in = resolver.openInputStream(uri);

            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(in, null, o);
            if (in != null) {
                in.close();
            }

            int scale = 1;
            int maxSize = isTablet ? AlarmSettingsActivity.IMAGE_MAX_SIZE_TABLET : AlarmSettingsActivity.IMAGE_MAX_SIZE;
            if (o.outHeight > maxSize || o.outWidth > maxSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = resolver.openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            if (in != null) {
                in.close();
            }

            return b;
        } catch (IOException e) {
            Log.e("getBitmapFromUri", "file not found for URI: " + uri.toString());
        }
        return null;
    }

    public static Bitmap getThemeThumbnail(AssetManager manager, String filePath,
                                           boolean isTablet) throws IOException {

        InputStream stream = new BufferedInputStream((manager.open(filePath)));
        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inSampleSize = isTablet ? 4 : 8;
        Bitmap bitmap =  BitmapFactory.decodeStream(stream, null, options);
        stream.close();
        return bitmap;
    }

    public static void saveOutput(Context context, Bitmap croppedImage, Uri mSaveUri) {

        if (mSaveUri != null) {
            OutputStream outputStream = null;
            ContentResolver mContentResolver = context.getContentResolver();
            try {
                outputStream = mContentResolver.openOutputStream(mSaveUri);
                if (outputStream != null) {
                    croppedImage.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                }
            } catch (Exception ex) {
                Log.e("CommonUtils", "Error compressing output: " + mSaveUri, ex);
                return;
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (Throwable t) {
                    // do nothing
                }
            }
        } else {
            Log.e("CommonUtils", "Image uri not defined");
        }
        croppedImage.recycle();
    }

    public static boolean isAppNotRunning() {  // If both bitmaps are null, app must not be running
        return portraitBg == null && landBg == null;
    }

    public static Bitmap getChronoBitmap(boolean isTimer) {
        return isTimer ? timerBitmap : stopwatchBitmap;
    }

    public static Bitmap getChronoBigBitmap(boolean isTimer) {
        return isTimer ? timerBigBitmap : stopwatchBigBitmap;
    }

    public static void setIsAnimation(boolean isAnimation) {
        BitmapController.isAnimation = isAnimation;
    }

    public static boolean isAnimation() {
        return isAnimation;
    }

    public static int getThemeNumber() {
        if (themeNumber < 0) {
            return 0;
        }
        return themeNumber;
    }

    public static void setThemeNumber(int themeNumber) {
        BitmapController.themeNumber = themeNumber;
    }
}

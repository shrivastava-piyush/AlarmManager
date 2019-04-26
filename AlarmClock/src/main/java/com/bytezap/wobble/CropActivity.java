package com.bytezap.wobble;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.bytezap.wobble.customviews.CropImage;
import com.bytezap.wobble.preference.AlarmSettingsActivity;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ToastGaffer;

import java.io.File;

public class CropActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = CropActivity.class.getSimpleName();
    private CropImage cropImage;
    private boolean isThemeActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (CommonUtils.isKOrLater()) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        CommonUtils.setLanguage(getResources(), CommonUtils.getLangCode());

        setContentView(R.layout.crop_layout);
        if (CommonUtils.isLOrLater()) {
            window.setStatusBarColor(Color.BLACK);
        }

        cropImage = findViewById(R.id.cropImageView);
        Button save = findViewById(R.id.crop_save);
        Button cancel = findViewById(R.id.crop_cancel);

        boolean isTablet = getResources().getBoolean(R.bool.config_isTablet);
        Uri imageUri = getIntent().getData();
        isThemeActivity = getIntent().getBooleanExtra(Clock.CROP_IS_THEME, false);

        if (!CommonUtils.is16OrLater()) {
            cropImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        Bitmap imageBitmap = null;
        try {
            imageBitmap = BitmapController.getBitmapFromUri(isTablet, getContentResolver(), imageUri);
            cropImage.setImageBitmap(imageBitmap);
        } catch (Throwable b) {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.invalid_file));
            finish();
        }

        if (imageBitmap!=null) {
            if (isTablet) {
                cropImage.setCropMode(imageBitmap.getWidth() > imageBitmap.getHeight() ? CropImage.CropMode.RATIO_FIT_IMAGE : CropImage.CropMode.RATIO_16_9);
            } else {
                cropImage.setCropMode(imageBitmap.getHeight() > imageBitmap.getWidth() ? CropImage.CropMode.RATIO_FIT_IMAGE : CropImage.CropMode.RATIO_3_4);
            }
        }

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.crop_save:
                try {
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                            + (isThemeActivity ? AlarmSettingsActivity.DIR_WALLPAPER_THEME : AlarmSettingsActivity.DIR_WALLPAPER_ALARM));
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    Bitmap image = cropImage.getCroppedBitmap();
                    if (image != null) {
                        File imageFile = File.createTempFile("egb", AlarmSettingsActivity.DEFAULT_WALLPAPER_EXTENSION, dir);
                        BitmapController.saveOutput(this, image, Uri.fromFile(imageFile));
                        if (isThemeActivity) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(Clock.THEME_BG_PATH, imageFile.getAbsolutePath());
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            SharedPreferences preferences = getSharedPreferences(Clock.MAIN_PREF, Context.MODE_PRIVATE);
                            SharedPreferences.Editor bgEditor = preferences.edit();
                            bgEditor.putString(Clock.ALARM_BG, imageFile.getAbsolutePath());
                            bgEditor.apply();

                            preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            preferences.edit().putString(SettingsActivity.ALARM_BACKGROUND, "2").apply();

                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                break;

            case R.id.crop_cancel:
                finish();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case Clock.REQUEST_PERMISSION_STORAGE_DOC:
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
                    finish();
                }
                break;
        }
    }

}

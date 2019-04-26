/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.barcode.CustomScannerView;
import com.bytezap.wobble.customviews.barcode.ScannerViewFinder;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ToastGaffer;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import me.dm7.barcodescanner.core.IViewFinder;

public class BarcodeScanner extends AppCompatActivity implements CustomScannerView.ResultHandler, View.OnClickListener, View.OnLongClickListener {

    public static final String BARCODE_TEXT = "barcode_text";
    private static final String FLASH_STATE = "FLASH_STATE";
    private static final String AUTO_FOCUS_STATE = "auto_focus_state";
    private CustomScannerView mScannerView;
    private boolean isAutoFocus, isFlash;
    private Button flash, autoFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CommonUtils.isLOrLater()) {
            getWindow().setStatusBarColor(Color.parseColor("#80000000"));
        }

        boolean isAnimation = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.ANIMATION, true);
        if (isAnimation) {
            overridePendingTransition(CommonUtils.isPortrait(getResources()) ? R.anim.enter_zoom : R.anim.enter_scale, R.anim.exit_zoom);
        }

        CommonUtils.setLanguage(getResources(), CommonUtils.getLangCode());

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

        setContentView(R.layout.scanner_layout);

        flash = findViewById(R.id.scanner_flash);
        autoFocus = findViewById(R.id.scanner_focus);
        ImageButton back = findViewById(R.id.scanner_back);

        mScannerView = new CustomScannerView(this) { // Programmatically initialize the scanner view
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new ScannerViewFinder(context);
            }
        };

        FrameLayout mainLayout = findViewById(R.id.scanner_layout);
        mainLayout.addView(mScannerView, 0);

        if (savedInstanceState != null) {
            isFlash = savedInstanceState.getBoolean(FLASH_STATE, false);
            isAutoFocus = savedInstanceState.getBoolean(AUTO_FOCUS_STATE, true);
            flash.setCompoundDrawablesWithIntrinsicBounds(isFlash ? R.drawable.ic_flash : R.drawable.ic_flash_off, 0, 0, 0);
            flash.setText(isFlash ? R.string.defualt_on : R.string.default_off);
            flash.setTextColor(isFlash ? Color.WHITE : Color.BLACK);
            autoFocus.setCompoundDrawablesWithIntrinsicBounds(isAutoFocus ? R.drawable.ic_autofocus : R.drawable.ic_autofocus_off, 0, 0, 0);
            autoFocus.setText(isAutoFocus ? R.string.defualt_on : R.string.default_off);
            autoFocus.setTextColor(isAutoFocus ? Color.WHITE : Color.BLACK);
        } else {
            isFlash = false;
            isAutoFocus = true;
        }

        flash.setOnClickListener(this);
        autoFocus.setOnClickListener(this);
        back.setOnClickListener(this);

        flash.setOnLongClickListener(this);
        autoFocus.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.scanner_flash:
                isFlash = !isFlash;
                flash.setCompoundDrawablesWithIntrinsicBounds(isFlash ? R.drawable.ic_flash : R.drawable.ic_flash_off, 0, 0, 0);
                flash.setText(isFlash ? R.string.defualt_on : R.string.default_off);
                flash.setTextColor(isFlash ? Color.WHITE : Color.BLACK);
                mScannerView.setFlash(isFlash);
                break;

            case R.id.scanner_focus:
                isAutoFocus = !isAutoFocus;
                autoFocus.setCompoundDrawablesWithIntrinsicBounds(isAutoFocus ? R.drawable.ic_autofocus : R.drawable.ic_autofocus_off, 0, 0, 0);
                autoFocus.setText(isAutoFocus ? R.string.defualt_on : R.string.default_off);
                autoFocus.setTextColor(isAutoFocus ? Color.WHITE : Color.BLACK);
                mScannerView.setAutoFocus(isAutoFocus);
                break;

            case R.id.scanner_back:
                mScannerView.stopCamera();
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results
        mScannerView.startCamera();
        mScannerView.setFlash(isFlash);
        mScannerView.setAutoFocus(isAutoFocus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(FLASH_STATE, isFlash);
        outState.putBoolean(AUTO_FOCUS_STATE, isAutoFocus);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void handleResult(Result result) {
        String barText = result.getText();
        if (isBarcodeSupported(result.getBarcodeFormat())) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(BARCODE_TEXT, barText);
            setResult(RESULT_OK, resultIntent);
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(40);
            finish();
        } else {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.barcode_not_supported));
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScannerView.resumeCameraPreview(BarcodeScanner.this);
                }
            }, 2000);
        }
    }

    private boolean isBarcodeSupported(BarcodeFormat format) {
        return CustomScannerView.ALL_FORMATS.contains(format);
    }

    @Override
    public boolean onLongClick(View v) {

        switch (v.getId()) {
            case R.id.scanner_flash:
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.default_flash));
                break;

            case R.id.scanner_focus:
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.default_autofocus));
                break;
        }
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

}

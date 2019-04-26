/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.barcode.CustomScannerView;
import com.bytezap.wobble.customviews.barcode.ScannerViewFinder;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ToastGaffer;
import com.google.zxing.Result;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import me.dm7.barcodescanner.core.IViewFinder;

    /*
    Separate fragment implemented for AlarmScreen, so that there is no gap in between
    clicking the dismiss button and starting barcode scanner
    */

public class BarcodeScannerFragment extends Fragment implements CustomScannerView.ResultHandler, View.OnClickListener, View.OnLongClickListener {

    private CustomScannerView mScannerView;
    private AlarmInstance instance;
    private boolean isAutoFocus, isFlash;
    private Button flash, autoFocus;
    private Context mContext;

    private ImageView checkedView;
    private ScannerViewFinder viewFinder;
    private Handler checkedHandler = new Handler();
    private ViewPropertyAnimator checkedAnimator;

    private final Runnable mCheckedRunnable = new Runnable() {
        @Override
        public void run() {
            hideChecked();
        }
    };

    public static BarcodeScannerFragment newInstance() {
        return new BarcodeScannerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mScannerView = new CustomScannerView(getActivity().getApplicationContext()) { // Programmatically initialize the scanner view
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                viewFinder =  new ScannerViewFinder(context);
                return viewFinder;
            }
        };

        View rootView = inflater.inflate(R.layout.scanner_layout, container, false);
        flash = rootView.findViewById(R.id.scanner_flash);
        autoFocus = rootView.findViewById(R.id.scanner_focus);
        checkedView = rootView.findViewById(R.id.barcode_checked);
        checkedAnimator = checkedView.animate();

        FrameLayout mainLayout = rootView.findViewById(R.id.scanner_layout);
        mainLayout.addView(mScannerView, 0);

        getActivity().findViewById(R.id.alarmScreen_layout).setVisibility(View.INVISIBLE);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = BarcodeScannerFragment.this.getActivity().getApplicationContext();

        if (CommonUtils.isLOrLater()) {
            getActivity().getWindow().setStatusBarColor(Color.BLACK);
        }

        long id = getActivity().getIntent().getLongExtra(AlarmAssistant.ID, -1);
        instance = AlarmAssistant.getInstance(mContext, id);

        isFlash = false;
        isAutoFocus = true;

        ImageButton back = getActivity().findViewById(R.id.scanner_back);
        back.setOnClickListener(this);
        flash.setOnClickListener(this);
        autoFocus.setOnClickListener(this);

        flash.setOnLongClickListener(this);
        autoFocus.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.scanner_flash:
                isFlash = !isFlash;
                flash.setCompoundDrawablesWithIntrinsicBounds(isFlash ? R.drawable.ic_flash : R.drawable.ic_flash_off, 0, 0, 0);
                flash.setText(isFlash ? R.string.defualt_on : R.string.default_off);
                flash.setTextColor(isAutoFocus ? Color.WHITE : Color.BLACK);
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
                closeFragment();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {

        switch (v.getId()){
            case R.id.scanner_flash:
                ToastGaffer.showToast(mContext, getString(R.string.default_flash));
                break;

            case R.id.scanner_focus:
                ToastGaffer.showToast(mContext, getString(R.string.default_autofocus));
                break;
        }
        return true;
    }

    private void closeFragment() {
        mScannerView.stopCamera();
        getActivity().getFragmentManager().beginTransaction().remove(BarcodeScannerFragment.this).commit();
        if (CommonUtils.isLOrLater()) {
            getActivity().getWindow().setStatusBarColor(Color.parseColor("#90000000"));
        }
        SlidingUpPanelLayout sLayout = getActivity().findViewById(R.id.sliding_layout);
        sLayout.setVisibility(View.VISIBLE);
        sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        getActivity().findViewById(R.id.alarmScreen_layout).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results
        mScannerView.startCamera();
        mScannerView.setFlash(isFlash);
        mScannerView.setAutoFocus(isAutoFocus);
    }

    @Override
    public void handleResult(Result result) {
        String barText = result.getText();
            if (instance.barcodeText != null && barText!=null) {
                if (barText.equals(instance.barcodeText)) {
                    AlarmTone.terminate(mContext);
                    viewFinder.setShouldDrawLaser(false);
                    showChecked();
                } else {
                    viewFinder.setShouldDrawLaser(false);
                    barErrorDialog();
                }
            } else {
                Log.e(BarcodeScannerFragment.class.getSimpleName(), "Result was null");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.resumeCameraPreview(BarcodeScannerFragment.this);
                    }
                }, 2000);
            }
    }

    private void showChecked() {

        checkedHandler.removeCallbacks(mCheckedRunnable);
        checkedHandler.postDelayed(mCheckedRunnable,
                1500);

        checkedView.setVisibility(View.VISIBLE);
        checkedAnimator.cancel();
        checkedAnimator
                .alpha(1)
                .setDuration(1000)
                .setListener(null);
    }

    private void hideChecked() {
        checkedHandler.removeCallbacks(mCheckedRunnable);
        if (checkedView.getVisibility() == View.VISIBLE) {
            checkedAnimator.cancel();
            checkedAnimator
                    .alpha(0)
                    .setDuration(400)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            //checkedView.setVisibility(View.GONE);
                            CommonUtils.sendAlarmBroadcast(getActivity(), instance.id, AlarmService.ALARM_DISMISS);
                            getActivity().finish();
                        }
                    });
        }
    }

    private void barErrorDialog(){

        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.AlertDialogStyle);
        AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
        dialog.setTitle(R.string.barcode_wrong);
        dialog.setMessage(getString(R.string.wrong_barcode_info));
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mScannerView.resumeCameraPreview(BarcodeScannerFragment.this);
                    mScannerView.setFlash(isFlash);
                    mScannerView.setAutoFocus(isAutoFocus);
                    viewFinder.setShouldDrawLaser(true);
                    dialog.dismiss();
                } catch (RuntimeException e) {
                    Log.e("barErrorDialog", e.toString());
                }
            }
        });
        dialog.show();
    }

}

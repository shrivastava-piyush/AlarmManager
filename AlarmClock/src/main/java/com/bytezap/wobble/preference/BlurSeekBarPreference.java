/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bytezap.wobble.R;

public final class BlurSeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

    // Namespaces to read attributes
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    // Attribute names
    private static final String ATTR_DEFAULT_VALUE = "defaultValue";

    // Default values for defaults
    private static final int DEFAULT_CURRENT_VALUE = 100;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;

    // Real defaults
    private final int mDefaultValue;
    private final int mMaxValue;
    private final int mMinValue;

    // Current value
    private int mCurrentValue;
    private int mPreviousValue;

    // View elements
    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context context;

    public BlurSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        // Read parameters from attributes
        mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
        mMinValue = array.getInteger(R.styleable.SeekBarPreference_min, DEFAULT_MIN_VALUE);
        mMaxValue = array.getInteger(R.styleable.SeekBarPreference_max, DEFAULT_MAX_VALUE);

        setSummary(getSummary());
        array.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        // Get current value from preferences
        mCurrentValue = getPersistedInt(mDefaultValue);
        mPreviousValue = mCurrentValue;

        // Inflate layout
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View view = inflater.inflate(R.layout.preference_seekbar_dialog, null);

        // Setup SeekBar
        mSeekBar = view.findViewById(R.id.pref_seek_bar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        // Setup text label for current value
        mValueText = view.findViewById(R.id.seekbar_current_value);
        String progress = Integer.toString(mCurrentValue);
        mValueText.setText(progress);

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        // Persist current value if needed
        if (shouldPersist()) {
            persistInt(mCurrentValue);
        }

        setSummary(getSummary());

        if (mCurrentValue != mPreviousValue) {
            getOnPreferenceChangeListener().onPreferenceChange(this, mCurrentValue);
        }
        notifyChanged();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mSeekBar.setProgress(mCurrentValue);
    }

    @NonNull
    @Override
    public CharSequence getSummary() {
        //Return summary string with current value
        int value = getPersistedInt(mDefaultValue);
        if (value == 0) {
            return context.getString(R.string.default_off);
        } else {
            return value + "%";
        }
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        mCurrentValue = value + mMinValue;
        mCurrentValue = Math.round(value/10)*10;
        mValueText.setText(Integer.toString(mCurrentValue));
    }

    public void onStartTrackingTouch(SeekBar seek) {
        // Not used
    }

    public void onStopTrackingTouch(SeekBar seek) {
        // Not used
    }
}
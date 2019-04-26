package com.bytezap.wobble.preference;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.CommonUtils;

public final class VolumeSeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

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

    // View elements
    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context context;

    //Player to set and play volume
    private MediaPlayer mediaPlayer;

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
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
        String progress = Integer.toString(mCurrentValue) + "%";
        mValueText.setText(progress);

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        try{
            mediaPlayer.stop();
        } catch (Exception e){
            e.printStackTrace();
        }
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;

        // Return if change was cancelled
        if (!positiveResult) {
            return;
        }

        // Persist current value if needed
        if (shouldPersist()) {
            persistInt(mCurrentValue);
        }

        setSummary(getSummary());
        notifyChanged();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mSeekBar.setProgress(mCurrentValue);
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public CharSequence getSummary() {
        //Return summary string with current value
        int value = getPersistedInt(mDefaultValue);
        if (value == 100) {
            return context.getString(R.string.default_maximum);
        } else if (value < 100 && value >= 60) {
            return context.getString(R.string.default_high);
        } else if (value <= 59 && value > 35) {
            return context.getString(R.string.default_medium);
        } else if (value <= 34 && value > 0) {
            return context.getString(R.string.default_low);
        } else {
            return context.getString(R.string.default_silent);
        }
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        // Update current value
        mediaPlayer.reset();

        mCurrentValue = value + mMinValue;
        // Update label with current value
        String progress = Integer.toString(mCurrentValue) + "%";
        mValueText.setText(progress);

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaPlayer.reset();
                mediaPlayer.release();
                return true;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });

        try {
            mediaPlayer.setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            mediaPlayer.setLooping(false);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setVolume(mCurrentValue/100.f, mCurrentValue/100.f);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ex) {
            // It could be possible that there is no default ringtone on the device. So use the fallback one
            try {
                // Reset the media player to clear error state
                mediaPlayer.reset();
                try {
                    // Reset the media player to clear error state
                    mediaPlayer.reset();
                    CommonUtils.setSourceFromResource(context, mediaPlayer, R.raw.countdown_alarm);
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mediaPlayer.setVolume(mCurrentValue/100.f, mCurrentValue/100.f);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception ignored){}
            } catch (Exception e) {
                // At this point just don't play anything
            }
        }
    }

    public void onStartTrackingTouch(SeekBar seek) {
        // Do nothing here
    }

    public void onStopTrackingTouch(SeekBar seek) {
        // Do nothing here
    }
}
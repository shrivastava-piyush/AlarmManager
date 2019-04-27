/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.bytezap.wobble.R;

import java.util.Calendar;
import java.util.TimeZone;

public class ElementalAnalogClock extends RelativeLayout {

    private static final int INVALID_ANGLE = -1;
    private static final int DEGREE_MINUTE = 6;
    private static final int MINUTE_TO_HOUR_DEGREE = 12;
    private static final int HOUR_TO_HOUR_DEGREE = 30;

    // Default Timezone
    private static final String TIMEZONE_ID = TimeZone.getDefault().getID();

    // Hands
    private ImageView mHourHand;
    private ImageView mMinuteHand;
    private ImageView mSecondHand;

    // States
    private boolean isRunning = false;
    private boolean isFirstTick = true;

    // Angle
    private int mHourAngle = INVALID_ANGLE;
    private int mMinuteAngle = INVALID_ANGLE;
    private int mSecondAngle = INVALID_ANGLE;

    // Resources
    private int mDialBackgroundResource = R.drawable.appwidget_light_dial;
    private int mHourBackgroundResource = R.drawable.analog_clock_hour_hand;
    private int mMinuteBackgroundResource = R.drawable.analog_clock_minute_hand;
    private int mSecondBackgroundResource = R.drawable.analog_clock_second_hand;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (isRunning) {
                proceed();
                mHandler.sendEmptyMessageDelayed(0, 1000);
            }
        }
    };

    public ElementalAnalogClock(Context context) {
        super(context);
        init(context);
    }

    public ElementalAnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        setResources(context, attrs);
        init(context);
    }

    public ElementalAnalogClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setResources(context, attrs);
        init(context);
    }

    private void setResources(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ElementalAnalogClock);

        mDialBackgroundResource = array.getResourceId(R.styleable.ElementalAnalogClock_analog_dial, R.drawable.analog_clock_dial);
        mHourBackgroundResource = array.getResourceId(R.styleable.ElementalAnalogClock_analog_hand_hour, R.drawable.analog_clock_hour_hand);
        mMinuteBackgroundResource = array.getResourceId(R.styleable.ElementalAnalogClock_analog_hand_minute, R.drawable.analog_clock_minute_hand);
        mSecondBackgroundResource = array.getResourceId(R.styleable.ElementalAnalogClock_analog_hand_second, R.drawable.analog_clock_second_hand);
        array.recycle();
    }

    private void init(Context context) {

        RelativeLayout.LayoutParams lp;
            mHourHand = new ImageView(context);
            setHourHandResource(mHourBackgroundResource);
            mHourHand.setScaleType(ScaleType.CENTER_INSIDE);
            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(CENTER_IN_PARENT);
            this.addView(mHourHand, lp);

            mMinuteHand = new ImageView(context);
            setMinuteHandResource(mMinuteBackgroundResource);
            mMinuteHand.setScaleType(ScaleType.CENTER_INSIDE);
            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(CENTER_IN_PARENT);
            this.addView(mMinuteHand, lp);

            mSecondHand = new ImageView(context);
            setSecondHandResource(mSecondBackgroundResource);
            mSecondHand.setScaleType(ScaleType.CENTER_INSIDE);
            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(CENTER_IN_PARENT);
            this.addView(mSecondHand, lp);

        ViewCompat.setLayoutDirection(this, ViewCompat.LAYOUT_DIRECTION_LTR);

            setBackgroundResource(mDialBackgroundResource);
    }

    public void toggle() {
        if (isRunning)
            stop();
        else
            start();
    }

    public void start() {
        if (isRunning)
            return;

        isRunning = true;
        isFirstTick = true;
        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageDelayed(0, 0);
    }

    public void stop() {
        if (!isRunning)
            return;

        isRunning = false;
        mHandler.removeMessages(0);

    }

    private void proceed() {
        if (!isRunning)
            return;
        TimeZone tz = TimeZone.getTimeZone(TIMEZONE_ID);

        Calendar tempCal = Calendar.getInstance();
        tempCal.setTimeZone(tz);

        int hour = tempCal.get(Calendar.HOUR);
        int min = tempCal.get(Calendar.MINUTE);
        int sec = tempCal.get(Calendar.SECOND);

        int newHourAngle = hour * HOUR_TO_HOUR_DEGREE + (min / MINUTE_TO_HOUR_DEGREE) * DEGREE_MINUTE;
        int newMinuteAngle = min * DEGREE_MINUTE;
        int newSecondAngle = sec * DEGREE_MINUTE;

        if (isFirstTick) {
            if (mHourAngle != INVALID_ANGLE && mMinuteAngle != INVALID_ANGLE && mSecondAngle != INVALID_ANGLE) {
                rotateClock(mHourHand, mHourAngle, newHourAngle);
                rotateClock(mMinuteHand, mMinuteAngle, newMinuteAngle);
                rotateClock(mSecondHand, mSecondAngle, newSecondAngle);
            } else {
                rotateClock(mHourHand, newHourAngle, newHourAngle);
                rotateClock(mMinuteHand, newMinuteAngle, newMinuteAngle);
                rotateClock(mSecondHand, newSecondAngle, newSecondAngle);
            }
            isFirstTick = false;
        } else {
            if (min == 0 && sec == 0)
                rotateClock(mHourHand, newHourAngle - DEGREE_MINUTE, newHourAngle);
            if (sec == 0)
                rotateClock(mMinuteHand, newMinuteAngle - DEGREE_MINUTE, newMinuteAngle);

            rotateClock(mSecondHand, newSecondAngle - DEGREE_MINUTE, newSecondAngle);
        }
        mHourAngle = newHourAngle;
        mMinuteAngle = newMinuteAngle;
        mSecondAngle = newSecondAngle;
    }

    private void rotateClock(ImageView view, int fromAngle, int toAngle) {
        Animation anim;
        anim = new RotateAnimation(fromAngle, toAngle,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        int gap = Math.abs(toAngle - fromAngle);
        if (gap == DEGREE_MINUTE) {
            anim.setDuration(150);
            anim.setInterpolator(new DecelerateInterpolator());
        }
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public void setDialResource(int id) {
        this.mDialBackgroundResource = id;
        setBackgroundResource(mDialBackgroundResource);
    }

    private void setHourHandResource(int id) {
        this.mHourBackgroundResource = id;
        mHourHand.setImageResource(id);
    }

    private void setMinuteHandResource(int id) {
        this.mMinuteBackgroundResource = id;
        mMinuteHand.setImageResource(id);
    }

    private void setSecondHandResource(int id) {
        this.mSecondBackgroundResource = id;
        mSecondHand.setImageResource(id);
    }
}

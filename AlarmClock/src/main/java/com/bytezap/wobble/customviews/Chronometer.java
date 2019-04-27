/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.BitmapController;

public class Chronometer extends View {

    private Bitmap chronoImage;
    private int timerStartY;
    private int mAppOffsetX = 0;
    private int mAppOffsetY = 0;

    private float minAngle = 0;
    private float secAngle = 0;

    private int timeInMillis = 0;

    private int canvasWidth = 320;
    private int canvasHeight = 480;
    private int secCenterX = 160;
    private int secCenterY = 240;
    private int minCenterX = 160;
    private int minCenterY = 240;

    private int secHalfWidth = 0;
    private int secHalfHeight = 0;
    private int minHalfWidth = 0;
    private int minHalfHeight = 0;
    private int hourHalfWidth = 0;
    private int hourHalfHeight = 0;

    private boolean isTimer;
    private final float twoPI = (float) (Math.PI * 2.0);
    private int min_size;

    private Drawable secHand;
    private Drawable minHand;

    public Chronometer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Chronometer,
                0, 0);

        try {
            isTimer = array.getBoolean(R.styleable.Chronometer_isTimer, true);
        } finally {
            array.recycle();
        }
        init();
    }

    private void init() {

        Resources res = getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        float screenHeight = metrics.heightPixels;
        float screenWidth = metrics.widthPixels;
        int minDim = Math.min((int) screenHeight, (int) screenWidth);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        double scalingFactor = calculateSize(res, minDim, options);

        secHand = ContextCompat.getDrawable(Chronometer.this.getContext(), isTimer ? R.drawable.timer_hand_second : R.drawable.stopwatch_hand_second);
        minHand = ContextCompat.getDrawable(Chronometer.this.getContext(), isTimer ? R.drawable.timer_hand_minute : R.drawable.stopwtach_hand_minute);

        secHalfWidth = secHand.getIntrinsicWidth() / 2;
        secHalfHeight = secHand.getIntrinsicHeight() / 2;

        minHalfWidth = minHand.getIntrinsicWidth() / 2;
        minHalfHeight = minHand.getIntrinsicHeight() / 2;

        minHalfHeight = (int) ((double) minHalfHeight * scalingFactor);
        minHalfWidth = (int) ((double) minHalfWidth * scalingFactor);
        secHalfHeight = (int) ((double) secHalfHeight * scalingFactor);
        secHalfWidth = (int) ((double) secHalfWidth * scalingFactor);
        hourHalfWidth = (int) ((double) hourHalfWidth * scalingFactor);
        hourHalfHeight = (int) ((double) hourHalfHeight * scalingFactor);

        timerStartY = (canvasHeight - chronoImage.getHeight()) / 2;
        mAppOffsetX = (canvasWidth - chronoImage.getWidth()) / 2;

        int imageHeight = chronoImage.getHeight();

        if (timerStartY < 0)
            mAppOffsetY = -timerStartY;

        secCenterY = timerStartY + (imageHeight * 54 / 100); // Have hand in center
        minCenterY = timerStartY + (imageHeight * 39 / 100);
        secCenterX = canvasWidth / 2;
        minCenterX = canvasWidth / 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }

    private double calculateSize(Resources res, int minDim, BitmapFactory.Options options) {
        double scalingFactor;

        chronoImage = BitmapController.getChronoBitmap(isTimer);
        // Sec hand is in 72.5% area(Ex: 580/800)
        if (minDim >= 1400) {
            if (isChronoEmpty()) {
                chronoImage = BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial790 : R.drawable.stopwatch_dial790, options);
            }
            scalingFactor = 1.108;
        } else if (minDim >= 1200) {
            if (isChronoEmpty()) {
                chronoImage = BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial700 : R.drawable.stopwatch_dial700, options);
            }
            scalingFactor = 0.987;
        } else if (minDim >= 1000) {
            if (isChronoEmpty()) {
                chronoImage = BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial640 : R.drawable.stopwatch_dial640, options);
            }
            scalingFactor = 0.902;
        } else if (minDim >= 720) {
            if (isChronoEmpty()) {
                chronoImage = BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial420 : R.drawable.stopwatch_dial420, options);
            }
            scalingFactor = 0.592;
        } else if (minDim >= 590) {
            if (isChronoEmpty()) {
                chronoImage =  BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial320 : R.drawable.stopwatch_dial320, options);
            }
            scalingFactor = 0.451;
        } else if (minDim >= 460) {
            if (isChronoEmpty()) {
                chronoImage = BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial260 : R.drawable.stopwatch_dial260, options);
            }
            scalingFactor = 0.352;
        } else if (minDim >= 300) {
            if (isChronoEmpty()) {
                chronoImage =  BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial150 : R.drawable.stopwatch_dial150, options);
            }
            scalingFactor = 0.211;
        } else {
            if (isChronoEmpty()) {
                chronoImage =  BitmapFactory.decodeResource(res, isTimer ? R.drawable.timer_dial110 : R.drawable.stopwatch_dial110, options);
            }
            scalingFactor = 0.155;
        }

        BitmapController.setChronoBitmap(chronoImage, isTimer);
        min_size = chronoImage.getWidth();
        return scalingFactor;
    }

    private boolean isChronoEmpty() {
        return chronoImage == null || chronoImage.isRecycled();
    }

    private int measure(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = min_size - 5;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {

        // Account for padding
        int xpad = (getPaddingLeft() + getPaddingRight());
        int ypad = (getPaddingTop() + getPaddingBottom());

        canvasWidth = w - xpad;
        canvasHeight = h - ypad;
        init();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the background image
        canvas.drawColor(Color.TRANSPARENT);
        if (chronoImage != null) {
            try{
                canvas.drawBitmap(chronoImage, mAppOffsetX, timerStartY + mAppOffsetY, null);
            } catch (RuntimeException e){
                init();
                invalidate();
            }
        }

        if (minHand != null && secHand != null) {
            // Draw the min hand with its current rotation
            canvas.save();
            canvas.rotate((float) Math.toDegrees(minAngle), minCenterX, minCenterY + mAppOffsetY);
            minHand.setBounds(minCenterX - minHalfWidth, minCenterY - minHalfHeight + mAppOffsetY,
                    minCenterX + minHalfWidth, minCenterY + mAppOffsetY + minHalfHeight);
            minHand.draw(canvas);
            canvas.restore();

            // Draw the sec hand with its current rotation
            canvas.save();
            canvas.rotate((float) Math.toDegrees(secAngle), secCenterX, secCenterY + mAppOffsetY);
            secHand.setBounds(secCenterX - secHalfWidth, secCenterY - secHalfHeight + mAppOffsetY,
                    secCenterX + secHalfWidth, secCenterY + mAppOffsetY + secHalfHeight);
            secHand.draw(canvas);
            canvas.restore();
        }
    }

    public void animateChrono(final int hours, final int minutes, final int seconds, boolean isRouteShort) {
        secAngle = secAngle % twoPI; // Avoid more than 1 rotation
        minAngle = minAngle % twoPI;

        // Force hands to go back to 0 not forward
        final float toSecsAngle = getShortestAngle(secAngle, twoPI * seconds / 60f, isRouteShort);
        final float toMinsAngle = getShortestAngle(minAngle, twoPI * ((minutes > 30 ? minutes - 30 : minutes) / 30f + seconds / 1800f), isRouteShort);

        float maxAngleChange = Math.max(Math.abs(secAngle - toSecsAngle), Math.abs(toMinsAngle - minAngle));
        int duration;
        if (maxAngleChange < Math.PI / 2) duration = 200;
        else if (maxAngleChange < Math.PI) duration = 400;
        else duration = 500;

        final ValueAnimator secsAnimation = ValueAnimator.ofFloat(secAngle, toSecsAngle);
        secsAnimation.setInterpolator(new DecelerateInterpolator());
        secsAnimation.setDuration(duration);

        final ValueAnimator minsAnimation = ValueAnimator.ofFloat(minAngle, toMinsAngle);
        minsAnimation.setInterpolator(new DecelerateInterpolator());
        minsAnimation.setDuration(duration);

        final ValueAnimator clockAnimation = ValueAnimator.ofInt(timeInMillis, (hours * 3600000 + minutes * 60000 + seconds * 1000));
        clockAnimation.setInterpolator(new DecelerateInterpolator());
        clockAnimation.setDuration(duration);

        secsAnimation.start();
        minsAnimation.start();
        clockAnimation.start();

        post(new Runnable() {
            @Override
            public void run() {

                if (secsAnimation.isRunning() || minsAnimation.isRunning() || clockAnimation.isRunning()) {
                    secAngle = (Float) secsAnimation.getAnimatedValue();
                    minAngle = (Float) minsAnimation.getAnimatedValue();
                    timeInMillis = (int) clockAnimation.getAnimatedValue();
                    invalidate();
                    postDelayed(this, 25);
                } else {
                    secAngle = toSecsAngle; //ensure the hands have ended at correct position
                    minAngle = toMinsAngle;
                    timeInMillis = hours * 3600000 + minutes * 60000 + seconds * 1000;
                    invalidate();
                }
            }
        });
    }

    private float getShortestAngle(final float fromAngle, final float toAngle, boolean isStopwatch) {
        if (isStopwatch) // hands must always go backwards
        {
            return toAngle; // timer reset always returns to 0,
        } else { // Taking the shortest route to 0 for timer
            float absFromMinusTo = Math.abs(fromAngle - toAngle);
            if (absFromMinusTo < Math.abs(fromAngle - (toAngle + twoPI))) {
                if (Math.abs(fromAngle - (toAngle - twoPI)) < absFromMinusTo) {
                    return (toAngle - twoPI);
                } else {
                    return toAngle;
                }
            } else return toAngle + twoPI;
        }
    }

    public void updateChronometer(int currentTime) {

        timeInMillis = currentTime;

        minAngle = twoPI * (timeInMillis / 1800000.0f);
        secAngle = twoPI * (timeInMillis / 60000.0f);

        invalidate();
    }

}

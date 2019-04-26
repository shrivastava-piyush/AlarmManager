/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews.listview;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.BitmapController;

public class AlarmListView extends ListView {
    private static final String TAG = AlarmListView.class.getName();
    protected boolean mIsShortList;
    protected int mOffsetY;
    protected AnimatorSet mAnimatorSet;
    protected float mLastPivotY;
    protected boolean mScaleYDirty;
    protected int mLastY;
    protected int mInertia;
    protected int mDownMotionY;
    protected boolean mIsTouching;
    protected int restrictedArea;
    protected Rect rect = new Rect();

    public AlarmListView(Context context) {
        super(context);
        init(context);
    }

    public AlarmListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlarmListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.setOverScrollMode(OVER_SCROLL_NEVER);
        this.setFadingEdgeLength(0);
        Resources res = context.getResources();
        restrictedArea = res.getDimensionPixelSize(R.dimen.alarm_padding);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!shouldPassTouchEvent(ev)) {
            return false;
        }

        if (!BitmapController.isAnimation()) {
            return super.onTouchEvent(ev);
        }

        int y = (int) ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                AlarmListViewUtils.onTouchDown(this, ev);
                this.mDownMotionY = (y - (this.mLastY - this.mDownMotionY));
                this.mLastY = y;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                this.mLastY = y;
                if (this.mIsTouching) {
                    this.mIsTouching = false;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (this.mIsTouching) {
                    this.mIsTouching = false;
                    AlarmListViewUtils.resetScale(this);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int offset = (y - this.mDownMotionY);

                if (!this.mIsTouching) {
                    AlarmListViewUtils.onTouchDown(this, ev);
                }
                this.mInertia = y - mDownMotionY;

                if (y > getHeight() - 5 || y < getHeight()/10) {
                    this.mLastY = y;
                    if (this.mIsTouching) {
                        this.mIsTouching = false;
                    }
                    break;
                }

                if (AlarmListViewUtils.needListScale(this, offset)) {
                    this.mLastY = y;
                } else if (y != this.mLastY) {
                    this.mLastY = y;
                }
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private boolean shouldPassTouchEvent(MotionEvent event) {
        try {
            getHitRect(rect);
            rect.left += restrictedArea;
            rect.right -= restrictedArea;
            return rect.contains((int) event.getX(), (int) event.getY());
        }catch (Exception e){
            return true;
        }
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (canvas != null && BitmapController.isAnimation()) {
            AlarmListViewUtils.onRenderTick(this, canvas);
        }
    }
}
package com.bytezap.wobble.customviews.gridview;

import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

import com.bytezap.wobble.utils.BitmapController;

public class CustomGridView extends GridView {

    private static final String TAG = CustomGridView.class.getName();
    protected boolean mIsShortList;
    protected int mOffsetY;
    protected AnimatorSet mAnimatorSet;
    protected float mLastPivotY;
    protected boolean mScaleYDirty;
    protected int mLastY;
    protected int mInertia;
    protected int mDownMotionY;
    protected boolean mIsTouching;

    public CustomGridView(Context context) {
        super(context);
        init();
    }

    public CustomGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.setOverScrollMode(OVER_SCROLL_NEVER);
        if (BitmapController.isAnimation()) {
            this.setFadingEdgeLength(0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!BitmapController.isAnimation()) {
            return super.onTouchEvent(ev);
        }

        int y = (int) ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN://0
            case MotionEvent.ACTION_POINTER_DOWN:
                GridViewUtils.onTouchDown(this, ev);
                this.mDownMotionY = (y - (this.mLastY - this.mDownMotionY));
                this.mLastY = y;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                this.mLastY = y;
                if (this.mIsTouching) {
                    this.mIsTouching = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (this.mIsTouching) {
                    this.mIsTouching = false;
                    GridViewUtils.resetScale(this);
                }
                break;
            case MotionEvent.ACTION_MOVE: {
                int offset = (y - this.mDownMotionY);

                if (!this.mIsTouching) {
                    GridViewUtils.onTouchDown(this, ev);
                }
                this.mInertia = y - mDownMotionY;

                if (GridViewUtils.needGridScale(this, offset)) {
                    this.mLastY = y;
                } else {
                    if (y != this.mLastY) {
                        this.mLastY = y;
                    }
                }
            }
            break;
        }

        return super.onTouchEvent(ev);
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (canvas != null  && BitmapController.isAnimation()) {
            GridViewUtils.onRenderTick(this, canvas);
        }
    }
}

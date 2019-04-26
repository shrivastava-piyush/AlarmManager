package com.bytezap.wobble.customviews.listview;

import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;

public class CustomListView extends ListView{
    private static final String TAG = CustomListView.class.getName();
    protected boolean mIsShortList;
    protected int mOffsetY;
    protected AnimatorSet mAnimatorSet;
    protected float mLastPivotY;
    protected boolean mScaleYDirty;
    protected int mLastY;
    protected int mInertia;
    protected int mDownMotionY;
    protected boolean mIsTouching;

    public CustomListView(Context context) {
        super(context);
        init();
    }

    public CustomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setOverScrollMode(OVER_SCROLL_NEVER);
        if (BitmapController.isAnimation()) {
            this.setFadingEdgeLength(0);
        }
        if (!CommonUtils.is17OrLater()) {
            setSelector(R.drawable.list_selector);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!BitmapController.isAnimation()) {
            return super.onTouchEvent(ev);
        }

        int y = (int) ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                ListViewUtils.onTouchDown(this, ev);
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
                    ListViewUtils.resetScale(this);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int offset = (y - this.mDownMotionY);

                if (!this.mIsTouching) {
                    ListViewUtils.onTouchDown(this, ev);
                }
                this.mInertia = y - mDownMotionY;

                if (ListViewUtils.needListScale(this, offset)) {
                    this.mLastY = y;
                } else {
                    if (y != this.mLastY) {
                        this.mLastY = y;
                    }
                }
                break;
        }

        return super.onTouchEvent(ev);
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (canvas != null  && BitmapController.isAnimation()) {
            ListViewUtils.onRenderTick(this, canvas);
        }
    }
}
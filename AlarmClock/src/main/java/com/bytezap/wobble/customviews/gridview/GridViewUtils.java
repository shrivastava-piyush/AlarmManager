/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews.gridview;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

public class GridViewUtils {
    private static final String TAG = GridViewUtils.class.getName();

    public static class BackOutGridInterpolater implements Interpolator {
        public float overshot = 0.0f;

        public float getInterpolation(float t) {
            float s = this.overshot == 0.0f ? 1.70158f : this.overshot;
            t -= 1;
            return ((t * t) * (((s + 1) * t) + s)) + 1;
        }
    }

    public static class CircOutGridInterpolator implements Interpolator {
        public float getInterpolation(float input) {
            input -= 1;
            return (float) Math.sqrt((double) (1 - (input * input)));
        }
    }

    public static void onTouchDown(CustomGridView gridView, MotionEvent ev) {
        gridView.mIsTouching = true;
        gridView.mInertia = 0;
        gridView.mOffsetY = 0;
        gridView.mDownMotionY = (int) ev.getY();
    }

    /**
     * @param gridView
     * @param offset
     * @return
     */
    public static boolean needGridScale(CustomGridView gridView, int offset) {

        if (gridView.mAnimatorSet != null && gridView.mAnimatorSet.isRunning()) {
            return true;
        }
        boolean atEdge = isScrollAtEdge(gridView, offset);
        if (atEdge) {
            gridView.mInertia = offset - gridView.mOffsetY;
            gridView.mScaleYDirty = true;
            gridView.invalidate();
        }

        return (gridView.mInertia != 0) && atEdge;
    }

    public static void resetScale(CustomGridView gridView) {
        gridView.mInertia = 0;
        gridView.setScaleY(1);
        gridView.invalidate();
    }

    public static void onRenderTick(CustomGridView gridView, Canvas canvas) {

        if (gridView.mAnimatorSet != null && gridView.mAnimatorSet.isRunning()) {
            setGridScale(gridView, canvas, 0, false);
        } else if (gridView.mIsTouching) {
            if (gridView.mScaleYDirty || gridView.getScaleY() != 1) {
                setGridScale(gridView, canvas, gridView.mInertia, false);
                gridView.mScaleYDirty = false;
            }
        } else if (isScrollAtEdge(gridView, gridView.mInertia) || gridView.getScaleY() != 1) {
            setGridScale(gridView, canvas, gridView.mInertia, true);
            gridView.mInertia = 0;
        } else {
            gridView.mInertia = (int) (gridView.mInertia * 0.98f);
            if (gridView.mInertia == 0) {
                gridView.setScaleY(1);
            }
        }
    }

    /**
     * Scale the grid
     *
     * @param gridView
     * @param canvas
     * @param offset
     * @param isTween
     */
    private static void setGridScale(final CustomGridView gridView, Canvas canvas, int offset, boolean isTween) {
        if (offset == 0) {
            canvas.scale(1, gridView.getScaleY(), 0, gridView.mLastPivotY);
            gridView.invalidate();
            return;
        }
        double scaleRatio = Math.min(Math.max(0.0d, (Math.sqrt((double) Math.abs(offset)) * 2.0d) * 0.001d), 0.1d);
        if (gridView.mIsShortList && offset < 0) {
            scaleRatio = -scaleRatio;
        }
        float scaleY = (float) (1 + scaleRatio);
        if (offset > 0 || gridView.mIsShortList) {
            gridView.mLastPivotY = 0.0f;
        } else {
            gridView.mLastPivotY = (float) gridView.getHeight();
        }
        gridView.setPivotX(0);
        gridView.setPivotY(gridView.mLastPivotY);
        if (isTween) {
            if (gridView.getScaleY() != 1) {
                canvas.scale(1, gridView.getScaleY(), 0, gridView.mLastPivotY);
                gridView.invalidate();
            }
            if (gridView.mAnimatorSet != null) {
                gridView.mAnimatorSet.cancel();
            }
            gridView.mAnimatorSet = new AnimatorSet();
            ValueAnimator scaleBackAnimator = ValueAnimator.ofFloat(scaleY, 1);
            scaleBackAnimator.setDuration(400);
            scaleBackAnimator.setInterpolator(new BackOutGridInterpolater());
            scaleBackAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    gridView.setScaleY((Float) animation.getAnimatedValue());
                    gridView.invalidate();
                }
            });
            if (gridView.getScaleY() == 1) {
                ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1, scaleY);
                scaleAnimator.setDuration(200);
                scaleAnimator.setInterpolator(new CircOutGridInterpolator());
                scaleAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        gridView.setScaleY((Float) animation.getAnimatedValue());
                        gridView.invalidate();
                    }
                });
                gridView.mAnimatorSet.play(scaleAnimator).before(scaleBackAnimator);
            } else {
                gridView.mAnimatorSet.play(scaleBackAnimator);
            }
            gridView.mAnimatorSet.start();
            return;
        }
        canvas.scale(1, scaleY, 0, gridView.mLastPivotY);
        gridView.setScaleY(scaleY);
        gridView.invalidate();
    }

    /**
     *
     * @param gridView
     * @param offset
     * @return
     */
    private static boolean isScrollAtEdge(CustomGridView gridView, int offset) {
        int childCount = gridView.getChildCount();
        if (childCount > 0) {
            boolean isScrollAtTop;
            boolean isScrollAtBottom;
            boolean isShortList;
            int firstPosition = gridView.getFirstVisiblePosition();
            int firstTop = gridView.getChildAt(0).getTop();
            int lastBottom = gridView.getChildAt(gridView.getChildCount() - 1).getBottom();
            int itemCount = gridView.getAdapter() != null ? gridView.getAdapter().getCount() : 0;

            isScrollAtTop = firstPosition == 0 || firstTop > gridView.getPaddingTop();

            isScrollAtBottom = (firstPosition + childCount == itemCount) || lastBottom < gridView.getHeight() - gridView.getPaddingBottom();
            isShortList = isScrollAtBottom && isScrollAtTop;
            gridView.mIsShortList = isShortList;

            if (gridView.mIsShortList) {
                if (gridView.mOffsetY == 0) {
                    gridView.mOffsetY = offset;
                }
            } else if (isScrollAtTop) {
                if (gridView.mOffsetY == 0 || offset < gridView.mOffsetY) {
                    gridView.mOffsetY = offset;
                }
            } else if (isScrollAtBottom && (gridView.mOffsetY == 0 || offset > gridView.mOffsetY)) {
                gridView.mOffsetY = offset;
            }
            if (isScrollAtTop && offset > 0) {
                return true;
            }
            return isScrollAtBottom && offset < 0;
        }
        return false;
    }

}
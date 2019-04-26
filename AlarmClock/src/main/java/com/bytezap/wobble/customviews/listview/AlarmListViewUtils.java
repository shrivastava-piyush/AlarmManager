/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews.listview;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

public class AlarmListViewUtils {
    private static final String TAG = AlarmListViewUtils.class.getName();

    private static class BackEaseOutInterpolater implements Interpolator {
        public float overshot = 0.0f;

        public float getInterpolation(float t) {
            float s = this.overshot == 0.0f ? 1.70158f : this.overshot;
            t -= 1;
            return ((t * t) * (((s + 1) * t) + s)) + 1;
        }
    }

    private static class CircEaseOutInterpolator implements Interpolator {
        public float getInterpolation(float input) {
            input -= 1;
            return (float) Math.sqrt((double) (1 - (input * input)));
        }
    }

    public static void onTouchDown(AlarmListView listView, MotionEvent ev) {
        listView.mIsTouching = true;
        listView.mInertia = 0;
        listView.mOffsetY = 0;
        listView.mDownMotionY = (int) ev.getY();
    }

    /**
     * @param listView
     * @param offset
     * @return
     */
    public static boolean needListScale(AlarmListView listView, int offset) {

        if (listView.mAnimatorSet != null && listView.mAnimatorSet.isRunning()) {
            return true;
        }
        boolean atEdge = isScrollAtEdge(listView, offset);
        if (atEdge) {
            listView.mInertia = offset - listView.mOffsetY;
            listView.mScaleYDirty = true;
            listView.invalidate();
        }

        //Log.d(TAG, "offset=" + offset + ",listView.mOffsetY:" + listView.mOffsetY + ",listView.mInertia:" + listView.mInertia + ",atEdge:" + atEdge);
        return (listView.mInertia != 0) && atEdge;
    }

    public static void resetScale(AlarmListView listView) {
        listView.mInertia = 0;
        listView.setScaleY(1);
        listView.invalidate();
    }

    public static void onRenderTick(AlarmListView listView, Canvas canvas) {

        if (listView.mAnimatorSet != null && listView.mAnimatorSet.isRunning()) {
            setListScale(listView, canvas, 0, false);
        } else if (listView.mIsTouching) {
            if (listView.mScaleYDirty || listView.getScaleY() != 1) {
                setListScale(listView, canvas, listView.mInertia, false);
                listView.mScaleYDirty = false;
            }
        } else if (isScrollAtEdge(listView, listView.mInertia) || listView.getScaleY() != 1) {
            setListScale(listView, canvas, listView.mInertia, true);
            listView.mInertia = 0;
        } else {
            listView.mInertia = (int) (listView.mInertia * 0.98f);
            if (listView.mInertia == 0) {
                listView.setScaleY(1);
            }
        }
    }

    /**
     * Scale the List
     *
     * @param listView
     * @param canvas
     * @param offset
     * @param isTween
     */
    private static void setListScale(final AlarmListView listView, Canvas canvas, int offset, boolean isTween) {
        if (offset == 0) {
            canvas.scale(1, listView.getScaleY(), 0, listView.mLastPivotY);
            listView.invalidate();
            return;
        }
        double scaleRatio = Math.min(Math.max(0.0d, (Math.sqrt((double) Math.abs(offset)) * 2.5d) * 0.001d), 0.1d);
        if (listView.mIsShortList && offset < 0) {
            scaleRatio = -scaleRatio;
        }
        float scaleY = (float) (1 + scaleRatio);
        if (offset > 0 || listView.mIsShortList) {
            listView.mLastPivotY = 0.0f;
        } else {
            listView.mLastPivotY = (float) listView.getHeight();
        }
        listView.setPivotX(0);
        listView.setPivotY(listView.mLastPivotY);
        if (isTween) {
            if (listView.getScaleY() != 1) {
                canvas.scale(1, listView.getScaleY(), 0, listView.mLastPivotY);
                listView.invalidate();
            }
            if (listView.mAnimatorSet != null) {
                listView.mAnimatorSet.cancel();
            }
            listView.mAnimatorSet = new AnimatorSet();
            ValueAnimator scaleBackAnimator = ValueAnimator.ofFloat(scaleY, 1);
            scaleBackAnimator.setDuration(400);
            scaleBackAnimator.setInterpolator(new BackEaseOutInterpolater());
            scaleBackAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    listView.setScaleY((Float) animation.getAnimatedValue());
                    listView.invalidate();
                }
            });
            if (listView.getScaleY() == 1) {
                ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1, scaleY);
                scaleAnimator.setDuration(200);
                scaleAnimator.setInterpolator(new CircEaseOutInterpolator());
                scaleAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        listView.setScaleY((Float) animation.getAnimatedValue());
                        listView.invalidate();
                    }
                });
                listView.mAnimatorSet.playSequentially(scaleAnimator, scaleBackAnimator);
            } else {
                listView.mAnimatorSet.play(scaleBackAnimator);
            }
            listView.mAnimatorSet.start();
            return;
        }
        canvas.scale(1, scaleY, 0, listView.mLastPivotY);
        listView.setScaleY(scaleY);
        listView.invalidate();
    }

    /**
     *
     * @param listView
     * @param offset
     * @return
     */
    private static boolean isScrollAtEdge(AlarmListView listView, int offset) {
        int childCount = listView.getChildCount();
        if (childCount > 0) {
            boolean isScrollAtTop;
            boolean isScrollAtBottom;
            boolean isShortList;
            int firstPosition = listView.getFirstVisiblePosition();
            int firstTop = listView.getChildAt(0).getTop();
            int lastBottom = listView.getChildAt(listView.getChildCount() - 1).getBottom();
            int itemCount = listView.getAdapter() != null ? listView.getAdapter().getCount() : 0;

            isScrollAtTop = firstPosition == 0 || firstTop > listView.getPaddingTop();

            isScrollAtBottom = (firstPosition + childCount == itemCount) || lastBottom < listView.getHeight() - listView.getPaddingBottom();
            isShortList = isScrollAtBottom && isScrollAtTop;
            listView.mIsShortList = isShortList;

            if (listView.mIsShortList) {
                if (listView.mOffsetY == 0) {
                    listView.mOffsetY = offset;
                }
            } else if (isScrollAtTop) {
                if (listView.mOffsetY == 0 || offset < listView.mOffsetY) {
                    listView.mOffsetY = offset;
                }
            } else if (isScrollAtBottom && (listView.mOffsetY == 0 || offset > listView.mOffsetY)) {
                listView.mOffsetY = offset;
            }
            if (isScrollAtTop && offset > 0) {
                return true;
            }
            return isScrollAtBottom && offset < 0;
        }
        return false;
    }

}
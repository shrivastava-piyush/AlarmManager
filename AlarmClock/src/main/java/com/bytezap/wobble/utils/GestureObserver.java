package com.bytezap.wobble.utils;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class GestureObserver extends SimpleOnGestureListener {

    public final static int SWIPE_UP = 1;
    public final static int SWIPE_DOWN = 2;
    public final static int SWIPE_LEFT = 3;
    public final static int SWIPE_RIGHT = 4;

    public final static int MODE_TRANSPARENT = 0;
    private final static int MODE_SOLID = 1;
    private final static int MODE_DYNAMIC = 2;

    private final static int ACTION_FAKE = -10; //an unlikely number
    private int swipe_Min_Distance_Vertical = 50;
    private int swipe_Min_Distance_Horizontal = 120;
    private int swipe_Max_Distance = 800;
    private int swipe_Min_Velocity = 15;

    private int mode = MODE_DYNAMIC;
    private boolean running = true;
    private boolean tapIndicator = false;

    private Activity context;
    private GestureDetector detector;
    private SimpleGestureListener listener;

    public GestureObserver(Activity context, SimpleGestureListener sgl) {

        this.context = context;
        this.detector = new GestureDetector(context, this);
        this.listener = sgl;
    }

    public void onTouchEvent(MotionEvent event) {

        if (!this.running)
            return;

        boolean result = this.detector.onTouchEvent(event);

        if (this.mode == MODE_SOLID)
            event.setAction(MotionEvent.ACTION_CANCEL);
        else if (this.mode == MODE_DYNAMIC) {

            if (event.getAction() == ACTION_FAKE)
                event.setAction(MotionEvent.ACTION_UP);
            else if (result)
                event.setAction(MotionEvent.ACTION_CANCEL);
            else if (this.tapIndicator) {
                event.setAction(MotionEvent.ACTION_DOWN);
                this.tapIndicator = false;
            }

        }
        //else just do nothing, it's transparent
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int m) {
        this.mode = m;
    }

    public void setEnabled(boolean status) {
        this.running = status;
    }

    public int getSwipeMaxDistance() {
        return this.swipe_Max_Distance;
    }

    public void setSwipeMaxDistance(int distance) {
        this.swipe_Max_Distance = distance;
    }

    public int getSwipeMinDistance() {
        return this.swipe_Min_Distance_Vertical;
    }

    public void setSwipeMinVertDistance(int distance) {
        this.swipe_Min_Distance_Vertical = distance;
    }

    public void setSwipeMinHorDistance(int distance) {
        this.swipe_Min_Distance_Horizontal = distance;
    }

    public int getSwipeMinVelocity() {
        return this.swipe_Min_Velocity;
    }

    public void setSwipeMinVelocity(int distance) {
        this.swipe_Min_Velocity = distance;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {

        final float xDistance = Math.abs(e1.getX() - e2.getX());
        final float yDistance = Math.abs(e1.getY() - e2.getY());

        if (xDistance > this.swipe_Max_Distance || yDistance > this.swipe_Max_Distance)
            return false;

        velocityX = Math.abs(velocityX);
        velocityY = Math.abs(velocityY);
        boolean result = false;

        if (velocityX > this.swipe_Min_Velocity && xDistance > this.swipe_Min_Distance_Horizontal) {
            if (e1.getX() > e2.getX()) // right to left
                this.listener.onSwipe(SWIPE_LEFT);
            else
                this.listener.onSwipe(SWIPE_RIGHT);

            result = true;
        } else if (velocityY > this.swipe_Min_Velocity && yDistance > this.swipe_Min_Distance_Vertical) {
            if (e1.getY() > e2.getY()) // bottom to up
                this.listener.onSwipe(SWIPE_UP);
            else
                this.listener.onSwipe(SWIPE_DOWN);

            result = true;
        }

        return result;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        this.tapIndicator = true;
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        this.listener.onLongPress();
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg) {
        this.listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg) {

        if (this.mode == MODE_DYNAMIC) {        // we owe an ACTION_UP, so we fake an
            arg.setAction(ACTION_FAKE);         //action, which will be converted to ACTION_UP later.
            this.context.dispatchTouchEvent(arg);
        }

        return false;
    }

    public interface SimpleGestureListener {
        void onSwipe(int direction);

        void onDoubleTap();

        void onLongPress();
    }

}
package com.bytezap.wobble.customviews;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class Wave extends Drawable {

    private Paint wavePaint;
    private int color;
    private int radius;
    private long animationTime = 1000;

    protected float waveScale;
    protected int alpha;

    private boolean shouldRepeat = true;

    private Interpolator waveInterpolator;
    private Interpolator alphaInterpolator;

    private Animator animator;
    private AnimatorSet animatorSet;

    public Wave(int color, int radius, long animationTime) {
        this(color, radius);
        this.animationTime = animationTime;
    }

    public Wave(int color, int radius) {
        this.color = color;
        this.radius = radius;
        this.waveScale = 0f;
        this.alpha = 255;

        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        animatorSet = new AnimatorSet();

    }

    @Override
    public void draw(Canvas canvas) {

        final Rect bounds = getBounds();

        // Drawing the wave
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setColor(color);
        wavePaint.setAlpha(alpha);
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius * waveScale, wavePaint);

    }

    public void setWaveInterpolator(Interpolator interpolator) {
        this.waveInterpolator = interpolator;
    }

    public void setAlphaInterpolator(Interpolator interpolator) {
        this.alphaInterpolator = interpolator;
    }

    public void startAnimation() {
        animator = createAnimation();
        animator.start();
    }

    public void stopAnimation() {
        if (animator.isRunning()) {
            animator.end();
        }
    }

    public void setShouldRepeat(boolean shouldRepeat){
        this.shouldRepeat = shouldRepeat;
    }


    public boolean isAnimationRunning() {
        return animator != null && animator.isRunning();
    }

    @Override
    @Keep
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        wavePaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return wavePaint.getAlpha();
    }


    @Keep
    protected void setWaveScale(float waveScale) {
        this.waveScale = waveScale;
        invalidateSelf();
    }

    protected float getWaveScale() {
        return waveScale;
    }

    private Animator createAnimation() {

        //Wave animation
        ObjectAnimator waveAnimator = ObjectAnimator.ofFloat(this, "waveScale", 0f, 1f);
        waveAnimator.setDuration(animationTime);
        if (waveInterpolator != null) {
            waveAnimator.setInterpolator(waveInterpolator);
        } else {
            waveAnimator.setInterpolator(new LinearInterpolator());
        }
        //The animation is repeated
        if (shouldRepeat) {
            waveAnimator.setRepeatCount(Animation.INFINITE);
            waveAnimator.setRepeatMode(ValueAnimator.RESTART);
        }

        //alpha animation
        ObjectAnimator alphaAnimator = ObjectAnimator.ofInt(this, "alpha", 255, 0);
        alphaAnimator.setDuration(animationTime);
        if (alphaInterpolator != null) {
            alphaAnimator.setInterpolator(alphaInterpolator);
        }
        if (shouldRepeat) {
            alphaAnimator.setRepeatCount(Animation.INFINITE);
            alphaAnimator.setRepeatMode(ValueAnimator.RESTART);
        }

        animatorSet.setStartDelay(200);
        animatorSet.playTogether(waveAnimator, alphaAnimator);

        return animatorSet;
    }
}

/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.utils;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.listview.CustomListView;

public class AnimUtils {

    public static void startListAnimation(Context context, final CustomListView listView) {

        if (CommonUtils.is16OrLater()) {
            listView.setHasTransientState(true);
        }

        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_from_bottom);

        LayoutAnimationController controller =
                new LayoutAnimationController(animation, 0.35f);
        listView.setLayoutAnimation(controller);
        listView.setLayoutAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (CommonUtils.is16OrLater()) {
                    listView.setHasTransientState(false);
                    listView.setLayoutAnimationListener(null);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        listView.startLayoutAnimation();
    }

    public static ObjectAnimator getPulseAnimator(View viewToAnimate, float decreaseRatio,
                                                  float increaseRatio) {
        Keyframe k0 = Keyframe.ofFloat(0f, 1f);
        Keyframe k1 = Keyframe.ofFloat(0.275f, decreaseRatio);
        Keyframe k2 = Keyframe.ofFloat(0.69f, increaseRatio);
        Keyframe k3 = Keyframe.ofFloat(1f, 1f);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe("scaleX", k0, k1, k2, k3);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe("scaleY", k0, k1, k2, k3);
        ObjectAnimator pulseAnimator =
                ObjectAnimator.ofPropertyValuesHolder(viewToAnimate, scaleX, scaleY);
        pulseAnimator.setDuration(400);

        return pulseAnimator;
    }

}

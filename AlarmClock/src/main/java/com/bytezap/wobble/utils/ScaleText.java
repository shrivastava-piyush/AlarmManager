package com.bytezap.wobble.utils;


import android.util.DisplayMetrics;

public class ScaleText {

    public static float scale(DisplayMetrics metrics, int optimalSize){

        float screenHeight = (float) metrics.heightPixels;
        float screenWidth = (float) metrics.widthPixels;

        float minDim = Math.min(screenHeight, screenWidth);

        if (minDim >= 1500) {
            return Math.round(optimalSize*1.2f);

        }else if (minDim >= 1400) {
            return Math.round(optimalSize*1.1f);

        }else if (minDim >= 1280) {
            return Math.round(optimalSize*0.95f);

        } else if (minDim >= 1000) {
            return Math.round(optimalSize*0.80f);

        } else if (minDim >= 720) {

            return Math.round(optimalSize*0.75f);
        } else if (minDim >= 590) {

            return Math.round(optimalSize*0.72f);
        }else if (minDim >= 460) {

            return Math.round(optimalSize*0.68f);
        } else if (minDim >= 320) {

            return Math.round(optimalSize*0.62f);
        } else if(minDim >= 240){

            return Math.round(optimalSize*0.55f);
        } else
        {
            return Math.round(optimalSize*0.5f);
        }
    }
}

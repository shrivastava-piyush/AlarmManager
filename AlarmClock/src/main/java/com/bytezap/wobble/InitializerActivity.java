/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class InitializerActivity extends AppCompatActivity{

    private Handler handler = new Handler();
    private AsyncTask<Void, Void, Void> asyncTask;
    private Runnable activityRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(InitializerActivity.this, Clock.class);
            intent.putExtra(Clock.IS_FROM_INITIALIZER, true);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Resources res = getResources();

        BitmapController.setIsAnimation(settings.getBoolean(SettingsActivity.ANIMATION, true));
        overridePendingTransition(0, 0);

        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(res, locale);

        setContentView(R.layout.initializer_layout);

        if (BitmapController.isAnimation()) {
            startAnimation();
        }
        /*There may be some conditions in which the AsyncTask will not be able to completely execute within 2 seconds.
         Nullify the Bg's so there is no chance for recycled bitmap to be used as background*/
        BitmapController.nullifyBackground();
        asyncTask = new initTask(getApplicationContext(), res, handler, activityRunnable).execute();
    }

    private void startAnimation(){
        final ImageView icon = findViewById(R.id.initializer_image);
        final TextView name = findViewById(R.id.initializer_name);
        final TextView tag = findViewById(R.id.initiazer_tag);
        Animation popUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.popup_with_effect);
        final Animation slideIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_left_slow);
        final Animation slideInLater = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_left_slow);
        slideInLater.setStartOffset(500);
        slideIn.setStartOffset(300);
        icon.setVisibility(View.INVISIBLE);
        name.setVisibility(View.INVISIBLE);
        tag.setVisibility(View.INVISIBLE);
        popUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                icon.setVisibility(View.VISIBLE);
                name.setVisibility(View.VISIBLE);
                tag.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        icon.startAnimation(popUp);
        name.startAnimation(slideIn);
        tag.startAnimation(slideInLater);
    }

    @Override
    public void onBackPressed() {
        try{
            handler.removeCallbacks(activityRunnable);
            asyncTask.cancel(true);
            BitmapController.recycleBitmap();
        } catch (Exception e){
            e.printStackTrace();
        }
        super.onBackPressed();
    }

    private static class initTask extends AsyncTask<Void, Void, Void>{

        private Resources res;
        private Context mContext;
        private Handler handler;
        private Runnable activityRunnable;

        initTask(Context context, Resources resources, Handler mHandler, Runnable runnable){
            this.res = resources;
            this.mContext = context;
            this.handler = mHandler;
            this.activityRunnable = runnable;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mContext != null) {
                int tNumber = mContext.getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
                BitmapController.setThemeNumber(tNumber);
                BitmapController.setNewBackground(mContext, res);

                Bitmap timerBitmap = BitmapController.initChronoBitmap(res, true);
                BitmapController.setChronoBitmap(timerBitmap, true);

                Bitmap stopwatchBitmap = BitmapController.initChronoBitmap(res, false);
                BitmapController.setChronoBitmap(stopwatchBitmap, false);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            handler.postDelayed(activityRunnable, 800);
        }
    }
}

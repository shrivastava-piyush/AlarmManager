package com.bytezap.wobble;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.bytezap.wobble.alarm.AlarmAssistant;
import com.bytezap.wobble.alarm.AlarmFragment;
import com.bytezap.wobble.alarm.media.MediaPickerActivity;
import com.bytezap.wobble.alarm.media.RingtonePickerActivity;
import com.bytezap.wobble.clock.ClockFragment;
import com.bytezap.wobble.clock.NightMode;
import com.bytezap.wobble.customviews.CustomPagerTransformer;
import com.bytezap.wobble.customviews.CustomViewPager;
import com.bytezap.wobble.customviews.PagerSlidingTabStrip;
import com.bytezap.wobble.preference.OtherSettingsActivity;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.stopwatch.StopwatchFragment;
import com.bytezap.wobble.theme.ThemeActivity;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.timer.TimerFragment;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.LicenceController;
import com.bytezap.wobble.utils.LinkDetector;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.TtsSpeaker;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Clock extends AppCompatActivity implements View.OnClickListener, PurchasesUpdatedListener {

    // Static values for fragments and activities
    public static final String ACTIVITY_BACKGROUND = "BackgroundNumber";
    public static final String IS_DIGITAL_COLORED = "nightColored";
    public static final String CHECKED_COUNT_ALARMS = "checkedAlarms";
    public static final String IS_ACTION_MODE = "actionMode";
    public static final String ACTIVITY_COLOR = "#2C363F";

    //preferences
    public static final String MAIN_PREF = "main_prefs";
    public static final String ALARM_PREFS = "alarm_prefs";
    public static final String TIMER_PREF = "timer_prefs";
    public static final String CLOCK_PREFS = "clock_prefs";
    public static final String STOPWATCH_PREFS = "stopwatch_prefs";
    public static final String THEME_PREFS = "theme_prefs";
    public static final String TIMER_TONE = "timerTone";

    //intent actions
    public static final String TAB_ALARM = "TAB_0";
    public static final String TAB_CLOCK = "TAB_1";
    public static final String TAB_TIMER = "TAB_2";
    public static final String TAB_STOPWATCH = "TAB_3";

    public static final String IS_FROM_INITIALIZER = "isInitializer";
    public static final String RECREATE = "activityRecreate";
    public static final String THEME_CHANGED = "themeChanged";
    public static final String RANDOM_THEME_CHANGED = "themeChangedRandom";
    public static final String ALARM_BG = "alarm_bg";
    public static final String ALARM_BG_RAND = "alarm_bg_rand";
    public static final String TTS_ERROR = "tts_error";
    public static final String THEME_BG_PATH = "theme_bg_path";
    public static final String CROP_IS_THEME = "is_theme";
    public static final String REFRESH_LIST = "refresh_alarm_list";
    public static final String THEME_NUMBER = "theme_number";

    // Related to MainActivity
    public static final String APP_URL = "market://details?id=com.bytezap.alarmclock";

    //Permissions
    public static final int REQUEST_PERMISSION_CAMERA = 7;
    public static final int REQUEST_PERMISSION_MICROPHONE = 8;
    public static final int REQUEST_PERMISSION_PHONE_STATE = 9;
    public static final int REQUEST_PERMISSION_STORAGE_DOC = 10;
    public static final int REQUEST_PERMISSION_STORAGE_MICRO = 11;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String CURRENT_PAGER_ITEM = "viewpagerItem";
    private static final String ITEM_SKU = "com.bytezap.ads";
    private static final String REMOVE_ADS = "remove_ads";
    private static final String TAG = Clock.class.getSimpleName();
    private static final String DO_NOT_SHOW = "dont_show_again";
    private static final String LAUNCH_COUNT = "launch_count";
    private static final int LAUNCHES_UNTIL_POPUP = 15;

    private CustomViewPager viewPager;
    private Window window;
    private SharedPreferences mainPrefs;
    private Toolbar toolbar;
    private Toolbar toolbarSettings;
    private boolean isFirstTime;
    private SharedPreferences settings;
    private BroadcastReceiver langReciever;
    boolean backToExit = false;

    // Purchase listeners
    /*final private IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener;
    final private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener;*/
    private BillingClient mBillingClient;

    public Clock() {
        /*mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {

                if (!result.isFailure() && info.getSku().equals(ITEM_SKU)) {
                    LicenceController.setCheckIAPLicense(true);
                    mainPrefs.edit()
                            .putString("modeEmancipated", LicenceController.getEmancipatedString())
                            .apply();
                } else {
                    Log.v(TAG, "purchase failed");
                }
            }
        };

        mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {

                if (!result.isFailure()) {
                    LicenceController.setCheckIAPLicense(inv.hasPurchase(ITEM_SKU));
                    if (isFirstTime && inv.hasPurchase(ITEM_SKU)) {
                        mainPrefs.edit()
                                .putString("modeEmancipated", "7bm7")
                                .apply();
                    }
                }
            }
        };*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        if (!TextUtils.isEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                case TAB_ALARM:
                    viewPager.setCurrentItem(0, true);
                    intent.setAction("");
                    break;
                case TAB_CLOCK:
                    viewPager.setCurrentItem(1, true);
                    intent.setAction("");
                    break;
                case TAB_TIMER:
                    viewPager.setCurrentItem(2, true);
                    intent.setAction("");
                    break;
                case TAB_STOPWATCH:
                    viewPager.setCurrentItem(3, true);
                    intent.setAction("");
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        BitmapController.setIsAnimation(settings.getBoolean(SettingsActivity.ANIMATION, true));
        boolean isInitializer = getIntent().getBooleanExtra(IS_FROM_INITIALIZER, false);
        if (savedInstanceState == null) {
            if (isInitializer) {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else if (BitmapController.isAnimation()){
                overridePendingTransition(R.anim.open_enter, R.anim.open_exit);
            }
        }

        window = getWindow();
        Resources res = getResources();

        String locale = settings.getString(SettingsActivity.LANGUAGE, "default");
        CommonUtils.setLangCode(locale);
        CommonUtils.setLanguage(res, locale);

        setContentView(R.layout.layout_main);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        toolbar = findViewById(R.id.toolbar_main);
        toolbarSettings = findViewById(R.id.toolbar_main_settings);

        viewPager = findViewById(R.id.viewpager);
        if (BitmapController.isAnimation()) {
            viewPager.setPageTransformer(true, new CustomPagerTransformer(Clock.this));
        }
        viewPager.setAdapter(new CustomPagerAdapter(Clock.this));
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAccessibilityDelegate(null);
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        if (CommonUtils.isMOrLater()) {
            viewPager.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                    if (toolbarSettings.getVisibility() == View.VISIBLE) {
                        toolbarSettings.setVisibility(View.GONE);
                        toolbar.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        PagerSlidingTabStrip tabs = findViewById(R.id.pager_tabs);
        tabs.setViewPager(viewPager);
        tabs.setTextColor(Color.parseColor("#70000000"));

        mainPrefs = getPrefs();

        //In-App Purchase
        String base64EncodedPublicKey =
                "<your license key here>"; //Todo: Enter license key here

        SharedPreferences sharedPreferences = getSharedPreferences(THEME_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        isFirstTime = sharedPreferences.getBoolean("isFirstTime", true);
        if (isFirstTime) {
            editor.putBoolean("isFirstTime", false);
            editor.putInt(ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
            editor.apply();
        }

        if (CommonUtils.is16OrLater()) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // Payment lifecycle starts here
        /*try {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult result) {

                    if (result.isSuccess()) {
                        Log.v(TAG, "Billing Setup is done");
                        Clock.this.mHelper.queryInventoryAsync(mReceivedInventoryListener);
                    } else if (result.isFailure()) {
                        Log.v(TAG, "Billing setup not done");
                    }
                }
            });
        } catch (Exception e) {
            Log.v(TAG, "IabHelper NullPointer Exception: " + e.toString());
        }*/

        try {
            mBillingClient = BillingClient.newBuilder(Clock.this).setListener(this).build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                    if (billingResponseCode == BillingClient.BillingResponse.OK) {
                        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
                            @Override
                            public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
                                // Process the result.
                                //Todo Keep a check for the change in function list param
                                if (responseCode == BillingClient.BillingResponse.OK
                                        && purchasesList != null) {
                                    for (Purchase purchase : purchasesList) {
                                        String sku = purchase.getSku();
                                        if (ITEM_SKU.equals(sku)) {
                                            LicenceController.setCheckIAPLicense(true);
                                            if (isFirstTime) {
                                                mainPrefs.edit()
                                                        .putString("modeEmancipated", "7bm7")
                                                        .apply();
                                            }
                                        } else {
                                            try{
                                                MobileAds.initialize(Clock.this, getString(R.string.banner_ad_id));
                                            } catch (Throwable b){
                                                b.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            });

        } catch (Exception e) {
            Log.v(TAG, "Billing Exception: " + e.toString());
        }

        //Set Current Item
        viewPager.setCurrentItem(sharedPreferences.getInt(CURRENT_PAGER_ITEM, 1));
        int tNumber = sharedPreferences.getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        if (tNumber == ThemeDetails.THEME_RANDOM) {
            int number;
            if (savedInstanceState != null) {
                number = savedInstanceState.getInt(THEME_NUMBER, 1);
            } else {
                number = new Random().nextInt(ThemeDetails.TOTAL_THEMES);
            }
            BitmapController.setThemeNumber(number);
        } else {
            BitmapController.setThemeNumber(tNumber);
        }
        //Set Background from SharedPreferences
        BitmapDrawable background = BitmapController.getCurrentBackground(res);
        if (background == null) {
            background = BitmapController.setNewBackground(getApplicationContext(), res);
        }
        if (background != null) {
            try {
                window.setBackgroundDrawable(null);
                window.setBackgroundDrawable(background);
            } catch (Throwable e) {
                Log.e(TAG, e.toString());
                window.setBackgroundDrawable(null);
                window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
            }
        } else {
            window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        //Listener for menu
        ImageButton menuButton = toolbar.findViewById(R.id.menu_button);
        ImageButton themeButton = toolbar.findViewById(R.id.theme_button);

        ImageButton infoButton = toolbarSettings.findViewById(R.id.settings_info);
        ImageButton adButton = toolbarSettings.findViewById(R.id.settings_ads);
        ImageButton nightButton = toolbarSettings.findViewById(R.id.settings_night_mode);
        ImageButton toneButton = toolbarSettings.findViewById(R.id.settings_tone);
        ImageButton mainButton = toolbarSettings.findViewById(R.id.settings_main);
        ImageButton closeButton = toolbarSettings.findViewById(R.id.settings_close);

        menuButton.setOnClickListener(this);
        themeButton.setOnClickListener(this);
        adButton.setOnClickListener(this);
        infoButton.setOnClickListener(this);
        nightButton.setOnClickListener(this);
        toneButton.setOnClickListener(this);
        mainButton.setOnClickListener(this);
        closeButton.setOnClickListener(this);

        /*if (CommonUtils.isLOrLater() && BitmapController.isAnimation()) {
            menuButton.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getApplicationContext(), R.drawable.menu_animator));
            themeButton.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getApplicationContext(), R.drawable.theme_animator));
        }*/

        // Need to update the alarms in case if another app or system kills this app
        updateSetAlarms(Clock.this, settings.getBoolean(SettingsActivity.VOICE, true));
    }

    private void updateSetAlarms(final AppCompatActivity activity, final boolean isVoice) {
        final AsyncTask<Void, Void, Void> checkTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AlarmAssistant.updateSetAlarms(getApplicationContext());
                if (isVoice) {
                    TtsSpeaker.initAndNotify(getApplicationContext());
                }
                return null;
            }
        };
        checkTask.execute();
    }

    private void showMenu(final ImageButton button) {
        /*ContextThemeWrapper wrapper = new ContextThemeWrapper(Clock.this, R.style.PopupMenuStyle);
        PopupMenu menu = new PopupMenu(wrapper, button);

        menu.inflate(R.menu.main_list);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.item_settings:
                        Intent intent = new Intent(Clock.this, SettingsActivity.class);
                        startActivity(intent);
                        break;

                    case R.id.item_ads:
                        if (LicenceController.checkLicense(getApplicationContext())) {
                            ToastGaffer.showToast(getApplicationContext(), getString(R.string.item_owned));
                        } else {
                            try {
                                mHelper.launchPurchaseFlow(Clock.this, ITEM_SKU, 10001,
                                        mPurchaseFinishedListener, REMOVE_ADS);
                            } catch (Throwable e) {
                                Log.v("purchaseListener", e.toString());
                                ToastGaffer.showToast(getApplicationContext(), getString(R.string.enable_google_play));
                            }
                        }
                        break;

                    case R.id.item_rate_app:
                        try {
                            Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(APP_URL));
                            startActivity(rateIntent);
                        } catch (Exception e) {
                            ToastGaffer.showToast(getApplicationContext(), getString(R.string.app_not_found));
                        }
                        break;

                    case R.id.item_choose_sound:
                        launchTimerTonePicker();
                        break;

                    case R.id.item_night_mode:
                        startActivity(new Intent(Clock.this, NightMode.class));
                        break;
                }
                return true;
            }
        });

        MenuItem item = menu.getMenu().findItem(R.id.item_choose_sound);
        item.setVisible(viewPager.getCurrentItem() == 2);
        item = menu.getMenu().findItem(R.id.item_night_mode);
        item.setVisible(viewPager.getCurrentItem() == 1);
        menu.show(); */

        final Toolbar toolbar = findViewById(R.id.toolbar_main);
        final Toolbar toolbarSettings = findViewById(R.id.toolbar_main_settings);
        toolbarSettings.setVisibility(View.INVISIBLE);
        int viewPagerItem = viewPager.getCurrentItem();
        toolbarSettings.findViewById(R.id.settings_night_mode).setVisibility(viewPagerItem == 1 ? View.VISIBLE : View.GONE);
        toolbarSettings.findViewById(R.id.settings_tone).setVisibility(viewPagerItem == 2 ? View.VISIBLE : View.GONE);

        Animation slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_bottom);
        final Animation slideUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toolbar.setVisibility(View.GONE);
                toolbarSettings.setVisibility(View.VISIBLE);
                toolbarSettings.startAnimation(slideUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        toolbar.startAnimation(slideDown);
    }

    private void launchTimerTonePicker() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(Clock.this, R.style.AlertDialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
        builder.setTitle(R.string.details_alarm_tone_default).setItems(R.array.timerRingtoneType, new selectTimerTone());

        AlertDialog picker = builder.create();
        DialogSupervisor.setDialog(picker);
        picker.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences alarmTone = getSharedPreferences(TIMER_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = alarmTone.edit();
        Uri uri;

        if (resultCode == RESULT_OK && (requestCode == 0 || requestCode == 1)) {
            uri = data.getData();
            if (uri != null) {
                editor.putString(TIMER_TONE, uri.toString());
                editor.apply();
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.tone_selected));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        SharedPreferences sharedPreferences = getSharedPreferences(THEME_PREFS, MODE_PRIVATE);
        int tNumber = sharedPreferences.getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT);
        if (tNumber == ThemeDetails.THEME_RANDOM) {
            outState.putInt(THEME_NUMBER, BitmapController.getThemeNumber());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        SharedPreferences setViewPager = getSharedPreferences(THEME_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = setViewPager.edit();
        editor.putInt(CURRENT_PAGER_ITEM, viewPager.getCurrentItem());
        editor.apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent() != null) {
            Intent resumeIntent = getIntent();
            if (!TextUtils.isEmpty(resumeIntent.getAction())) {
                switch (resumeIntent.getAction()) {
                    case TAB_ALARM:
                        viewPager.setCurrentItem(0, true);
                        resumeIntent.setAction("");
                        break;
                    case TAB_CLOCK:
                        viewPager.setCurrentItem(1, true);
                        resumeIntent.setAction("");
                        break;
                    case TAB_TIMER:
                        viewPager.setCurrentItem(2, true);
                        resumeIntent.setAction("");
                        break;
                    case TAB_STOPWATCH:
                        viewPager.setCurrentItem(3, true);
                        resumeIntent.setAction("");
                        break;
                }
            }
        }

        if (langReciever == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(RECREATE);
            filter.addAction(THEME_CHANGED);
            filter.addAction(RANDOM_THEME_CHANGED);
            filter.addAction(TTS_ERROR);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            registerReceiver(langReciever = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null) {
                        switch (action){
                            case RECREATE:
                            case THEME_CHANGED:
                                recreate();
                                break;

                            case RANDOM_THEME_CHANGED:
                                finish();
                                Intent restartClock = new Intent(Clock.this, Clock.class);
                                startActivity(restartClock);
                                overridePendingTransition(0,0);
                                break;

                            case Intent.ACTION_LOCALE_CHANGED:
                                if (CommonUtils.getLangCode().equals("default")) {
                                    CommonUtils.refreshDefaultLocale();
                                    recreate();
                                }
                                break;

                            case TTS_ERROR:
                                if (isFirstTime) {
                                    ToastGaffer.showToast(context, context.getString(R.string.tts_warning), true);
                                }
                                break;
                        }
                    }
                }
            }, filter);
        }

        if (settings.getBoolean(SettingsActivity.KEEP_SCREEN_ON, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu_button:
                showMenu((ImageButton) v);
                break;

            case R.id.theme_button:
                Intent themeIntent = new Intent(this, ThemeActivity.class);
                startActivity(themeIntent);
                break;

            case R.id.settings_main:
                Intent intent = new Intent(Clock.this, SettingsActivity.class);
                startActivity(intent);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toolbarSettings.setVisibility(View.GONE);
                        toolbar.setVisibility(View.VISIBLE);
                    }
                }, 2000);
                break;

            case R.id.settings_ads:
                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
                if (resultCode != ConnectionResult.SUCCESS) {
                    if (apiAvailability.isUserResolvableError(resultCode)) {
                        apiAvailability.getErrorDialog(this, resultCode,
                                PLAY_SERVICES_RESOLUTION_REQUEST).show();
                    } else {
                        ToastGaffer.showToast(getApplicationContext(), getString(R.string.enable_google_play));
                    }
                } else {
                    if (LicenceController.checkLicense(getApplicationContext())) {
                        ToastGaffer.showToast(getApplicationContext(), getString(R.string.item_owned));
                    } else {
                        /*try {
                            mHelper.launchPurchaseFlow(Clock.this, ITEM_SKU, 10001,
                                    mPurchaseFinishedListener, REMOVE_ADS);
                        } catch (Throwable e) {
                            Log.v("purchaseListener", e.toString());
                        }*/
                        try {
                            /*mHelper.launchSubscriptionPurchaseFlow(getActivity(), selectedItem, PLAY_SERVICES_RESOLUTION_REQUEST,
                                    mPurchaseFinishedListener, REMOVE_ADS);*/
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSku(ITEM_SKU)
                                    .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                                    .build();
                            int responseCode = mBillingClient.launchBillingFlow(Clock.this, flowParams);
                        } catch (Throwable e) {
                            Log.v("purchaseListener", e.toString());
                            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;

            case R.id.settings_tone:
                launchTimerTonePicker();
                break;

            case R.id.settings_night_mode:
                startActivity(new Intent(Clock.this, NightMode.class));
                toolbarSettings.setVisibility(View.GONE);
                toolbar.setVisibility(View.VISIBLE);
                break;

            case R.id.settings_info:
                if (BitmapController.isAnimation()) {
                    try{
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this);
                        ActivityCompat.startActivity(this, new Intent(getApplicationContext(), AboutActivity.class), options.toBundle());
                    } catch (Exception e){
                        Log.e(OtherSettingsActivity.SettingsFragment.class.getSimpleName(), "Could not start transition: " + e.getMessage());
                        startActivity(new Intent(this, AboutActivity.class));
                    }
                } else {
                    startActivity(new Intent(this, AboutActivity.class));
                }

                Handler infoHandler = new Handler();
                infoHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toolbarSettings.setVisibility(View.GONE);
                        toolbar.setVisibility(View.VISIBLE);
                    }
                }, 2000);
                break;

            case R.id.settings_close:
                final Toolbar toolbar = findViewById(R.id.toolbar_main);
                final Toolbar toolbarSettings = findViewById(R.id.toolbar_main_settings);

                Animation slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_bottom);
                final Animation slideUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
                slideDown.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        toolbarSettings.setVisibility(View.GONE);
                        toolbar.setVisibility(View.VISIBLE);
                        toolbar.startAnimation(slideUp);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                toolbarSettings.startAnimation(slideDown);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        //AppRater Dialog
        SharedPreferences prefs = getSharedPreferences(Clock.MAIN_PREF, 0);
        LinkDetector detector = new LinkDetector(getApplicationContext());
        if (!prefs.getBoolean(Clock.DO_NOT_SHOW, false) && detector.isNetworkAvailable()) {
            SharedPreferences.Editor editor = prefs.edit();
            int launch_count = prefs.getInt(Clock.LAUNCH_COUNT, 0) + 1;
            editor.putInt(Clock.LAUNCH_COUNT, launch_count).apply();
            if (launch_count >= LAUNCHES_UNTIL_POPUP) {
                showRateDialog(editor);
                return;
            }
        }

        boolean isCustom = CommonUtils.isCustomRom() || CommonUtils.isXiaomi();

        if (isFirstTime && isCustom) {
            ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.AlertDialogStyle);
            AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
            dialog.setTitle(R.string.autostart_title);
            dialog.setMessage(getString(R.string.autostart_alert));
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok_gotit), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent whitelistIntent = CommonUtils.prepareIntentForAutoStart(Clock.this);
                    if (whitelistIntent != null && CommonUtils.isMOrLater()) {
                        try {
                            startActivity(whitelistIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastGaffer.showToast(Clock.this, "There seems to be an error. Please try again later.");
                        }
                    }
                    Clock.super.onBackPressed();
                }
            });
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Clock.super.onBackPressed();
                }
            });
            DialogSupervisor.setDialog(dialog);
            dialog.show();
        } else {
            if (backToExit) {
                super.onBackPressed();
                return;
            }
            this.backToExit = true;
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.press_back));
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    backToExit = false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            //AppRater Dialog
            SharedPreferences prefs = getSharedPreferences(Clock.MAIN_PREF, 0);
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (!prefs.getBoolean(Clock.DO_NOT_SHOW, false) && detector.isNetworkAvailable()) {
                SharedPreferences.Editor editor = prefs.edit();
                int launch_count = prefs.getInt(Clock.LAUNCH_COUNT, 0) + 1;
                editor.putInt(Clock.LAUNCH_COUNT, launch_count).apply();
                if (launch_count >= LAUNCHES_UNTIL_POPUP) {
                    showRateDialog(editor);
                    return true;
                }
            }
            super.onBackPressed();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(R.anim.close_enter, R.anim.close_exit);
        }
    }

    @Override
    protected void onDestroy() {
        ToastGaffer.cancelPreviousToast();
        DialogSupervisor.cancelDialog();
        if (!isChangingConfigurations()) {
            LicenceController.setIsAdFirstTime(true);
            getWindow().setBackgroundDrawable(null);
            TtsSpeaker.shutDown(getApplicationContext());
        }
        if (langReciever != null) {
            unregisterReceiver(langReciever);
        }
        super.onDestroy();
        if (!isChangingConfigurations()) {
            BitmapController.recycleBitmap();
        }
    }

    private void showRateDialog(final SharedPreferences.Editor editor) {

        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ll = inflater.inflate(R.layout.app_rater_dialog, null);

        final AlertDialog appRater = new AlertDialog.Builder(Clock.this).create();
        appRater.setView(ll);

        Button rateButton = ll.findViewById(R.id.app_rate);
        rateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editor != null) {
                    editor.putBoolean(DO_NOT_SHOW, true).apply();
                }
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(APP_URL)));
                appRater.dismiss();
            }
        });

        Button noButton = ll.findViewById(R.id.app_rate_no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editor != null) {
                    editor.putBoolean(DO_NOT_SHOW, true).apply();
                    appRater.dismiss();
                    finish();
                }
            }
        });

        Button laterButton = ll.findViewById(R.id.app_rate_later);
        laterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editor != null) {
                    editor.clear().apply();
                    appRater.dismiss();
                    finish();
                }
            }
        });

        appRater.setCanceledOnTouchOutside(false);
        appRater.show();
    }

    private SharedPreferences getPrefs() {
        return mainPrefs = getSharedPreferences(MAIN_PREF, MODE_PRIVATE);
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<com.android.billingclient.api.Purchase> purchases) {
        if (purchases != null && responseCode == BillingClient.BillingResponse.OK) {
            if (purchases.get(0).getSku().equals(ITEM_SKU)) {
                LicenceController.setCheckIAPLicense(true);
                mainPrefs.edit()
                        .putString("modeEmancipated", LicenceController.getEmancipatedString())
                        .apply();

                if (toolbarSettings.getVisibility() == View.VISIBLE) {
                    toolbarSettings.setVisibility(View.GONE);
                    toolbar.setVisibility(View.VISIBLE);
                }
            } else {
                ToastGaffer.showToast(Clock.this, "Seems like an issue at our end. Could you please contact us via the \"About\" section?");
            }
        } else if (purchases != null && responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            ToastGaffer.showToast(Clock.this, "Seems like the process got interrupted. Could you please try again?");
        } else if (purchases != null && responseCode == BillingClient.BillingResponse.SERVICE_DISCONNECTED) {
            ToastGaffer.showToast(Clock.this, "Seems like an Internet issue. Could you please try again?");
        } else if (purchases != null && responseCode == BillingClient.BillingResponse.DEVELOPER_ERROR) {
            ToastGaffer.showToast(Clock.this, "Seems like an issue at our end. Could you please contact us via the \"About\" section?");
        } else {
            ToastGaffer.showToast(Clock.this, "Sorry, purchase failed. Could you please try again or contact the developer?");
            Log.v(TAG, "Purchase failed");
        }
    }

    // Select the Tone for timer
    private class selectTimerTone implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    try {
                        startActivityForResult(new Intent(Clock.this, RingtonePickerActivity.class), 0);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    return;
                case 1:
                    try {
                        startActivityForResult(new Intent(Clock.this, MediaPickerActivity.class), 1);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    break;

                default:
                    break;
            }
        }
    }

    public class CustomPagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTitleProvider {

        private final int[] ICONS = {R.drawable.tab_alarm, R.drawable.tab_clock,
                R.drawable.tab_timer, R.drawable.tab_stopwatch};
        private final String[] TITLE = getResources().getStringArray(R.array.pager_items);
        private SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public CustomPagerAdapter(AppCompatActivity activity) {
            super(activity.getSupportFragmentManager());
        }

        // Register the fragment when the item is instantiated
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = getRegisteredFragment(position);
            if (fragment == null) {
                switch (position) {
                    case 0:
                        fragment = new AlarmFragment();
                        break;
                    case 1:
                        fragment = new ClockFragment();
                        break;
                    case 2:
                        fragment = new TimerFragment();
                        break;
                    case 3:
                        fragment = new StopwatchFragment();
                        break;
                }
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return TITLE.length;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        // Returns the fragment for the position (if instantiated)
        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLE[position];
        }

        @Override
        public int getPageIconResId(int position) {
            return ICONS[position];
        }
    }

}

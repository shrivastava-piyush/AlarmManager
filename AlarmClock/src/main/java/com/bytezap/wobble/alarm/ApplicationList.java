/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.LicenceController;
import com.bytezap.wobble.utils.LinkDetector;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

public class ApplicationList extends AppCompatActivity {

    private CustomListView listView;
    private List<App> infoList = new ArrayList<>();
    private List<App> appsFiltered = new ArrayList<>();
    private PackageManager packageManager;
    private boolean isSearchOpened = false;
    private Drawable mIconCloseSearch, mIconOpenSearch;
    private TextView title;
    private ImageButton button;
    private EditText searchBar;
    private ProgressBar bar;
    private InputMethodManager iManager;
    private int orientation;

    private AdView adView;
    private final Handler adsHandler = new Handler();
    private final Runnable adsRunnable = new Runnable() {
        boolean isFirstTime = true;

        @Override
        public void run() {
            if (LicenceController.isIsAdFirstTime()) {
                if (isFirstTime) {
                    adView = findViewById(R.id.launch_adView);
                    adView.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            LicenceController.setIsAdFirstTime(false);
                            adView.setVisibility(View.VISIBLE);
                        }
                    });
                    isFirstTime = false;
                    adsHandler.postDelayed(adsRunnable, 300);
                } else {
                    adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                            .build());
                }
            } else {
                adView = findViewById(R.id.launch_adView);
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        adView.setVisibility(View.VISIBLE);
                    }
                });
                adView.loadAd(new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build());
            }
        }
    };

    private void createList(){
        final AsyncTask<Void, Void, List<App>> createListTask = new AsyncTask<Void, Void, List<App>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<App> doInBackground(Void... params) {
                List<ApplicationInfo> installedAppList = packageManager
                        .getInstalledApplications(PackageManager.GET_META_DATA);
                List<ApplicationInfo> intentFilterList = new ArrayList<>();
                for (ApplicationInfo info : installedAppList) {
                    // Only allow apps that have a launcher intent to be chosen
                    if (packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                        intentFilterList.add(info);
                    }
                }
                List<App> apps = new ArrayList<>();
                for (ApplicationInfo info : intentFilterList)
                {
                    App app = new App(info.loadIcon(packageManager), info.loadLabel(packageManager).toString(), info.packageName);
                    apps.add(app);
                }
                return apps;
            }

            @Override
            protected void onPostExecute(List<App> list) {
                infoList = list;
                bar.setVisibility(View.GONE);
                AppListAdapter adapter = new AppListAdapter(ApplicationList.this, list);
                listView.setAdapter(adapter);
                if (BitmapController.isAnimation()) {
                    AnimUtils.startListAnimation(getApplicationContext(), listView);
                }
            }
        };
        createListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BitmapController.isAnimation()) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.fade_out);
        }

        if (CommonUtils.isLOrLater()) {
            getWindow().setStatusBarColor(Color.parseColor("#80000000"));
        }

        Resources res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        setContentView(R.layout.application_layout);

        orientation = res.getConfiguration().orientation;
        setBackground(res.getConfiguration());

        Toolbar toolbar = findViewById(R.id.toolbar_launch);
        setSupportActionBar(toolbar);

        title = toolbar.findViewById(R.id.select_app_title);
        button = toolbar.findViewById(R.id.search_app);
        searchBar = toolbar.findViewById(R.id.search_app_bar);
        bar = findViewById(R.id.app_bar);
        iManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mIconCloseSearch = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_action_unchecked);
        mIconOpenSearch = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_action_search);

        packageManager = getPackageManager();
        listView = findViewById(R.id.launch_list);

        if (CommonUtils.isLOrLater()) {
            try {
                TypedValue outValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                button.setBackgroundResource(outValue.resourceId);
            } catch (Throwable b) {
                b.printStackTrace();
            }
        }

        createList();
        appsFiltered = infoList;

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App app;
                if (isSearchOpened) {
                    if (appsFiltered.isEmpty()) {
                        return;
                    }
                    app = appsFiltered.get(position);
                } else {
                    app = infoList.get(position);
                }
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AlarmDetails.LAUNCH_APP_NAME, app.getAppLabel());
                resultIntent.putExtra(AlarmDetails.LAUNCH_APP_PACKAGE, app.getAppPackageName());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSearchOpened) {
                    openSearchBar();
                } else {
                    searchBar.setText("");
                }
            }
        });

        searchBar.addTextChangedListener(new SearchWatcher());

        //Ads
        if (!LicenceController.checkLicense(getApplicationContext())) {
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (detector.isNetworkAvailable()) {
                adsHandler.post(adsRunnable);
            }
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (orientation!= newConfig.orientation) { //Checking for orientation change to avoid unnecessary calls to setBackground
            setBackground(newConfig);
            orientation = newConfig.orientation;
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.slide_out_left);
        }
    }

    @Override
    protected void onDestroy() {
        if (adView!=null) {
            adView.destroy();
        }
        adsHandler.removeCallbacks(adsRunnable);
        super.onDestroy();
    }

    private void setBackground(Configuration newConfig){
        try {
            int tNumber = getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, 0);
            getWindow().setBackgroundDrawable(BitmapController.getDetailsBitmap(newConfig, getResources(), tNumber));
        } catch (NullPointerException e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            if (isSearchOpened) {
                closeSearchBar();
            } else {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSearchBar() {

        // Set custom view on action bar
        title.setVisibility(View.INVISIBLE);
        searchBar.setVisibility(View.VISIBLE);
        searchBar.requestFocus();
        iManager.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT);

        button.setImageDrawable(mIconCloseSearch);
        isSearchOpened = true;
    }

    private void closeSearchBar() {
        // Remove custom view
        title.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.INVISIBLE);

        button.setImageDrawable(mIconOpenSearch);
        iManager.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
        getListAdapter().updateList(infoList);

        isSearchOpened = false;
    }

    private AppListAdapter getListAdapter() {
        return (AppListAdapter) listView.getAdapter();
    }

    private ArrayList<App> performSearch(String query) {

        // First we split the query so that we're able
        // to search word by word (in lower case)
        String[] queryByWords = query.toLowerCase().split("\\s+");

        // Empty list to fill with matches
        ArrayList<App> appFiltered = new ArrayList<>();

        // Go through initial releases and perform search
        for (App app : infoList) {

            // Content to search through (in lower case)
            String content = (
                    app.getAppLabel()
            ).toLowerCase();

            for (String word : queryByWords) {

                // There is a match only if all of the words are contained
                int numberOfMatches = queryByWords.length;

                // All query words have to be contained,
                // otherwise the release is filtered out
                if (content.contains(word)) {
                    numberOfMatches--;
                } else {
                    break;
                }

                // They all match
                if (numberOfMatches == 0) {
                    appFiltered.add(app);
                }

            }

        }

        return appFiltered;
    }

    private class SearchWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence c, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            appsFiltered = performSearch(s.toString());
            getListAdapter().updateList(appsFiltered);
        }

    }
}

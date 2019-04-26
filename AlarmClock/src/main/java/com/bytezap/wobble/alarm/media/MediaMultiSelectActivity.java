/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm.media;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.alarm.AlarmDetails;
import com.bytezap.wobble.customviews.listview.CustomListView;
import com.bytezap.wobble.database.MediaProvider;
import com.bytezap.wobble.utils.AnimUtils;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.LicenceController;
import com.bytezap.wobble.utils.LinkDetector;
import com.bytezap.wobble.utils.ToastGaffer;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

public class MediaMultiSelectActivity extends AppCompatActivity {

    private final static String TAG = MediaMultiSelectActivity.class.getSimpleName();
    private final static String projection[] = {MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            BaseColumns._ID,};
    private final static String sortOrder = MediaStore.Audio.Media.TITLE + " ASC " + ", " + BaseColumns._ID + " DESC";
    private boolean isSearchOpened = false;
    private Drawable mIconCloseSearch;
    private Drawable mIconOpenSearch;
    private MediaAdapter mMediaAdapter;
    private EditText searchBar;
    private TextView title;
    private ImageButton button;
    private InputMethodManager iManager;
    private ArrayList<Long> longList = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private AdView adView;
    private final Handler adsHandler = new Handler();
    private final Runnable adsRunnable = new Runnable() {
        boolean isFirstTime = true;

        @Override
        public void run() {
            if (LicenceController.isIsAdFirstTime()) {
                if (isFirstTime) {
                    adView = findViewById(R.id.multi_media_adView);
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
                adView = findViewById(R.id.multi_media_adView);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getResources();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.fade_out);
        }

        CommonUtils.setLanguage(res, CommonUtils.getLangCode());
        try {
            int tNumber = getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE).getInt(Clock.ACTIVITY_BACKGROUND, 0);
            getWindow().setBackgroundDrawable(BitmapController.getDetailsBitmap(getResources(), tNumber));
        } catch (NullPointerException e) {
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        if (CommonUtils.is16OrLater()) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.multi_media_layout);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        Toolbar toolbar = findViewById(R.id.toolbar_multi_media);
        setSupportActionBar(toolbar);

        iManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        searchBar = findViewById(R.id.search_multi_media_bar);
        title = toolbar.findViewById(R.id.select_multi_media_title);
        button = toolbar.findViewById(R.id.search_multi_media);
        final TextView selectView = findViewById(R.id.media_selection);

        if (CommonUtils.isLOrLater()) {
            try {
                TypedValue outValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                button.setBackgroundResource(outValue.resourceId);
            } catch (Throwable b) {
                b.printStackTrace();
            }
        }

        mIconCloseSearch = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_action_unchecked);
        mIconOpenSearch = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_action_search);

        final CustomListView listView = findViewById(R.id.multi_media_list);
        Cursor mCursor = MediaProvider.queryMedia(getApplicationContext(), MediaProvider.mediaUri, projection, MediaStore.Audio.Media.IS_MUSIC + "!= 0", null, sortOrder);
        if (null == mCursor) {
            return;
        }

        mMediaAdapter = new MediaAdapter(this,
                mCursor,
                new String[]{},
                new int[]{},
                0);

        if (savedInstanceState == null) {
            long[] ids = getIntent().getLongArrayExtra(AlarmDetails.SONG_IDS);
            if (ids!=null) {
                arrayToList(longList, ids);
            }
        } else {
            arrayToList(longList, savedInstanceState.getLongArray(AlarmDetails.SONG_IDS));
        }

        selectView.setText(longList.size() + getString(R.string.alarm_selected));
        listView.setAdapter(mMediaAdapter);
        LinearLayout emptyView = findViewById(R.id.multi_media_emptyView);
        listView.setEmptyView(emptyView);
        listView.setFastScrollEnabled(true);
        listView.setTextFilterEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Cursor cursor = mMediaAdapter.getCursor();
                    if (cursor != null) {
                        cursor.moveToPosition(position);
                        int size = longList.size();
                        Long mediaId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                        if (longList.contains(mediaId)) {
                            longList.remove(mediaId);
                            size--;
                        } else {
                            if (size < 20) {
                                longList.add(mediaId);
                                size++;
                            } else {
                                ToastGaffer.showToast(getApplicationContext(), getString(R.string.song_limit));
                            }
                            mMediaAdapter.notifyDataSetChanged();
                        }
                        selectView.setText(size + getString(R.string.alarm_selected));
                        mMediaAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.invalid_file));
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mMediaAdapter.getCursor();
                if (cursor != null) {
                    cursor.moveToPosition(position);
                    final long mediaId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                    startPlaying(ContentUris.withAppendedId(MediaProvider.mediaUri, mediaId));
                    return true;
                }
                return false;
            }
        });

        mMediaAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                if (constraint != null) {
                    return MediaProvider.queryMediaByTitle(getApplicationContext(), projection, constraint.toString(), sortOrder);
                }
                return null;
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

        if (BitmapController.isAnimation()) {
            AnimUtils.startListAnimation(getApplicationContext(), listView);
        }

        //Ads
        if (!LicenceController.checkLicense(getApplicationContext())) {
            LinkDetector detector = new LinkDetector(getApplicationContext());
            if (detector.isNetworkAvailable()) {
                adsHandler.post(adsRunnable);
            }
        }

        SharedPreferences preferences = getSharedPreferences(Clock.ALARM_PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean("isMusicFirstTime", true)) {
            preferences.edit().putBoolean("isMusicFirstTime", false).apply();
            adsHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CommonUtils.makeWhiteSnackBar(listView, getString(R.string.song_instruction));
                }
            }, 3000);
        }
    }

    private void startPlaying(Uri uri){

        if (uri == null) {
            return;
        }

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset();
        }

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaPlayer.reset();
                mediaPlayer.release();
                return true;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });

        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setLooping(false);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ex) {
            try {
                // Reset the media player to clear error state
                mediaPlayer.reset();
            } catch (Exception e) {
                // At this point just don't play anything
            }
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                try {
                    mediaPlayer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLongArray(AlarmDetails.SONG_IDS, listToArray());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        stopPlaying();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    private void openSearchBar() {

        title.setVisibility(View.INVISIBLE);
        searchBar.setVisibility(View.VISIBLE);
        button.setImageDrawable(mIconCloseSearch);
        searchBar.requestFocus();
        iManager.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT);

        isSearchOpened = true;
    }

    private void closeSearchBar() {
        // Remove custom view
        title.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.INVISIBLE);

        button.setImageDrawable(mIconOpenSearch);
        iManager.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);

        if (!TextUtils.isEmpty(searchBar.getText())) {
            searchBar.setText("");
        }
        isSearchOpened = false;
    }

    private long[] listToArray(){
        long[] array = new long[20];
        for (int i = 0; i< longList.size(); i++) {
            array[i] = longList.get(i);
        }
        return array;
    }

    private void arrayToList(ArrayList<Long> list, long[] ids){
        for (int i = 0; i< ids.length; i++) {
            if (ids[i] > 0) {
                list.add(i, ids[i]);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            if (isSearchOpened) {
                closeSearchBar();
            } else {
                int size = longList.size();
                if (size > 1) {
                    setResult(RESULT_OK, new Intent().putExtra(AlarmDetails.SONG_IDS, listToArray()));
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.songs_selected, size));
                } else if (size > 0) {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.more_songs_needed));
                }
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        int size = longList.size();
        if (size > 1) {
            setResult(RESULT_OK, new Intent().putExtra(AlarmDetails.SONG_IDS, listToArray()));
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.songs_selected, size));
        } else if(size > 0) {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.more_songs_needed));
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (adView!=null) {
            adView.destroy();
        }
        adsHandler.removeCallbacks(adsRunnable);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.slide_out_left);
        }
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
            mMediaAdapter.getFilter().filter(s.toString());
            mMediaAdapter.notifyDataSetChanged();
        }

    }

    private class MediaAdapter extends SimpleCursorAdapter implements Filterable {

        private int songId;
        private int titleId;
        private int artistId;
        private MediaHolder holder;

        public MediaAdapter(Context context, Cursor cursor, String[] from, int[] to, int flags) {
            super(context, R.layout.multi_media_item, cursor, from, to, flags);

            songId = cursor.getColumnIndex(BaseColumns._ID);
            titleId = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            artistId = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            setHolder(view);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Object tag = view.getTag();
            if (tag != null) {
                holder = (MediaHolder) tag;
            } else {
                setHolder(view);
            }

            String string = cursor.getString(titleId);
            if (string == null) {
                string = getString(R.string.unknown);
            }
            holder.title.setText(string);
            string = cursor.getString(artistId);
            if (string == null) {
                string = getString(R.string.unknown);
            }
            holder.artist.setText(string);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);
            ImageView box = convertView.findViewById(R.id.mediaIndicator);
            Long mediaId = getCursor().getLong(songId);
            box.setVisibility(longList.contains(mediaId) ? View.VISIBLE : View.INVISIBLE);
            return convertView;
        }

        private void setHolder(View convertView) {
            holder = new MediaHolder();
            holder.title = convertView.findViewById(R.id.mediaTitle);
            holder.artist = convertView.findViewById(R.id.mediaArtist);
        }

        private class MediaHolder {
            private TextView title;
            private TextView artist;
        }
    }
}

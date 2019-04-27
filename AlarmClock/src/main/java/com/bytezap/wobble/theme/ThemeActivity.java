package com.bytezap.wobble.theme;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.CropActivity;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.customviews.gridview.CustomGridView;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.Blur;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.GestureObserver;
import com.bytezap.wobble.utils.ToastGaffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ThemeActivity extends AppCompatActivity implements View.OnClickListener, GestureObserver.SimpleGestureListener {

    private static final String THEME_ID = "themeId";
    private static final String PATH = "wallpaperPath";
    private String[] mImageNames;
    private final String[] mImagesPath = {
            "theme/bg0.jpg", "theme/bg1.jpg", "theme/bg2.jpg", "theme/bg3.jpg", "theme/bg4.jpg", "theme/bg5.jpg", "theme/bg6.jpg",
            "theme/bg7.jpg", "theme/bg8.jpg", "theme/bg9.jpg", "theme/bg10.jpg", "theme/bg11.jpg"
    };
    private int themeId = 0;
    private SharedPreferences preferences;
    private BitmapDrawable background;
    private ImageAdapter adapter;
    private List<Bitmap> list = new ArrayList<>(mImagesPath.length);
    private CustomGridView gridView;
    private Resources res;
    private TextView themeName;
    private ImageButton themeOptions;
    private String path = "Null";
    private boolean hasChangedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        res = getResources();
        CommonUtils.setLanguage(res, CommonUtils.getLangCode());

        if (BitmapController.isAnimation()) {
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out);
        }

        setContentView(R.layout.theme_changer);

        Toolbar toolbar = findViewById(R.id.theme_toolbar);
        setSupportActionBar(toolbar);

        LinearLayout setTheme = findViewById(R.id.theme_set);
        LinearLayout themeViewer = findViewById(R.id.theme_viewer);
        themeName = findViewById(R.id.theme_name);
        themeOptions = findViewById(R.id.theme_options);

        if (CommonUtils.isLOrLater()) {
            try {
                TypedValue outValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                themeOptions.setBackgroundResource(outValue.resourceId);
            } catch (Throwable b) {
                b.printStackTrace();
            }
        }

        gridView = findViewById(R.id.theme_grid);
        if (!BitmapController.isThemeListEmpty()) {
            list = BitmapController.getThemeList();
            adapter = new ImageAdapter(getApplicationContext(), list);
            gridView.setAdapter(adapter);
            if (BitmapController.isAnimation()) {
                startRefreshAnimation();
            }
        } else {
            BitmapWorkerTask workerTask = new BitmapWorkerTask();
            workerTask.execute();
        }

        mImageNames = res.getStringArray(R.array.themeNames);

        preferences = getSharedPreferences(Clock.THEME_PREFS, MODE_PRIVATE);

        if (savedInstanceState == null) {
            themeId = preferences.getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_BLUE_NIGHT);
            background = BitmapController.getCurrentBackground(res);
            if (background == null) {
                // This might happen if the user rotates the app while onCreate is being executed
                background = BitmapController.setNewBackground(getApplicationContext(), res);
            }
            themeName.setText(mImageNames[themeId]);
        } else {
            themeId = savedInstanceState.getInt(THEME_ID);
            path = savedInstanceState.getString(PATH, "Null");
            if (themeId == ThemeDetails.THEME_CUSTOM && path.equals("Null")) {
                background = BitmapController.getCurrentBackground(res);
                if (background == null || background.getBitmap().isRecycled()) {
                    background = BitmapController.setNewBackground(getApplicationContext(), res);
                }
                themeName.setText(themeId != ThemeDetails.THEME_CUSTOM ? mImageNames[themeId] : getString(R.string.default_wallpaper));
            } else if (themeId == ThemeDetails.THEME_CUSTOM) {
                try {
                    background = BitmapController.getCustomDrawable(path, res);
                } catch (Exception e) {
                    background = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.bg0));
                }
                themeName.setText(R.string.default_wallpaper);
            } else if (themeId < ThemeDetails.TOTAL_THEMES) {
                background = BitmapController.getAssetDrawable(res, themeId);
                themeName.setText(mImageNames[themeId]);
            }
        }

        final Window window = getWindow();
        try{
            window.setBackgroundDrawable(null);
            window.setBackgroundDrawable(background);
        } catch (Throwable e){
            window.setBackgroundDrawable(null);
            window.getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        setTheme.setOnClickListener(this);
        themeOptions.setOnClickListener(this);

        /*if (CommonUtils.isLOrLater() && BitmapController.isAnimation()) {
            themeOptions.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getApplicationContext(), R.drawable.menu_animator));
        }*/

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
                if (themeId == position) {
                    return;
                }
                switchTheme(position);
            }
        });

        if (BitmapController.isAnimation()) {
            final GestureObserver detector = new GestureObserver(ThemeActivity.this, this);
            themeViewer.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    detector.onTouchEvent(event);
                    return true;
                }
            });
        }
    }

    private void startRefreshAnimation() {

        if (CommonUtils.is16OrLater()) {
            gridView.setHasTransientState(true);
        }
        AnimationSet set = new AnimationSet(true);

        Animation animation = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f);
        animation.setDuration(400);
        set.addAnimation(animation);

        animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(400);
        set.addAnimation(animation);

        LayoutAnimationController controller =
                new LayoutAnimationController(set, 0.35f);
        gridView.setLayoutAnimation(controller);
        gridView.setLayoutAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (CommonUtils.is16OrLater()) {
                    gridView.setHasTransientState(false);
                    gridView.setLayoutAnimationListener(null);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        gridView.startLayoutAnimation();
    }

    private void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(THEME_ID, themeId);
        outState.putString(PATH, path);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    @Override
    public void onSwipe(int direction) {
        switch (direction) {
            case GestureObserver.SWIPE_RIGHT:
                swipeTheme(true);
                break;
            case GestureObserver.SWIPE_LEFT:
                swipeTheme(false);
                break;
        }
    }

    @Override
    public void onDoubleTap() {
        if (themeId >= 0 && themeId <= ThemeDetails.TOTAL_THEMES) {
            preferences.edit().putInt(Clock.ACTIVITY_BACKGROUND, themeId).apply();
            BitmapController.setBackground(background, res, true);
            sendThemeBroadcast();
            finish();
        }
    }

    @Override
    public void onLongPress() {

    }

    private void swipeTheme(final boolean isRight) {
        final AsyncTask<Void, Void, Void>  swipeTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (isRight) {
                    themeId++;
                    if (themeId > ThemeDetails.THEME_THISTLE_PURPLE) {
                        themeId = ThemeDetails.THEME_BLUE_NIGHT;
                    }
                } else {
                    themeId--;
                    if (themeId < ThemeDetails.THEME_RAINY_DAY) {
                        themeId = ThemeDetails.THEME_THISTLE_PURPLE;
                    }
                }
                BitmapController.setThemeNumber(themeId);
                background = BitmapController.getAssetDrawable(res, themeId);
                if (background != null) {
                    int blurLevel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.BLUR_BG, 0);
                    if (blurLevel != 0) {
                        background = new BitmapDrawable(res, Blur.apply(getApplicationContext(), background.getBitmap(), blurLevel));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                themeName.setText(mImageNames[themeId]);
                getWindow().setBackgroundDrawable(background);
                adapter.notifyDataSetChanged();
            }
        };
        swipeTask.execute();
    }

    private void switchTheme(final int position) {
        final AsyncTask<Void, Void, Void>  swipeTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (background!=null && hasChangedOnce) {
                    hasChangedOnce = true;
                    background.getBitmap().recycle();
                }
                themeId = position;
                BitmapController.setThemeNumber(themeId);
                background = BitmapController.getAssetDrawable(res, themeId);
                if (background != null) {
                    int blurLevel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.BLUR_BG, 0);
                    if (blurLevel != 0) {
                        background = new BitmapDrawable(res, Blur.apply(getApplicationContext(), background.getBitmap(), blurLevel));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                themeName.setText(mImageNames[themeId]);
                if (background != null) {
                    getWindow().setBackgroundDrawable(background);
                }
                adapter.notifyDataSetChanged();
            }
        };
        swipeTask.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.theme_set:
                if (themeId >= 0 && (themeId <= ThemeDetails.TOTAL_THEMES + 1)) {
                    if (themeId == preferences.getInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_DEFAULT) && themeId != ThemeDetails.THEME_CUSTOM) {
                        finish();
                        return;
                    }
                    BitmapController.setThemeNumber(themeId);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(Clock.ACTIVITY_BACKGROUND, themeId);
                    if (themeId == ThemeDetails.THEME_CUSTOM) {
                        editor.putString(Clock.THEME_BG_PATH, path);
                    }
                    editor.apply();
                    BitmapController.setBackground(background, res, true);
                    sendThemeBroadcast();
                    finish();
                }
                break;

            case R.id.theme_options:
                showThemeMenu(themeOptions);
                break;
        }
    }

    // Send broadcast to the activity
    private void sendThemeBroadcast(){
        Intent intent = new Intent();
        intent.setAction(Clock.THEME_CHANGED);
        sendBroadcast(intent);
    }

    // Send broadcast to the activity
    private void sendRandomThemeBroadcast(){
        Intent intent = new Intent();
        intent.setAction(Clock.RANDOM_THEME_CHANGED);
        sendBroadcast(intent);
    }

    private void showThemeMenu(ImageButton button) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(ThemeActivity.this, R.style.PopupMenuStyle);
        PopupMenu menu = new PopupMenu(wrapper, button);

        menu.inflate(R.menu.theme_list);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.theme_random:
                        if (themeId >= 0 && themeId <= ThemeDetails.TOTAL_THEMES + 1) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    preferences.edit().putInt(Clock.ACTIVITY_BACKGROUND, ThemeDetails.THEME_RANDOM).apply();
                                    themeId = new Random().nextInt(ThemeDetails.TOTAL_THEMES);
                                    BitmapController.setThemeNumber(themeId);
                                    background = BitmapController.getAssetDrawable(res, themeId);
                                    if (background != null) {
                                        int blurLevel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.BLUR_BG, 0);
                                        if (blurLevel != 0) {
                                            background = new BitmapDrawable(res, Blur.apply(getApplicationContext(), background.getBitmap(), blurLevel));
                                        }
                                    }
                                    BitmapController.setBackground(background, res, true);
                                    sendRandomThemeBroadcast();
                                    finish();
                                }
                            });
                        }
                        break;

                    case R.id.theme_custom:
                        if (CommonUtils.isMOrLater()) {
                            // Reinforce the permission check here
                            if (ContextCompat.checkSelfPermission(ThemeActivity.this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {

                                ActivityCompat.requestPermissions(ThemeActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Clock.REQUEST_PERMISSION_STORAGE_DOC);
                            } else {
                                accessStorage();
                            }
                        } else {
                            accessStorage();
                        }
                        break;
                }
                return true;
            }
        });

        menu.show();
    }

    private void accessStorage() {
        Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        imageIntent.setType("image/*");
        imageIntent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(imageIntent, 0);
        } catch (Exception e) {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.no_file_manager));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 0 && data != null) {
            Uri uri = data.getData();
            String s = null;
            if (uri != null) {
                s = getContentResolver().getType(uri);
            }
            if (s != null) {
                if (s.contains("image")) {
                    Intent cropIntent = new Intent(getApplicationContext(), CropActivity.class);
                    cropIntent.setData(data.getData());
                    cropIntent.putExtra(Clock.CROP_IS_THEME, true);
                    startActivityForResult(cropIntent, 1);
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.wrong_file_type));
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == 1 && data != null) {
            path = data.getStringExtra(Clock.THEME_BG_PATH);
            if (new File(path).exists()) {
                themeId = ThemeDetails.THEME_CUSTOM;
                try {
                    background = BitmapController.getCustomDrawable(path, res);
                    if (background != null) {
                        int blurLevel = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.BLUR_BG, 0);
                        if (blurLevel != 0) {
                            background = new BitmapDrawable(res, Blur.apply(getApplicationContext(), background.getBitmap(), blurLevel));
                        }
                        getWindow().setBackgroundDrawable(background);
                    }
                } catch (Exception e) {
                    getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
                }
                themeName.setText(R.string.default_wallpaper);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } else {
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.invalid_file));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Clock.REQUEST_PERMISSION_STORAGE_DOC:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessStorage();
                } else {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
                }
                break;
        }
    }

    private void deleteTempFile() {
        File tempFile = new File(path);
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    private static class ImageHolder {
        @SuppressWarnings("unchecked")
        private static <T extends View> T get(View view, int id) {
            SparseArray<View> viewHolder;
            viewHolder = (SparseArray<View>) view.getTag();
            if (viewHolder == null) {
                viewHolder = new SparseArray<>();
                view.setTag(viewHolder);
            }
            View childView = viewHolder.get(id);
            if (childView == null) {
                childView = view.findViewById(id);
                viewHolder.put(id, childView);
            }
            return (T) childView;
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            findViewById(R.id.theme_bar).setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            BitmapController.initList(mImagesPath.length);
            boolean isTablet = res.getBoolean(R.bool.config_isTablet);
            AssetManager manager = res.getAssets();
            for (String mImageId : mImagesPath) {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapController.getThemeThumbnail(manager, mImageId, isTablet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bitmap != null) {
                    list.add(bitmap);
                    BitmapController.addObjectToList(bitmap);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            findViewById(R.id.theme_bar).setVisibility(View.GONE);
            adapter = new ImageAdapter(getApplicationContext(), list);
            gridView.setAdapter(adapter);
            if (BitmapController.isAnimation()) {
                startRefreshAnimation();
            }
        }
    }

    private class ImageAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private List<Bitmap> objectList;

        public ImageAdapter(Context context, List<Bitmap> list) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.objectList = list;
        }

        @Override
        public int getCount() {
            return objectList.size();
        }

        @Override
        public Object getItem(int position) {
            if (objectList != null) {
                return objectList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView image;
            FrameLayout highLight;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.theme_list_item, parent, false);
            }

            image = ImageHolder.get(convertView, R.id.theme_image);
            highLight = ImageHolder.get(convertView, R.id.theme_item_selected);

            Bitmap themeBitmap = (Bitmap) getItem(position);
            if (themeBitmap!=null) {
                try{
                    image.setImageBitmap(themeBitmap);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            setItemHighlighted(themeId == position, highLight);
            return convertView;
        }

        private void setItemHighlighted(boolean selected, View border) {
            border.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }

    }
}

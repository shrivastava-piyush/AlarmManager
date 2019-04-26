/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.customviews.CustomViewPager;
import com.bytezap.wobble.customviews.PagerSlidingTabStrip;
import com.bytezap.wobble.customviews.listview.AlarmListView;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.DataManager;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.SwipeDismissListViewTouchListener;
import com.bytezap.wobble.utils.ToastGaffer;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class AlarmFragment extends android.support.v4.app.Fragment implements AlarmInterface, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = AlarmFragment.class.getSimpleName();
    private static final String ALARM_COUNT = "SelectedCount";
    private static final int NEW_ALARM = 1;
    private static final int OLD_ALARM = 2;
    private AlarmAdapter mAdapter;
    private Context mContext;
    private DataManager dbManager;
    private AlarmListView listView;
    private boolean isInActionMode = false;
    private ActionMode actionMode;
    private AlarmObject activeAlarm = null;
    private List<AlarmObject> deletedAlarms = new LinkedList<>();
    private Snackbar snackbar;
    private int alarmDesc = 0;
    private BroadcastReceiver listReceiver;
    public AlarmFragment() {
        //Empty Constructor
    }

    private boolean areAllItemsNotVisible(SparseBooleanArray array) {
        int count = 0;
        for (int i = 0; i <= (array.size() - 1); i++) {
            if ((array.keyAt(i) > listView.getLastVisiblePosition())) {
                count++;
            }
        }
        return count == array.size();
    }

    private void showSnackBar(int size) {
        snackbar = Snackbar.make(getActivity().findViewById(R.id.alarm_coordinator), CommonUtils.getAlarmDeletionMessage(mContext, size), Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(Color.parseColor("#90000000"));
        TextView snackText = sbView.findViewById(android.support.design.R.id.snackbar_action);
        if (snackText!=null) {
            snackText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_action_undo, 0, 0, 0);
        }
        snackbar.setAction(R.string.default_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAlarm();
            }
        });
        snackbar.show();
    }

    private void addAlarm() {
        final AsyncTask<Void, Void, Void> alarmTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                AlarmAssistant.cancelAlarms(mContext, dbManager);
                for (int i = 0; i < deletedAlarms.size(); i++) {
                    AlarmObject alarm = deletedAlarms.get(i);
                    alarm.id = dbManager.createAlarm(alarm);
                    if (alarm.isEnabled) {
                        dbManager.createInstance(alarm);
                    }
                }
                AlarmAssistant.checkAndSetAlarms(mContext, dbManager);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                List<AlarmObject> alarms = dbManager.getAlarms();
                mAdapter.setAlarms(alarms);
                if (BitmapController.isAnimation()) {
                    startRefreshAnimation(deletedAlarms.size() == alarms.size());
                }
                deletedAlarms = new LinkedList<>();
            }
        };
        alarmTask.execute();
    }

    private void startListAnimation(final FloatingActionButton addAlarm) {
        if (CommonUtils.is16OrLater()) {
            listView.setHasTransientState(true);
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.zoom_with_effect);
        LayoutAnimationController controller =
                new LayoutAnimationController(animation, 0.9f);
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
                addAlarm.show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        listView.startLayoutAnimation();
    }

    private void startRefreshAnimation(boolean areAllDeleted) {

        if (CommonUtils.is16OrLater()) {
            listView.setHasTransientState(true);
        }

        LayoutAnimationController controller;
        if (areAllDeleted) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in_later);
            controller = new LayoutAnimationController(animation, 1);
        } else {
            AnimationSet set = new AnimationSet(true);

            Animation animation = new ScaleAnimation(1.1f, 1.0f, 0.95f, 1.0f);
            animation.setDuration(400);
            set.addAnimation(animation);

            animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(400);
            set.addAnimation(animation);

            controller = new LayoutAnimationController(set, 0.75f);
            controller.setOrder(LayoutAnimationController.ORDER_REVERSE);
        }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.alarm_fragment, container, false);

        mContext = getActivity().getApplicationContext();
        listView = rootView.findViewById(R.id.alarmList);

        new loadAlarms(savedInstanceState == null, rootView).execute();

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setupAlarmDetails(id, false);
            }
        });

        return rootView;
    }

    private class loadAlarms extends AsyncTask<Void, Void, Void> {

        private List<AlarmObject> alarmList;
        private final View rootView;
        private boolean isNotRotated;
        private FloatingActionButton addAlram;

        loadAlarms(boolean isNotRotated, View rootView) {
            this.isNotRotated = isNotRotated;
            this.rootView = rootView;
            this.addAlram = rootView.findViewById(R.id.alarm_add_new);
        }

        @Override
        protected void onPreExecute() {
            final CustomViewPager pager = getActivity().findViewById(R.id.viewpager);
            if (BitmapController.isAnimation() && isNotRotated && pager.getCurrentItem() == 0) {
                addAlram.hide();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mContext != null) {
                dbManager = DataManager.getInstance(mContext);
            }
            if (dbManager != null) {
                alarmList = dbManager.getAlarms();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter = new AlarmAdapter(mContext, ThemeDetails.isAlarmBgWhite(BitmapController.getThemeNumber()), AlarmFragment.this, alarmList);
            listView.setEmptyView(rootView.findViewById(R.id.emptyView));
            listView.setAdapter(mAdapter);
            try {
                final CustomViewPager pager = getActivity().findViewById(R.id.viewpager);
                if (pager != null && BitmapController.isAnimation() && isNotRotated && pager.getCurrentItem() == 0) {
                    startListAnimation(addAlram);
                }
            } catch (Throwable e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final FloatingActionButton addAlarm = getActivity().findViewById(R.id.alarm_add_new);
        addAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupAlarmDetails(-1, false);
            }
        });

        addAlarm.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Calendar calendar = Calendar.getInstance();
                TimePickerDialog pickerDialog = TimePickerDialog.newInstance(AlarmFragment.this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(mContext));
                pickerDialog.setTitle(getString(R.string.instant_alarm));
                pickerDialog.setAccentColor(ThemeDetails.getThemeAccent(BitmapController.getThemeNumber()));
                pickerDialog.dismissOnPause(true);
                alarmDesc = NEW_ALARM;
                pickerDialog.show(getActivity().getFragmentManager(), "AlarmTimeDialog");
                return true;
            }
        });

        final PagerSlidingTabStrip tabStrip = getActivity().findViewById(R.id.pager_tabs);
        final Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        final CustomViewPager pager = getActivity().findViewById(R.id.viewpager);

        final SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {

                            @Override
                            public boolean canDismiss(int position) {
                                return mAdapter.getItem(position) != null;
                            }

                            @Override
                            public void onDismiss(ListView listView, final int[] reverseSortedPositions) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AlarmAssistant.cancelAlarms(mContext, dbManager);
                                        for (int position : reverseSortedPositions) {
                                            //Delete alarm from DB by id
                                            AlarmObject alarm = mAdapter.getItem(position);
                                            if (alarm!=null) {
                                                deletedAlarms.clear();
                                                deletedAlarms.add(alarm);
                                                dbManager.deleteAlarmById(alarm.id);
                                                dbManager.deleteInstanceById(alarm.id);
                                                mAdapter.remove(alarm);
                                            }
                                        }
                                        AlarmAssistant.checkAndSetAlarms(mContext, dbManager);
                                        showSnackBar(1);
                                    }
                                });
                            }
                        });

        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // Calls toggleSelection method from AlarmListAdapter
                mAdapter.toggleSelection(position, checked);
                // Capture total checked items
                int checkedCount = mAdapter.getSelectedCount();
                // Set the title according to total checked items
                mode.setTitle(checkedCount + mContext.getString(R.string.alarm_selected));
                mode.invalidate();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mAdapter != null && dbManager != null) {
                    mAdapter.setAlarms(dbManager.getAlarms());
                }
                isInActionMode = true;
                actionMode = mode;
                touchListener.setEnabled(false);
                listView.setOnScrollListener(null);
                mode.getMenuInflater().inflate(R.menu.cab_alarm, menu);
                if (snackbar != null && snackbar.isShownOrQueued()) {
                    snackbar.dismiss();
                }
                addAlarm.hide();
                tabStrip.setVisibility(View.INVISIBLE);
                toolbar.setVisibility(View.GONE); //This is needed in order to free the space covered by toolbar
                pager.setSwipeable(false);
                if (CommonUtils.isLOrLater()) {
                    getActivity().getWindow().setStatusBarColor(Color.parseColor("#90000000"));
                }

                if (savedInstanceState != null) {
                    mode.setTitle(savedInstanceState.getInt(ALARM_COUNT) + mContext.getString(R.string.alarm_selected));
                } else if (mAdapter != null) {
                    mode.setTitle(mAdapter.getSelectedCount() + mContext.getString(R.string.alarm_selected));
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem itemEdit = menu.findItem(R.id.alarm_edit);
                MenuItem itemReplicate = menu.findItem(R.id.alarm_replicate);
                MenuItem itemSelect = menu.findItem(R.id.alarm_select_all);
                MenuItem itemSkip = menu.findItem(R.id.alarm_skip);
                MenuItem itemUnSkip = menu.findItem(R.id.alarm_unskip);
                MenuItem itemSkipCheck = menu.findItem(R.id.alarm_skip_check);
                MenuItem itemPreview = menu.findItem(R.id.alarm_preview);
                boolean isSingleAlarm = mAdapter.getSelectedCount() == 1;

                itemEdit.setVisible(isSingleAlarm);
                itemReplicate.setVisible(isSingleAlarm);

                if (isSingleAlarm) {
                    AlarmObject alarm = mAdapter.getSelectedAlarm();
                    AlarmInstance instance = AlarmAssistant.getInstance(mContext, alarm.id);

                    if (instance != null && !alarm.isOneTimeAlarm() && alarm.isEnabled) {
                        itemSkip.setVisible(instance.alarmState == AlarmInstance.ALARM_STATE_FRESHLY_STARTED || instance.alarmState == AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
                        itemUnSkip.setVisible(instance.alarmState == AlarmInstance.ALARM_STATE_SKIPPED);
                        itemSkipCheck.setVisible(instance.alarmState == AlarmInstance.ALARM_STATE_DISMISSED_WITH_CHECK);
                    }

                    itemPreview.setVisible(instance != null && alarm.isEnabled && (instance.alarmState == AlarmInstance.ALARM_STATE_FRESHLY_STARTED || instance.alarmState == AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK));
                } else {
                    itemSkip.setVisible(false);
                    itemUnSkip.setVisible(false);
                    itemSkipCheck.setVisible(false);
                    itemPreview.setVisible(false);
                }
                itemSelect.setVisible(mAdapter.areSomeSelected());
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

                long id;
                switch (item.getItemId()) {

                    case R.id.alarm_replicate:
                        long replicateId = mAdapter.getSelectedAlarmId();
                        if (replicateId != -1) {
                            setupAlarmDetails(replicateId, true);
                        }
                        return true;

                    case R.id.alarm_edit:
                        id = mAdapter.getSelectedAlarmId();
                        if (id != -1) {
                            setupAlarmDetails(id, false);
                        }
                        return true;

                    case R.id.alarm_skip:
                        id = mAdapter.getSelectedAlarmId();
                        if (id != -1) {
                            AlarmAssistant.skipAlarm(mContext, id, true);
                        }
                        mode.finish();
                        return true;

                    case R.id.alarm_unskip:
                        activeAlarm = mAdapter.getSelectedAlarm();
                        if (activeAlarm != null) {
                            AlarmAssistant.unSkipAlarm(mContext, activeAlarm);
                        }
                        mode.finish();
                        return true;

                    case R.id.alarm_skip_check:
                        id = mAdapter.getSelectedAlarmId();
                        if (id != -1) {
                            AlarmInstance instance = dbManager.getInstanceById(id);
                            AlarmAssistant.cancelAlarm(mContext, instance);
                            AlarmAssistant.updateAlarmWithStateNoCheck(mContext, instance);
                            AlarmAssistant.checkAndSetAlarm(mContext, instance);
                        }
                        mode.finish();
                        return true;

                    case R.id.alarm_delete:
                        final SparseBooleanArray selected = mAdapter.getSelectedIds();
                        if (mAdapter.getCount() > selected.size()) {
                            getActivity().runOnUiThread(new DeleteAlarms(selected));
                        } else {
                            new DeleteAllAlarms(selected.size()).execute();
                        }
                        mode.finish();
                        return true;

                    case R.id.alarm_select_all:
                        int count = mAdapter.toggleAll();
                        mode.setTitle(count + mContext.getString(R.string.alarm_selected));
                        mode.invalidate();
                        return true;

                    case R.id.alarm_preview:
                        id = mAdapter.getSelectedAlarmId();
                        if (id != -1) {
                            Intent serviceIntent = new Intent(getActivity(), AlarmService.class);
                            serviceIntent.putExtra(AlarmAssistant.ID, id);
                            serviceIntent.putExtra(AlarmAssistant.PREVIEW, true);
                            if (CommonUtils.isOOrLater()) {
                                getActivity().startForegroundService(serviceIntent);
                            } else {
                                getActivity().startService(serviceIntent);
                            }
                            mode.finish();
                        }
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mode.finish();
                isInActionMode = false;
                touchListener.setEnabled(true);
                listView.setOnScrollListener(touchListener.makeScrollListener());
                if (CommonUtils.isLOrLater()) {
                    getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
                }

                actionMode = null;
                mAdapter.removeSelection();
                if (BitmapController.isAnimation()) {
                    tabStrip.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_top));
                    toolbar.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_bottom));
                    addAlarm.show();
                } else {
                    addAlarm.setVisibility(View.VISIBLE);
                }

                tabStrip.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
                pager.setSwipeable(true);
            }

        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mAdapter != null) {
            outState.putInt(ALARM_COUNT, mAdapter.getSelectedCount());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {

        switch (alarmDesc) {
            case NEW_ALARM:
                createInstantAlarm(hourOfDay, minute);
                break;

            case OLD_ALARM:
                updateOldAlarm(hourOfDay, minute);
                break;
        }
    }

    private void createInstantAlarm(final int hourOfDay, final int minute) {

        final AsyncTask<Void, Void, Void> instantAlarmTask = new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                activeAlarm = new AlarmObject(hourOfDay, minute);
                activeAlarm.id = dbManager.createAlarm(activeAlarm);
                AlarmInstance instance = dbManager.createInstance(activeAlarm);
                if (activeAlarm.id!=-1) {
                    AlarmAssistant.checkAndSetAlarm(mContext, instance);
                } else {
                    AlarmAssistant.cancelAlarms(mContext, dbManager);
                    AlarmAssistant.checkAndSetAlarms(mContext, dbManager);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mAdapter != null) {
                    mAdapter.setAlarms(dbManager.getAlarms());
                    mAdapter.notifyDataSetChanged();
                }
                CommonUtils.showAlarmToast(mContext, activeAlarm.getNextAlarmTime().getTimeInMillis());
            }
        };

        instantAlarmTask.execute();

    }

    private void updateOldAlarm(final int hourOfDay, final int minute) {

        final AsyncTask<Void, Void, Void> oldAlarmTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                if (activeAlarm == null) {
                    return null;
                }

                activeAlarm.hour = hourOfDay;
                activeAlarm.minutes = minute;
                activeAlarm.isEnabled = true;

                dbManager.updateAlarm(activeAlarm);
                AlarmInstance instance = dbManager.doesInstanceExist(activeAlarm.id) ? dbManager.updateInstanceFromAlarm(activeAlarm) : dbManager.createInstance(activeAlarm);
                if (instance != null) {
                    AlarmAssistant.cancelAlarm(mContext, instance);
                    AlarmAssistant.checkAndSetAlarm(mContext, instance);
                    AlarmAssistant.updateClosestAlarm(mContext, dbManager.getInstances());
                    CommonUtils.sendAlarmBroadcast(mContext, instance.id, AlarmService.ALARM_STATE_CHANGE);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mAdapter != null) {
                    mAdapter.setAlarms(dbManager.getAlarms());
                    mAdapter.notifyDataSetChanged();
                }

                CommonUtils.showAlarmToast(mContext, activeAlarm.getNextAlarmTime().getTimeInMillis());
            }
        };

        oldAlarmTask.execute();
    }

    @Override
    public void onPause() {
        SharedPreferences preferences = mContext.getSharedPreferences(Clock.ALARM_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Clock.IS_ACTION_MODE);
        editor.putBoolean(Clock.IS_ACTION_MODE, isInActionMode);
        if (isInActionMode && mAdapter != null) {
            mAdapter.putIds(editor);
        }
        editor.apply();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = mContext.getSharedPreferences(Clock.ALARM_PREFS, Context.MODE_PRIVATE);
        isInActionMode = preferences.getBoolean(Clock.IS_ACTION_MODE, false);
        if (isInActionMode && actionMode != null && mAdapter != null) {
            mAdapter.getIds(preferences);
            actionMode.invalidate();
        }
        if (listReceiver == null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Clock.REFRESH_LIST);
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            getActivity().registerReceiver(listReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    String action = intent.getAction();
                    if (action == null) {
                        return;
                    }

                    switch (action){
                        case Clock.REFRESH_LIST:
                            mAdapter.setAlarms(dbManager.getAlarms());
                            break;

                        case Intent.ACTION_TIME_CHANGED:
                            mAdapter.refreshFormat();
                            break;
                    }
                }
            }, intentFilter);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (actionMode != null) {
            actionMode.finish();
        }
        if (resultCode == Clock.RESULT_OK && requestCode == 10) { // Only update the alarms when necessary
            if (mAdapter != null) {
                mAdapter.setAlarms(dbManager.getAlarms());
            }
        }
    }

    public void toggleAlarm(int position, final boolean isEnabled) {
        activeAlarm = mAdapter.getItem(position);

        final AsyncTask<Void, Void, Void> setAlarm = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if (activeAlarm != null) {
                    activeAlarm.isEnabled = isEnabled;
                    dbManager.updateAlarm(activeAlarm);
                    if (isEnabled) {
                        AlarmInstance instance = dbManager.createInstance(activeAlarm);
                        AlarmAssistant.cancelAlarm(mContext, instance);
                        AlarmAssistant.checkAndSetAlarm(mContext, instance);
                    } else {
                        AlarmInstance instance = dbManager.getInstanceById(activeAlarm.id);
                        if (instance!=null) {
                            AlarmAssistant.cancelAlarm(mContext, instance);
                            dbManager.deleteInstanceById(activeAlarm.id);
                        }
                    }
                    AlarmAssistant.updateClosestAlarm(mContext, dbManager.getInstances());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mAdapter.setAlarms(dbManager.getAlarms());
                mAdapter.notifyDataSetChanged();
                if (isEnabled) {
                    //Checking for phone state permission, most probable place
                    if (CommonUtils.isMOrLater()) {
                        if (ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.READ_PHONE_STATE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ToastGaffer.showToast(getActivity().getApplicationContext(), getString(R.string.m_perm_telephone), true);
                            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                                    Clock.REQUEST_PERMISSION_PHONE_STATE);
                        } else {
                            CommonUtils.showAlarmToast(getActivity().getApplicationContext(), activeAlarm.getNextAlarmTime().getTimeInMillis());
                        }
                    } else {
                        CommonUtils.showAlarmToast(getActivity().getApplicationContext(), activeAlarm.getNextAlarmTime().getTimeInMillis());
                    }
                }
            }
        };
        setAlarm.execute();
    }

    @Override
    public void selectTime(View view, int position) {
        activeAlarm = mAdapter.getItem(position);
        if (activeAlarm != null) {
            TimePickerDialog pickerDialog = TimePickerDialog.newInstance(AlarmFragment.this, activeAlarm.hour, activeAlarm.minutes, DateFormat.is24HourFormat(mContext));
            pickerDialog.setAccentColor(ThemeDetails.getThemeAccent(BitmapController.getThemeNumber()));
            pickerDialog.dismissOnPause(true);
            alarmDesc = OLD_ALARM;
            pickerDialog.show(getActivity().getFragmentManager(), "AlarmTimeDialog");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Clock.REQUEST_PERMISSION_PHONE_STATE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                ToastGaffer.showToast(mContext, getString(R.string.telephone_warning));
            } else {
                ToastGaffer.cancelPreviousToast();
            }
        }
    }

    // Start alarm details activity
    private void setupAlarmDetails(long id, boolean isReplicate) {
        Intent intent = new Intent(getActivity(), AlarmDetails.class);
        intent.putExtra(AlarmAssistant.ID, id);
        intent.putExtra(AlarmDetails.IS_REPLICATE, isReplicate);
        startActivityForResult(intent, 10);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listReceiver !=null) {
            getActivity().unregisterReceiver(listReceiver);
        }
    }

    private class DeleteAlarms implements Runnable {

        private final SparseBooleanArray array;
        private final int ARRAY_SIZE;
        private Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.shrink_with_effect);
        private Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAdapter.setAlarms(mAdapter.getCount() > ARRAY_SIZE ? dbManager.getAlarms() : null);
                showSnackBar(ARRAY_SIZE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        DeleteAlarms(SparseBooleanArray array$alarm) {
            array = array$alarm;
            ARRAY_SIZE = array.size();
        }

        @Override
        public void run() {
            //Cancel Alarms
            for (int i = 0; i <= (ARRAY_SIZE - 1); i++) {
                final int POSITION = array.keyAt(i);
                final AlarmObject selectedAlarm = mAdapter
                        .getItem(POSITION);

                if (selectedAlarm != null) {
                    deletedAlarms.add(selectedAlarm);
                    AlarmInstance selectedInstance = dbManager.getInstanceById(selectedAlarm.id);
                    if (selectedInstance!=null) {
                        AlarmAssistant.cancelAlarm(mContext, selectedInstance);
                        dbManager.deleteInstanceById(selectedInstance.id);
                    }
                    //Delete alarm from DB by id
                    dbManager.deleteAlarmById(selectedAlarm.id);

                    if (BitmapController.isAnimation()) {
                        int firstPosition = listView.getFirstVisiblePosition();
                        int lastPosition = listView.getLastVisiblePosition();

                        if (POSITION >= firstPosition && POSITION <= lastPosition) {
                            View deleteView = listView.getChildAt(POSITION - firstPosition);
                            try {
                                if (i == ARRAY_SIZE - 1) {
                                    animation.setAnimationListener(listener);
                                }
                                deleteView.startAnimation(animation);
                            } catch (Exception e) {
                                mAdapter.setAlarms(dbManager.getAlarms());
                                showSnackBar(ARRAY_SIZE);
                            }
                        } else if (i == ARRAY_SIZE - 1 && POSITION > lastPosition) { //Item which is not visible
                            animation.setAnimationListener(listener);
                        }

                        if (i == ARRAY_SIZE - 1 && areAllItemsNotVisible(array)) {
                            mAdapter.setAlarms(dbManager.getAlarms());
                            showSnackBar(ARRAY_SIZE);
                        }
                    } else {
                        try {
                            if (i == ARRAY_SIZE - 1) {
                                mAdapter.setAlarms(dbManager.getAlarms());
                            }
                        } catch (Exception ignored) {
                        }
                        showSnackBar(ARRAY_SIZE);
                    }
                }
            }
            //Update indicator
            AlarmAssistant.updateClosestAlarm(mContext, dbManager.getInstances());
        }
    }

    private class DeleteAllAlarms extends AsyncTask<Void, Void, Void> {

        private final int ARRAY_SIZE;
        private Animation animation = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.shrink_with_effect);
        private Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAdapter.setAlarms(dbManager.getAlarms());
                showSnackBar(ARRAY_SIZE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        DeleteAllAlarms(int ARRAY_SIZE) {
            this.ARRAY_SIZE = ARRAY_SIZE;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //Delete all alarms from DB
            AlarmAssistant.cancelAlarms(mContext, dbManager);
            deletedAlarms = dbManager.getAlarms();
            dbManager.deleteAlarms();
            dbManager.deleteInstances();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (BitmapController.isAnimation()) {
                animation.setAnimationListener(listener);
                listView.startAnimation(animation);
            } else {
                mAdapter.setAlarms(null);
                showSnackBar(ARRAY_SIZE);
            }
        }
    }

}

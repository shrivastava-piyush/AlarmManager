/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.utils.AppWakeLock;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.MessageSpeaker;
import com.bytezap.wobble.utils.NotificationProvider;
import com.bytezap.wobble.utils.ToastGaffer;
import com.bytezap.wobble.utils.TtsSpeaker;

import java.io.File;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class AlarmService extends Service {

    public static final String ALARM_SNOOZE = "ALARM_SCREEN_SNOOZE";
    public static final String ALARM_DISMISS = "ALARM_SCREEN_DISMISS";
    public static final String ALARM_UPDATE_STATE = "ALARM_SCREEN_UPDATE";
    public static final String ALARM_STATE_CHANGE = "ALARM_STATE_CHANGE";
    public static final String ALARM_TTS_ERROR = "ALARM_TTS_ERROR";
    public static final String ALARM_RESTART = "ALARM_RESTART";

    private TelephonyManager telephonyMgr;
    private static final String TAG = AlarmService.class.getSimpleName();
    private AlarmInstance alarmInstance = null;
    private boolean isReceiverRegistered = false, isVoice = true, isPreview = false;
    private TextToSpeech speaker = null;
    private SharedPreferences settings;

    private Handler alarmHandler = new Handler();
    private Runnable toneRunnable = new Runnable() {
        @Override
        public void run() {
            //Start the tone
            AlarmTone.start(getApplicationContext(), alarmInstance);
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (action != null) {
                long id = intent.getLongExtra(AlarmAssistant.ID, -1);
                switch (action) {
                    case ALARM_SNOOZE:
                        if (id != -1) {
                            if (alarmInstance.id == id) {
                                finalSnooze();
                            }
                        }
                        break;

                    case ALARM_DISMISS:
                        if (id != -1) {
                            if (alarmInstance.id == id) {
                                finalDismissWithLaunch();
                            }
                        }
                        break;

                    case ALARM_UPDATE_STATE:
                        if (!isPreview) {
                            AlarmAssistant.updateDismissState(getApplicationContext(), alarmInstance);
                        }
                        break;

                    case ALARM_STATE_CHANGE:
                        if (id != -1) {
                            if (alarmInstance.id == id) {
                                if (alarmInstance.dismissMethod == AlarmObject.DISMISS_METHOD_DEFAULT) {
                                    AlarmTone.terminate(getApplicationContext());
                                    TtsSpeaker.stopTts(getApplicationContext());
                                    MessageSpeaker.stopMessage(getApplicationContext());
                                    stopForeground(true);
                                    NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                                    stopSelf();
                                } else {
                                    //if (!isTaskFinished) {
                                        NotificationProvider.showAlarmNotification(getApplicationContext(), alarmInstance, alarmInstance.snoozeTimeIndex == 0);
                                        // TODO: 26/06/2016 Find a better way to bring the activity front
                                    //}
                                }
                            }
                        }
                        break;

                    case ALARM_TTS_ERROR:
                        if (alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_AFTER_TIME && alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                            speakMessage();
                        } else {
                            if (AlarmTone.isToneNotRunning()) {
                                alarmHandler.post(toneRunnable);
                            }
                        }
                        break;

                    case Intent.ACTION_SCREEN_OFF:
                        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        sendBroadcast(closeDialog);
                        break;

                    case ALARM_RESTART:
                        if (id != -1) {
                            if (alarmInstance.id == id) {
                                Intent bringToFront = new Intent(getApplicationContext(), AlarmScreen.class);
                                bringToFront.putExtra(AlarmAssistant.ID, id);
                                bringToFront.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                //bringToFront.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                try{
                                    startActivity(bringToFront);
                                } catch (Throwable b) {
                                    b.printStackTrace();
                                }
                            }
                        }
                        break;
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        telephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Check latest call state
        telephonyMgr.listen(new CallListener(),
                PhoneStateListener.LISTEN_CALL_STATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALARM_SNOOZE);
        intentFilter.addAction(ALARM_DISMISS);
        intentFilter.addAction(ALARM_UPDATE_STATE);
        intentFilter.addAction(ALARM_STATE_CHANGE);
        intentFilter.addAction(ALARM_TTS_ERROR);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(ALARM_RESTART);
        registerReceiver(mReceiver, intentFilter);
        isReceiverRegistered = true;
    }

    private void startAlarm() {
        if (telephonyMgr.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
            if (alarmInstance.alarmState != AlarmInstance.ALARM_STATE_TRIGGERED && !isPreview) {
                AlarmAssistant.setAlarmState(getApplicationContext(), alarmInstance, AlarmInstance.ALARM_STATE_TRIGGERED);
            }
            startAlarmActivity();
        } else if (!isPreview){
            AlarmAssistant.snoozeAlarm(getApplicationContext(), Calendar.getInstance(), alarmInstance);
        }
    }

    private void setNextAlarm() {
        if (alarmInstance.isOneTimeAlarm()) {
            NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
            AlarmAssistant.disableAlarmRemoveInstance(getApplicationContext(), alarmInstance);
            if(!BitmapController.isAppNotRunning()){
                sendBroadcast(new Intent(Clock.REFRESH_LIST));
            }
        } else {
            AlarmAssistant.setAlarmState(getApplicationContext(), alarmInstance, AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK);
            AlarmAssistant.rescheduleAlarm(getApplicationContext(), alarmInstance);
        }
    }

    private void startAlarmActivity(){
        NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
        int maxSnoozeTime = getResources().getIntArray(R.array.final_snooze_values)[alarmInstance.snoozeTimeIndex];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        Intent alarmIntent = new Intent(getApplicationContext(), AlarmScreen.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        alarmIntent.putExtra(AlarmAssistant.ID, alarmInstance.id);
        alarmIntent.putExtra(AlarmAssistant.PREVIEW, isPreview);
        startActivity(alarmIntent);

        int brightnessDuration = Integer.parseInt(settings.getString(SettingsActivity.INCREASING_BRIGHTNESS, "0"));
        if (brightnessDuration!=0 && (alarmInstance.snoozeTimes == 0)) {
            //First check if the instance is not started after the actual time
            int hourDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int mainGap = (alarmInstance.hour*60 + alarmInstance.minutes) - (hourDay*60 + minute);
            if (mainGap < brightnessDuration/60) {
                brightnessDuration = mainGap*60;
            }
        }

        if (brightnessDuration != 0 && alarmInstance.snoozeTimes == 0) {
            new VoiceToneTask().execute();
        } else {
            alarmHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new VoiceToneTask().execute();
                }
            }, brightnessDuration*1000);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return START_REDELIVER_INTENT;
        }

        AppWakeLock.acquireCpuWakeLock(getApplicationContext());

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        isVoice = settings.getBoolean(SettingsActivity.VOICE, true);

        long id = intent.getLongExtra(AlarmAssistant.ID, -1);
        isPreview = intent.getBooleanExtra(AlarmAssistant.PREVIEW, false);
        alarmInstance = AlarmAssistant.getInstance(getApplicationContext(), id);

        if (alarmInstance == null) {
            //In case a Db fetch fails, create an alarm with a permanent id
            alarmInstance = new AlarmInstance();
            alarmInstance.id = 999;
            Calendar calendar = Calendar.getInstance();
            alarmInstance.date = calendar.get(Calendar.DATE);
            alarmInstance.month = calendar.get(Calendar.MONTH);
            alarmInstance.year = calendar.get(Calendar.YEAR);
            alarmInstance.hour = calendar.get(Calendar.HOUR_OF_DAY);
            alarmInstance.minutes = calendar.get(Calendar.MINUTE);
        }

        if (alarmInstance != null) {

            int maxSnoozeTime = getResources().getIntArray(R.array.final_snooze_values)[alarmInstance.snoozeTimeIndex];
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            Notification notification = NotificationProvider.showAlarmNotification(getApplicationContext(), alarmInstance, CommonUtils.shouldDisableSnooze(maxSnoozeTime, calendar, alarmInstance));
            startForeground(alarmInstance.hashCode(), notification);

            Calendar currentTime = Calendar.getInstance();

            long timeDiff = Math.abs(Calendar.getInstance().getTimeInMillis() - alarmInstance.getAlarmTime().getTimeInMillis());
            if (timeDiff > 60000
                    && alarmInstance.alarmState != AlarmInstance.ALARM_STATE_TRIGGERED && !isPreview) {
                Log.v(TAG, "Alarm in past or future has been called due to time change");
                NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            switch (alarmInstance.alarmState) {
                case AlarmInstance.ALARM_STATE_FRESHLY_STARTED:
                case AlarmInstance.ALARM_STATE_TRIGGERED:
                case AlarmInstance.ALARM_STATE_DISMISSED_WITH_NO_CHECK:
                case AlarmInstance.ALARM_STATE_SNOOZED:

                    if (AlarmScreen.isAlarmActive) {
                        sendBroadcast(new Intent(ALARM_UPDATE_STATE));
                    }

                    AudioManager aManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (aManager != null) {
                        switch (aManager.getRingerMode()) {

                            case AudioManager.RINGER_MODE_SILENT:
                                if (settings.getBoolean(SettingsActivity.PLAY_IN_SILENT_MODE, true)) {
                                    startAlarm();
                                } else if (!isPreview) {
                                    setNextAlarm();
                                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.alarm_not_started));
                                }
                                break;

                            case AudioManager.RINGER_MODE_VIBRATE:
                                if (settings.getBoolean(SettingsActivity.PLAY_IN_SILENT_MODE, true)) {
                                    startAlarm();
                                } else if (!isPreview) {
                                    setNextAlarm();
                                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.alarm_not_started));
                                }
                                break;

                            case AudioManager.RINGER_MODE_NORMAL:
                                startAlarm();
                                break;
                        }
                    }
                    return START_STICKY;

                case AlarmInstance.ALARM_STATE_DISMISSED_WITH_CHECK: {
                    if (isPreview) {
                        NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                        stopForeground(true);
                        stopSelf();
                        break;
                    }

                    if (telephonyMgr.getCallState() == TelephonyManager.CALL_STATE_IDLE && !AlarmScreen.isAlarmActive) { //Check if phone is in idle state & no alarm is active
                        if (WakeUpCheck.isWakeUpCheckActive) {
                            sendBroadcast(new Intent(WakeUpCheck.WAKE_UP_CHECK_UPDATE));
                        }
                        Intent alarmIntent = new Intent(getApplicationContext(), WakeUpCheck.class);
                        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        alarmIntent.putExtras(intent);
                        startActivity(alarmIntent);
                    } else {
                        AlarmAssistant.setWakeupCheck(getApplicationContext(), alarmInstance, currentTime);
                    }
                    NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                    stopForeground(true);
                    stopSelf();
                }
                break;

                case AlarmInstance.ALARM_STATE_PRE_DISMISS:
                case AlarmInstance.ALARM_STATE_SKIPPED:
                    //These 2 cases must never be reached. Still let's be prepared.
                    Log.e(TAG, "Alarm called for obscured state: " + alarmInstance.alarmState);
                    if (!isPreview) {
                        setNextAlarm();
                    }
                    NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                    stopForeground(true);
                    stopSelf();
                    break;

                default:
                    Log.e(TAG, "Alarm called for invalid state: " + alarmInstance.alarmState);
                    if (!isPreview) {
                        setNextAlarm();
                    }
                    NotificationProvider.cancelAlarmNotification(getApplicationContext(), alarmInstance.hashCode());
                    stopForeground(true);
                    stopSelf();
                    break;
            }

        } else {
            Log.v(TAG, "Invalid pendingIntent tried to start the alarmInstance");
            AlarmAssistant.updateSetAlarms(getApplicationContext());
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (isReceiverRegistered && mReceiver!=null) {
            unregisterReceiver(mReceiver);
        }
        telephonyMgr.listen(new CallListener(), PhoneStateListener.LISTEN_NONE);
        AlarmTone.terminate(getApplicationContext());
        super.onDestroy();
    }

    private void finalSnooze() {
        AlarmTone.terminate(getApplicationContext());
        if (isVoice && alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_AFTER_SNOOZE && alarmInstance.vocalMessage != null) {
            if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
                TtsSpeaker.speak(getApplicationContext(), alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), true);
            } else if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                MessageSpeaker.sayMessage(getApplicationContext(), alarmInstance.vocalMessage, true);
            }
        } else {
            TtsSpeaker.setShouldShutDown(true);
            TtsSpeaker.shutDown(getApplicationContext());
            MessageSpeaker.stopMessage(getApplicationContext());
        }
        stopForeground(true);
        if (!isPreview) {
            AlarmAssistant.snoozeAlarm(getApplicationContext(), Calendar.getInstance(), alarmInstance);
            AlarmScreen.isAlarmActive = false;
        } else {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.preview_dismissed));
        }
        stopSelf();
    }

    private void finalDismissWithLaunch() {
        AlarmTone.terminate(getApplicationContext());
        if (isVoice && alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_AFTER_DISMISS && alarmInstance.vocalMessage != null) {
            if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
                TtsSpeaker.speak(getApplicationContext(), alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), true);
            } else if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                if (new File(alarmInstance.vocalMessage).exists()) {
                    MessageSpeaker.sayMessage(getApplicationContext(), alarmInstance.vocalMessage, true);
                }
            }
        } else if (isVoice) {
            TtsSpeaker.setShouldShutDown(true);
            TtsSpeaker.shutDown(getApplicationContext());
            MessageSpeaker.stopMessage(getApplicationContext());
        }
        if (!isPreview) {
            AlarmAssistant.updateDismissState(getApplicationContext(), alarmInstance);
        }
        if (alarmInstance.isLaunchApp) {
            CommonUtils.launchApp(getApplicationContext(), alarmInstance.launchAppPkg);
        }
        stopForeground(true);
        if (!isPreview) {
            AlarmScreen.isAlarmActive = false;
        } else {
            ToastGaffer.showToast(getApplicationContext(), getString(R.string.preview_dismissed));
        }
        stopSelf();
    }

    private void startVoiceAlert(final int hour, final int minute, final String ampm) {
        switch (alarmInstance.vocalMessagePlace) {

            case AlarmObject.ALARM_VOCAL_BEFORE_TIME:
                if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
                    try {
                        if (alarmInstance.hour < 16 && alarmInstance.hour >= 4) {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? alarmInstance.vocalMessage + ".." + getString(R.string.alarm_voice_morning, hour, minute, ampm) : alarmInstance.vocalMessage + ".." + "Good Morning! It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
                        } else if (alarmInstance.hour > 4 && alarmInstance.hour <= 8) {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? alarmInstance.vocalMessage + ".." + getString(R.string.alarm_voice_evening, hour, minute, ampm) : alarmInstance.vocalMessage + ".." + "Good Evening! It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
                        } else {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? alarmInstance.vocalMessage + ".." + getString(R.string.alarm_voice_normal, hour, minute, ampm) : alarmInstance.vocalMessage + ".." + "It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
                        }
                        setSpeakerListener();
                    } catch (Exception e) {
                        onTimeSpoken();
                    }

                } else if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                    if (new File(alarmInstance.vocalMessage).exists()) {
                        MediaPlayer player = MessageSpeaker.sayMessage(getApplicationContext(), alarmInstance.vocalMessage, false);
                        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                speakTime(hour, minute, ampm);
                                return true;
                            }
                        });

                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                speakTime(hour, minute, ampm);
                            }
                        });
                    } else {
                        speakTime(hour, minute, ampm);
                    }
                }
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_TIME:
                if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
                    try {
                        if (alarmInstance.hour < 16 && alarmInstance.hour >= 4) {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_morning, hour, minute, ampm) + ".." + alarmInstance.vocalMessage : "Good Morning! It's " + hour + " " + minute + " " + ampm + ".." + alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), false);
                        } else if (alarmInstance.hour > 4 && alarmInstance.hour <= 8) {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_evening, hour, minute, ampm) + ".." + alarmInstance.vocalMessage : "Good Evening! It's " + hour + " " + minute + " " + ampm + ".." + alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), false);
                        } else {
                            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_normal, hour, minute, ampm) + ".." + alarmInstance.vocalMessage : "It's " + hour + " " + minute + " " + ampm + ".." + alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), false);
                        }
                        setSpeakerListener();
                    } catch (Exception e) {
                        onTimeSpoken();
                    }
                } else if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                    speakTime(hour, minute, ampm);
                    try {
                        if (CommonUtils.is15OrLater()) {
                            speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onStart(String utteranceId) {

                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    speakMessage();
                                }

                                @Override
                                public void onError(String utteranceId) {
                                    speakMessage();
                                }
                            });
                        } else {
                            speaker.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                                @Override
                                public void onUtteranceCompleted(String utteranceId) {
                                    speakMessage();
                                }
                            });
                        }
                    } catch (Exception e) {
                        speakMessage();
                    }
                }
                break;

            case AlarmObject.ALARM_VOCAL_REPLACE_TIME:
                if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_TEXT) {
                    speaker = TtsSpeaker.speak(getApplicationContext(), alarmInstance.vocalMessage, TtsSpeaker.isTtsOff(), false);
                    setSpeakerListener();
                } else if (alarmInstance.vocalMessageType == AlarmObject.VOCAL_TYPE_AUDIO) {
                    if (new File(alarmInstance.vocalMessage).exists()) {
                        MediaPlayer player = MessageSpeaker.sayMessage(getApplicationContext(), alarmInstance.vocalMessage, false);
                        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                alarmHandler.post(toneRunnable);
                                MessageSpeaker.stopMessage(getApplicationContext());
                                return true;
                            }
                        });

                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                MessageSpeaker.stopMessage(getApplicationContext());
                                alarmHandler.post(toneRunnable);
                            }
                        });
                    } else {
                        alarmHandler.post(toneRunnable);
                        MessageSpeaker.stopMessage(getApplicationContext());
                    }
                }
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_SNOOZE:
            case AlarmObject.ALARM_VOCAL_AFTER_DISMISS:
            case AlarmObject.ALARM_VOCAL_NOT_CALLED:
                speakTime(hour, minute, ampm);
                break;
        }
    }

    private void setSpeakerListener() {
        try {
            if (CommonUtils.is15OrLater()) {
                speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        onTimeSpoken();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        onTimeSpoken();
                    }
                });
            } else {
                speaker.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        onTimeSpoken();
                    }
                });
            }
        } catch (Exception e) {
            onTimeSpoken();
        }
    }

    private void onTimeSpoken() {
        shutTtsOff();
        alarmHandler.post(toneRunnable);
    }

    private void shutTtsOff() {
        if (CommonUtils.is15OrLater()) {
            speaker.setOnUtteranceProgressListener(null);
        } else {
            speaker.setOnUtteranceCompletedListener(null);
        }
        if (!BitmapController.isAppNotRunning() && (alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_NOT_CALLED || alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_BEFORE_TIME || alarmInstance.vocalMessagePlace == AlarmObject.ALARM_VOCAL_AFTER_TIME)) {
            TtsSpeaker.shutDown(getApplicationContext());
        }
    }

    private void speakTime(int hour, int minute, String ampm) {
        if (alarmInstance.hour < 16 && alarmInstance.hour >= 4) {
            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_morning, hour, minute, ampm) : "Good Morning! It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
        } else if (alarmInstance.hour > 4 && alarmInstance.hour <= 8) {
            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_evening, hour, minute, ampm) : "Good Evening! It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
        } else {
            speaker = TtsSpeaker.speak(getApplicationContext(), TtsSpeaker.isLangSupported() ? getString(R.string.alarm_voice_normal, hour, minute, ampm) : "It's " + hour + " " + minute + " " + ampm + ".", TtsSpeaker.isTtsOff(), false);
        }
        setSpeakerListener();
    }

    private void speakMessage() {
        if (new File(alarmInstance.vocalMessage).exists()) {
            MediaPlayer player = MessageSpeaker.sayMessage(getApplicationContext(), alarmInstance.vocalMessage, false);
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    MessageSpeaker.stopMessage(getApplicationContext());
                    alarmHandler.post(toneRunnable);
                    return true;
                }
            });

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    MessageSpeaker.stopMessage(getApplicationContext());
                    alarmHandler.post(toneRunnable);
                }
            });
        } else {
            MessageSpeaker.stopMessage(getApplicationContext());
            alarmHandler.post(toneRunnable);
        }
    }

    private class VoiceToneTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (alarmInstance != null) {
                Calendar calendar = Calendar.getInstance();
                int amPm = calendar.get(Calendar.AM_PM);
                int hourDay = isPreview ? alarmInstance.hour : calendar.get(Calendar.HOUR_OF_DAY);

                boolean isFormat24 = DateFormat.is24HourFormat(getApplicationContext());
                int hour = isFormat24 ? hourDay : CommonUtils.getFormatted12Hour(hourDay);
                int minute = isPreview ? alarmInstance.minutes : calendar.get(Calendar.MINUTE);

                DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
                String[] amPmStrings = symbols.getAmPmStrings();

                //Voice Notification
                if (isVoice) {
                    TtsSpeaker.setShouldShutDown(false);
                    String a = amPm == 0 ? "a.m" : "p.m";
                    startVoiceAlert(hour, minute, !isFormat24 ? Locale.getDefault().getLanguage().contains("en") ? a : amPmStrings[amPm] : "");
                } else {
                    //Start the tone
                    alarmHandler.post(toneRunnable);
                }
            }
            return null;
        }
    }

    private class CallListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
           if(state != TelephonyManager.CALL_STATE_IDLE){
               Log.v(TAG, "Device is not in idle state");
           }
        }
    }

}
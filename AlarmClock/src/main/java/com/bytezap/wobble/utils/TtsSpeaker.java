package com.bytezap.wobble.utils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.alarm.AlarmScreen;
import com.bytezap.wobble.alarm.AlarmService;

import java.util.HashMap;
import java.util.Locale;

public class TtsSpeaker {

    private static final String UTTERANCE_ID = "VoiceNotifier";
    private static TextToSpeech speaker;
    private static int streamVolume;

    /*This flag has to be used to account for the lag
          in the initialization of Google Tts Engine*/
    private static boolean shouldShutDown;
    static {
        speaker = null;
        streamVolume = 0;
        shouldShutDown = false;
    }

    public static TextToSpeech speak(final Context context, final String text, final boolean isFirsTime, boolean isLastTime) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int userAppVolume = Math.round(PreferenceManager.getDefaultSharedPreferences(context).getInt(SettingsActivity.VOICE_ALERT_VOLUME, 80) / 100.0f * maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, userAppVolume, 0);
            final Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);
            final HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
            map.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (speaker == null) {
                speaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            setLanguage(CommonUtils.getCurrentLocale());
                            if (shouldShutDown) {
                                shutDown(context);
                            } else if (isFirsTime) {
                                if (CommonUtils.isLOrLater()) {
                                    speaker.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, UTTERANCE_ID);
                                } else {
                                    speaker.speak(text, TextToSpeech.QUEUE_FLUSH, map);
                                }
                            }
                        } else if (AlarmScreen.isAlarmActive) {
                            speaker = null;
                            context.sendBroadcast(new Intent(AlarmService.ALARM_TTS_ERROR));
                        }
                    }
                });
            }

            if (isLastTime && BitmapController.isAppNotRunning()) {
                if (CommonUtils.is15OrLater()) {
                    speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            shutDown(context);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            shutDown(context);
                        }
                    });
                } else {
                    speaker.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                        @Override
                        public void onUtteranceCompleted(String utteranceId) {
                            shutDown(context);
                        }
                    });
                }
            }

            if (!isFirsTime) {
                if (speaker.isSpeaking()) {
                    speaker.stop();
                }
                speaker.setSpeechRate(0.9f);
                if (CommonUtils.isLOrLater()) {
                    speaker.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, UTTERANCE_ID);
                } else {
                    speaker.speak(text, TextToSpeech.QUEUE_FLUSH, map);
                }
            }

        } catch (Throwable t) {
            Log.e("VoiceNotifier", t.getMessage());
        }
        return speaker;
    }

    private static void setLanguage(Locale locale) {

        switch (speaker.getDefaultEngine()) {

            case "com.svox.pico":
                switch (locale.getLanguage()) {

                    case "en":
                        speaker.setLanguage(Locale.ENGLISH);
                        break;

                    case "fr":
                        speaker.setLanguage(Locale.FRENCH);
                        break;

                    case "de":
                        speaker.setLanguage(Locale.GERMAN);
                        break;

                    case "es":
                        speaker.setLanguage(new Locale("spa"));
                        break;

                    case "us":
                        speaker.setLanguage(Locale.US);
                        break;

                    case "uk":
                        speaker.setLanguage(Locale.UK);
                        break;

                    default:
                        speaker.setLanguage(Locale.ENGLISH);
                        break;
                }
                break;

            case "com.google.android.tts":
                switch (locale.getLanguage()) {

                    case "en":
                        speaker.setLanguage(Locale.ENGLISH);
                        break;

                    case "fr":
                        speaker.setLanguage(Locale.FRENCH);
                        break;

                    case "de":
                        speaker.setLanguage(Locale.GERMAN);
                        break;

                    case "es":
                        speaker.setLanguage(new Locale("spa"));
                        break;

                    case "us":
                        speaker.setLanguage(Locale.US);
                        break;

                    case "uk":
                        speaker.setLanguage(Locale.UK);
                        break;

                    case "ru":
                        speaker.setLanguage(new Locale("rus"));
                        break;

                    case "hi":
                        speaker.setLanguage(new Locale("hin"));
                        break;

                    case "ja":
                        speaker.setLanguage(Locale.JAPANESE);
                        break;

                    case "ko":
                        speaker.setLanguage(Locale.KOREAN);
                        break;

                    default:
                        speaker.setLanguage(Locale.ENGLISH);
                        break;
                }
                break;

            default:
                switch (locale.getLanguage()) {

                    case "en":
                        speaker.setLanguage(Locale.ENGLISH);
                        break;

                    case "fr":
                        speaker.setLanguage(Locale.FRENCH);
                        break;

                    case "de":
                        speaker.setLanguage(Locale.GERMAN);
                        break;

                    case "es":
                        speaker.setLanguage(new Locale("spa"));
                        break;

                    case "us":
                        speaker.setLanguage(Locale.US);
                        break;

                    case "uk":
                        speaker.setLanguage(Locale.UK);
                        break;

                    default:
                        speaker.setLanguage(Locale.ENGLISH);
                        break;
                }
                break;
        }
    }

    public static boolean isLangSupported() {
        String locale = CommonUtils.getLangCode();
        return locale.equals("en") || locale.equals("fr") || locale.equals("de")
                || locale.equals("es") || locale.equals("hi") || locale.equals("ru") || locale.equals("ko") || locale.equals("ja");
    }

    public static boolean isTtsOff() {
        return speaker == null;
    }

    public static boolean isSpeaking() {
        return speaker != null && speaker.isSpeaking();
    }

    public static void initAndNotify(final Context context) {
        try {
            if (speaker == null) {
                speaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            setLanguage(Locale.getDefault());
                            if (shouldShutDown) {
                                shutDown(context);
                            }
                        } else {
                            speaker = null;
                            context.sendBroadcast(new Intent(Clock.TTS_ERROR));
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Log.e("VoiceNotifier", "Tts could not be instantiated");
        }
    }

    public static void init(final Context context) {
        try {
            if (speaker == null) {
                speaker = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            setLanguage(Locale.getDefault());
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Log.e("VoiceNotifier", "Tts could not be instantiated");
        }
    }

    public static void setShouldShutDown(boolean shouldShutDown) {
        TtsSpeaker.shouldShutDown = shouldShutDown;
    }

    public static void stopTts(Context context) {
        if (speaker != null) {
            speaker.stop();
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamVolume, 0);
            audioManager.abandonAudioFocus(null);
            if (CommonUtils.is15OrLater()) {
                speaker.setOnUtteranceProgressListener(null);
            } else {
                speaker.setOnUtteranceCompletedListener(null);
            }
        }
    }

    public static void shutDown(Context context) {
        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, streamVolume, 0);
            audioManager.abandonAudioFocus(null);
            speaker = null;
            streamVolume = 0;
        }
    }
}

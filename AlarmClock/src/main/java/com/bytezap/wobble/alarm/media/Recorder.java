/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm.media;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bytezap.wobble.Clock;
import com.bytezap.wobble.R;
import com.bytezap.wobble.preference.SettingsActivity;
import com.bytezap.wobble.alarm.AlarmDetails;
import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.ToastGaffer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recorder extends AppCompatActivity implements View.OnClickListener, MediaRecorder.OnErrorListener {

    public static final String FILE_PATH = "dir_path";
    private static final String DEFAULT_DIR = "/Wobble/Audio";
    private static final String DEFAULT_NAME = "audio";
    private static final String DEFAULT_EXTENSION = ".amr";
    private static final int EXTERNAL_STORAGE_BLOCK_THRESHOLD = 32;
    private static final String TAG = "Recorder";
    private final Handler recorderHandler = new Handler();
    private long mLastClickTime;
    private MediaRecorder mRecorder = null;
    private File directory, tempFile = null;
    private long mStartTime = 0;
    private int audioLength = 0;
    private TextView time;
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            long currTime = System.currentTimeMillis();
            long actualTime = currTime - mStartTime;
            int secs = (int) (actualTime / 1000) % 60;
            int mins = (int) ((actualTime / 1000) / 60) % 60;
            time.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            recorderHandler.postDelayed(timeRunnable, 1000);
        }
    };
    private FloatingActionButton start, stop, play, pause;
    private Button recordedAudio, saveAudio;
    private int messagePlace = 0;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private CountDownTimer timer;

    public Recorder() {
        mLastClickTime = 0;
        try {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + DEFAULT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            directory = dir;
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonUtils.setLanguage(getApplicationContext().getResources(), CommonUtils.getLangCode());

        setContentView(R.layout.audio_message_layout);
        getWindow().setBackgroundDrawableResource(R.drawable.faded_bg);

        if (BitmapController.isAnimation()) {
            overridePendingTransition(R.anim.fade_in_later, R.anim.fade_out);
        }  else {
            overridePendingTransition(0, 0);
        }

        setVolumeControlStream(AudioManager.STREAM_ALARM);
        messagePlace = getIntent().getIntExtra(AlarmDetails.DETAILS_VOCAL_MESSAGE_PLACE, 0);

        start = findViewById(R.id.recorder_start);
        stop = findViewById(R.id.recorder_stop);
        play = findViewById(R.id.recorder_play);
        pause = findViewById(R.id.recorder_pause);
        time = findViewById(R.id.recorder_time);
        recordedAudio = findViewById(R.id.recorded_audio);
        saveAudio = findViewById(R.id.save_audio);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        recordedAudio.setOnClickListener(this);
        saveAudio.setOnClickListener(this);
        recordedAudio.setVisibility(doFilesExist() ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean diskSpaceAvailable() {
        StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        // keep one free block
        try {
            if (CommonUtils.is18OrLater()) {
                return fs.getAvailableBlocksLong() > EXTERNAL_STORAGE_BLOCK_THRESHOLD;
            } else {
                return fs.getAvailableBlocks() > EXTERNAL_STORAGE_BLOCK_THRESHOLD;
            }
        } catch (Exception e) {
            return true;
        }
    }

    private boolean doFilesExist() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + Recorder.DEFAULT_DIR);
            if (dir.isDirectory() && dir.exists()) {
                File[] contents = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(Recorder.DEFAULT_EXTENSION);
                    }
                });
                if (contents != null && contents.length != 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean doesRecordExist(String path) {
        try {
            if (!TextUtils.isEmpty(path)) {
                File file = new File(directory.getAbsolutePath() + "/" + path);
                return file.exists();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private void saveAudioDialog() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(Recorder.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vM = inflater.inflate(R.layout.audio_message_dialog, null);

        final EditText vocalTitle = vM.findViewById(R.id.alarm_details_vocal_holder);
        /*String s = tempFile.getName();
        if (!TextUtils.isEmpty(s)) {
            vocalTitle.setText(s);
        }*/
        vocalTitle.setSelection(vocalTitle.getText().length());
        final AlertDialog mVocalMessage = new AlertDialog.Builder(wrapper).create();
        mVocalMessage.setView(vM);
        mVocalMessage.setTitle(R.string.alarm_details_vocal_message);
        final RadioGroup group = vM.findViewById(R.id.radio_group);

        mVocalMessage.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = vocalTitle.getText().toString();
                        if (!TextUtils.isEmpty(name)) {
                            int place = setMessagePlace(group.getCheckedRadioButtonId());
                            renameTempFile(name);
                            saveFile(place);
                            dialog.dismiss();
                            finish();
                        }
                    }
                });

        mVocalMessage.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        boolean isVoice = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.VOICE, true);
        if (!isVoice) {
            TextView alert = vM.findViewById(R.id.voice_warning);
            alert.setVisibility(View.VISIBLE);
        }
        DialogSupervisor.setDialog(mVocalMessage);
        mVocalMessage.show();
    }

    private void savePreviousAudioDialog(final String path) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(Recorder.this, R.style.AlertDialogStyle);
        final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vM = inflater.inflate(R.layout.message_place_layout, null);

        final AlertDialog mVocalMessage = new AlertDialog.Builder(wrapper).create();
        mVocalMessage.setView(vM);
        mVocalMessage.setTitle(R.string.recorded_message);
        final RadioGroup group = vM.findViewById(R.id.radio_group);

        if (messagePlace != AlarmObject.ALARM_VOCAL_NOT_CALLED) {
            setMessageID(group);
        }

        mVocalMessage.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(path)) {
                            int place = setMessagePlace(group.getCheckedRadioButtonId());
                            try {
                                Intent audioIntent = new Intent();
                                audioIntent.putExtra(FILE_PATH, path);
                                audioIntent.putExtra(AlarmDetails.DETAILS_VOCAL_MESSAGE_PLACE, place);
                                setResult(RESULT_OK, audioIntent);
                            } catch (Exception e) {
                                Log.v("Recorder", e.toString());
                            }
                            dialog.dismiss();
                            finish();
                        }
                    }
                });

        mVocalMessage.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (tempFile != null) {
                            tempFile.delete();
                        }
                        dialog.dismiss();
                        finish();
                    }
                });

        boolean isVoice = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.VOICE, true);
        if (!isVoice) {
            TextView alert = vM.findViewById(R.id.voice_warning);
            alert.setVisibility(View.VISIBLE);
        }

        DialogSupervisor.setDialog(mVocalMessage);
        mVocalMessage.show();
    }

    private void launchAudioPicker() {
        final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + Recorder.DEFAULT_DIR);
        if (dir.isDirectory() && dir.exists()) {
            final String[] contents = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(Recorder.DEFAULT_EXTENSION);
                }
            });

            ContextThemeWrapper wrapper = new ContextThemeWrapper(Recorder.this, R.style.AlertDialogStyle);
            AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
            builder.setTitle(R.string.message_select).setItems(contents, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        String path = String.valueOf(directory.getAbsolutePath() + "/" + contents[which]);
                        savePreviousAudioDialog(path);
                        dialog.dismiss();
                    } catch (Exception e) {
                        dialog.dismiss();
                    }
                }
            });

            AlertDialog dialog = builder.create();
            DialogSupervisor.setDialog(dialog);
            dialog.show();
        }
    }

    @Override
    protected void onPause() {
        stopPlaying();
        super.onPause();
    }

    private void startRecording() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showErrorDialog(false);
        } else if (!diskSpaceAvailable()) {
            showErrorDialog(true);
        } else {
            stopRecording();
            if (tempFile == null) {
                try {
                    tempFile = File.createTempFile(DEFAULT_NAME, DEFAULT_EXTENSION, directory);
                } catch (IOException e) {
                    return;
                }
            }
            record();
        }
    }

    private void record() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setAudioSamplingRate(16000);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        mRecorder.setOutputFile(tempFile.getAbsolutePath());
        mRecorder.setOnErrorListener(this);

        // Handle IOException
        try {
            mRecorder.prepare();
        } catch (IOException exception) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.start();
            start.setVisibility(View.INVISIBLE);
            stop.setVisibility(View.VISIBLE);
            time.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shrink_with_effect);
            if (recordedAudio.getVisibility() == View.VISIBLE) {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        recordedAudio.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                recordedAudio.startAnimation(animation);
            }
            mStartTime = System.currentTimeMillis();
            recorderHandler.removeCallbacks(timeRunnable);
            recorderHandler.post(timeRunnable);
        } catch (RuntimeException exception) {
            Log.e(TAG, exception.getMessage());
            AudioManager audioMngr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            boolean isInCall = (audioMngr.getMode() == AudioManager.MODE_IN_CALL);
            if (isInCall) {
                mRecorder.stop();
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void stopRecording() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
            } catch (RuntimeException ignored) {
            }
            mRecorder.release();
            recorderHandler.removeCallbacks(timeRunnable);
            audioLength = (int) ((System.currentTimeMillis() - mStartTime) / 1000);
            if (audioLength == 0) {
                // round up to 1 second if it's too short
                audioLength = 1;
            }
            mRecorder = null;
            saveAudio.setVisibility(View.VISIBLE);
            saveAudio.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
        }
    }

    private int setMessagePlace(int checkedId) {

        switch (checkedId) {
            case R.id.vocal_after_time:
                return AlarmObject.ALARM_VOCAL_AFTER_TIME;

            case R.id.vocal_before_time:
                return AlarmObject.ALARM_VOCAL_BEFORE_TIME;

            case R.id.vocal_after_dismiss:
                return AlarmObject.ALARM_VOCAL_AFTER_DISMISS;

            case R.id.vocal_after_snoozing:
                return AlarmObject.ALARM_VOCAL_AFTER_SNOOZE;

            case R.id.vocal_replace_time:
                return AlarmObject.ALARM_VOCAL_REPLACE_TIME;

            default:
                return AlarmObject.ALARM_VOCAL_AFTER_TIME;
        }
    }

    private void setMessageID(RadioGroup group) {

        switch (messagePlace) {
            case AlarmObject.ALARM_VOCAL_AFTER_TIME:
                group.check(R.id.vocal_after_time);
                break;

            case AlarmObject.ALARM_VOCAL_BEFORE_TIME:
                group.check(R.id.vocal_before_time);
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_DISMISS:
                group.check(R.id.vocal_after_dismiss);
                break;

            case AlarmObject.ALARM_VOCAL_AFTER_SNOOZE:
                group.check(R.id.vocal_after_snoozing);
                break;

            case AlarmObject.ALARM_VOCAL_REPLACE_TIME:
                group.check(R.id.vocal_replace_time);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Clock.REQUEST_PERMISSION_MICROPHONE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                ToastGaffer.showToast(getApplicationContext(), getString(R.string.permission_denied));
            }
        }
    }

    @Override
    public void onBackPressed() {
        stopRecording();
        if (tempFile != null) {
            tempFile.delete();
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            boolean isPortrait = CommonUtils.isPortrait(getResources());
            overridePendingTransition(0, isPortrait ? R.anim.exit_zoom : R.anim.exit_scale);
        }
    }

    private void renameTempFile(String name) {
        if (tempFile != null) {
            if (!TextUtils.isEmpty(name)) {
                String oldName = tempFile.getAbsolutePath();
                File newFile = new File(tempFile.getParent() + "/" + name + DEFAULT_EXTENSION);
                if (!TextUtils.equals(oldName, newFile.getAbsolutePath())) {
                    if (tempFile.renameTo(newFile)) {
                        tempFile = newFile;
                    }
                }
            }
        }
    }

    @Override
    public void onLowMemory() {
        stopRecording();
        super.onLowMemory();
    }

    private void saveFile(int messagePlace) {
        if (audioLength == 0)
            return;
        Uri uri;
        try {
            uri = this.addToMediaDB(tempFile);
        } catch (Exception ex) {
            //Database manipulation failure
            return;
        }
        if (uri == null) {
            return;
        }

        Intent audioIntent = new Intent();
        audioIntent.putExtra(FILE_PATH, tempFile.getAbsolutePath());
        audioIntent.putExtra(AlarmDetails.DETAILS_VOCAL_MESSAGE_PLACE, messagePlace);
        setResult(RESULT_OK, audioIntent);
    }

    private Uri addToMediaDB(File file) {
        ContentValues cv = new ContentValues();
        long current = System.currentTimeMillis();
        long modDate = file.lastModified();
        Date date = new Date(current);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss aa", Locale.getDefault());
        String title = formatter.format(date);
        long audioLengthMillis = audioLength * 1000L;

        /* Lets label the recorded audio file as NON-MUSIC so that the file
         won't be displayed automatically, except for in the playlist */
        cv.put(MediaStore.Audio.Media.IS_MUSIC, "0");

        cv.put(MediaStore.Audio.Media.TITLE, title);
        cv.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
        cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
        cv.put(MediaStore.Audio.Media.DURATION, audioLengthMillis);
        cv.put(MediaStore.Audio.Media.MIME_TYPE, "audio/amr");
        cv.put(MediaStore.Audio.Media.ARTIST, "");
        cv.put(MediaStore.Audio.Media.ALBUM, "");
        Log.d(TAG, "Inserting audio record: " + cv.toString());
        ContentResolver resolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.d(TAG, "ContentURI: " + base);
        Uri result = resolver.insert(base, cv);
        if (result == null) {
            Log.w(TAG, "Error saving the file");
            return null;
        }

        /* Notify those applications such as Music listening to the
         scanner events that a recorded audio file just created.*/
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
        return result;
    }

    private void showErrorDialog(boolean isStorageFull) {

        ContextThemeWrapper wrapper = new ContextThemeWrapper(Recorder.this, R.style.AlertDialogStyle);
        AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
        dialog.setMessage(isStorageFull ? getString(R.string.storage_full) : getString(R.string.sd_not_available));
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        DialogSupervisor.setDialog(dialog);
        dialog.show();
    }

    private void startPlaying(String path){

        if (!new File(path).exists()) {
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
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
                return true;
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
            }
        });

        try {
            play.setVisibility(View.INVISIBLE);
            pause.setVisibility(View.VISIBLE);
            mediaPlayer.setDataSource(path);
            mediaPlayer.setLooping(false);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.prepare();
            mediaPlayer.start();
            timer = new CountDownTimer(audioLength * 1000L, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secs = (int) (millisUntilFinished / 1000) % 60;
                    int mins = (int) ((millisUntilFinished / 1000) / 60) % 60;
                    time.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                }

                @Override
                public void onFinish() {
                    time.setText(String.format(Locale.getDefault(), "%02d:%02d", 0, 0));
                    play.setVisibility(View.VISIBLE);
                    pause.setVisibility(View.INVISIBLE);
                }
            };
            timer.start();
        } catch (Exception ex) {
            try {
                // Reset the media player to clear error state
                mediaPlayer.reset();
            } catch (Exception ignored) {}
            play.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
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
    protected void onDestroy() {
        if (timer!=null) {
            timer.cancel();
        }
        recorderHandler.removeCallbacks(timeRunnable);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (System.currentTimeMillis() - mLastClickTime < 300) {
            // in order to avoid user clicking too quickly
            return;
        }

        switch (v.getId()) {
            case R.id.recorder_start:
                if (CommonUtils.isMOrLater()) {
                    if (ContextCompat.checkSelfPermission(Recorder.this,
                            Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(Recorder.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                Clock.REQUEST_PERMISSION_MICROPHONE);
                    } else {
                        startRecording();
                    }
                } else {
                    startRecording();
                }
                break;

            case R.id.recorder_stop:
                stop.setVisibility(View.INVISIBLE);
                play.setVisibility(View.VISIBLE);
                saveAudio.setVisibility(View.VISIBLE);
                stopRecording();
                break;

            case R.id.recorder_play:
                startPlaying(tempFile.getAbsolutePath());
                break;

            case R.id.recorder_pause:
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
                stopPlaying();
                time.setText(String.format(Locale.getDefault(), "%02d:%02d", 0, 0));
                break;

            case R.id.recorded_audio:
                if (doFilesExist()) {
                    launchAudioPicker();
                }
                break;

            case R.id.save_audio:
                saveAudioDialog();
                break;
        }

        mLastClickTime = System.currentTimeMillis();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        stopRecording();
    }
}

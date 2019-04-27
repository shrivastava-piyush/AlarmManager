package com.bytezap.wobble.alarm;

import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.ShakeEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class ShakeFragment extends Fragment implements ShakeEventListener.ShakeListener {

    private TextView shakeInstruction, shakeIndicator, shakeTitle;
    private AlarmInstance instance;
    private Context mContext;
    private int counter = 0;
    private int maxShake = 30;
    private boolean isMusicStopped = false;
    private SensorManager sManager;
    private ShakeEventListener sensorListener;
    private AlarmScreenInterface alarmListener;
    private Handler shakeHandler = new Handler();
    private Runnable shakeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMusicStopped) {
                AlarmTone.resume(mContext, instance.isVibrate);
                isMusicStopped = false;
            }
        }
    };

    public static ShakeFragment newInstance() {
        return new ShakeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.shake_layout, container, false);
        shakeInstruction = rootView.findViewById(R.id.shake_instruction);
        shakeIndicator = rootView.findViewById(R.id.shake_counter);
        shakeTitle = rootView.findViewById(R.id.shake_title);
        final ImageButton backButton = rootView.findViewById(R.id.shake_back);

        mContext = ShakeFragment.this.getActivity().getApplicationContext();
        sManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        sensorListener = new ShakeEventListener(this);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long id = getActivity().getIntent().getLongExtra(AlarmAssistant.ID, -1);
        instance = AlarmAssistant.getInstance(mContext, id);

        if (instance !=null && alarmListener!=null) {
            boolean isDismissFrag = alarmListener.isDismissFrag();
            maxShake = isDismissFrag ? instance.dismissShake : instance.snoozeShake;
            shakeTitle.setText(isDismissFrag ? R.string.shake_turn_off : R.string.shake_snooze);
        }
        shakeInstruction.setText(getString(R.string.shake_times, maxShake));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            alarmListener = (AlarmScreenInterface) getActivity();
        } catch (Exception ignored) {
            Log.e("MathGame", "Listener could be implemented");
        }
    }

    private void registerShakeSensor(){
        if (sManager!=null) {
            sManager.registerListener(sensorListener,
                    sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterShakeSensor(){
        sManager.unregisterListener(sensorListener);
    }

    @Override
    public void onShake() {
        counter++;
        if (instance !=null) {
            shakeIndicator.setText(String.valueOf(counter));
            if (counter == maxShake) {
                dismiss();
            }
        } else {
            //Unnecessary optimization. Find a better way later
            shakeIndicator.setText(String.valueOf(counter));
            if (counter == 30) {
                AlarmTone.terminate(mContext);
                getActivity().finish();
            }
        }
        if (!AlarmTone.isToneNotRunning() && !isMusicStopped) {
            isMusicStopped = true;
            AlarmTone.pause(mContext);
            shakeHandler.postDelayed(shakeRunnable, 3500);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterShakeSensor();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerShakeSensor();
    }

    private void dismiss() {
        if (instance !=null && alarmListener != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlarmTone.terminate(mContext);
                    isMusicStopped = false;
                    CommonUtils.sendAlarmBroadcast(mContext, instance.id, alarmListener.isDismissFrag() ? AlarmService.ALARM_DISMISS : AlarmService.ALARM_SNOOZE);
                    getActivity().finish();
                }
            });
        } else {
            Log.e(ShakeFragment.class.getSimpleName(), "Alarm or alarmListener was null");
            AlarmTone.terminate(mContext);
            getActivity().finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shakeHandler.removeCallbacks(shakeRunnable);
    }

    private void closeFragment() {
        getActivity().getFragmentManager().beginTransaction().remove(ShakeFragment.this).commit();
        SlidingUpPanelLayout sLayout = getActivity().findViewById(R.id.sliding_layout);
        sLayout.setVisibility(View.VISIBLE);
        sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        getActivity().findViewById(R.id.alarmScreen_layout).setVisibility(View.VISIBLE);
    }
}

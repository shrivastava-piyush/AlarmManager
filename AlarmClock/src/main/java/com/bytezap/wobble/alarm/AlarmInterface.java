package com.bytezap.wobble.alarm;

import android.view.View;

interface AlarmInterface {

     void toggleAlarm(int position, boolean isEnabled);

     void selectTime(View view, int position);
}

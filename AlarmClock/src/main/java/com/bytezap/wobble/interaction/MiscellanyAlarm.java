/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.interaction;

public class MiscellanyAlarm {

    private String time;
    private String repeat;

    public MiscellanyAlarm(String alarmTime, String alarmRepeat) {
        this.time = alarmTime;
        this.repeat = alarmRepeat;
    }

    public String getTime()
    {
        return time;
    }

    public String getRepeat()
    {
        return repeat;
    }

    public void setTime(String alarmTime)
    {
        time = alarmTime;
    }

    public void setRepeat(String alarmRepeat)
    {
        repeat = time;
    }

}

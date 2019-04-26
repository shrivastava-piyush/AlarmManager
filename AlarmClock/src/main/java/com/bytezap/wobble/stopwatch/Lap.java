package com.bytezap.wobble.stopwatch;

import android.view.View;
import android.widget.ListView;

class Lap {
    private int lapCounter;
    private String lapTime;

    Lap(int lapNumber, String lap) {
        this.lapCounter = lapNumber;
        this.lapTime = lap;
    }

    int getLapCounter()
    {
        return lapCounter;
    }

    String getLapTime()
    {
        return lapTime;
    }

    public void setLapCounter(int count)
    {
        lapCounter = count;
    }

    void setLapTime(String time)
    {
        lapTime = time;
    }

    void updateView(LapAdapter adapter, ListView laps) {
        View lapView = laps.findViewWithTag(this);
        if (lapView != null) {
            adapter.updateLapText(lapView, this);
        }
    }

}


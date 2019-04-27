package com.bytezap.wobble.stopwatch;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.theme.ThemeDetails;
import com.bytezap.wobble.utils.BitmapController;

import java.util.ArrayList;

class LapAdapter extends ArrayAdapter<Lap> {

    private LayoutInflater layoutInflater;
    private Context context;
    private ArrayList<Lap> list;

    LapAdapter(Context c, ArrayList<Lap> mList) {
        super(c, 0, mList);
        context = c;
        layoutInflater = LayoutInflater.from(context);
        list = mList;
    }

    @Override
    public Lap getItem(int position) {
        if (list!=null) {
           return list.get(position);
        }
        return null;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public int getCount() {
        return list.isEmpty() ? 0 : list.size();
    }

    @NonNull
    @Override
    public View getView(int position, View lapView, @NonNull ViewGroup parent) {

        if (lapView == null) {
            lapView = layoutInflater.inflate(R.layout.lap_list_item, null);
        }

        Lap lap = getItem(position);
        lapView.setTag(lap);
        String s = context.getString(R.string.lap_text) + Integer.toString(lap != null ? lap.getLapCounter() : 0) + ": ";
        TextView lapNumber = lapView.findViewById(R.id.lapNum);
        lapNumber.setText(s);
        TextView lapTime = lapView.findViewById(R.id.lapTime);
        lapTime.setShadowLayer(1.5f, -1.5f, 1.5f, Color.GRAY);
        updateLapText(lapView, lap);

        return lapView;
    }

    void updateLapText(View view, Lap lap){
        TextView lapTime = view.findViewById(R.id.lapTime);
        lapTime.setText(lap.getLapTime());
    }

}

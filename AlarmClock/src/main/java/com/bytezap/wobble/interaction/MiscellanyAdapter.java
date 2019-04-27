/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.interaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bytezap.wobble.R;

public class MiscellanyAdapter extends ArrayAdapter<MiscellanyAlarm> {

    private LayoutInflater layoutInflater;

    public MiscellanyAdapter(Context context, int resource) {
        super(context, resource);
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getCount() == 0) {
            return null;
        }
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.miscellany_item, null);
        }

        MiscellanyAlarm alarm = getItem(position);
        TextView time = convertView.findViewById(R.id.miscellany_time);
        TextView repeat = convertView.findViewById(R.id.miscellany_repeat);
        time.setText(alarm.getTime());
        repeat.setText(alarm.getRepeat());
        return convertView;

    }
}

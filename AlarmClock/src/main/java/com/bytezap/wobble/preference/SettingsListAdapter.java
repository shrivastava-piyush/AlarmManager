/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.preference;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytezap.wobble.R;

import java.util.List;

class SettingsListAdapter extends BaseAdapter {

    private List<PrefName> nameList;
    private LayoutInflater inflater;

    SettingsListAdapter(Activity context, List<PrefName> list) {
        super();
        this.nameList = list;
        inflater = context.getLayoutInflater();
    }

    private class ViewHolder {
        TextView name;
        ImageView icon;
    }

    public int getCount() {
        return nameList.size();
    }

    public Object getItem(int position) {
        if (nameList != null) {
            return nameList.get(position);
        }
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.settings_list_item, null);

            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.settingsName);
            holder.icon = convertView.findViewById(R.id.settingsIcon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PrefName object = (PrefName) getItem(position);
        holder.icon.setImageDrawable(object.getIcon());
        holder.name.setText(object.getName());

        return convertView;
    }

}
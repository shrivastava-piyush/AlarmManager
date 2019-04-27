/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.alarm;

import java.util.List;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytezap.wobble.R;

class AppListAdapter extends BaseAdapter {
 
    private List<App> packageList;
    private LayoutInflater inflater;

    public AppListAdapter(Activity context, List<App> packageList) {
        super();
        this.packageList = packageList;
        inflater = context.getLayoutInflater();
    }
 
    private class ViewHolder {
        TextView appName;
        ImageView appIcon;
    }
 
    public int getCount() {
        return packageList.size();
    }
 
    public Object getItem(int position) {
        if (packageList!=null) {
            return packageList.get(position);
        }
        return null;
    }
 
    public long getItemId(int position) {
        return 0;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.app_list_item, null);

            holder = new ViewHolder();
            holder.appName = convertView.findViewById(R.id.launchAppName);
            holder.appIcon = convertView.findViewById(R.id.launchAppIcon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        App app = (App) getItem(position);
        holder.appIcon.setImageDrawable(app.getAppIcon());
        holder.appName.setText(app.getAppLabel());
 
        return convertView;
    }

    public void updateList(List<App> list){
        this.packageList = list;
        notifyDataSetChanged();

    }
}
/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.timer;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.database.PresetObject;

import java.util.List;
import java.util.Locale;

class PresetAdapter extends ArrayAdapter<PresetObject>{

    private LayoutInflater layoutInflater;
    private PresetHolder holder;
    private PresetInterface listener;
    private Context context;
    private List<PresetObject> presets;

    PresetAdapter(Context c, List<PresetObject> mList, PresetInterface listener) {
        super(c, 0, mList);
        this.context = c;
        this.layoutInflater = LayoutInflater.from(context);
        this.presets = mList;
        this.listener = listener;
    }

    @Nullable
    @Override
    public PresetObject getItem(int position) {
        if (presets !=null) {
            return presets.get(position);
        }
        return null;
    }

    @Override
    public int getCount() {
        return presets != null ? presets.size() : 0;
    }

    @Override
    public void remove(PresetObject object) {
        if (presets != null) {
            presets.remove(object);
            notifyDataSetChanged();
        }
    }

    @Override
    public void add(PresetObject object) {
        if (presets != null) {
            presets.add(object);
            notifyDataSetChanged();
        }
    }

    void setPresets(List<PresetObject> presets){
        this.presets = presets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View presetView, @NonNull ViewGroup parent) {

        if (presetView == null) {
            presetView = layoutInflater.inflate(R.layout.timer_item, null);
            setHolder(presetView);
        } else {
            Object tag = presetView.getTag();
            if (tag != null) {
                holder = (PresetHolder) tag;
            } else {
                setHolder(presetView);
            }
        }

        holder.preset = getItem(position);
        if (holder.preset == null) {
            return presetView;
        }

        holder.presetLayout.setTag(position);
        holder.edit.setTag(position);
        holder.delete.setTag(position);
        holder.start.setTag(position);

        holder.name.setText(!TextUtils.isEmpty(holder.preset.name) ? holder.preset.name : context.getString(R.string.default_timer_text) + " " + (position + 1));

        String hourFormat = holder.preset.hours >= 100 ? "%03d" : "%02d";
        holder.time.setText(String.format(Locale.getDefault(), hourFormat+":%02d:%02d", holder.preset.hours, holder.preset.minutes, holder.preset.seconds));
        holder.time.setShadowLayer(1.5f, -1.5f, 1.5f, Color.GRAY);

        return presetView;
    }

    private void setHolder(View view){
        holder = new PresetHolder();
        holder.name = view.findViewById(R.id.presetName);
        holder.time = view.findViewById(R.id.presetTime);
        holder.presetLayout = view.findViewById(R.id.preset_layout);
        holder.edit = view.findViewById(R.id.preset_edit);
        holder.delete = view.findViewById(R.id.preset_delete);
        holder.start = view.findViewById(R.id.preset_start);

        holder.presetLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    listener.setPresetTime((int) v.getTag());
                }
            }
        });

        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    listener.editPreset((int) v.getTag());
                }
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    listener.deletePreset((int) v.getTag());
                }
            }
        });

        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null) {
                    listener.startPreset((int) v.getTag());
                }
            }
        });
        view.setTag(holder);
    }

    private class PresetHolder {
        private TextView name;
        private TextView time;
        private LinearLayout presetLayout;
        private ImageButton edit;
        private ImageButton delete;
        private ImageButton start;

        private PresetObject preset;
    }
}

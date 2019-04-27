/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytezap.wobble.R;

public class TweakedSwitch extends LinearLayout {

	private TextView label;
	private TextView summary;
	private CheckBox button;

	public TweakedSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
        init(context, attrs);
	}

    private void init(Context context, AttributeSet attrs){

        final View rootView = View.inflate(context, R.layout.tweaked_switch, this);
        label = rootView.findViewById(R.id.tweaked_title);
        summary = rootView.findViewById(R.id.tweaked_summary);
        button = rootView.findViewById(R.id.tweaked_checkbox);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TweakedSwitch);
        boolean isVisible = typedArray.getBoolean(R.styleable.TweakedSwitch_isSummaryVisible, false);
        boolean isChecked = typedArray.getBoolean(R.styleable.TweakedSwitch_checked, false);
        String labelText = typedArray.getString(R.styleable.TweakedSwitch_text);
        String summaryText = typedArray.getString(R.styleable.TweakedSwitch_summary);

        try {
            setText(labelText);
            setSummary(summaryText);
            setChecked(isChecked);
            summary.setVisibility(isVisible ? VISIBLE : GONE);
        } finally {
            typedArray.recycle();
        }
    }

	public void setText(String text) {
		label.setText(text);
	}

    public String getText() {
		return label.getText().toString();
	}

	public void setSummary(String text) {
		summary.setText(text);
	}

    public String getSummary() {
        return summary.getText().toString();
    }

	public void setChecked(boolean isChecked) {
		button.setChecked(isChecked);
        label.setTextColor(isChecked ? Color.WHITE : Color.GRAY);
        summary.setTextColor(isChecked ? Color.WHITE : Color.GRAY);
	}

	public boolean isChecked() {
        return button.isChecked();
    }

    public void toggle(){
        button.toggle();
        label.setTextColor(button.isChecked() ? Color.WHITE : Color.GRAY);
        summary.setTextColor(button.isChecked() ? Color.WHITE : Color.GRAY);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isChecked = isChecked();
        ss.label = getText();
        ss.summary = getSummary();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.isChecked);
        if (!TextUtils.isEmpty(ss.summary)) {
            setSummary(ss.summary);
        }
        requestLayout();
    }

    public static class SavedState extends BaseSavedState {
        boolean isChecked;
        String label;
        String summary;


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isChecked = (in.readInt() != 0);
            label = in.readString();
            summary = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flag) {
            super.writeToParcel(out, flag);
            out.writeInt(isChecked ? 1 : 0);
            out.writeString(label);
            out.writeString(summary);
        }

        public static final Creator CREATOR = new Creator() {
            public SavedState createFromParcel(final Parcel inParcel) {
                return new SavedState(inParcel);
            }

            public SavedState[] newArray(final int inSize) {
                return new SavedState[inSize];
            }
        };
    }
}

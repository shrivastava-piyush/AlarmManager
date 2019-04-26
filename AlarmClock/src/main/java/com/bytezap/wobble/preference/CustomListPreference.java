package com.bytezap.wobble.preference;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CustomListPreference extends ListPreference {

	public CustomListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onBindView( View view) {
		super.onBindView(view);
		TextView t = view.findViewById(android.R.id.title);
        t.setSingleLine(false);
        t.setEllipsize(null);
        t.setMaxLines(2);
	}
}
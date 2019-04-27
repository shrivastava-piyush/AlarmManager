package com.bytezap.wobble.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bytezap.wobble.R;
import com.bytezap.wobble.theme.ThemeDetails;

public class ToastGaffer {

    private static Toast currentToast = null;

    public static void showToast(Context context, String toastText) {
        showToast(context, toastText, false);
    }

    public static void showToast(Context context, String toastText, boolean isToastLong){
        if (currentToast != null)
            currentToast.cancel();

        Toast toast = new Toast(context);
        currentToast = toast;
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toastLayout = li.inflate(R.layout.custom_toast, null);
        boolean toastDark = ThemeDetails.isToastDark(BitmapController.getThemeNumber());
        toastLayout.setBackgroundResource(toastDark ? R.drawable.toast_bg : R.drawable.toast_white_bg);
        TextView toastTextView = toastLayout.findViewById(R.id.custom_toast_layout);
        toastTextView.setTextColor(toastDark ? Color.WHITE : Color.BLACK);
        toastTextView.setText(toastText);
        toast.setDuration(isToastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setView(toastLayout);
        toast.show();
    }

    public static void showWhiteToast(Context context, String toastText, boolean isToastLong){
        if (currentToast != null)
            currentToast.cancel();

        Toast toast = new Toast(context);
        currentToast = toast;
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toastLayout = li.inflate(R.layout.custom_toast, null);
        toastLayout.setBackgroundResource(R.drawable.toast_white_bg);
        TextView toastTextView = toastLayout.findViewById(R.id.custom_toast_layout);
        toastTextView.setTextColor(Color.BLACK);
        toastTextView.setText(toastText);
        toast.setDuration(isToastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.setView(toastLayout);
        toast.show();
    }

    public static void cancelPreviousToast() {
        if (currentToast != null)
            currentToast.cancel();
        currentToast = null;
    }

}

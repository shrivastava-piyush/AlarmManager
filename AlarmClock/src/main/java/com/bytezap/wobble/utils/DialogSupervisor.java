package com.bytezap.wobble.utils;

import android.app.AlertDialog;

public class DialogSupervisor {
    private static AlertDialog currentDialog = null;

    private DialogSupervisor(){}

    // Prevent more than one instance of any dialog to popup
    public static void setDialog(AlertDialog dialog) {
        if (currentDialog != null){
            try{
                currentDialog.cancel();
            } catch (Exception ignored){}
        }

        currentDialog = dialog;

    }

    public static void cancelDialog() {
        if (currentDialog != null){
            try{
                currentDialog.cancel();
            } catch (Exception ignored){}
            currentDialog = null;
        }
    }
}

package com.bytezap.wobble.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.bytezap.wobble.Clock;

public class LicenceController {

    private static boolean checkIAPLicense;
    private static boolean isAdFirstTime;

    static {
        checkIAPLicense = false;
        isAdFirstTime  = true;
    }

    public static boolean checkLicense(Context context) {
        SharedPreferences mainPrefs = context.getSharedPreferences(Clock.MAIN_PREF, Context.MODE_PRIVATE);
        String modeNoAd = mainPrefs.getString("modeEmancipated", "0");
        return modeNoAd.equals(getEmancipatedString()) || getCheckIAPLicense();
    }

    public static String getEmancipatedString(){
        return getFirstHalf() + getSecondHalf();
    }

    private static String getFirstHalf(){
        return "7b";
    }

    private static String getSecondHalf(){
        return "m7";
    }

    public static boolean getCheckIAPLicense(){
        return checkIAPLicense;
    }

    public static void setCheckIAPLicense(boolean isLicensed){
        checkIAPLicense = isLicensed;
    }

    public static boolean isIsAdFirstTime(){
        return isAdFirstTime;
    }

    public static void setIsAdFirstTime(boolean isAdFirstTime) {
        LicenceController.isAdFirstTime = isAdFirstTime;
    }
}

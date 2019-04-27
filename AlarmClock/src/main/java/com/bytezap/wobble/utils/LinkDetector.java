package com.bytezap.wobble.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class LinkDetector {

    private Context mContext;

    public LinkDetector(Context context){
        this.mContext = context;
    }

    public boolean isNetworkAvailable(){
        ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
            return  (activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting());
        }
        return false;
    }
}

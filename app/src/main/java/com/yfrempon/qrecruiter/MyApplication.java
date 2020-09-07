package com.yfrempon.qrecruiter;

import android.app.Application;
import android.content.Context;

//Class for referencing the current 'context' of an activity

public class MyApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
    
    public static void setContext(Context mContext) {
        context = mContext;
    }
}

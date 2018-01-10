package com.com.jumptool;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class Settings {
    public static final String KEY_RUNNING = "key_running";
    public static final String KEY_RATE = "key_rate";
    public static final String KEY_VIBRATE = "key_vibrate";

    public static final float DEFAULT_RATE = 0.26f;

    private boolean mIsInit = false;
    private boolean mVibrateEnable;
    private float mRate;
    private static Settings sInstance = new Settings();

    private Settings(){

    }

    public synchronized float getRate(){
        return mRate;
    }

    public synchronized boolean isVibrateEnable(){
        return mVibrateEnable;
    }

    public synchronized static Settings getInstance(){
        return sInstance;
    }

    public synchronized void init(Context context){
        if(mIsInit){
            return;
        }
        mIsInit = true;
        sync(context);
    }


    public synchronized void sync(Context context){
        if(context == null){
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        mVibrateEnable = sp.getBoolean(KEY_VIBRATE, false);
        mRate = Float.valueOf(sp.getString(KEY_RATE, String.valueOf(DEFAULT_RATE)));
        if(mRate <= 0){
            mRate = DEFAULT_RATE;
        }
    }

}

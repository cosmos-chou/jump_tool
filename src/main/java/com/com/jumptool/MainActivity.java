package com.com.jumptool;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import com.com.jumptools.R;

public class MainActivity extends PreferenceActivity{

    Intent mServiceIntent;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setup();
    }

    private void setup(){
        mServiceIntent = new Intent(this, JumpTools.class);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        sp.registerOnSharedPreferenceChangeListener(mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                System.out.println("preference change " + key);
                if(TextUtils.equals(Settings.KEY_RUNNING, key)){
                    boolean running = sp.getBoolean(Settings.KEY_RUNNING, false);
                    if(running){
                        startService(mServiceIntent);
                    }else{
                        stopService(mServiceIntent);
                    }
                }else {
                    Settings.getInstance().sync(MainActivity.this);
                }
            }
        });
        sp.edit().putString(Settings.KEY_RATE, sp.getString(Settings.KEY_RATE, String.valueOf(Settings.DEFAULT_RATE))).commit();
        Settings.getInstance().init(this);
        boolean running = sp.getBoolean(Settings.KEY_RUNNING, false);
        if(running){
            startService(mServiceIntent);
        }
        View view = getListView();
        getListView().setPadding(view.getPaddingLeft(), 200, view.getPaddingRight(), view.getPaddingBottom());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListener = null;
    }
}

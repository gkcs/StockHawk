package com.sam_chordas.android.stockhawk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferenceManager {

    static SharedPreferences pref;
    static SharedPreferences.Editor editor;
    static Context context;

    public static final String KEY_GCM_STATUS = "gcm_status";
    public static final String KEY_SHOW_PERCENTAGE = "show_percentage";

    // Constructor
    public SharedPreferenceManager(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = pref.edit();
        this.context = context;
    }

    public static SharedPreferenceManager init(Context context) {
        return new SharedPreferenceManager(context);
    }

    public static void setGCMStatus(boolean bool) {
        init(context);
        editor.putBoolean(KEY_GCM_STATUS, bool);
        editor.commit();
    }

    public static boolean getGCMStatus() {
        init(context);
        return pref.getBoolean(KEY_GCM_STATUS, true);
    }

    public static void setShowPercentage(boolean bool) {
        init(context);
        editor.putBoolean(KEY_SHOW_PERCENTAGE, bool);
        editor.commit();
    }

    public static boolean getShowPercentage() {
        init(context);
        return pref.getBoolean(KEY_SHOW_PERCENTAGE, true);
    }
}
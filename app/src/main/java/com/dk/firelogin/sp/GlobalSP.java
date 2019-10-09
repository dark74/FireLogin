package com.dk.firelogin.sp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;


public class GlobalSP {
    private static GlobalSP instance = null;
    private Context mContext;
    private String SP_FILE_NAME = "global";
    private SharedPreferences preferences = null;
    private SharedPreferences.Editor mEditor = null;

    /**
     * sp中存储字段key
     **/
    public static String LAST_SUBMIT_LOCATION = "last_submit_location";
    public static String TODAY_SIGNIN = "today_signin";
    public static String LAST_SINGIN_DATE = "last_sing_date";
    public static String LAST_OPEN_DATE = "last_open_date";


    private GlobalSP(Context context) {
        if (context != null) {
            mContext = context;
        }

        if (preferences == null) {
            preferences = mContext.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
            mEditor = preferences.edit();
        }

    }

    public static GlobalSP getInstance(Context context) {
        if (instance == null) {
            synchronized (GlobalSP.class) {
                if (instance == null) {
                    instance = new GlobalSP(context);
                }
            }
        }
        return instance;
    }

    // boolean
    public void setBoolean(String key, boolean value) {
        if (mEditor != null) {
            mEditor.putBoolean(key, value);
        }
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    //String
    public void setString(String key, String value) {
        if (mEditor != null) {
            mEditor.putString(key, value);
            mEditor.commit();
        }
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    // int
    public void setInt(String key, int value) {
        if (mEditor != null) {
            mEditor.putInt(key, value);
            mEditor.commit();
        }
    }

    public int getInt(String key) {
        return preferences.getInt(key, -1);
    }

    // float
    public void setFloat(String key, float value) {
        if (mEditor != null) {
            mEditor.putFloat(key, value);
            mEditor.commit();
        }
    }

    public float getFloat(String key) {
        return preferences.getFloat(key, 0f);
    }

    // long
    public void setLong(String key, long value) {
        if (mEditor != null) {
            mEditor.putLong(key, value);
            mEditor.commit();
        }
    }

    public long getLong(String key) {
        return preferences.getLong(key, 0l);
    }

    //stringSet
    public void setStringSet(String key, Set<String> values) {
        if (mEditor != null) {
            mEditor.putStringSet(key, values);
            mEditor.commit();
        }
    }

    public Set<String> getStringSet(String key) {
        return preferences.getStringSet(key, null);
    }

}

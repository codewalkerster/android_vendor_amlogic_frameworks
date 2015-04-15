/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droidlogic;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.UEventObserver;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.lang.reflect.Method;

import com.droidlogic.app.SystemControlManager;

/**
 * Stub implementation of (@link android.media.tv.TvInputService}.
 */
public class CecService extends Service {
    private static final boolean DEBUG = true;
    private static final String TAG = "HdmiCecOutput";
    private static final String SWITCH_STATE = "SWITCH_STATE";
    private static final String PREFERENCE_BOX_SETTING = "preference_box_settings";
    private static final String CEC_SYS = "/sys/class/amhdmitx/amhdmitx0/cec_config";
    private static final String CEC_DEVICE_FILE = "/sys/devices/virtual/switch/lang_config/state";
    private static final String SWITCH_AUTO_CHANGE_LANGUAGE = "switch_auto_change_languace";
    private static final String SWITCH_ON = "true";
    private static final String SWITCH_OFF = "false";
    private static final int FUN_AUTO_CHANGE_LANGUAGE = 0x03;

    private boolean startObServing = false;
    private String cec_config_path = "DEVPATH=/devices/virtual/switch/lang_config";
    private SystemControlManager mSystemControlManager;

    public CecService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startListenCecDev();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        mSystemControlManager = new SystemControlManager(this);
        super.onCreate();
    }

    public void startListenCecDev() {
        Log.d(TAG, "startListenCecDev()");
        if (new File(CEC_DEVICE_FILE).exists() && !startObServing) {
            mCedObserver.startObserving(cec_config_path);
            startObServing = true;
        }
    }

    public UEventObserver mCedObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            if (DEBUG) Log.d(TAG, "onUEvent()");

            String mNewLanguage = event.get(SWITCH_STATE);
            if (DEBUG) Log.d(TAG, "get the language code is : " + mNewLanguage);

            if (!isChangeLanguageOpen()) {
                if (DEBUG) Log.d(TAG, "cec language not open");
                return;
            }
            int i = -1;
            String[] cec_language_list = getResources().getStringArray(R.array.cec_language);
            for (int j = 0; j < cec_language_list.length; j++) {
                if (mNewLanguage != null && mNewLanguage.trim().equals(cec_language_list[j])) {
                    i = j;
                    break;
                }
            }
            if (i >= 0) {
                String able = getResources().getConfiguration().locale.getCountry();
                String[] language_list = getResources().getStringArray(R.array.language);
                String[] country_list = getResources().getStringArray(R.array.country);
                if (able.equals(country_list[i])) {
                    if (DEBUG) Log.d(TAG, "no need to change language");
                    return;
                } else {
                    Locale l = new Locale(language_list[i], country_list[i]);
                    if (DEBUG) Log.d(TAG, "change the language right now !!!");
                    updateLanguage(l);
                }
            } else {
                Log.d(TAG, "the language code is not support right now !!!");
            }
        }
    };

    public void updateLanguage(Locale locale) {
        try {
            Object objIActMag;
            Class clzIActMag = Class.forName("android.app.IActivityManager");
            Class clzActMagNative = Class.forName("android.app.ActivityManagerNative");
            Method mtdActMagNative$getDefault = clzActMagNative.getDeclaredMethod("getDefault");

            objIActMag = mtdActMagNative$getDefault.invoke(clzActMagNative);
            Method mtdIActMag$getConfiguration = clzIActMag.getDeclaredMethod("getConfiguration");
            Configuration config = (Configuration) mtdIActMag$getConfiguration.invoke(objIActMag);
            config.locale = locale;

            Class[] clzParams = { Configuration.class };
            Method mtdIActMag$updateConfiguration = clzIActMag.getDeclaredMethod("updateConfiguration", clzParams);
            mtdIActMag$updateConfiguration.invoke(objIActMag, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isChangeLanguageOpen(){
        SharedPreferences sharedpreference = getSharedPreferences(PREFERENCE_BOX_SETTING, Context.MODE_PRIVATE);
        String isCecLanguageOpen = sharedpreference.getString(SWITCH_AUTO_CHANGE_LANGUAGE, SWITCH_OFF);
        if (isCecLanguageOpen.equals(SWITCH_ON)) {
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        if (startObServing) {
            mCedObserver.stopObserving();
        }
        super.onDestroy();
    }
}

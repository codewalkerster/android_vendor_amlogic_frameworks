
package com.droidlogic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.droidlogic.app.UsbCameraManager;

public class BootComplete extends BroadcastReceiver {
    private static final String TAG             = "BootComplete";
    private static final String FIRST_RUN       = "first_run";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            //set default show_ime_with_hard_keyboard 1, then first boot can show the ime.
            if (getFirstRun(context)) {
                Log.i(TAG, "first running: " + context.getPackageName());
                try {
                    Settings.Secure.putInt(context.getContentResolver(),
                            Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 1);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "could not find hard keyboard ", e);
                }

                setFirstRun(context, false);
            }

            //use to check whether disable camera or not
            new UsbCameraManager(context).bootReady();
            context.startService(new Intent(context, HdmiService.class));
            cecLanguageCheck(context);
        }
    }

    public void setFirstRun(Context c, boolean firstRun){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(FIRST_RUN, firstRun);
        editor.commit();
    }

    public boolean getFirstRun(Context c){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getBoolean(FIRST_RUN, true);
    }

    public void cecLanguageCheck(Context context){
        Intent serviceIntent = new Intent(context, CecService.class);
        serviceIntent.setAction("CEC_LANGUAGE_AUTO_SWITCH");
        context.startService(serviceIntent);
    }
}


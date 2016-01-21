
package com.droidlogic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.droidlogic.app.PlayBackManager;
import com.droidlogic.app.UsbCameraManager;
import com.droidlogic.HdmiCecExtend;

public class BootComplete extends BroadcastReceiver {
    private static final String TAG             = "BootComplete";
    private static final String FIRST_RUN       = "first_run";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            //set default show_ime_with_hard_keyboard 1, then first boot can show the ime.
            if (SettingsPref.getFirstRun(context)) {
                Log.i(TAG, "first running: " + context.getPackageName());
                try {
                    Settings.Secure.putInt(context.getContentResolver(),
                            Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 1);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "could not find hard keyboard ", e);
                }

                SettingsPref.setFirstRun(context, false);
            }

            //use to check whether disable camera or not
            new UsbCameraManager(context).bootReady();

            new PlayBackManager(context).initHdmiSelfadaption();

            new HdmiCecExtend(context);

            //start optimization service
            context.startService(new Intent(context, Optimization.class));
        }
    }
}


package com.droidlogic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.UEventObserver;
import android.util.Log;

import com.droidlogic.app.OutputModeManager;
import com.droidlogic.app.SystemControlManager;

public class HdmiService extends Service  {
    private static final String TAG = "HdmiService";

    /**
     * Sticky broadcast of the current HDMI plugged state.
     */
    public final static String ACTION_HDMI_PLUGGED              = "android.intent.action.HDMI_PLUGGED";

    /**
     * Extra in {@link #ACTION_HDMI_PLUGGED} indicating the state: true if
     * plugged in to HDMI, false if not.
     */
    public final static String EXTRA_HDMI_PLUGGED_STATE         = "state";

    /**
     *  broadcast of the current HDMI output mode changed.
     */
    public final static String ACTION_HDMI_MODE_CHANGED         = "android.intent.action.HDMI_MODE_CHANGED";

    /**
     * Extra in {@link #ACTION_HDMI_MODE_CHANGED} indicating the mode:
     */
    public final static String EXTRA_HDMI_MODE                  = "mode";

    private static final String VIDEO2_CTRL_PATH                = "/sys/class/video2/clone";
    private static final String VFM_CTRL_PATH                   = "/sys/class/vfm/map";
    private static final String DISPLAY_MODE_PATH               = "/sys/class/display/mode";

    private Context mContext;
    private OutputModeManager mOutputMode;
    private SystemControlManager mSystemControl;

    private boolean mHdmiHwPlugged = false;
    private boolean mHdmiPlugged = false;

    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            boolean plugged = intent.getBooleanExtra(EXTRA_HDMI_PLUGGED_STATE, false);

            Log.i(TAG, "onReceive " + ACTION_HDMI_PLUGGED + " state:" + plugged);
            setHdmiPlugged(plugged);
        }
    };

    @Override
    public void onCreate() {
        mContext = this;

        Log.i(TAG, "onCreate ");

        mOutputMode = new OutputModeManager(this);
        mSystemControl = new SystemControlManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HDMI_PLUGGED);
        Intent intent = mContext.registerReceiver(mBroadcastReceiver, filter);
        if (intent != null) {
            boolean plugged = intent.getBooleanExtra(EXTRA_HDMI_PLUGGED_STATE, false);

            Log.i(TAG, "registerReceiver " + ACTION_HDMI_PLUGGED + " state:" + plugged);
            setHdmiPlugged(plugged);
        }

        initHdmiState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setHdmiPlugged(boolean plugged) {
        if (mHdmiPlugged != plugged) {

            Log.i(TAG, "HDMI state from " + (mHdmiPlugged?"plugged":"unplugged") +
                " -> " + (plugged?"plugged":"unplugged"));

            mHdmiPlugged = plugged;
            if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)){
                if (plugged)
                    mOutputMode.setHdmiPlugged();
                else
                    mOutputMode.setHdmiUnPlugged();
            }
        }
    }

    private void initHdmiState() {
        boolean plugged = false;

        // watch for HDMI plug messages if the hdmi switch exists
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)){
                mOutputMode.initOutputMode();
            }

            /*
            final String filename = "/sys/class/switch/hdmi/state";
            String state = mSystemControl.readSysFs(filename);

            Log.i(TAG, "HDMI plugged :" + state);

            plugged = (0 != Integer.parseInt(state));
            */
        }
    }

    private void setDualDisplay(boolean hdmiPlugged) {
        String isCameraBusy = mSystemControl.getPropertyString("camera.busy", "0");

        if (!isCameraBusy.equals("0")) {
            Log.w(TAG, "setDualDisplay, camera is busy");
            return;
        }

        if (hdmiPlugged) {
            mSystemControl.writeSysFs(VIDEO2_CTRL_PATH, "0");
            mSystemControl.writeSysFs(VFM_CTRL_PATH, "rm default_ext");
            mSystemControl.writeSysFs(VFM_CTRL_PATH, "add default_ext vdin0 amvideo2");
            mSystemControl.writeSysFs(VIDEO2_CTRL_PATH, "1");
        } else {
            mSystemControl.writeSysFs(VIDEO2_CTRL_PATH, "0");
            mSystemControl.writeSysFs(VFM_CTRL_PATH, "rm default_ext");
            mSystemControl.writeSysFs(VFM_CTRL_PATH, "add default_ext vdin vm amvideo");
        }
    }

    private String getCurDisplayMode() {
        String modeStr = "panel";
        modeStr = mSystemControl.readSysFs(DISPLAY_MODE_PATH);
        return (modeStr == null)? "panel" : modeStr;
    }
}

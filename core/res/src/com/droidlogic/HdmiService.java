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
     * Sticky broadcast of the current HDMI hw plugged state.
     */
    public final static String ACTION_HDMI_HW_PLUGGED           = "android.intent.action.HDMI_HW_PLUGGED";

    /**
     * Extra in {@link #ACTION_HDMI_HW_PLUGGED} indicating the state: true if
     * plugged in to HDMI hw, false if not.
     */
    public final static String EXTRA_HDMI_HW_PLUGGED_STATE      = "state";

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

    @Override
    public void onCreate() {
        mContext = this;

        mOutputMode = new OutputModeManager(this);
        mSystemControl = new SystemControlManager(this);

        initHdmiState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        return super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private UEventObserver mHDMIObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            setHdmiHwPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };

    /*
    private UEventObserver mHoldObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            Log.d("PhoneWindownManager","holdkey"+event.get("SWITCH_STATE"));
            try {
                IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
                if("1".equals(event.get("SWITCH_STATE"))){
                    wm.thawRotation();
                }else{
                    wm.freezeRotation(-1);//use current orientation
                }
            } catch (RemoteException exc) {
                Log.w(TAG, "Unable to save auto-rotate setting");
            }
        }
    };
    */

    void setHdmiHwPlugged(boolean plugged) {
        if (mHdmiHwPlugged != plugged) {
            Log.i(TAG, "setHdmiHwPlugged " + plugged);
            mHdmiHwPlugged = plugged;

            if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)){
                if (plugged)
                    mOutputMode.setHdmiPlugged();
                else
                    mOutputMode.setHdmiUnPlugged();
            }

            /*
            Intent intent = new Intent(WindowManagerPolicy.ACTION_HDMI_HW_PLUGGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(EXTRA_HDMI_HW_PLUGGED_STATE, plugged);
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);

            if (mSystemControl.getPropertyBoolean("ro.vout.dualdisplay", false)) {
                setDualDisplay(plugged);

                Intent it = new Intent(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
                it.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                it.putExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, plugged);
                mContext.sendStickyBroadcastAsUser(it, UserHandle.OWNER);
            }*/
        }
    }

    private void initHdmiState() {
        boolean plugged = false;

        // watch for HDMI plug messages if the hdmi switch exists
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            if (mSystemControl.getPropertyBoolean("ro.platform.has.mbxuimode", false)){
                mOutputMode.initOutputMode();
            }
            mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");

            final String filename = "/sys/class/switch/hdmi/state";
            String state = mSystemControl.readSysFs(filename);

            Log.i(TAG, "initHdmiState :" + state);

            plugged = (0 != Integer.parseInt(state));
        }

        /*
        mHdmiHwPlugged = plugged;
        if (!mSystemControl.getPropertyBoolean("ro.vout.dualdisplay", false)) {
            if (getCurDisplayMode().equals("panel") ||
                !plugged ||
                SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)) {
                plugged = false;
            }
        }

        if (mSystemControl.getPropertyBoolean("ro.vout.dualdisplay", false)) {
            setDualDisplay(plugged);
        }

        if (mSystemControl.getPropertyBoolean("ro.vout.dualdisplay2", false)) {
            plugged = false;
            setDualDisplay(plugged);
        }

        Intent it = new Intent(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
        it.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        it.putExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, plugged);
        mContext.sendStickyBroadcastAsUser(it, UserHandle.OWNER);
        */
    }

    /*
    private void initHoldkeyState( IWindowManager windowManager ) {
        boolean hold = false;
        if (new File("/sys/devices/virtual/switch/hold_key/state").exists()) {
            mHoldObserver.startObserving("DEVPATH=/devices/virtual/switch/hold_key");
            final String filename = "/sys/class/switch/hold_key/state";

            hold = (0 != Integer.parseInt(mSystemControl.readSysFs(filename)));
            if(hold){
                windowManager.thawRotation();
            }else{
                windowManager.freezeRotation(-1);//use current orientation
            }
        }
    }
    */

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
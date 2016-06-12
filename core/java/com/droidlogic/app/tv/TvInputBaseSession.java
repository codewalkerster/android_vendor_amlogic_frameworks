package com.droidlogic.app.tv;

import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.provider.Settings;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;

import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiTvClient;
import android.provider.Settings.Global;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.HdmiTvClient.SelectCallback;

public abstract class TvInputBaseSession extends TvInputService.Session implements Handler.Callback {
    private static final boolean DEBUG = true;
    private static final String TAG = "TvInputBaseSession";
    private static final int MSG_DO_TUNE = 0;
    private static final int MSG_DO_PRI_CMD = 9;
    private static final int RETUNE_TIMEOUT = 20; // 1 second

    private Context mContext;
    private int mNumber;
    private String mInputId;
    private int mDeviceId;
    private Surface mSurface;
    private TvInputManager mTvInputManager;
    private HdmiControlManager mHdmiManager;
    private HdmiTvClient mHdmiTvClient = null;
    private boolean mHasRetuned = false;
    private Handler mSessionHandler;
    private TvControlManager mTvControlManager;
    private int timeout = RETUNE_TIMEOUT;
    private int mCurPort = -1;

    protected int ACTION_FAILED = -1;
    protected int ACTION_SUCCESS = 1;
    private final int TV_SOURCE_EXTERNAL = 0;
    private final int TV_SOURCE_INTERNAL = 1;

    public TvInputBaseSession(Context context, String inputId, int deviceId) {
        super(context);
        mContext = context;
        mInputId = inputId;
        mDeviceId = deviceId;

        mTvControlManager = TvControlManager.getInstance();
        mSessionHandler = new Handler(context.getMainLooper(), this);
        /* add DeviceEventListener for HDMI event */
        mHdmiManager = (HdmiControlManager) mContext.getSystemService(Context.HDMI_CONTROL_SERVICE);
    }

    public void setNumber(int number) {
        mNumber = number;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getInputId() {
        return mInputId;
    }

    public int getDeviceId() {
        return mDeviceId;
    }

    public abstract Hardware getHardware();
    public abstract TvStreamConfig[] getConfigs();
    public abstract int getCurrentSessionNumber();
    public abstract void setCurrentSession();

    private int startTvPlay() {
        Log.d(TAG, "startTvPlay inputId=" + mInputId + " number=" + mNumber + " surface=" + mSurface);
        if (getHardware() != null && mSurface != null && mSurface.isValid()) {
            getHardware().setSurface(mSurface, getConfigs()[0]);
            selectHdmiDevice(TV_SOURCE_EXTERNAL);
            return ACTION_SUCCESS;
        }
        return ACTION_FAILED;
    }

    public int stopTvPlay() {
        if (getHardware() != null) {

            getHardware().setSurface(null, null);
            return ACTION_SUCCESS;

         }
        return ACTION_SUCCESS;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void doRelease() {
        Log.d(TAG, "doRelease");
    }

    public int doTune(Uri uri) {
        Log.d(TAG, "doTune, uri = " + uri);

        if (getConfigs() == null || startTvPlay() == ACTION_FAILED) {
            Log.d(TAG, "doTune failed, timeout=" + timeout + ", retune 50ms later ...");

            if (timeout > 0) {
                Message msg = mSessionHandler.obtainMessage(MSG_DO_TUNE, uri);
                mSessionHandler.sendMessageDelayed(msg, 50);
                timeout--;
                return ACTION_FAILED;
            }
        }

        return ACTION_SUCCESS;
    }

    public void doAppPrivateCmd(String action, Bundle bundle) {
        //do something
        if (DroidLogicTvUtils.ACTION_ATV_AUTO_SCAN.equals(action)) {
            mTvControlManager.AtvAutoScan(TvControlManager.ATV_VIDEO_STD_PAL, TvControlManager.ATV_AUDIO_STD_I, 0, 1);
        } else if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)) {
            mTvControlManager.DtvSetTextCoding("GB2312");
            mTvControlManager.DtvAutoScan();
        } else if (DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
            if (bundle != null) {
                mTvControlManager.DtvSetTextCoding("GB2312");
                mTvControlManager.DtvManualScan(bundle.getInt(DroidLogicTvUtils.PARA_MANUAL_SCAN));
            }
        } else if (DroidLogicTvUtils.ACTION_STOP_SCAN.equals(action)) {
            mTvControlManager.DtvStopScan();
        } else if (DroidLogicTvUtils.ACTION_ATV_PAUSE_SCAN.equals(action)) {
            mTvControlManager.AtvDtvPauseScan();
        } else if (DroidLogicTvUtils.ACTION_ATV_RESUME_SCAN.equals(action)) {
            mTvControlManager.AtvDtvResumeScan();
        }
    }

    public void doUnblockContent(TvContentRating rating) {}

    public void doSetSurface(Surface surface) {
        Log.d(TAG, "doSetSurface inputId=" + mInputId + " number=" + mNumber + " surface=" + surface);
        timeout = RETUNE_TIMEOUT;

        if (surface != null && !surface.isValid()) {
            Log.d(TAG, "onSetSurface get invalid surface");
            return;
        } else if (surface != null) {
            setCurrentSession();
        }
        mSurface = surface;

        if (mSurface == null && getHardware() != null && mNumber == getCurrentSessionNumber()) {
            Log.d(TAG, "surface is null, so stop TV play");
            stopTvPlay();
        }
    }

    @Override
    public void onRelease() {
        doRelease();
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        doSetSurface(surface);
        if (surface == null)
            selectHdmiDevice(TV_SOURCE_INTERNAL);
        return false;
    }

    @Override
    public void onSurfaceChanged(int format, int width, int height) {
    }

    @Override
    public void onSetStreamVolume(float volume) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onAppPrivateCommand(String action, Bundle data) {
        if (DEBUG)
            Log.d(TAG, "onAppPrivateCommand, action = " + action);

        if (mSessionHandler == null)
            return;
        Message msg = mSessionHandler.obtainMessage(MSG_DO_PRI_CMD);
        msg.setData(data);
        msg.obj = action;
        msg.sendToTarget();
    }

    @Override
    public boolean onTune(Uri channelUri) {
        if (DEBUG)
            Log.d(TAG, "onTune, channelUri=" + channelUri);

        mSessionHandler.obtainMessage(MSG_DO_TUNE, channelUri).sendToTarget();
        return false;
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnblockContent(TvContentRating unblockedRating) {
        if (DEBUG)
            Log.d(TAG, "onUnblockContent");

        doUnblockContent(unblockedRating);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (DEBUG)
            Log.d(TAG, "handleMessage, msg.what=" + msg.what);
        switch (msg.what) {
            case MSG_DO_TUNE:
                mSessionHandler.removeMessages(MSG_DO_TUNE);
                doTune((Uri)msg.obj);
                break;
            case MSG_DO_PRI_CMD:
                doAppPrivateCmd((String)msg.obj, msg.getData());
                break;
        }
        return false;
    }

    private boolean mInHdmi = false;
    private int mSourcePort = 0;

    private void setCurPort(int port) {
        mCurPort = port;
    }

    public void selectHdmiDevice(int mode) {
        boolean cecOption = (Global.getInt(mContext.getContentResolver(), Global.HDMI_CONTROL_ENABLED, 1) == 1);
        Log.d(TAG, "CEC selectHdmiDevice, deviceId:" + mDeviceId + ", cecOption:" + cecOption);
        if (mHdmiTvClient == null) {
            if (mHdmiManager == null) {
                Log.d(TAG, "CEC NULL mHdmiManager");
                return;
            }
            mHdmiTvClient = mHdmiManager.getTvClient();
        }
        if (!cecOption || mHdmiTvClient == null) {
            Log.d(TAG, "cec not enabled or null mHdmiTvClient");
            return ;
        }
        Log.d(TAG, "CEC selectHdmiDevice, mode:" + mode + ", mInHdmi:" + mInHdmi);
        if (mode == TV_SOURCE_EXTERNAL) {
            int fit = DroidLogicTvUtils.DEVICE_ID_HDMI1 - 1;
            int id = mDeviceId - fit;
            Log.d(TAG, "CEC selectHdmiDevice, id:"+ id + ", mInHdmi:" + mInHdmi+ ", curPort:" + mCurPort);
            for (HdmiDeviceInfo info : mHdmiTvClient.getDeviceList()) {
                mSourcePort = info.getPhysicalAddress() >> 12;
                Log.d(TAG, "CEC selectHdmiDevice, id:"+ id + ", port:" + mSourcePort);
                if (id == mSourcePort && mCurPort != id) {
                    mHdmiTvClient.deviceSelect(info.getLogicalAddress(), new SelectCallback() {
                        @Override
                        public void onComplete(int result) {
                            Log.d(TAG, "CEC selectHdmiDevice success");
                            mInHdmi = true;
                            setCurPort(mSourcePort);
                        }
                    });
                    return;
                } else if (id == mSourcePort && mCurPort == id) {
                    Log.d(TAG, "CEC already selected port:" + id);
                    return ;
                }
            }
            if (mInHdmi) {
                /* select internal source if no hdmi */
                Log.d(TAG, "CEC selectHdmiDevice, check to internal source");
                mHdmiTvClient.deviceSelect(0, new SelectCallback() {
                    @Override
                    public void onComplete(int result) {
                        Log.d(TAG, "CEC selectInternal success");
                        mInHdmi = false;
                        setCurPort(-1);
                    }
                });
            }
        } else if (mode == TV_SOURCE_INTERNAL) {
            if (mInHdmi) {
                /* select internal source if no hdmi */
                Log.d(TAG, "CEC selectHdmiDevice, check to internal source");
                mHdmiTvClient.deviceSelect(0, new SelectCallback() {
                    @Override
                    public void onComplete(int result) {
                        Log.d(TAG, "CEC selectInternal success");
                        mInHdmi = false;
                        setCurPort(-1);
                    }
                });
            }
        }
    }
}

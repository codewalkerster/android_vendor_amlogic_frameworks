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

public abstract class TvInputBaseSession extends TvInputService.Session implements Handler.Callback {
    private static final boolean DEBUG = true;
    private static final String TAG = "TvInputBaseSession";
    private static final int MSG_DO_TUNE = 0;

    private Context mContext;
    private int mNumber;
    private String mInputId;
    private int mDeviceId;
    private Surface mSurface;
    private TvInputManager mTvInputManager;
    private boolean mHasRetuned = false;
    private Handler mSessionHandler;
    private TvControlManager mTvControlManager;

    protected int ACTION_FAILED = -1;
    protected int ACTION_SUCCESS = 1;

    public TvInputBaseSession(Context context, String inputId, int deviceId) {
        super(context);
        mContext = context;
        mInputId = inputId;
        mDeviceId = deviceId;

        mTvControlManager = TvControlManager.getInstance();
        mSessionHandler = new Handler(context.getMainLooper(), this);
    }

    public void setNumber(int number) {
        mNumber = number;
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
    public abstract void setCurrentSessionNumber(int number);

    private int startTvPlay() {
        Log.d(TAG, "startTvPlay inputId=" + mInputId + " number=" + mNumber);
        if (getHardware() != null && mSurface != null && mSurface.isValid()) {
            getHardware().setSurface(mSurface, getConfigs()[0]);
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

        return startTvPlay();
    }

    public void doAppPrivateCmd(String action, Bundle bundle) {
        //do something
		if (DroidLogicTvUtils.ACTION_ATV_AUTO_SCAN.equals(action)) {
            mTvControlManager.AtvAutoScan(TvControlManager.ATV_VIDEO_STD_PAL, TvControlManager.ATV_AUDIO_STD_I, 0);
        } else if (DroidLogicTvUtils.ACTION_DTV_AUTO_SCAN.equals(action)) {
            mTvControlManager.DtvAutoScan();
        } else if (DroidLogicTvUtils.ACTION_DTV_MANUAL_SCAN.equals(action)) {
            if (bundle != null) {
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
        Log.d(TAG, "doSetSurface inputId=" + mInputId + " number=" + mNumber);

        if (surface != null && !surface.isValid()) {
            Log.d(TAG, "onSetSurface get invalid surface");
            return;
        } else if (surface != null) {
            setCurrentSessionNumber(mNumber);
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
        Log.d(TAG, "onSetSurface inputId=" + mInputId + " number=" + mNumber + " surface=" + surface);
        doSetSurface(surface);
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

        doAppPrivateCmd(action, data);
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
        }
        return false;
    }
}

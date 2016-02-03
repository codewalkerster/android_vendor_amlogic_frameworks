package com.droidlogic.app.tv;

import java.util.List;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TVInSignalInfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

public class DroidLogicTvInputService extends TvInputService implements TVInSignalInfo.SigInfoChangeListener {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

    private TvInputBaseSession mSession;
    private String mCurrentInputId;


    /**
     * inputId should get from subclass which must invoke {@link super#onCreateSession(String)}
     */
    @Override
    public Session onCreateSession(String inputId) {
        mCurrentInputId = inputId;

        return null;
    }

    /**
     * get session has been created by {@code onCreateSession}, and input id of session.
     * @param session {@link HdmiInputSession} or {@link AVInputSession}
     */
    protected void registerInputSession(TvInputBaseSession session) {
        mSession = session;
        TvControlManager.getInstance().SetSigInfoChangeListener(this);
    }

    /**
     * update {@code mInfoList} when hardware device is added or removed.
     * @param hInfo {@linkHardwareInfo} get from HAL.
     * @param info {@link TvInputInfo} will be added or removed.
     * @param isRemoved {@code true} if you want to remove info. {@code false} otherwise.
     */
    protected void updateInfoListIfNeededLocked(TvInputHardwareInfo hInfo,
            TvInputInfo info, boolean isRemoved) {
        if (isRemoved) {
            mInfoList.remove(hInfo.getDeviceId());
        }else {
            mInfoList.put(hInfo.getDeviceId(), info);
        }

        if (DEBUG)
            Log.d(TAG, "size of mInfoList is " + mInfoList.size());
    }

    protected TvInputInfo getTvInputInfo(TvInputHardwareInfo hardwareInfo) {
        return mInfoList.get(hardwareInfo.getDeviceId());
    }

    protected int getHardwareDeviceId(String input_id) {
        int id = 0;
        for (int i=0; i<mInfoList.size(); i++) {
            if (input_id.equals(mInfoList.valueAt(i).getId())) {
                id = mInfoList.keyAt(i);
                break;
            }
        }

        if (DEBUG)
            Log.d(TAG, "device id is " + id);
        return id;
    }

    protected String getTvInputInfoLabel(int device_id) {
        String label = null;
        switch (device_id) {
            case DroidLogicTvUtils.DEVICE_ID_ATV:
                label = ChannelInfo.LABEL_ATV;
                break;
            case DroidLogicTvUtils.DEVICE_ID_DTV:
                label = ChannelInfo.LABEL_DTV;
                break;
            case DroidLogicTvUtils.DEVICE_ID_AV1:
                label = ChannelInfo.LABEL_AV1;
                break;
            case DroidLogicTvUtils.DEVICE_ID_AV2:
                label = ChannelInfo.LABEL_AV2;
                break;
            case DroidLogicTvUtils.DEVICE_ID_HDMI1:
                label = ChannelInfo.LABEL_HDMI1;
                break;
            case DroidLogicTvUtils.DEVICE_ID_HDMI2:
                label = ChannelInfo.LABEL_HDMI2;
                break;
            case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                label = ChannelInfo.LABEL_HDMI3;
                break;
            default:
                break;
        }
        return label;
    }

    protected ResolveInfo getResolveInfo(String cls_name) {
        if (TextUtils.isEmpty(cls_name))
            return null;
        ResolveInfo ret_ri = null;
        Context context = getApplicationContext();

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);

        for (ResolveInfo ri: services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }

            if (DEBUG)
                Log.d(TAG, "cls_name = " + cls_name + ", si.name = " + si.name);

            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

    protected void stopTv() {
        Log.d(TAG, "stop tv, mCurrentInputId =" + mCurrentInputId);
        TvControlManager.getInstance().StopTv();
    }

    protected void releasePlayer() {
        TvControlManager.getInstance().StopPlayProgram();
    }

    private String getInfoLabel() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        return tim.getTvInputInfo(mCurrentInputId).loadLabel(this).toString();
    }

    @Override
    public void onSigChange(TVInSignalInfo signal_info) {
        TVInSignalInfo.SignalStatus status = signal_info.sigStatus;

        if (DEBUG)
            Log.d(TAG, "onSigChange" + status.ordinal() + status.toString());

        if (status == TVInSignalInfo.SignalStatus.TVIN_SIG_STATUS_NOSIG
                || status == TVInSignalInfo.SignalStatus.TVIN_SIG_STATUS_NULL
                || status == TVInSignalInfo.SignalStatus.TVIN_SIG_STATUS_NOTSUP) {
            mSession.notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
        }else if (status == TVInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE) {
            mSession.notifyVideoAvailable();
            int device_id = mSession.getDeviceId();
            String[] strings;
            Bundle bundle = new Bundle();
            switch (device_id) {
            case DroidLogicTvUtils.DEVICE_ID_HDMI1:
            case DroidLogicTvUtils.DEVICE_ID_HDMI2:
            case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                if (DEBUG)
                    Log.d(TAG, "signal_info.fmt.toString() for hdmi=" + signal_info.sigFmt.toString());

                strings = signal_info.sigFmt.toString().split("_");
                TVInSignalInfo.SignalFmt fmt = signal_info.sigFmt;
                if (fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X480I_60HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X480I_120HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X480I_240HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_2880X480I_60HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_2880X480I_60HZ) {
                    strings[4] = "480I";
                } else if (fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X576I_50HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X576I_100HZ
                        || fmt == TVInSignalInfo.SignalFmt.TVIN_SIG_FMT_HDMI_1440X576I_200HZ) {
                    strings[4] = "576I";
                }

                bundle.putInt(DroidLogicTvUtils.SIG_INFO_TYPE, DroidLogicTvUtils.SIG_INFO_TYPE_HDMI);
                bundle.putString(DroidLogicTvUtils.SIG_INFO_LABEL, getInfoLabel());
                if (strings != null && strings.length <= 4)
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS,"0_0HZ");
                else
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, strings[4]
                            + "_" + signal_info.reserved + "HZ");
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, bundle);
                break;
            case DroidLogicTvUtils.DEVICE_ID_AV1:
            case DroidLogicTvUtils.DEVICE_ID_AV2:
                if (DEBUG)
                    Log.d(TAG, "tmpInfo.fmt.toString() for av=" + signal_info.sigFmt.toString());

                strings = signal_info.sigFmt.toString().split("_");
                bundle.putInt(DroidLogicTvUtils.SIG_INFO_TYPE, DroidLogicTvUtils.SIG_INFO_TYPE_AV);
                bundle.putString(DroidLogicTvUtils.SIG_INFO_LABEL, getInfoLabel());
                if (strings != null && strings.length <= 4)
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, "");
                else
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, strings[4]);
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, bundle);
                break;
            case DroidLogicTvUtils.DEVICE_ID_ATV:
                if (DEBUG)
                    Log.d(TAG, "tmpInfo.fmt.toString() for atv=" + signal_info.sigFmt.toString());

                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, null);
                break;
            case DroidLogicTvUtils.DEVICE_ID_DTV:
                if (DEBUG)
                    Log.d(TAG, "tmpInfo.fmt.toString() for dtv=" + signal_info.sigFmt.toString());

                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_EVENT, null);
                break;
            default:
                break;
            }
        }
    }
}

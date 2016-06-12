package com.droidlogic.app.tv;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TVInSignalInfo;

import android.provider.Settings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.media.tv.TvContract.Channels;

public class DroidLogicTvInputService extends TvInputService implements
        TVInSignalInfo.SigInfoChangeListener, TvControlManager.StorDBEventListener,
        TvControlManager.ScanningFrameStableListener {
    private static final String TAG = DroidLogicTvInputService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int DISPLAY_NUM_START_DEF = 1;

    private SparseArray<TvInputInfo> mInfoList = new SparseArray<>();

    private TvInputBaseSession mSession;
    private String mCurrentInputId;
    public Hardware mHardware;
    public TvStreamConfig[] mConfigs;
    private int mDeviceId = -1;
    private String mInputId;

    private int c_displayNum = DISPLAY_NUM_START_DEF;
    private TvDataBaseManager mTvDataBaseManager;
    private TvControlManager mTvControlManager;
    private HardwareCallback mHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            if (DEBUG)
                Log.d(TAG, "onReleased");

            mHardware = null;
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            if (DEBUG)
                Log.d(TAG, "onStreamConfigChanged");
            mConfigs = configs;
        }
    };

    /**
     * inputId should get from subclass which must invoke {@link super#onCreateSession(String)}
     */
    @Override
    public Session onCreateSession(String inputId) {
        TvInputManager tm = (TvInputManager)this.getSystemService(Context.TV_INPUT_SERVICE);
        mCurrentInputId = inputId;

        if (mHardware != null && mDeviceId != -1) {
            tm.releaseTvInputHardware(mDeviceId, mHardware);
            mConfigs = null;
        }

        mCurrentInputId = inputId;
        mDeviceId = getHardwareDeviceId(inputId);
        mHardware = tm.acquireTvInputHardware(mDeviceId, mHardwareCallback, tm.getTvInputInfo(inputId));

        return null;
    }

    /**
     * get session has been created by {@code onCreateSession}, and input id of session.
     * @param session {@link HdmiInputSession} or {@link AVInputSession}
     */
    protected void registerInputSession(TvInputBaseSession session) {
        Log.d(TAG, "registerInputSession");
        mSession = session;
        mTvControlManager = TvControlManager.getInstance();
        mTvControlManager.SetSigInfoChangeListener(this);
        mTvControlManager.setStorDBListener(this);
        mTvControlManager.setScanningFrameStableListener(this);
        mTvDataBaseManager = new TvDataBaseManager(getApplicationContext());
    }

    /**
     * update {@code mInfoList} when hardware device is added or removed.
     * @param hInfo {@linkHardwareInfo} get from HAL.
     * @param info {@link TvInputInfo} will be added or removed.
     * @param isRemoved {@code true} if you want to remove info. {@code false} otherwise.
     */
    protected void updateInfoListIfNeededLocked(TvInputHardwareInfo hInfo,
            TvInputInfo info, boolean isRemoved) {
        updateInfoListIfNeededLocked(hInfo.getDeviceId(), info, isRemoved);
    }

    protected void updateInfoListIfNeededLocked(int Id, TvInputInfo info,
            boolean isRemoved) {
        if (isRemoved) {
            mInfoList.remove(Id);
        } else {
            mInfoList.put(Id, info);
        }

        if (DEBUG)
            Log.d(TAG, "size of mInfoList is " + mInfoList.size());
    }

    protected boolean hasInfoExisted(TvInputHardwareInfo hInfo) {
        return mInfoList.get(hInfo.getDeviceId()) == null ? false : true;
    }

    protected TvInputInfo getTvInputInfo(TvInputHardwareInfo hardwareInfo) {
        return mInfoList.get(hardwareInfo.getDeviceId());
    }

    protected TvInputInfo getTvInputInfo(int devId) {
        return mInfoList.get(devId);
    }

    protected int getHardwareDeviceId(String input_id) {
        int id = 0;
        for (int i = 0; i < mInfoList.size(); i++) {
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

        for (ResolveInfo ri : services) {
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
        mTvControlManager.StopTv();
    }

    protected void releasePlayer() {
        mTvControlManager.StopPlayProgram();
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
        } else if (status == TVInSignalInfo.SignalStatus.TVIN_SIG_STATUS_STABLE) {
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
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, "0_0HZ");
                else
                    bundle.putString(DroidLogicTvUtils.SIG_INFO_ARGS, strings[4] + "_"
                            + signal_info.reserved + "HZ");
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

    public static String mode2type(int mode) {
        String type = "";
        switch (mode) {
        case TVChannelParams.MODE_DTMB:
            type = Channels.TYPE_DTMB;
            break;
        case TVChannelParams.MODE_QPSK:
            type = Channels.TYPE_DVB_S;
            break;
        case TVChannelParams.MODE_QAM:
            type = Channels.TYPE_DVB_C;
            break;
        case TVChannelParams.MODE_OFDM:
            type = Channels.TYPE_DVB_T;
            break;
        case TVChannelParams.MODE_ATSC:
            type = Channels.TYPE_ATSC_C;
            break;
        case TVChannelParams.MODE_ANALOG:
            type = Channels.TYPE_PAL;
            break;
        case TVChannelParams.MODE_ISDBT:
            type = Channels.TYPE_ISDB_T;
            break;
        default:
            break;
        }
        return type;
    }

    private int getidxByDefLan(String[] strArray) {
        if (strArray == null)
            return 0;
        String[] defLanArray = { "chi", "zho", "ita", "spa", "ara" };
        for (int i = 0; i < strArray.length; i++) {
            for (String lan : defLanArray) {
                if (strArray[i].equals(lan))
                    return i;
            }
        }
        return 0;
    }

    private ChannelInfo createAtvChannelInfo(TvControlManager.ScannerEvent event) {
        String ATVName = "ATV program";
        return new ChannelInfo.Builder()
               .setInputId(mCurrentInputId == null ? "NULL" : mCurrentInputId)
               .setType(mode2type(event.mode))
               .setServiceType(Channels.SERVICE_TYPE_AUDIO_VIDEO)//default is SERVICE_TYPE_AUDIO_VIDEO
               .setServiceId(0)
               .setDisplayNumber(Integer.toString(c_displayNum))
               .setDisplayName(ATVName+" "+Integer.toString(c_displayNum))
               .setLogoUrl(null)
               .setOriginalNetworkId(0)
               .setTransportStreamId(0)
               .setVideoPid(0)
               .setVideoStd(event.videoStd)
               .setVfmt(0)
               .setVideoWidth(0)
               .setVideoHeight(0)
               .setAudioPids(null)
               .setAudioFormats(null)
               .setAudioLangs(null)
               .setAudioStd(event.audioStd)
               .setIsAutoStd(event.isAutoStd)
               .setAudioTrackIndex(0)
               .setAudioCompensation(0)
               .setPcrPid(0)
               .setFrequency(event.freq)
               .setBandwidth(0)
               .setFineTune(0)
               .setBrowsable(true)
               .setIsFavourite(false)
               .setPassthrough(false)
               .setLocked(false)
               .setDisplayNameMulti("xxx" + ATVName)
               .build();
    }

    private ChannelInfo createDtvChannelInfo(TvControlManager.ScannerEvent event) {
        String name = null;
        String serviceType;

        try {
            name = TVMultilingualText.getText(event.programName);
        } catch (Exception e) {
            e.printStackTrace();
            name = "????";
        }

        if (event.srvType == 1) {
            serviceType = Channels.SERVICE_TYPE_AUDIO_VIDEO;
        } else if (event.srvType == 2) {
            serviceType = Channels.SERVICE_TYPE_AUDIO;
        } else {
            serviceType = Channels.SERVICE_TYPE_OTHER;
        }

        return new ChannelInfo.Builder()
               .setInputId(mCurrentInputId)
               .setType(mode2type(event.mode))
               .setServiceType(serviceType)
               .setServiceId(event.serviceID)
               .setDisplayNumber(Integer.toString(c_displayNum))
               .setDisplayName(name)
               .setLogoUrl(null)
               .setOriginalNetworkId(event.orig_net_id)
               .setTransportStreamId(event.ts_id)
               .setVideoPid(event.vid)
               .setVideoStd(0)
               .setVfmt(event.vfmt)
               .setVideoWidth(0)
               .setVideoHeight(0)
               .setAudioPids(event.aids)
               .setAudioFormats(event.afmts)
               .setAudioLangs(event.alangs)
               .setAudioStd(0)
               .setIsAutoStd(event.isAutoStd)
               //.setAudioTrackIndex(getidxByDefLan(event.alangs))
               .setAudioCompensation(0)
               .setPcrPid(event.pcr)
               .setFrequency(event.freq)
               .setBandwidth(event.bandwidth)
               .setFineTune(0)
               .setBrowsable(true)
               .setIsFavourite(false)
               .setPassthrough(false)
               .setLocked(false)
               .setSubtitleTypes(event.stypes)
               .setSubtitlePids(event.sids)
               .setSubtitleStypes(event.sstypes)
               .setSubtitleId1s(event.sid1s)
               .setSubtitleId2s(event.sid2s)
               .setSubtitleLangs(event.slangs)
               //.setSubtitleTrackIndex(getidxByDefLan(event.slangs))
               .setDisplayNameMulti(event.programName)
               .setFreeCa(event.free_ca)
               .setScrambled(event.scrambled)
               .setSdtVersion(event.sdtVersion)
               .build();
    }

    public static class ScanMode {
        private int scanMode;

        ScanMode(int ScanMode) {
            scanMode = ScanMode;
        }

        public int getMode() {
            return (scanMode >> 24) & 0xf;
        }

        public int getATVMode() {
            return (scanMode >> 16) & 0xf;
        }

        public int getDTVMode() {
            return (scanMode & 0xFFFF);
        }

        public boolean isDTVManulScan() {
            return (getATVMode() == 0x7) && (getDTVMode() == 0x2);
        }

        public boolean isDTVAutoScan() {
            return (getATVMode() == 0x7) && (getDTVMode() == 0x1);
        }

        public boolean isATVScan() {
            return (getATVMode() != 0x7) && (getDTVMode() == 0x7);
        }

        public boolean isATVManualScan() {
            return (getATVMode() == 0x2) && (getDTVMode() == 0x7);
        }

        public boolean isATVAutoScan() {
            return (getATVMode() == 0x1) && (getDTVMode() == 0x7);
        }
    }

    private ScanMode mScanMode = null;


    public static class SortMode {
        private int sortMode;

        SortMode(int SortMode) {
            sortMode = SortMode;
        }
        public int getDTVSortMode() {
            return (sortMode&0xFFFF);
        }

        public boolean isLCNSort() {
            return (getDTVSortMode() == 0x2);
        }
    }

    private SortMode mSortMode = null;

    private ArrayList<TvControlManager.ScannerLcnInfo> mLcnInfo = null;

    /*for store in search*/
    private Integer c_displayNum2 = null;
    private boolean isFinalStoreStage = false;
    private boolean isRealtimeStore = false;

    @Override
    public void StorDBonEvent(TvControlManager.ScannerEvent event) {
        ChannelInfo channel = null;
        String name = null;
        Log.e(TAG, "onEvent:" + event.type + " :" + c_displayNum);
        Bundle bundle = null;
        switch (event.type) {
        case TvControlManager.EVENT_SCAN_BEGIN:
            Log.d(TAG, "Scan begin");

            mScanMode = new ScanMode(event.scan_mode);
            mSortMode = new SortMode(event.sort_mode);
            c_displayNum = DISPLAY_NUM_START_DEF;
            c_displayNum2 = new Integer(DISPLAY_NUM_START_DEF);
            isFinalStoreStage = false;
            isRealtimeStore = false;

            bundle = getBundleByScanEvent(event);
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_SCAN_BEGIN_EVENT, bundle);
            break;

        case TvControlManager.EVENT_LCN_INFO_DATA:
            if (mLcnInfo == null)
                mLcnInfo = new ArrayList<TvControlManager.ScannerLcnInfo>();
            mLcnInfo.add(event.lcnInfo);
            Log.d(TAG, "Lcn["+event.lcnInfo.netId+":"+event.lcnInfo.tsId+":"+event.lcnInfo.serviceId+"]");
            Log.d(TAG, "\t[0:"+event.lcnInfo.lcn[0]+":"+event.lcnInfo.visible[0]+":"+event.lcnInfo.valid[0]+"]");
            Log.d(TAG, "\t[1:"+event.lcnInfo.lcn[1]+":"+event.lcnInfo.visible[1]+":"+event.lcnInfo.valid[1]+"]");
            break;

        case TvControlManager.EVENT_DTV_PROG_DATA:
            Log.d(TAG, "dtv prog data");

            if (!isFinalStoreStage)
                isRealtimeStore = true;

            if (mScanMode == null) {
                Log.d(TAG, "mScanMode is null, store return.");
                return;
            }

            if (mScanMode.isDTVManulScan())
                initChannelsExist();

            channel = createDtvChannelInfo(event);

            if (c_displayNum2 != null)
                channel.setDisplayNumber(Integer.toString(c_displayNum2));
            else
                channel.setDisplayNumber(String.valueOf(c_displayNum));

            Log.d(TAG, "reset number to " + channel.getDisplayNumber());

            channel.print();
            onDTVChannelStore(event, channel);

            if (c_displayNum2 != null) {
                Log.d(TAG, "mid store, num:"+c_displayNum2);
                c_displayNum2++;//count for realtime stage
            } else {
                Log.d(TAG, "final store, num: " + c_displayNum);
                bundle = GetDisplayNumBunlde(c_displayNum);
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM_EVENT, bundle);
                c_displayNum++;//count for store stage
            }
            break;

        case TvControlManager.EVENT_ATV_PROG_DATA:
            Log.d(TAG, "atv prog data");

            if (!isFinalStoreStage)
                isRealtimeStore = true;

            initChannelsExist();

            channel = createAtvChannelInfo(event);
            channel.print();

            if (mScanMode.isATVManualScan())
                onUpdateCurrentChannel(channel, true);
            else
                mTvDataBaseManager.updateOrinsertAtvChannelWithNumber(channel);

            Log.d(TAG, "onEvent,displayNum:" + c_displayNum);

            if (isFinalStoreStage) {
                bundle = GetDisplayNumBunlde(c_displayNum);
                mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM_EVENT, bundle);
                c_displayNum++;//count for storestage
            }
            break;

        case TvControlManager.EVENT_SCAN_PROGRESS:
            Log.d(TAG, event.precent + "%\tfreq[" + event.freq + "] lock[" + event.lock + "] strength[" + event.strength + "] quality[" + event.quality + "]");

            //take evt:progress as a store-loop end.
            if (!isFinalStoreStage
                && (event.mode != TVChannelParams.MODE_ANALOG)
                && !mScanMode.isDTVManulScan()) {
                onTVChannelStoreEnd(isRealtimeStore, isFinalStoreStage);
                c_displayNum2 = DISPLAY_NUM_START_DEF;//dtv pop all channels scanned every store-loop
            }

            bundle = getBundleByScanEvent(event);
            if ((event.mode == TVChannelParams.MODE_ANALOG) && (event.lock == 0x11)) {
                bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM, c_displayNum);
                c_displayNum++;//count for progress stage
            }
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_PROCESS_EVENT, bundle);
            break;

        case TvControlManager.EVENT_STORE_BEGIN:
            Log.d(TAG, "Store begin");

            //reset for store stage
            isFinalStoreStage = true;
            c_displayNum = DISPLAY_NUM_START_DEF;
            c_displayNum2 = null;
            if (mLcnInfo != null)
                mLcnInfo.clear();

            bundle = getBundleByScanEvent(event);
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_STORE_BEGIN_EVENT, bundle);
            break;

        case TvControlManager.EVENT_STORE_END:
            Log.d(TAG, "Store end");

            onTVChannelStoreEnd(isRealtimeStore, isFinalStoreStage);

            bundle = getBundleByScanEvent(event);
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_STORE_END_EVENT, bundle);
            break;

        case TvControlManager.EVENT_SCAN_END:
            Log.d(TAG, "Scan end");

            mTvControlManager.DtvStopScan();

            bundle = getBundleByScanEvent(event);
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_SCAN_END_EVENT, bundle);
            break;

        case TvControlManager.EVENT_SCAN_EXIT:
            Log.d(TAG, "Scan exit.");

            isFinalStoreStage = false;
            isRealtimeStore = false;
            c_displayNum = DISPLAY_NUM_START_DEF;
            c_displayNum2 = null;
            if (mLcnInfo != null) {
                mLcnInfo.clear();
                mLcnInfo = null;
            }

            bundle = getBundleByScanEvent(event);
            mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_SCAN_EXIT_EVENT, bundle);
            break;

        default:
            break;
        }
    }

    private Bundle getBundleByScanEvent(TvControlManager.ScannerEvent mEvent) {
        Bundle bundle = new Bundle();
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_TYPE, mEvent.type);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_PRECENT, mEvent.precent);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_TOTALCOUNT, mEvent.totalcount);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_LOCK, mEvent.lock);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_CNUM, mEvent.cnum);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_FREQ, mEvent.freq);
        bundle.putString(DroidLogicTvUtils.SIG_INFO_C_PROGRAMNAME, mEvent.programName);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_SRVTYPE, mEvent.srvType);
        bundle.putString(DroidLogicTvUtils.SIG_INFO_C_MSG, mEvent.msg);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_STRENGTH, mEvent.strength);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_QUALITY, mEvent.quality);
        // ATV
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_VIDEOSTD, mEvent.videoStd);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_AUDIOSTD, mEvent.audioStd);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_ISAUTOSTD, mEvent.isAutoStd);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_FINETUNE, mEvent.fineTune);
        // DTV
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_MODE, mEvent.mode);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_SR, mEvent.sr);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_MOD, mEvent.mod);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_BANDWIDTH, mEvent.bandwidth);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_OFM_MODE, mEvent.ofdm_mode);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_TS_ID, mEvent.ts_id);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_ORIG_NET_ID, mEvent.orig_net_id);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_SERVICEiD, mEvent.serviceID);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_VID, mEvent.vid);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_VFMT, mEvent.vfmt);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_AIDS, mEvent.aids);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_AFMTS, mEvent.afmts);
        bundle.putStringArray(DroidLogicTvUtils.SIG_INFO_C_ALANGS, mEvent.alangs);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_ATYPES, mEvent.atypes);
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_PCR, mEvent.pcr);

        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_STYPES, mEvent.stypes);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_SIDS, mEvent.sids);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_SSTYPES, mEvent.sstypes);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_SID1S, mEvent.sid1s);
        bundle.putIntArray(DroidLogicTvUtils.SIG_INFO_C_SID2S, mEvent.sid2s);
        bundle.putStringArray(DroidLogicTvUtils.SIG_INFO_C_SLANGS, mEvent.slangs);

        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM, -1);

        return bundle;
    }

    private Bundle GetDisplayNumBunlde(int displayNum) {
        Bundle bundle = new Bundle();

        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM, displayNum);

        return bundle;
    }

    public static final int LCN_OVERFLOW_INIT_START = DISPLAY_NUM_START_DEF;//900;

    private ArrayList<ChannelInfo> mChannelsOld = null;

    private boolean on_dtv_channel_store_tschanged = true;

    private ArrayList<ChannelInfo> mChannelsNew = null;
    private int lcn_overflow_start = LCN_OVERFLOW_INIT_START;
    private int display_number_start = DISPLAY_NUM_START_DEF;

    private void initChannelsExist() {
        //get all old channles exist.
        //init display number count.
        if (mChannelsOld == null) {
            String InputId = mSession.getInputId();
            mChannelsOld = mTvDataBaseManager.getChannelList(InputId, Channels.SERVICE_TYPE_AUDIO_VIDEO);
            mChannelsOld.addAll(mTvDataBaseManager.getChannelList(InputId, Channels.SERVICE_TYPE_AUDIO));

            c_displayNum = mChannelsOld.size() + 1;
            Log.d(TAG, "Store> channel next:" + c_displayNum);
        }
    }

    private void onDTVChannelStore(TvControlManager.ScannerEvent event, ChannelInfo channel) {

        if (mScanMode == null) {
            Log.d(TAG, "mScanMode is null, store return.");
            return;
        }

        if (mChannelsNew == null)
            mChannelsNew = new ArrayList();

        mChannelsNew.add(channel);

        Log.d(TAG, "store save [" + channel.getNumber() + "][" + channel.getFrequency() + "][" + channel.getServiceType() + "][" + channel.getDisplayName() + "]");

        if (mScanMode.isDTVManulScan()) {
            if (on_dtv_channel_store_tschanged) {
                on_dtv_channel_store_tschanged = false;
                if (mChannelsOld != null) {
                    Log.d(TAG, "remove channels with freq!="+channel.getFrequency());
                    //remove channles with diff freq from old channles
                    Iterator<ChannelInfo> iter = mChannelsOld.iterator();
                    while (iter.hasNext()) {
                        ChannelInfo c = iter.next();
                        if (c.getFrequency() != channel.getFrequency())
                            iter.remove();
                    }
                }
            }
        }
    }

    private void onTVChannelStoreEnd(boolean isRealtimeStore, boolean isFinalStore) {
        Bundle bundle = null;

        Log.d(TAG, "isRealtimeStore:" + isRealtimeStore + " isFinalStore:"+ isFinalStore);

        if (mChannelsNew != null) {

            /*sort channels by serviceId*/
            Collections.sort(mChannelsNew, new Comparator<ChannelInfo> () {
                @Override
                public int compare(ChannelInfo a, ChannelInfo b) {
                    /*sort: frequency 1st, serviceId 2nd*/
                    int A = a.getFrequency();
                    int B = b.getFrequency();
                    return (A > B) ? 1 : ((A == B) ? (a.getServiceId() - b.getServiceId()) : -1);
                }
            });

            ArrayList<ChannelInfo> mChannels = new ArrayList();

            for (ChannelInfo c : mChannelsNew) {

                //Calc display number / LCN
                if (mSortMode.isLCNSort()) {
                    if (isRealtimeStore)
                        updateChannelLCN(c, mChannels);
                    else
                        updateChannelLCN(c);
                    c.setDisplayNumber(String.valueOf(c.getLCN()));
                    Log.d(TAG, "LCN DisplayNumber:"+ c.getDisplayNumber());
                    Settings.System.putString(this.getContentResolver(), DroidLogicTvUtils.TV_KEY_DTV_NUMBER_MODE, "lcn");
                } else {
                    if (isRealtimeStore)
                        updateChannelNumber(c, mChannels);
                    else
                        updateChannelNumber(c);
                    Log.d(TAG, "NUM DisplayNumber:"+ c.getDisplayNumber());
                }

                if (isRealtimeStore)
                    mTvDataBaseManager.updateOrinsertDtvChannelWithNumber(c);
                else
                    mTvDataBaseManager.insertDtvChannel(c, c.getNumber());

                Log.d(TAG, ((isRealtimeStore) ? "update/insert [" : "insert [") + c.getNumber()
                    + "][" + c.getFrequency() + "][" + c.getServiceType() + "][" + c.getDisplayName() + "]");

                if (isFinalStore) {
                    bundle = GetDisplayNumBunlde(c.getNumber());
                    mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_DISPLAYNUM_EVENT, bundle);
                }
            }
        }

        if (mScanMode != null) {
            if (mScanMode.isDTVManulScan()) {
                if (mChannelsOld != null) {
                    mTvDataBaseManager.deleteChannels(mChannelsOld);
                    for (ChannelInfo c : mChannelsOld)
                        Log.d(TAG, "rm ch[" + c.getNumber() + "][" + c.getDisplayName() + "][" + c.getFrequency() + "]");
                }
            }
        }

        lcn_overflow_start = LCN_OVERFLOW_INIT_START;
        display_number_start = DISPLAY_NUM_START_DEF;
        on_dtv_channel_store_tschanged = true;
        mChannelsOld = null;
        mChannelsNew = null;
    }

    private boolean isChannelInListbyId(ChannelInfo channel, ArrayList<ChannelInfo> list) {
        if (list == null)
            return false;

        for (ChannelInfo c : list)
            if (c.getId() == channel.getId())
                return true;

        return false;
    }

    private void updateChannelNumber(ChannelInfo channel) {
        updateChannelNumber(channel, null);
    }

    private void updateChannelNumber(ChannelInfo channel, ArrayList<ChannelInfo> channels) {
        boolean ignoreDBCheck = false;//mScanMode.isDTVAutoScan();
        int number = -1;
        String InputId = mSession.getInputId();
        ArrayList<ChannelInfo> chs = null;

        if (!ignoreDBCheck) {
            if (channels != null)
                chs = channels;
            else
                chs = mTvDataBaseManager.getChannelList(InputId, ChannelInfo.COMMON_PROJECTION,
                        null, null);
            for (ChannelInfo c : chs) {
                if ((c.getNumber() >= display_number_start) && !isChannelInListbyId(c, mChannelsOld)) {
                    display_number_start = c.getNumber() + 1;
                }
            }
        }
        Log.d(TAG, "display number start from:" + display_number_start);

        Log.d(TAG, "Service["+channel.getOriginalNetworkId()+":"+channel.getTransportStreamId()+":"+channel.getServiceId()+"]");

        if (channel.getServiceType() == Channels.SERVICE_TYPE_OTHER) {
            Log.d(TAG, "Service["+channel.getServiceId()+"] is Type OTHER, ignore NUMBER update and set to unbrowsable");
            channel.setBrowsable(false);
            return;
        }

        if (mChannelsOld != null) {//may only in manual search
            for (ChannelInfo c : mChannelsOld) {
                if ((c.getOriginalNetworkId() == channel.getOriginalNetworkId())
                    && (c.getTransportStreamId() == channel.getTransportStreamId())
                    && (c.getServiceId() == channel.getServiceId())) {
                    //same freq, reuse number if old channel is identical
                    Log.d(TAG, "found num:" + c.getDisplayNumber() + " by same old service["+c.getOriginalNetworkId()+":"+c.getTransportStreamId()+":"+c.getServiceId()+"]");
                    number = c.getNumber();
                }
            }
        }

        //service totally new
        if (number < 0)
            number = display_number_start++;

        Log.d(TAG, "update displayer number["+number+"]");

        channel.setDisplayNumber(String.valueOf(number));

        if (channels != null)
            channels.add(channel);
    }

    private void updateChannelLCN(ChannelInfo channel) {
        updateChannelLCN(channel, null);
    }

    private void updateChannelLCN(ChannelInfo channel, ArrayList<ChannelInfo> channels) {
        boolean ignoreDBCheck = false;//mScanMode.isDTVAutoScan();

        int lcn = -1;
        int lcn_1 = -1;
        int lcn_2 = -1;
        boolean visible = true;
        boolean swapped = false;

        String InputId = mSession.getInputId();

        ArrayList<ChannelInfo> chs = null;

        if (!ignoreDBCheck) {
            if (channels != null)
                chs = channels;
            else
                chs = mTvDataBaseManager.getChannelList(InputId, ChannelInfo.COMMON_PROJECTION,
                        null, null);
            for (ChannelInfo c : chs) {
                if ((c.getLCN() >= lcn_overflow_start) && !isChannelInListbyId(c, mChannelsOld)) {
                    lcn_overflow_start = c.getLCN() + 1;
                }
            }
        }
        Log.d(TAG, "lcn overflow start from:"+lcn_overflow_start);

        Log.d(TAG, "Service["+channel.getOriginalNetworkId()+":"+channel.getTransportStreamId()+":"+channel.getServiceId()+"]");

        if (channel.getServiceType() == Channels.SERVICE_TYPE_OTHER) {
            Log.d(TAG, "Service["+channel.getServiceId()+"] is Type OTHER, ignore LCN update and set to unbrowsable");
            channel.setBrowsable(false);
            return;
        }

        if (mLcnInfo != null) {
            for (TvControlManager.ScannerLcnInfo l : mLcnInfo) {
                if ((l.netId == channel.getOriginalNetworkId())
                    && (l.tsId == channel.getTransportStreamId())
                    && (l.serviceId == channel.getServiceId())) {

                    Log.d(TAG, "lcn found:");
                    Log.d(TAG, "\tlcn[0:"+l.lcn[0]+":"+l.visible[0]+":"+l.valid[0]+"]");
                    Log.d(TAG, "\tlcn[1:"+l.lcn[1]+":"+l.visible[1]+":"+l.valid[1]+"]");

                    // lcn found, use lcn[0] by default.
                    lcn_1 = l.valid[0] == 0 ? -1 : l.lcn[0];
                    lcn_2 = l.valid[1] == 0 ? -1 : l.lcn[1];
                    lcn = lcn_1;
                    visible = l.visible[0] == 0 ? false : true;

                    if ((lcn_1 != -1) && (lcn_2 != -1) && !ignoreDBCheck) {
                        // check for lcn already exist just on Maunual Scan
                        // look for service with sdlcn equal to l's hdlcn, if found, change the service's lcn to it's hdlcn
                        ChannelInfo ch = null;
                        if (channels != null) {
                            for (ChannelInfo c : channels) {
                                if (c.getLCN() == lcn_2)
                                    ch = c;
                            }
                        } else {
                            chs = mTvDataBaseManager.getChannelList(InputId, ChannelInfo.COMMON_PROJECTION,
                                    ChannelInfo.COLUMN_LCN+"=?",
                                    new String[]{String.valueOf(lcn_2)});
                            if (chs.size() > 0) {
                                if (chs.size() > 1)
                                    Log.d(TAG, "Warning: found " + chs.size() + "channels with lcn="+lcn_2);
                                ch = chs.get(0);
                            }
                        }
                        if ((ch != null) && !isChannelInListbyId(ch, mChannelsOld)) {// do not check those will be deleted.
                            Log.d(TAG, "swap exist lcn["+ch.getLCN()+"] -> ["+ch.getLCN2()+"]");
                            Log.d(TAG, "\t for Service["+ch.getOriginalNetworkId()+":"+ch.getTransportStreamId()+":"+ch.getServiceId()+"]");

                            ch.setLCN(ch.getLCN2());
                            ch.setLCN1(ch.getLCN2());
                            ch.setLCN2(lcn_2);
                            if (channels == null)
                                mTvDataBaseManager.updateChannelInfo(ch);

                            swapped = true;
                        }
                    } else if (lcn_1 == -1) {
                        lcn = lcn_2;
                        visible = l.visible[1] == 0 ? false : true;
                        Log.d(TAG, "lcn[0] invalid, use lcn[1]");
                    }
                }
            }
        }

        Log.d(TAG, "Service visible = "+visible);
        channel.setBrowsable(visible);

        if (!swapped) {
            if (lcn >= 0) {
                ChannelInfo ch = null;
                if (channels != null) {
                    for (ChannelInfo c : channels) {
                        if (c.getLCN() == lcn)
                            ch = c;
                    }
                } else {
                    chs = mTvDataBaseManager.getChannelList(InputId, ChannelInfo.COMMON_PROJECTION,
                            ChannelInfo.COLUMN_LCN+"=?",
                            new String[]{String.valueOf(lcn)});
                    if (chs.size() > 0) {
                        if (chs.size() > 1)
                            Log.d(TAG, "Warning: found " + chs.size() + "channels with lcn="+lcn);
                        ch = chs.get(0);
                    }
                }
                if (ch != null) {
                    if (!isChannelInListbyId(ch, mChannelsOld)) {//do not check those will be deleted.
                        Log.d(TAG, "found lcn conflct:" + lcn + " by service["+ch.getOriginalNetworkId()+":"+ch.getTransportStreamId()+":"+ch.getServiceId()+"]");
                        lcn = lcn_overflow_start++;
                    }
                }
            } else {
                Log.d(TAG, "no LCN info found for service");
                if (mChannelsOld != null) {//may only in manual search
                    for (ChannelInfo c : mChannelsOld) {
                        if ((c.getOriginalNetworkId() == channel.getOriginalNetworkId())
                            && (c.getTransportStreamId() == channel.getTransportStreamId())
                            && (c.getServiceId() == channel.getServiceId())) {
                            //same freq, reuse lcn if old channel is identical
                            Log.d(TAG, "found lcn:" + c.getLCN() + " by same old service["+c.getOriginalNetworkId()+":"+c.getTransportStreamId()+":"+c.getServiceId()+"]");
                            lcn = c.getLCN();
                        }
                    }
                }
                //service totally new
                if (lcn < 0)
                    lcn = lcn_overflow_start++;
            }
        }

        Log.d(TAG, "update LCN[0:"+lcn+" 1:"+lcn_1+" 2:"+lcn_2+"]");

        channel.setLCN(lcn);
        channel.setLCN1(lcn_1);
        channel.setLCN2(lcn_2);

        if (channels != null)
            channels.add(channel);
    }

    @Override
    public void onFrameStable(TvControlManager.ScanningFrameStableEvent event) {
        Log.d(TAG, "scanning frame stable!");
        Bundle bundle = new Bundle();
        bundle.putInt(DroidLogicTvUtils.SIG_INFO_C_FREQ, event.CurScanningFrq);
        mSession.notifySessionEvent(DroidLogicTvUtils.SIG_INFO_C_SCANNING_FRAME_STABLE_EVENT, bundle);
    }

    public void onUpdateCurrentChannel(ChannelInfo channel, boolean store) {}

}


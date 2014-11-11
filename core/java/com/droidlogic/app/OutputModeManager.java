package com.droidlogic.app;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OutputModeManager {

    private static final String TAG = "OutputModeManager";
    private static final boolean DEBUG = false;

    /**
     * The saved value for Outputmode auto-detection.
     * One integer
     * @hide
     */
    public static final String DISPLAY_OUTPUTMODE_AUTO = "display_outputmode_auto";

    /**
     *  broadcast of the current HDMI output mode changed.
     */
    public final static String ACTION_HDMI_MODE_CHANGED = "android.intent.action.HDMI_MODE_CHANGED";

    /**
     * Extra in {@link #ACTION_HDMI_MODE_CHANGED} indicating the mode:
     */
    public final static String EXTRA_HDMI_MODE = "mode";

    private static final String CVBS_MODE_PROP = "ubootenv.var.cvbsmode";
    private static final String HDMI_MODE_PROP = "ubootenv.var.hdmimode";
    private static final String COMMON_MODE_PROP = "ubootenv.var.outputmode";
    private final String PASSTHROUGH_PROPERTY = "ubootenv.var.digitaudiooutput";

    private final String DigitalRawFile = "/sys/class/audiodsp/digital_raw";
    private final String mAudoCapFile = "/sys/class/amhdmitx/amhdmitx0/aud_cap";
    private final String HDMI_AUIDO_SWITCH = "/sys/class/amhdmitx/amhdmitx0/config";
    private final String SPDIF_AUIDO_SWITCH ="/sys/devices/platform/spdif-dit.0/spdif_mute";

    private static final String PpscalerRectFile = "/sys/class/ppmgr/ppscaler_rect";
    private static final String UpdateFreescaleFb0File = "/sys/class/graphics/fb0/update_freescale";
    private static final String FreescaleFb0File = "/sys/class/graphics/fb0/free_scale";
    private static final String FreescaleFb1File = "/sys/class/graphics/fb1/free_scale";
    private static final String mHdmiPluggedVdac = "/sys/class/aml_mod/mod_off";
    private static final String mHdmiUnpluggedVdac = "/sys/class/aml_mod/mod_on";
    private static final String HDMI_SUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private static final String PpscalerFile = "/sys/class/ppmgr/ppscaler";
    private static final String VideoAxisFile = "/sys/class/video/axis";
    private static final String request2XScaleFile = "/sys/class/graphics/fb0/request2XScale";
    private static final String scaleAxisOsd0File = "/sys/class/graphics/fb0/scale_axis";
    private static final String scaleAxisOsd1File = "/sys/class/graphics/fb1/scale_axis";
    private static final String scaleOsd1File = "/sys/class/graphics/fb1/scale";
    private static final String OutputModeFile = "/sys/class/display/mode";
    private static final String OutputAxisFile= "/sys/class/display/axis";
    private static final String windowAxisFile = "/sys/class/graphics/fb0/window_axis";
    private static final String blankFb0File = "/sys/class/graphics/fb0/blank";

    private static final String[] COMMON_MODE_VALUE_LIST =  { "480i","480p","576i","576p","720p","1080i","1080p","720p50hz","1080i50hz","1080p50hz","480cvbs","576cvbs","4k2k24hz","4k2k25hz", "4k2k30hz", "4k2ksmpte","1080p24hz" };

    private final static String sel_480ioutput_x = "ubootenv.var.480ioutputx";
    private final static String sel_480ioutput_y = "ubootenv.var.480ioutputy";
    private final static String sel_480ioutput_width = "ubootenv.var.480ioutputwidth";
    private final static String sel_480ioutput_height = "ubootenv.var.480ioutputheight";
    private final static String sel_480poutput_x = "ubootenv.var.480poutputx";
    private final static String sel_480poutput_y = "ubootenv.var.480poutputy";
    private final static String sel_480poutput_width = "ubootenv.var.480poutputwidth";
    private final static String sel_480poutput_height = "ubootenv.var.480poutputheight";
    private final static String sel_576ioutput_x = "ubootenv.var.576ioutputx";
    private final static String sel_576ioutput_y = "ubootenv.var.576ioutputy";
    private final static String sel_576ioutput_width = "ubootenv.var.576ioutputwidth";
    private final static String sel_576ioutput_height = "ubootenv.var.576ioutputheight";
    private final static String sel_576poutput_x = "ubootenv.var.576poutputx";
    private final static String sel_576poutput_y = "ubootenv.var.576poutputy";
    private final static String sel_576poutput_width = "ubootenv.var.576poutputwidth";
    private final static String sel_576poutput_height = "ubootenv.var.576poutputheight";
    private final static String sel_720poutput_x = "ubootenv.var.720poutputx";
    private final static String sel_720poutput_y = "ubootenv.var.720poutputy";
    private final static String sel_720poutput_width = "ubootenv.var.720poutputwidth";
    private final static String sel_720poutput_height = "ubootenv.var.720poutputheight";
    private final static String sel_1080ioutput_x = "ubootenv.var.1080ioutputx";
    private final static String sel_1080ioutput_y = "ubootenv.var.1080ioutputy";
    private final static String sel_1080ioutput_width = "ubootenv.var.1080ioutputwidth";
    private final static String sel_1080ioutput_height = "ubootenv.var.1080ioutputheight";
    private final static String sel_1080poutput_x = "ubootenv.var.1080poutputx";
    private final static String sel_1080poutput_y = "ubootenv.var.1080poutputy";
    private final static String sel_1080poutput_width = "ubootenv.var.1080poutputwidth";
    private final static String sel_1080poutput_height = "ubootenv.var.1080poutputheight";
    private final static String sel_4k2k24hzoutput_x = "ubootenv.var.4k2k24hz_x";
    private final static String sel_4k2k24hzoutput_y = "ubootenv.var.4k2k24hz_y";
    private final static String sel_4k2k24hzoutput_width = "ubootenv.var.4k2k24hz_width";
    private final static String sel_4k2k24hzoutput_height = "ubootenv.var.4k2k24hz_height";
    private final static String sel_4k2k25hzoutput_x = "ubootenv.var.4k2k25hz_x";
    private final static String sel_4k2k25hzoutput_y = "ubootenv.var.4k2k25hz_y";
    private final static String sel_4k2k25hzoutput_width = "ubootenv.var.4k2k25hz_width";
    private final static String sel_4k2k25hzoutput_height = "ubootenv.var.4k2k25hz_height";
    private final static String sel_4k2k30hzoutput_x = "ubootenv.var.4k2k30hz_x";
    private final static String sel_4k2k30hzoutput_y = "ubootenv.var.4k2k30hz_y";
    private final static String sel_4k2k30hzoutput_width = "ubootenv.var.4k2k30hz_width";
    private final static String sel_4k2k30hzoutput_height = "ubootenv.var.4k2k30hz_height";
    private final static String sel_4k2ksmpteoutput_x = "ubootenv.var.4k2ksmpte_x";
    private final static String sel_4k2ksmpteoutput_y = "ubootenv.var.4k2ksmpte_y";
    private final static String sel_4k2ksmpteoutput_width = "ubootenv.var.4k2ksmpte_width";
    private final static String sel_4k2ksmpteoutput_height = "ubootenv.var.4k2ksmpte_height";

    private static final int OUTPUT480_FULL_WIDTH = 720;
    private static final int OUTPUT480_FULL_HEIGHT = 480;
    private static final int OUTPUT576_FULL_WIDTH = 720;
    private static final int OUTPUT576_FULL_HEIGHT = 576;
    private static final int OUTPUT720_FULL_WIDTH = 1280;
    private static final int OUTPUT720_FULL_HEIGHT = 720;
    private static final int OUTPUT1080_FULL_WIDTH = 1920;
    private static final int OUTPUT1080_FULL_HEIGHT = 1080;
    private static final int OUTPUT4k2k_FULL_WIDTH = 3840;
    private static final int OUTPUT4k2k_FULL_HEIGHT = 2160;
    private static final int OUTPUT4k2ksmpte_FULL_WIDTH = 4096;
    private static final int OUTPUT4k2ksmpte_FULL_HEIGHT = 2160;

    private static final String mDisplayAxis1080 = " 1920 1080 ";
    private static final String mDisplayAxis720 = " 1280 720 ";
    private static final String mDisplayAxis576 = " 720 576 ";
    private static final String mDisplayAxis480 = " 720 480 ";

    private static final String FREQ_DEFAULT = "";
    private static final String FREQ_SETTING = "50hz";

    private static boolean ifModeSetting = false;
    private final Context mContext;
    final Object mLock = new Object[0];

    private SystemControlManager mSystenControl;

    public OutputModeManager(Context context) {
        mContext = context;

        mSystenControl = new SystemControlManager(context);
    }

    public void setOutputMode(final String mode) {
        setOutputModeNowLocked(mode);
    }

    public void setOutputModeNowLocked(final String mode){
        synchronized (mLock) {
            String curMode = readSysfs(OutputModeFile);
            String newMode = mode;

            if(curMode == null || curMode.length() < 4){
                if (DEBUG)
                    Log.d(TAG,"===== something wrong !!!" );
                curMode =  "720p";
            }
            if (DEBUG)
                Log.d(TAG,"===== change mode form *" + curMode + "* to *"+ newMode+"* ");
            if(newMode.equals(curMode)){
                if (DEBUG)
                    Log.d(TAG,"===== The same mode as current , do nothing !");
                return ;
            }

            shadowScreen(curMode);

            if(newMode.contains("cvbs")){
                 openVdac(newMode);
            }else{
                 closeVdac(newMode);
            }

            writeSysfs(OutputModeFile, newMode);

            int[] curPosition = getPosition(newMode);
            int[] oldPosition = getPosition(curMode);
            int axis[] = {0, 0, 0, 0};

            String axisStr = readSysfs(VideoAxisFile);
            String[] axisArray = axisStr.split(" ");

            for(int i=0; i<axisArray.length; i++) {
                if(i == axis.length){
                    break;
                }
                try {
                    axis[i] =  Integer.parseInt(axisArray[i]);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String mWinAxis = curPosition[0]+" "+curPosition[1]+" "+(curPosition[0]+curPosition[2]-1)+" "+(curPosition[1]+curPosition[3]-1);

            if(getPropertyBoolean("ro.platform.has.realoutputmode", false)){
                if (getPropertyBoolean("ro.platform.has.native720", false)){
                    if(newMode.contains("1080")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1279 719");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                     }else if(newMode.contains("720")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1279 719");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else if(newMode.contains("576")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1279 719");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else if(newMode.contains("480")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1279 719");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else{
                        Log.d(TAG,"===== can't support this mode : " + newMode);
                        return;
                    }
                }else {
                    if(newMode.contains("4k2k")){   
                        //open freescale ,  scale up from 1080p to 4k
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1919 1079");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");  
                    }else if(newMode.contains("1080")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1919 1079");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");  
                     }else if(newMode.contains("720")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1919 1079");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else if(newMode.contains("576")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1919 1079");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else if(newMode.contains("480")){
                        writeSysfs("/sys/class/graphics/fb0/freescale_mode","1");
                        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1919 1079");
                        writeSysfs("/sys/class/graphics/fb0/window_axis",mWinAxis);
                        writeSysfs("/sys/class/video/axis",mWinAxis);
                        writeSysfs("/sys/class/graphics/fb0/free_scale","0x10001");
                    }else{
                        Log.d(TAG,"===== can't support this mode : " + newMode);
                        return;
                    }
                }
            }else {
                String value = curPosition[0] + " " + curPosition[1]
                    + " " + (curPosition[2] + curPosition[0] )
                    + " " + (curPosition[3] + curPosition[1] )+ " " + 0;
                setM6FreeScaleAxis(newMode);
                writeSysfs(OutputModeFile,newMode);
                writeSysfs(PpscalerRectFile, value);
                writeSysfs(UpdateFreescaleFb0File, "1");
            }

            setProperty(COMMON_MODE_PROP, newMode);
            saveNewMode2Prop(newMode);

            Intent intent = new Intent(ACTION_HDMI_MODE_CHANGED);
            //intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(EXTRA_HDMI_MODE, newMode);
            mContext.sendStickyBroadcast(intent);
        }
        return;
    }

    public void setOutputWithoutFreeScaleLocked(String newMode){
        Log.d(TAG,"===== setOutputWithoutFreeScale()");
        int[] curPosition = { 0, 0, 0, 0 };
        int[] oldPosition = { 0, 0, 0, 0 };
        int axis[] = {0, 0, 0, 0};

        String curMode = readSysfs(OutputModeFile);
        if (DEBUG)
            Log.d(TAG,"===== change mode form *" + curMode + "* to *"+ newMode+"* , WithoutFreeScale");
        if(newMode.equals(curMode)){
            if (DEBUG)
                Log.d(TAG,"===== The same mode as current , do nothing !");
            return ;
        }

        synchronized (mLock) {
            if(newMode.contains("cvbs")){
                 openVdac(newMode);
            }else{
                 closeVdac(newMode);
            }
            shadowScreen(curMode);
            writeSysfs(PpscalerFile, "0");
            writeSysfs(FreescaleFb0File, "0");
            writeSysfs(FreescaleFb1File, "0");
            writeSysfs(OutputModeFile, newMode);
            setProperty(COMMON_MODE_PROP, newMode);
            saveNewMode2Prop(newMode);

            Intent intent = new Intent(ACTION_HDMI_MODE_CHANGED);
            //intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(EXTRA_HDMI_MODE, newMode);
            mContext.sendStickyBroadcast(intent);

            curPosition = getPosition(newMode);
            oldPosition = getPosition(curMode);
            String axisStr = readSysfs(VideoAxisFile);
            String[] axisArray = axisStr.split(" ");

            for(int i=0; i<axisArray.length; i++) {
                if(i == axis.length){
                    break;
                }
                try {
                    axis[i] =  Integer.parseInt(axisArray[i]);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(getPropertyBoolean("ro.platform.has.realoutputmode", false)){
               /* String display_value = curPosition[0] + " "+ curPosition[1] + " "
                        + 1920+ " "+ 1080+ " "
                        + curPosition[0]+ " " + curPosition[1]+ " " + 18+ " " + 18;
                writeSysfs(OutputAxisFile, display_value);
                if (DEBUG)
                    Log.d("OutputSettings", "outputmode change:curPosition[2]:"+curPosition[2]+" curPosition[3]:"+curPosition[3]+"\n");*/
            }else {
                if((newMode.equals(COMMON_MODE_VALUE_LIST[5])) || (newMode.equals(COMMON_MODE_VALUE_LIST[6]))
                            || (newMode.equals(COMMON_MODE_VALUE_LIST[8])) || (newMode.equals(COMMON_MODE_VALUE_LIST[9]))){
                    writeSysfs(OutputAxisFile, ((int)(curPosition[0]/2))*2 + " " + ((int)(curPosition[1]/2))*2 
                        + " 1280 720 "+ ((int)(curPosition[0]/2))*2 + " "+ ((int)(curPosition[1]/2))*2 + " 18 18");
                    writeSysfs(scaleAxisOsd0File, "0 0 " + (960 - (int)(curPosition[0]/2) - 1)
                        + " " + (1080 - (int)(curPosition[1]/2) - 1));
                    writeSysfs(request2XScaleFile, "7 " + ((int)(curPosition[2]/2)) + " " + ((int)(curPosition[3]/2))*2);
                    writeSysfs(scaleAxisOsd1File, "1280 720 " + ((int)(curPosition[2]/2))*2 + " " + ((int)(curPosition[3]/2))*2);
                    writeSysfs(scaleOsd1File, "0x10001");
                }else{
                    writeSysfs(OutputAxisFile, curPosition[0] + " " + curPosition[1]
                        + " 1280 720 "+ curPosition[0] + " "+ curPosition[1] + " 18 18");
                    writeSysfs(request2XScaleFile, "16 " + curPosition[2] + " " + curPosition[3]);
                    writeSysfs(scaleAxisOsd1File, "1280 720 " + curPosition[2] + " " + curPosition[3]);
                    writeSysfs(scaleOsd1File, "0x10001");
                }

                int oldX = oldPosition[0];
                int oldY = oldPosition[1];
                int oldWidth = oldPosition[2];
                int oldHeight = oldPosition[3];
                int curX = curPosition[0];
                int curY = curPosition[1];
                int curWidth = curPosition[2];
                int curHeight = curPosition[3];
                int temp1 = curX;
                int temp2 = curY;
                int temp3 = curWidth;
                int temp4 = curHeight;
                if (DEBUG){
                    Log.d(TAG, "change2NewModeWithoutFreeScale, old is: "
                        + oldX + " " + oldY + " " + oldWidth + " " + oldHeight);
                    Log.d(TAG, "change2NewModeWithoutFreeScale, new is: "
                        + curX + " " + curY + " " + curWidth + " " + curHeight);
                    Log.d(TAG, "change2NewModeWithoutFreeScale, axis is: "
                        + axis[0] + " " + axis[1] + " " + axis[2] + " " + axis[3]);
                }
                if(!((axis[0] == 0) && (axis[1] == 0) && (axis[2] == -1) && (axis[3] == -1))
                        && !((axis[0] == 0) && (axis[1] == 0) && (axis[2] == 0) && (axis[3] == 0))) {
                    temp1 = (axis[0] - oldX) * curWidth / oldWidth + curX;
                    temp2 = (axis[1] - oldY) * curHeight / oldHeight + curY;
                    temp3 = (axis[2] - axis[0] + 1) * curWidth / oldWidth;
                    temp4 = (axis[3] - axis[1] + 1) * curHeight / oldHeight;
                }
                if (DEBUG)
                    Log.d(TAG, "change2NewModeWithoutFreeScale, changed axis is: "
                        + temp1 + " " + temp2 + " " + (temp3 + temp1 - 1) + " " + (temp4 + temp2 - 1));
                writeSysfs(VideoAxisFile, temp1 + " " + temp2 + " "
                    + (temp3 + temp1 - 1) + " " + (temp4 + temp2 - 1));
            }
        }
    }

    private void saveNewMode2Prop(String newMode){
        if((newMode != null) && newMode.contains("cvbs")){
            setProperty(CVBS_MODE_PROP, newMode);
        }
        else{
            setProperty(HDMI_MODE_PROP, newMode);
        }
    }

    private void closeVdac(String outputmode){
       if(getPropertyBoolean("ro.platform.hdmionly",false)){
           if(!outputmode.contains("cvbs")){
               writeSysfs(mHdmiPluggedVdac,"vdac");
           }
       }
    }
    private void openVdac(String outputmode){
        if(getPropertyBoolean("ro.platform.hdmionly",false)){
            if(outputmode.contains("cvbs")){
                writeSysfs(mHdmiUnpluggedVdac,"vdac");
            }
        }
    }

    private void setM6FreeScaleAxis(String mode){
        writeSysfs("/sys/class/graphics/fb0/free_scale_axis","0 0 1279 719");
        writeSysfs("/sys/class/graphics/fb0/free_scale","1");
    }

    public String getHdmiSupportList(){
        String str = null;
        StringBuilder value = new StringBuilder();
        try {
            FileReader fr = new FileReader(HDMI_SUPPORT_LIST_SYSFS);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null){
                        if(str.contains("*")){
                            value.append(str.substring(0,str.length()-1));
                        }else{
                            value.append(str);
                        }
                        value.append(",");
                    }
                };
                fr.close();
                br.close();
                if(value != null){
                    if (DEBUG)
                        Log.d(TAG,"=====TV support list is : " + value.toString());
                    return value.toString();
                }
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public  int[] getPosition(String mode) {
        int[] curPosition = { 0, 0, 1280, 720 };
        int index = 4; // 720p
        for (int i = 0; i < COMMON_MODE_VALUE_LIST.length; i++) {
            if (mode.equalsIgnoreCase(COMMON_MODE_VALUE_LIST[i]))
                 index = i;
        }

        switch (index) {
        case 0: // 480i
            curPosition[0] = getPropertyInt(sel_480ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_480ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_480ioutput_width, OUTPUT480_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_480ioutput_height, OUTPUT480_FULL_HEIGHT);
            break;
        case 1: // 480p
            curPosition[0] = getPropertyInt(sel_480poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_480poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_480poutput_width, OUTPUT480_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_480poutput_height, OUTPUT480_FULL_HEIGHT);
            break;
        case 2: // 576i
            curPosition[0] = getPropertyInt(sel_576ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_576ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_576ioutput_width, OUTPUT576_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_576ioutput_height, OUTPUT576_FULL_HEIGHT);
            break;
        case 3: // 576p
            curPosition[0] = getPropertyInt(sel_576poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_576poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_576poutput_width, OUTPUT576_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_576poutput_height, OUTPUT576_FULL_HEIGHT);
            break;
        case 4: // 720p
        case 7: // 720p50hz
            curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
            break;

        case 5: // 1080i
        case 8: // 1080i50hz
            curPosition[0] = getPropertyInt(sel_1080ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_1080ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_1080ioutput_width, OUTPUT1080_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_1080ioutput_height, OUTPUT1080_FULL_HEIGHT);
            break;

        case 6: // 1080p
        case 9: // 1080p50hz
        case 16://1080p24hz
            curPosition[0] = getPropertyInt(sel_1080poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_1080poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_1080poutput_width, OUTPUT1080_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_1080poutput_height, OUTPUT1080_FULL_HEIGHT);
            break;
        case 10: // 480cvbs
            curPosition[0] = getPropertyInt(sel_480ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_480ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_480ioutput_width, OUTPUT480_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_480ioutput_height, OUTPUT480_FULL_HEIGHT);
            break;
        case 11: // 576cvbs
            curPosition[0] = getPropertyInt(sel_576ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_576ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_576ioutput_width, OUTPUT576_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_576ioutput_height, OUTPUT576_FULL_HEIGHT);
            break;
        case 12: // 4k2k24hz
            curPosition[0] = getPropertyInt(sel_4k2k24hzoutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2k24hzoutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2k24hzoutput_width, OUTPUT4k2k_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2k24hzoutput_height, OUTPUT4k2k_FULL_HEIGHT);
            break;
        case 13: // 4k2k25hz
            curPosition[0] = getPropertyInt(sel_4k2k25hzoutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2k25hzoutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2k25hzoutput_width, OUTPUT4k2k_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2k25hzoutput_height, OUTPUT4k2k_FULL_HEIGHT);
            break;
        case 14: // 4k2k30hz
            curPosition[0] = getPropertyInt(sel_4k2k30hzoutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2k30hzoutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2k30hzoutput_width, OUTPUT4k2k_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2k30hzoutput_height, OUTPUT4k2k_FULL_HEIGHT);
            break;
        case 15: // 4k2ksmpte
            curPosition[0] = getPropertyInt(sel_4k2ksmpteoutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2ksmpteoutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2ksmpteoutput_width, OUTPUT4k2ksmpte_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2ksmpteoutput_height, OUTPUT4k2ksmpte_FULL_HEIGHT);
            break;
        default: // 720p
            curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
            break;
        }

        return curPosition;
    }

    public String getBestMatchResolution() {
        String[] supportList = null;
        String value = readSupportList(HDMI_SUPPORT_LIST_SYSFS);
        if(value.indexOf("480") >= 0 || value.indexOf("576") >= 0
            ||value.indexOf("720") >= 0||value.indexOf("1080") >= 0 || value.indexOf("4k2k") >= 0){
            supportList = (value.substring(0, value.length()-1)).split(",");
            if (DEBUG)
                Log.d(TAG,"===== supportList size() is " + supportList.length);
        }

        if (supportList != null){
            for (int index = 0; index < supportList.length; index++) {
                if (DEBUG)
                    Log.d(TAG,"===== suport mode : " + supportList[index]);
                if (supportList[index].contains("*")) {
                    if (DEBUG)
                        Log.d(TAG,"===== best mode is : " + supportList[index]);
                    String str = supportList[index];
                    return str.substring(0,str.length()-1);
                }
            }
        }

        return getPropertyString("ro.platform.best_outputmode", "720p");
    }

    public String getSupportedResolution() {
        String curMode = getPropertyString("ubootenv.var.hdmimode", "720p");
        String value = readSupportList(HDMI_SUPPORT_LIST_SYSFS);
        String[] supportList = null;

        if(value.indexOf("480") >= 0 || value.indexOf("576") >= 0
            ||value.indexOf("720") >= 0||value.indexOf("1080") >= 0 || value.indexOf("4k2k") >= 0){
            supportList = (value.substring(0, value.length()-1)).split(",");
        }

        if(supportList == null) {
            return curMode;
        }
        for (int index = 0; index < supportList.length; index++) {
            if (supportList[index].equals(curMode)) {
                return curMode;
            }
        }
        curMode = getBestMatchResolution();

        return curMode;
    }

    private String getDisplayAxisByMode(String mode){
        if(mode.indexOf("1080") >= 0)
            return mDisplayAxis1080;
        else if(mode.indexOf("720") >= 0)
            return mDisplayAxis720;
        else if(mode.indexOf("576") >= 0)
            return mDisplayAxis576;
        else
            return mDisplayAxis480;
    }

    public void initOutputMode(){
        String curMode = readSysfs(OutputModeFile);
        if (isHDMIPlugged()){
            if (curMode.contains("cvbs") || !curMode.equals(getSupportedResolution()))
                setHdmiPlugged();
            else
                return;
        } else {
            if (!curMode.contains("cvbs"))
                setHdmiUnPlugged();
            else return;
        }
    }

    public void setHdmiUnPlugged(){
        Log.d(TAG,"===== hdmiUnPlugged()");
        if(getPropertyBoolean("ro.platform.has.realoutputmode", false)){
            if(getPropertyBoolean("ro.platform.hdmionly",true)){     
                String cvbsmode = getPropertyString("ubootenv.var.cvbsmode","576cvbs");
                setOutputMode(cvbsmode);
                synchronized (mLock) {
                    writeSysfs(mHdmiUnpluggedVdac,"vdac");//open vdac
                }
            }
            return ;
        } else {
            if(getPropertyBoolean("ro.platform.hdmionly",true)){  
                String cvbsmode = getPropertyString("ubootenv.var.cvbsmode","576cvbs");
                if(isFreeScaleClosed()){
                    setOutputWithoutFreeScaleLocked(cvbsmode);
                }else{
                    setOutputMode(cvbsmode);
                }
                synchronized (mLock) {
                    writeSysfs(mHdmiUnpluggedVdac,"vdac");//open vdac
                }
            }
        }
    }

    public void setHdmiPlugged(){
        int isAutoHdmiMode = 1;
        /*
        try {
            //isAutoHdmiMode = Settings.Global.getInt(mContext.getContentResolver(), DISPLAY_OUTPUTMODE_AUTO);
        } catch (Settings.SettingNotFoundException se) {
            Log.d(TAG, "Error: "+se);
        }
        */
        Log.d(TAG,"===== hdmiPlugged(): "+isAutoHdmiMode);
        if(getPropertyBoolean("ro.platform.has.realoutputmode", false)){
            if(getPropertyBoolean("ro.platform.hdmionly",true)){
                writeSysfs(mHdmiPluggedVdac,"vdac");
                if(isAutoHdmiMode != 0){
                        setOutputMode(filterResolution(getBestMatchResolution()));
                }else{
                    String mHdmiOutputMode = getSupportedResolution();
                    setOutputMode(mHdmiOutputMode);
                }
            }
            switchHdmiPassthough();
            return;
        } else {
            if(getPropertyBoolean("ro.platform.hdmionly",true)){
                writeSysfs(mHdmiPluggedVdac, "vdac");
                if(isAutoHdmiMode != 0){
                    if (isFreeScaleClosed()) {
                        setOutputWithoutFreeScaleLocked(filterResolution(getBestMatchResolution()));
                    }else{
                        setOutputMode(filterResolution(getBestMatchResolution()));
                    }

                }else{
                    String mHdmiOutputMode = getSupportedResolution();
                    if(isFreeScaleClosed())
                        setOutputWithoutFreeScaleLocked(mHdmiOutputMode);
                    else
                        setOutputMode(mHdmiOutputMode);
                }
                switchHdmiPassthough();
                writeSysfs(blankFb0File,"0");
            }
        }
    }

    public boolean isFreeScaleClosed(){
        String freeScaleStatus = readSysfs(FreescaleFb0File);
        if(freeScaleStatus.contains("0x0")){
            Log.d(TAG,"freescale is closed");
            return true;
        }else{
            Log.d(TAG,"freescale is open");
            return false;
        }
    }

    public String filterResolution(String resolution){
        if (resolution.contains("480i")) {
            resolution = "480i";
        } else if(resolution.contains("480cvbs")){
            resolution = "480cvbs";
        }else if (resolution.contains("480p")) {
            resolution = "480p";
        } else if (resolution.contains("576i")) {
            resolution = "576i";
        } else if (resolution.contains("576cvbs")) {
            resolution = "576cvbs";
        } else if (resolution.contains("576p")) {
            resolution = "576p";
        } else if (resolution.contains("720p")) {
            if (resolution.contains(FREQ_SETTING)) {
                resolution = "720p" + FREQ_SETTING;
            } else {
                resolution ="720p" + FREQ_DEFAULT;
            }
        } else if (resolution.contains("1080i")) {
            if (resolution.contains(FREQ_SETTING)) {
                resolution = "1080i" + FREQ_SETTING;
            } else {
                resolution = "1080i" + FREQ_DEFAULT;
            }
        } else if (resolution.contains("1080p")) {
            if (resolution.contains(FREQ_SETTING)) {
                resolution = "1080p" + FREQ_SETTING;
            } else {
                resolution = "1080p" + FREQ_DEFAULT;
            }
        }

        return resolution;
    }

    public boolean isHDMIPlugged() {
        String status = readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");
        if ("1".equals(status))
            return true;
        else
            return false;
    }

    private  String readSupportList(String path) {
        String str = null;
        StringBuilder value = new StringBuilder();
        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null){
                        value.append(str);
                        value.append(",");
                    }
                };
                fr.close();
                br.close();
                if(value != null){    
                    Log.d(TAG,"===== TV support list is : " + value.toString());
                    return value.toString();
                }
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean ifModeIsSetting() {
        return ifModeSetting;
    }

    private void shadowScreen(final String mode){
        writeSysfs(blankFb0File, "1");
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ifModeSetting = true;
                    Thread.sleep(1000);
                    writeSysfs(blankFb0File, "0");
                    ifModeSetting = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        task.start();
    }

    private void switchHdmiPassthough(){
        String value = getPropertyString(PASSTHROUGH_PROPERTY, "PCM");

        if(value.contains(":auto")){
            autoSwitchHdmiPassthough();
        }else{
            setDigitalVoiceValue(value);
        }
    }

    public int autoSwitchHdmiPassthough (){

        String mAudioCapInfo = readSysfs(mAudoCapFile);
        if(mAudioCapInfo.contains("Dobly_Digital+")){
            writeSysfs(DigitalRawFile,"2");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "HDMI passthrough:auto");
            return 2;
        }else if(mAudioCapInfo.contains("AC-3")){
            writeSysfs(DigitalRawFile,"1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_unmute");
            setProperty(PASSTHROUGH_PROPERTY, "SPDIF passthrough:auto");
            return 1;
        }else{
            writeSysfs(DigitalRawFile,"0");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "PCM:auto");
            return 0;
        }
    }

    public void setDigitalVoiceValue(String value) {
        // value : "PCM" ,"RAW","SPDIF passthrough","HDMI passthrough"
        setProperty(PASSTHROUGH_PROPERTY, value);

        if ("PCM".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        } else if ("RAW".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_unmute");
        } else if ("SPDIF passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_unmute");
        } else if ("HDMI passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "2");
            writeSysfs(SPDIF_AUIDO_SWITCH, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        }
    }

    public void enableDobly_DRC (boolean enable){
        if (enable){       //open DRC
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drchighcutscale 0x64");
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drclowboostscale 0x64");
        } else {           //close DRC
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drchighcutscale 0");
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drclowboostscale 0");
        }
    }

    public void setDoblyMode (String mode){
        //"CUSTOM_0","CUSTOM_1","LINE","RF"; default use "LINE"
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 3){
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drcmode" + " " + mode);
        } else {
            writeSysfs("/sys/class/audiodsp/ac3_drc_control", "drcmode" + " " + "2");
        }
    }

    public void setDTS_DownmixMode(String mode){
        // 0: Lo/Ro;   1: Lt/Rt;  default 0
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 1){
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdmxmode" + " " + mode);
        } else {
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdmxmode" + " " + "0");
        }
    }

    public void enableDTS_DRC_scale_control (boolean enable){
        if (enable) {
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdrcscale 0x64");
        } else {
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdrcscale 0");
        }
    }

    public void enableDTS_Dial_Norm_control (boolean enable){
        if (enable) {
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdialnorm 1");
        } else {
            writeSysfs("/sys/class/audiodsp/dts_dec_control", "dtsdialnorm 0");
        }
    }

    private String getProperty(String key){
        if(DEBUG)
            Log.i(TAG, "getProperty key:" + key);
        return mSystenControl.getProperty(key);
    }

    private String getPropertyString(String key, String def){
        if(DEBUG)
            Log.i(TAG, "getPropertyString key:" + key + " def:" + def);
        return mSystenControl.getPropertyString(key, def);
    }

    private int getPropertyInt(String key,int def){
        if(DEBUG)
            Log.i(TAG, "getPropertyInt key:" + key + " def:" + def);
        return mSystenControl.getPropertyInt(key, def);
    }

    private long getPropertyLong(String key,long def){
        if(DEBUG)
            Log.i(TAG, "getPropertyLong key:" + key + " def:" + def);
        return mSystenControl.getPropertyLong(key, def);
    }

    private boolean getPropertyBoolean(String key,boolean def){
        if(DEBUG)
            Log.i(TAG, "getPropertyBoolean key:" + key + " def:" + def);
        return mSystenControl.getPropertyBoolean(key, def);
    }

    private void setProperty(String key, String value){
        if(DEBUG)
            Log.i(TAG, "setProperty key:" + key + " value:" + value);
        mSystenControl.setProperty(key, value);
    }

    private String readSysfs(String path) {

        return mSystenControl.readSysFs(path);
        /*
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return null;
        }

        String str = null;
        StringBuilder value = new StringBuilder();

        if(DEBUG)
            Log.i(TAG, "readSysfs path:" + path);

        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null)
                        value.append(str);
                };
                fr.close();
                br.close();
                if(value != null)
                    return value.toString();
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        */
    }

    private boolean writeSysfs(String path, String value) {
        if(DEBUG)
            Log.i(TAG, "writeSysfs path:" + path + " value:" + value);

        return mSystenControl.writeSysFs(path, value);
        /*
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return false;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path), 64);
            try {
                writer.write(value);
            } finally {
                writer.close();
            }
            return true;

        } catch (IOException e) {
            Log.e(TAG, "IO Exception when write: " + path, e);
            return false;
        }
        */
    }
}


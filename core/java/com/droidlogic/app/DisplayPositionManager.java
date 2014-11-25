package com.droidlogic.app;

import android.content.Context;
import android.util.Log;

public class DisplayPositionManager {
    private final static String TAG = "DisplayPositionManager";
    private final static boolean DEBUG = false;
    private Context mContext = null;
    private SystemControlManager sw = null;
    private OutputModeManager om = null;

    private final static int MAX_Height = 100;
    private final static int MIN_Height = 80;
    private static int screen_rate = MAX_Height;

    // sysfs path
    private final static String DISPLAY_MODE                        = "/sys/class/display/mode";
    private final static String FB0_FREE_SCALE_UPDATE               = "/sys/class/graphics/fb0/update_freescale";
    private final static String FB0_FREE_SCALE                      = "/sys/class/graphics/fb0/free_scale";
    private static final String FB0_WINDOW_AXIS                     = "/sys/class/graphics/fb0/window_axis";
    private final static String PPMGR_PPSCALER_RECT                 = "/sys/class/ppmgr/ppscaler_rect";
    private final static String CPU0_SCALING_MIN_FREQ               = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";

    private String mCurrentLeftString = null;
    private String mCurrentTopString = null;
    private String mCurrentWidthString = null;
    private String mCurrentHeightString = null;

    private int mCurrentLeft = 0;
    private int mCurrentTop = 0;
    private int mCurrentWidth = 0;
    private int mCurrentHeight = 0;
    private int mCurrentRate = MAX_Height;
    private int mCurrentRight = 0;
    private int mCurrentBottom = 0;

    private int mPreLeft = 0;
    private int mPreTop = 0;
    private int mPreRight = 0;
    private int mPreBottom = 0;
    private int mPreWidth = 0;
    private int mPreHeight = 0;

    private  String mCurrentMode = null;

    private int mMaxRight = 0;
    private int mMaxBottom=0;
    private int offsetStep = 2;  // because 20% is too large ,so we divide a value to smooth the view

    public DisplayPositionManager(Context context) {
        mContext = context;
        sw = new SystemControlManager(mContext);
        om = new OutputModeManager(mContext);
        initPostion();
    }

    public void initPostion() {
        mCurrentMode = sw.readSysFs(DISPLAY_MODE).replaceAll("\n","");
        initStep(mCurrentMode);
        initCurrentPostion();
        screen_rate = getInitialRateValue();
        if (!sw.getPropertyBoolean("ro.platform.has.realoutputmode", false)) {
            writeFile(FB0_FREE_SCALE , "1");
        }
        setScalingMinFreq(408000);
    }

    private void initCurrentPostion() {
        int [] position = om.getPosition(mCurrentMode);
        mPreLeft = mCurrentLeft = position[0];
        mPreRight = mCurrentTop  = position[1];
        mPreWidth = mCurrentWidth = position[2];
        mPreHeight = mCurrentHeight= position[3];
    }

    public int getInitialRateValue() {
        mCurrentMode = sw.readSysFs(DISPLAY_MODE).replaceAll("\n","");
        initStep(mCurrentMode);
        int m = (100*2*offsetStep)*mPreLeft ;
        if (m == 0) {
            return 100;
        }
        int rate =  100 - m/(mMaxRight+1) - 1;
        return rate;
    }

    public int getCurrentRateValue(){
        return screen_rate;
    }

    private void zoom(int step) {
        screen_rate = screen_rate + step;
        if (screen_rate >MAX_Height) {
            screen_rate = MAX_Height;
        }else if (screen_rate <MIN_Height) {
            screen_rate = MIN_Height ;
        }
        zoomByPercent(screen_rate);
    }

    public void zoomIn(){
        zoom(1);
    }

    public void zoomOut(){
        zoom(-1);
    }

    public void saveDisplayPosition() {
        if ( !isScreenPositionChanged())
            return;

        om.savePosition(mCurrentLeft, mCurrentTop, mCurrentWidth, mCurrentHeight);
        setScalingMinFreq(96000);
    }

    private void writeFile(String file, String value) {
        sw.writeSysFs(file, value);
    }

    private final void setScalingMinFreq(int scalingMinFreq) {
        int minFreq = scalingMinFreq;
        String minFreqString = Integer.toString(minFreq);

        sw.writeSysFs(CPU0_SCALING_MIN_FREQ, minFreqString);
    }
    private void initStep(String mode) {
        if (mode.contains("480")) {
            mMaxRight = 719;
            mMaxBottom = 479;
        }else if (mode.contains("576")) {
            mMaxRight = 719;
            mMaxBottom = 575;
        }else if (mode.contains("720")) {
            mMaxRight = 1279;
            mMaxBottom = 719;
        }else if (mode.contains("1080")) {
            mMaxRight = 1919;
            mMaxBottom = 1079;
        }else if (mode.contains("4k")) {
            if (mode.contains("4k2ksmpte")) {
                mMaxRight = 4096;
                mMaxBottom = 2059;
            }else {
                mMaxRight = 3839;
                mMaxBottom = 2159;
            }
        }else {
            mMaxRight = 1919;
            mMaxBottom = 1079;
        }
    }

    public void zoomByPercent(int percent){

        if (percent > 100 ) {
            percent = 100;
            return ;
        }

        if (percent < 80 ) {
            percent = 80;
            return ;
        }

        mCurrentMode = sw.readSysFs(DISPLAY_MODE).replaceAll("\n","");
        initStep(mCurrentMode);

        mCurrentLeft = (100-percent)*(mMaxRight)/(100*2*offsetStep);
        mCurrentTop  = (100-percent)*(mMaxBottom)/(100*2*offsetStep);
        mCurrentRight = mMaxRight - mCurrentLeft;
        mCurrentBottom = mMaxBottom - mCurrentTop;
        mCurrentWidth = mCurrentRight - mCurrentLeft;
        mCurrentHeight = mCurrentBottom - mCurrentTop ;

        setPosition(mCurrentLeft, mCurrentTop,mCurrentRight, mCurrentBottom, 0);
    }
    private void setPosition(int l, int t, int r, int b, int mode) {
        String str = "";
        int left =  l;
        int top =  t;
        int right =  r;
        int bottom =  b;
        int width = mCurrentWidth;
        int hight = mCurrentHeight;

        if (left < 0) {
            left = 0 ;
        }

        if (top < 0) {
            top = 0 ;
        }
        right = Math.min(right,mMaxRight);
        bottom = Math.min(bottom,mMaxBottom);

        if (sw.getPropertyBoolean("ro.platform.has.realoutputmode", false)) {
            writeFile(FB0_WINDOW_AXIS, left+" "+top+" "+(right-1)+" "+(bottom-1));
            //writeFile(free_scale,"0x10001");
        } else {
            str = left + " " + top + " " + right + " " + bottom + " " + mode;
            writeFile(PPMGR_PPSCALER_RECT, str);
            writeFile(FB0_FREE_SCALE_UPDATE, "1");
        }
    }

    public boolean isScreenPositionChanged(){
        if (mPreLeft== mCurrentLeft && mPreTop == mCurrentTop
            && mPreWidth == mCurrentWidth && mPreHeight == mCurrentHeight)
            return false;
        else
            return true;
    }
}

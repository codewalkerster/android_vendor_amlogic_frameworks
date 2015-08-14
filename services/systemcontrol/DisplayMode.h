/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  @author   Tellen Yu
 *  @version  2.0
 *  @date     2014/10/23
 *  @par function description:
 *  - 1 set display mode
 */

#ifndef ANDROID_DISPLAY_MODE_H
#define ANDROID_DISPLAY_MODE_H

#include <linux/fb.h>
#include "SysWrite.h"
#include "common.h"

#define DEVICE_STR_MID                  "MID"
#define DEVICE_STR_MBOX                 "MBOX"
#define DEVICE_STR_TV                   "TV"

#define DEFAULT_OUTPUT_MODE             "1080p60hz"
#define DISPLAY_CFG_FILE                "/system/etc/mesondisplay.cfg"
#define DISPLAY_FB0                     "/dev/graphics/fb0"
#define DISPLAY_FB1                     "/dev/graphics/fb1"
#define SYSFS_DISPLAY_MODE              "/sys/class/display/mode"
#define SYSFS_DISPLAY_MODE2             "/sys/class/display2/mode"
#define SYSFS_DISPLAY_AXIS              "/sys/class/display/axis"
#define SYSFS_VIDEO_AXIS                "/sys/class/video/axis"
#define DISPLAY_FB0_BLANK               "/sys/class/graphics/fb0/blank"
#define DISPLAY_FB1_BLANK               "/sys/class/graphics/fb1/blank"

#define DISPLAY_FB0_FREESCALE           "/sys/class/graphics/fb0/free_scale"
#define DISPLAY_FB1_FREESCALE           "/sys/class/graphics/fb1/free_scale"
#define DISPLAY_FB0_FREESCALE_MODE      "/sys/class/graphics/fb0/freescale_mode"
#define DISPLAY_FB1_FREESCALE_MODE      "/sys/class/graphics/fb1/freescale_mode"
#define DISPLAY_FB0_SCALE_AXIS          "/sys/class/graphics/fb0/scale_axis"
#define DISPLAY_FB1_SCALE_AXIS          "/sys/class/graphics/fb1/scale_axis"
#define DISPLAY_FB1_SCALE               "/sys/class/graphics/fb1/scale"

#define DISPLAY_FB0_FREESCALE_AXIS      "/sys/class/graphics/fb0/free_scale_axis"
#define DISPLAY_FB0_WINDOW_AXIS         "/sys/class/graphics/fb0/window_axis"

#define DISPLAY_HPD_STATE               "/sys/class/amhdmitx/amhdmitx0/hpd_state"
#define DISPLAY_HDMI_EDID               "/sys/class/amhdmitx/amhdmitx0/disp_cap"
#define DISPLAY_HDMI_AVMUTE             "/sys/devices/virtual/amhdmitx/amhdmitx0/avmute"

#define AUDIO_DSP_DIGITAL_RAW           "/sys/class/audiodsp/digital_raw"

#define PROP_HDMIONLY                   "ro.platform.hdmionly"
#define PROP_LCD_DENSITY                "ro.sf.lcd_density"
#define PROP_WINDOW_WIDTH               "const.window.w"
#define PROP_WINDOW_HEIGHT              "const.window.h"
#define PROP_HAS_CVBS_MODE              "ro.platform.has.cvbsmode"
#define PROP_BEST_OUTPUT_MODE           "ro.platform.best_outputmode"
#define PROP_BOOTANIM                   "init.svc.bootanim"
#define PROP_FS_MODE                    "const.filesystem.mode"
#define PROP_BOOTANIM_DELAY             "const.bootanim.delay"


#define ENV_480I_X                      "ubootenv.var.480i_x"
#define ENV_480I_Y                      "ubootenv.var.480i_y"
#define ENV_480I_W                      "ubootenv.var.480i_w"
#define ENV_480I_H                      "ubootenv.var.480i_h"
#define ENV_480P_X                      "ubootenv.var.480p_x"
#define ENV_480P_Y                      "ubootenv.var.480p_y"
#define ENV_480P_W                      "ubootenv.var.480p_w"
#define ENV_480P_H                      "ubootenv.var.480p_h"
#define ENV_576I_X                      "ubootenv.var.576i_x"
#define ENV_576I_Y                      "ubootenv.var.576i_y"
#define ENV_576I_W                      "ubootenv.var.576i_w"
#define ENV_576I_H                      "ubootenv.var.576i_h"
#define ENV_576P_X                      "ubootenv.var.576p_x"
#define ENV_576P_Y                      "ubootenv.var.576p_y"
#define ENV_576P_W                      "ubootenv.var.576p_w"
#define ENV_576P_H                      "ubootenv.var.576p_h"
#define ENV_720P_X                      "ubootenv.var.720p_x"
#define ENV_720P_Y                      "ubootenv.var.720p_y"
#define ENV_720P_W                      "ubootenv.var.720p_w"
#define ENV_720P_H                      "ubootenv.var.720p_h"
#define ENV_1080I_X                     "ubootenv.var.1080i_x"
#define ENV_1080I_Y                     "ubootenv.var.1080i_y"
#define ENV_1080I_W                     "ubootenv.var.1080i_w"
#define ENV_1080I_H                     "ubootenv.var.1080i_h"
#define ENV_1080P_X                     "ubootenv.var.1080p_x"
#define ENV_1080P_Y                     "ubootenv.var.1080p_y"
#define ENV_1080P_W                     "ubootenv.var.1080p_w"
#define ENV_1080P_H                     "ubootenv.var.1080p_h"
#define ENV_4K2K24HZ_X                  "ubootenv.var.4k2k24hz_x"
#define ENV_4K2K24HZ_Y                  "ubootenv.var.4k2k24hz_y"
#define ENV_4K2K24HZ_W                  "ubootenv.var.4k2k24hz_w"
#define ENV_4K2K24HZ_H                  "ubootenv.var.4k2k24hz_h"
#define ENV_4K2K25HZ_X                  "ubootenv.var.4k2k25hz_x"
#define ENV_4K2K25HZ_Y                  "ubootenv.var.4k2k25hz_y"
#define ENV_4K2K25HZ_W                  "ubootenv.var.4k2k25hz_w"
#define ENV_4K2K25HZ_H                  "ubootenv.var.4k2k25hz_h"
#define ENV_4K2K30HZ_X                  "ubootenv.var.4k2k30hz_x"
#define ENV_4K2K30HZ_Y                  "ubootenv.var.4k2k30hz_y"
#define ENV_4K2K30HZ_W                  "ubootenv.var.4k2k30hz_w"
#define ENV_4K2K30HZ_H                  "ubootenv.var.4k2k30hz_h"
#define ENV_4K2K50HZ_X                  "ubootenv.var.4k2k50hz_x"
#define ENV_4K2K50HZ_Y                  "ubootenv.var.4k2k50hz_y"
#define ENV_4K2K50HZ_W                  "ubootenv.var.4k2k50hz_w"
#define ENV_4K2K50HZ_H                  "ubootenv.var.4k2k50hz_h"
#define ENV_4K2K60HZ_X                  "ubootenv.var.4k2k60hz_x"
#define ENV_4K2K60HZ_Y                  "ubootenv.var.4k2k60hz_y"
#define ENV_4K2K60HZ_W                  "ubootenv.var.4k2k60hz_w"
#define ENV_4K2K60HZ_H                  "ubootenv.var.4k2k60hz_h"
#define ENV_4K2KSMPTE_X                 "ubootenv.var.4k2ksmpte_x"
#define ENV_4K2KSMPTE_Y                 "ubootenv.var.4k2ksmpte_y"
#define ENV_4K2KSMPTE_W                 "ubootenv.var.4k2ksmpte_w"
#define ENV_4K2KSMPTE_H                 "ubootenv.var.4k2ksmpte_h"

#define FULL_WIDTH_480                  720
#define FULL_HEIGHT_480                 480
#define FULL_WIDTH_576                  720
#define FULL_HEIGHT_576                 576
#define FULL_WIDTH_720                  1280
#define FULL_HEIGHT_720                 720
#define FULL_WIDTH_1080                 1920
#define FULL_HEIGHT_1080                1080
#define FULL_WIDTH_4K2K                 3840
#define FULL_HEIGHT_4K2K                2160
#define FULL_WIDTH_4K2KSMPTE            4096
#define FULL_HEIGHT_4K2KSMPTE           2160

#define DESITY_720P                      "160"
#define DESITY_1080P                     "240"
#define DESITY_2160P                     "480"

enum {
    DISPLAY_TYPE_NONE                   = 0,
    DISPLAY_TYPE_TABLET                 = 1,
    DISPLAY_TYPE_MBOX                   = 2,
    DISPLAY_TYPE_TV                     = 3
};

enum {
    DISPLAY_MODE_480I                   = 0,
    DISPLAY_MODE_480P                   = 1,
    DISPLAY_MODE_480CVBS                = 2,
    DISPLAY_MODE_576I                   = 3,
    DISPLAY_MODE_576P                   = 4,
    DISPLAY_MODE_576CVBS                = 5,
    DISPLAY_MODE_720P50HZ               = 6,
    DISPLAY_MODE_720P                   = 7,
    DISPLAY_MODE_1080P24HZ              = 8,
    DISPLAY_MODE_1080I50HZ              = 9,
    DISPLAY_MODE_1080P50HZ              = 10,
    DISPLAY_MODE_1080I                  = 11,
    DISPLAY_MODE_1080P                  = 12,
    DISPLAY_MODE_4K2K24HZ               = 13,
    DISPLAY_MODE_4K2K25HZ               = 14,
    DISPLAY_MODE_4K2K30HZ               = 15,
    DISPLAY_MODE_4K2K50HZ               = 16,
    DISPLAY_MODE_4K2K50HZ420            = 17,
    DISPLAY_MODE_4K2K60HZ               = 18,
    DISPLAY_MODE_4K2K60HZ420            = 19,
    DISPLAY_MODE_4K2KSMPTE              = 20,
    DISPLAY_MODE_TOTAL                  = 21
};


typedef struct mbox_data {
    char edid[MAX_STR_LEN];
    char hpd_state[MAX_STR_LEN];
    char current_mode[MAX_STR_LEN];
    char ubootenv_hdmimode[MAX_STR_LEN];
}mbox_data_t;

// ----------------------------------------------------------------------------

class DisplayMode
{
public:
    DisplayMode(const char *path);
    ~DisplayMode();

    void init();

    void getDisplayInfo(int &type, char* socType, char* defaultUI);
    void getFbInfo(int &fb0w, int &fb0h, int &fb0bits, int &fb0trip,
        int &fb1w, int &fb1h, int &fb1bits, int &fb1trip);

    void setLogLevel(int level);
    int dump(char *result);
    void setMboxOutputMode(const char* outputmode);
    void setOsdMouse(const char* curMode);
    void setOsdMouse(int x, int y, int w, int h);
    void setPosition(int left, int top, int width, int height);
    void getPosition(const char* curMode, int *position);
    static void* startHdmiPlugDetectLoop(void *data);
    static void* bootanimDetect(void *data);
    static void* tmpDisableOsd(void *data);

private:

    bool getBootEnv(const char* key, char* value);
    void setBootEnv(const char* key, char* value);

    int parseConfigFile();
    void setTabletDisplay();
    void setMboxDisplay(char* hpdstate);
    void getBestHdmiMode(char * mode, mbox_data* data);
    void filterHdmiMode(char * mode, mbox_data* data);
    void getHdmiMode(char *mode, mbox_data* data);
    bool isBestOutputmode();
    void getCurrentHdmiData(mbox_data_t* data);
    void startHdmiPlugDetectThread();
    void startBootanimDetectThread();
    void startDisableOsdThread();
    void setTVDisplay();
    void setFbParameter(const char* fbdev, struct fb_var_screeninfo var_set);

    int getBootenvInt(const char* key, int defaultVal);

    const char* pConfigPath;
    int mDisplayType;
    int mFb0Width;
    int mFb0Height;
    int mFb0FbBits;
    bool mFb0TripleEnable;//Triple Buffer enable or not

    int mFb1Width;
    int mFb1Height;
    int mFb1FbBits;
    bool mFb1TripleEnable;//Triple Buffer enable or not

    int mDisplayWidth;
    int mDisplayHeight;

    char mSocType[MAX_STR_LEN];
    char mDefaultUI[MAX_STR_LEN];//this used for mbox
    int mLogLevel;
    SysWrite *pSysWrite = NULL;
    bool initDisplay;
};

#endif // ANDROID_DISPLAY_MODE_H

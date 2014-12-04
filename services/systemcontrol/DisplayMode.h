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

#define DISPLAY_FB0_FREESCALE_AXIS      "/sys/class/graphics/fb0/free_scale_axis"
#define DISPLAY_FB0_WINDOW_AXIS         "/sys/class/graphics/fb0/window_axis"

#define DISPLAY_HPD_STATE               "/sys/class/amhdmitx/amhdmitx0/hpd_state"

#define AUDIO_DSP_DIGITAL_RAW           "/sys/class/audiodsp/digital_raw"

#define PROP_HDMIONLY                   "ro.platform.hdmionly"
#define PROP_LCD_DENSITY                "ro.sf.lcd_density"
#define PROP_HAS_CVBS_MODE              "ro.platform.has.cvbsmode"

enum {
    DISPLAY_TYPE_NONE               = 0,
    DISPLAY_TYPE_TABLET			    = 1,
    DISPLAY_TYPE_MBOX               = 2,
    DISPLAY_TYPE_TV                 = 3
};

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

private:

    bool getBootEnv(const char* key, char* value);
    void setBootEnv(const char* key, char* value);

    int parseConfigFile();
    void setTabletDisplay();
    void setMboxDisplay();
    void setTVDisplay();
    void setFbParameter(const char* fbdev, struct fb_var_screeninfo var_set);

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

    char mSocType[MAX_STR_LEN];
    char mDefaultUI[MAX_STR_LEN];//this used for mbox
    int mLogLevel;
    SysWrite *pSysWrite = NULL;
};

#endif // ANDROID_DISPLAY_MODE_H
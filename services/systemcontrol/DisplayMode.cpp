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

#define LOG_TAG "SystemControl"
//#define LOG_NDEBUG 0

#include <stdio.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

#include "ubootenv.h"
#include "DisplayMode.h"
#include "SysTokenizer.h"

#define MODE_480P                   "480p"
#define MODE_480I                   "480i"
#define MODE_480CVBS                "480cvbs"
#define MODE_576P                   "576p"
#define MODE_576I                   "576i"
#define MODE_576CVBS                "576cvbs"
#define MODE_4K2K24HZ               "4k2k24hz"
#define MODE_4K2K25HZ               "4k2k25hz"
#define MODE_4K2K30HZ               "4k2k30hz"
#define MODE_4K2KSMPTE              "4k2ksmpte"
#define MODE_1080P                  "1080p"
#define MODE_1080I                  "1080i"
#define MODE_1080P24HZ              "1080p24hz"
#define MODE_1080P50HZ              "1080p50hz"
#define MODE_1080I50HZ              "1080i50hz"
#define MODE_720P                   "720p"
#define MODE_720P50HZ               "720p50hz"

#define UBOOTENV_DIGITAUDIO         "ubootenv.var.digitaudiooutput"
#define UBOOTENV_HDMIMODE           "ubootenv.var.hdmimode"
#define UBOOTENV_CVBSMODE           "ubootenv.var.cvbsmode"
#define UBOOTENV_OUTPUTMODE         "ubootenv.var.outputmode"
#define UBOOTENV_ISBESTMODE         "ubootenv.var.is.bestmode"


static void copy_if_gt0(uint32_t *src, uint32_t *dst, unsigned cnt)
{
	do {
		if ((int32_t) *src > 0)
			*dst = *src;
		src++;
		dst++;
	} while (--cnt);
}

static void copy_changed_values(
		struct fb_var_screeninfo *base,
		struct fb_var_screeninfo *set)
{
	//if ((int32_t) set->xres > 0) base->xres = set->xres;
	//if ((int32_t) set->yres > 0) base->yres = set->yres;
	//if ((int32_t) set->xres_virtual > 0)   base->xres_virtual = set->xres_virtual;
	//if ((int32_t) set->yres_virtual > 0)   base->yres_virtual = set->yres_virtual;
	copy_if_gt0(&set->xres, &base->xres, 4);

	if ((int32_t) set->bits_per_pixel > 0) base->bits_per_pixel = set->bits_per_pixel;
	//copy_if_gt0(&set->bits_per_pixel, &base->bits_per_pixel, 1);

	//if ((int32_t) set->pixclock > 0)       base->pixclock = set->pixclock;
	//if ((int32_t) set->left_margin > 0)    base->left_margin = set->left_margin;
	//if ((int32_t) set->right_margin > 0)   base->right_margin = set->right_margin;
	//if ((int32_t) set->upper_margin > 0)   base->upper_margin = set->upper_margin;
	//if ((int32_t) set->lower_margin > 0)   base->lower_margin = set->lower_margin;
	//if ((int32_t) set->hsync_len > 0) base->hsync_len = set->hsync_len;
	//if ((int32_t) set->vsync_len > 0) base->vsync_len = set->vsync_len;
	//if ((int32_t) set->sync > 0)  base->sync = set->sync;
	//if ((int32_t) set->vmode > 0) base->vmode = set->vmode;
	copy_if_gt0(&set->pixclock, &base->pixclock, 9);
}

DisplayMode::DisplayMode(const char *path)
    :mDisplayType(DISPLAY_TYPE_MBOX),
    mFb0Width(-1),
    mFb0Height(-1),
    mFb0FbBits(-1),
    mFb0TripleEnable(true),
    mFb1Width(-1),
    mFb1Height(-1),
    mFb1FbBits(-1),
    mFb1TripleEnable(true),
    mDisplayWidth(FULL_WIDTH_1080),
    mDisplayHeight(FULL_HEIGHT_1080),
    mLogLevel(LOG_LEVEL_DEFAULT) {

    if (NULL == path) {
        pConfigPath = DISPLAY_CFG_FILE;
    }
    else {
        pConfigPath = path;
    }

#if !defined(ODROIDC)
    SYS_LOGI("display mode config path: %s", pConfigPath);
#endif

    pSysWrite = new SysWrite();
    initDisplay = true;
}

DisplayMode::~DisplayMode() {
}

void DisplayMode::init() {
#if defined(ODROIDC)
    setMboxDisplay(NULL);
#else
    parseConfigFile();

    SYS_LOGI("display mode init type: %d [0:none 1:tablet 2:mbox 3:tv]", mDisplayType);
    if (DISPLAY_TYPE_TABLET == mDisplayType) {
        setTabletDisplay();
    }
    else if (DISPLAY_TYPE_MBOX == mDisplayType) {
        setMboxDisplay(NULL);
        startHdmiPlugDetectThread();
    }
    else if (DISPLAY_TYPE_TV == mDisplayType) {
        setTVDisplay();
    }
    initDisplay = false;
#endif
}

void DisplayMode:: getDisplayInfo(int &type, char* socType, char* defaultUI) {
    type = mDisplayType;
    if (NULL != socType)
        strcpy(socType, mSocType);

    if (NULL != defaultUI)
        strcpy(defaultUI, mDefaultUI);
}

void DisplayMode:: getFbInfo(int &fb0w, int &fb0h, int &fb0bits, int &fb0trip,
        int &fb1w, int &fb1h, int &fb1bits, int &fb1trip) {
    fb0w = mFb0Width;
    fb0h = mFb0Height;
    fb0bits = mFb0FbBits;
    fb0trip = mFb0TripleEnable?1:0;

    fb1w = mFb1Width;
    fb1h = mFb1Height;
    fb1bits = mFb1FbBits;
    fb1trip = mFb1TripleEnable?1:0;
}

void DisplayMode::setLogLevel(int level){
    mLogLevel = level;
}

bool DisplayMode::getBootEnv(const char* key, char* value) {
    const char* p_value = bootenv_get(key);

    if (mLogLevel > LOG_LEVEL_1)
        SYS_LOGI("getBootEnv key:%s value:%s", key, p_value);

	if (p_value) {
        strcpy(value, p_value);
        return true;
	}
    return false;
}

void DisplayMode::setBootEnv(const char* key, char* value) {
    if (mLogLevel > LOG_LEVEL_1)
        SYS_LOGI("setBootEnv key:%s value:%s", key, value);

    bootenv_update(key, value);
}

int DisplayMode::parseConfigFile(){
    const char* WHITESPACE = " \t\r";

    SysTokenizer* tokenizer;
    int status = SysTokenizer::open(pConfigPath, &tokenizer);
    if (status) {
        SYS_LOGE("Error %d opening display config file %s.", status, pConfigPath);
    } else {
        while (!tokenizer->isEof()) {

            if(mLogLevel > LOG_LEVEL_1)
                SYS_LOGI("Parsing %s: %s", tokenizer->getLocation(), tokenizer->peekRemainderOfLine());

            tokenizer->skipDelimiters(WHITESPACE);

            if (!tokenizer->isEol() && tokenizer->peekChar() != '#') {

                char *token = tokenizer->nextToken(WHITESPACE);
                if(!strcmp(token, DEVICE_STR_MID)){
                    mDisplayType = DISPLAY_TYPE_TABLET;

                    tokenizer->skipDelimiters(WHITESPACE);
                    strcpy(mSocType, tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb0Width = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb0Height = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb0FbBits = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb0TripleEnable = (0 == atoi(tokenizer->nextToken(WHITESPACE)))?false:true;

                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb1Width = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb1Height = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb1FbBits = atoi(tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    mFb1TripleEnable = (0 == atoi(tokenizer->nextToken(WHITESPACE)))?false:true;

                } else if (!strcmp(token, DEVICE_STR_MBOX)) {
                    mDisplayType = DISPLAY_TYPE_MBOX;

                    tokenizer->skipDelimiters(WHITESPACE);
                    strcpy(mSocType, tokenizer->nextToken(WHITESPACE));
                    tokenizer->skipDelimiters(WHITESPACE);
                    strcpy(mDefaultUI, tokenizer->nextToken(WHITESPACE));
                } else if (!strcmp(token, DEVICE_STR_TV)) {
                    mDisplayType = DISPLAY_TYPE_TV;

                    tokenizer->skipDelimiters(WHITESPACE);
                    strcpy(mSocType, tokenizer->nextToken(WHITESPACE));
                }else {
                    SYS_LOGE("%s: Expected keyword, got '%s'.", tokenizer->getLocation(), token);
                    break;
                }
            }

            tokenizer->nextLine();
        }
        delete tokenizer;
    }
    return status;
}

void DisplayMode::fbset(int width, int height, int bits)
{
    struct fb_var_screeninfo var_set;

    mFb0Width = width;
    mFb0Height = height;
    mFb0FbBits = bits;

    var_set.xres = mFb0Width;
    var_set.yres = mFb0Height;
    var_set.xres_virtual = mFb0Width;
    var_set.yres_virtual = mFb0Height * (mFb0TripleEnable ? 3 : 2);
    var_set.bits_per_pixel = mFb0FbBits;
    setFbParameter(DISPLAY_FB0, var_set);

    pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
    var_set.xres = mFb1Width;
    var_set.yres = mFb1Height;
    var_set.xres_virtual = mFb1Width;
    var_set.yres_virtual = mFb1Height * (mFb1TripleEnable ? 3 : 2);
    var_set.bits_per_pixel = mFb1FbBits;
    setFbParameter(DISPLAY_FB1, var_set);

    mDisplayWidth = width;
    mDisplayHeight = height;
}

void DisplayMode::setTabletDisplay() {
    struct fb_var_screeninfo var_set;

    var_set.xres = mFb0Width;
	var_set.yres = mFb0Height;
	var_set.xres_virtual = mFb0Width;
    if(mFb0TripleEnable)
	    var_set.yres_virtual = 3*mFb0Height;
    else
        var_set.yres_virtual = 2*mFb0Height;
	var_set.bits_per_pixel = mFb0FbBits;
    setFbParameter(DISPLAY_FB0, var_set);

    pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
    var_set.xres = mFb1Width;
	var_set.yres = mFb1Height;
	var_set.xres_virtual = mFb1Width;
    if (mFb1TripleEnable)
	    var_set.yres_virtual = 3*mFb1Height;
    else
        var_set.yres_virtual = 2*mFb1Height;
	var_set.bits_per_pixel = mFb1FbBits;
    setFbParameter(DISPLAY_FB1, var_set);

    char axis[512] = {0};
    sprintf(axis, "%d %d %d %d %d %d %d %d",
        0, 0, mFb0Width, mFb0Height, 0, 0, mFb1Width, mFb1Height);

    pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, "panel");
    pSysWrite->writeSysfs(SYSFS_DISPLAY_AXIS, axis);

    pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
}

void DisplayMode::setMboxDisplay(char* hpdstate) {
    mbox_data_t* data = (mbox_data_t *)malloc(sizeof(mbox_data_t));
    if (data == NULL) {
        SYS_LOGE(" malloc memory for mboxdata failed");
        return;
    }
    memset(data, 0, sizeof(mbox_data_t));

    if (hpdstate == NULL) {
        getCurrentHdmiData(data);
        hpdstate = data->hpd_state;
    } else {
        strcpy(data->hpd_state, hpdstate);
        getCurrentHdmiData(data);
    }

    char current_mode[MAX_STR_LEN] = {0};
    char outputmode[MAX_STR_LEN] = {0};

    strcpy(current_mode, data->current_mode);

#if defined(ODROIDC)
    getBootEnv(UBOOTENV_HDMIMODE, data->ubootenv_hdmimode);

    if (!strncmp(data->ubootenv_hdmimode, "1080", 3))
        fbset(1920, 1080, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "640x480", 7))
        fbset(640, 480, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "800x600", 7))
        fbset(800, 600, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "800x480", 7))
        fbset(800, 480, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1024x600", 8))
        fbset(1024, 600, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1024x768", 8))
        fbset(1024, 768, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "800", 3))
        fbset(1280, 800, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1280x1024", 9))
        fbset(1280, 1024, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1360x768", 8))
        fbset(1360, 768, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1366x768", 8))
        fbset(1366, 768, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1440x900", 8))
        fbset(1440, 900, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1600x900", 8))
        fbset(1600, 900, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1680x1050", 9))
        fbset(1680, 1050, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "1920x1200", 9))
        fbset(1920, 1200, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "480p", 4))
        fbset(720, 480, 32);
    else if (!strncmp(data->ubootenv_hdmimode, "576p", 4))
        fbset(720, 576, 32);
    else
        fbset(1280, 720, 32);

    strcpy(outputmode, data->ubootenv_hdmimode);
    strcpy(mDefaultUI, outputmode);

    setBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
#else

    if (pSysWrite->getPropertyBoolean(PROP_HDMIONLY, true)) {
        if (!strcmp(hpdstate, "1")){
            if ((!strcmp(current_mode, MODE_480CVBS) || !strcmp(current_mode, MODE_576CVBS))
                    && initDisplay) {
                pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
                pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
            }

            getHdmiMode(outputmode, data);
            setBootEnv(UBOOTENV_HDMIMODE, outputmode);
        }
        else {
            getBootEnv(UBOOTENV_CVBSMODE, outputmode);
        }

        setBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
    }
    else {
        getBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
    }

    //if the tv don't support current outputmode,then switch to best outputmode
    if (strcmp(hpdstate, "1")) {
        if (strcmp(outputmode, MODE_480CVBS) && strcmp(outputmode, MODE_576CVBS)) {
            strcpy(outputmode, MODE_576CVBS);
        }
    }
#endif

    SYS_LOGI("init mbox display hpdstate:%s, old outputmode:%s, new outputmode:%s\n",
        hpdstate,
        current_mode,
        outputmode);

    if (strlen(outputmode) == 0)
        strcpy(outputmode, mDefaultUI);

    char value[MAX_STR_LEN] = {0};

    if ((!strcmp(outputmode, MODE_480I) || !strcmp(outputmode, MODE_576I)) &&
        (pSysWrite->getPropertyBoolean(PROP_HAS_CVBS_MODE, false))) {
        const char *mode = "";
        if (!strcmp(outputmode, MODE_480I)) {
            mode = MODE_480CVBS;
        } else if (!strcmp(outputmode, MODE_576I)) {
            mode = MODE_576CVBS;
        }

        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, mode);
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE2, "null");
    } else
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, outputmode);

    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_MODE, "1");
    if (initDisplay) {
        pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE_MODE, "1");
        pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
    }

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
        0, 0, mDisplayWidth - 1, mDisplayHeight - 1);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_AXIS, axis);

    pSysWrite->writeSysfs(DISPLAY_PPMGR, "0");
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0");

    //init osd mouse
    setOsdMouse(current_mode);
    setOverscan(current_mode);

    pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
    if (initDisplay)
        pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");

    //audio
    getBootEnv(UBOOTENV_DIGITAUDIO, value);
    char audiovalue[5];
    if (!strcmp(value,"SPDIF passthrough")) {
        strcpy(audiovalue, "1");
    }
    else if (!strcmp(value, "HDMI passthrough")) {
        strcpy(audiovalue, "2");
    }
    else {
        strcpy(audiovalue, "0");
    }
    pSysWrite->writeSysfs(AUDIO_DSP_DIGITAL_RAW, audiovalue);

    free(data);
    data = NULL;
}

//get the best hdmi mode by edid
void DisplayMode::getBestHdmiMode(char* mode, mbox_data_t* data) {
    char* arrayMode[MAX_STR_LEN] = {0};
    char* tmp;
    int len;

  /*  len = strlen(data->edid);
    tmp = data->edid;
    int i = 0;

    do {
        if (strlen(tmp) == 0)
            break;
        char* pos = strchr(tmp, 0x0a);
        *pos = 0;

        arrayMode[i] = tmp;
        tmp = pos + 1;
        i++;
    } while (tmp <= data->edid + len -1);

    for (int j = 0; j < i; j++) {
        char* pos = strchr(arrayMode[j], '*');
        if (pos != NULL) {
            *pos = 0;
            strcpy(mode, arrayMode[j]);
            break;
        }
    }*/
    char* pos = strchr(data->edid, '*');
    if (pos != NULL) {
        char* findReturn = pos;
        while (*findReturn != 0x0a && findReturn >= data->edid) {
            findReturn--;
        }
        *pos = 0;
        strcpy(mode, findReturn + 1);
        SYS_LOGI(" set HDMI to best edid mode: %s\n", mode);
    }

    if (strlen(mode) == 0) {
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, DEFAULT_OUTPUT_MODE, mode);
    }
}

//check if the edid support current hdmi mode
void DisplayMode::filterHdmiMode(char* mode, mbox_data_t* data) {
    char currentHdmiMode[MAX_STR_LEN] = {0};
    char edid[MAX_STR_LEN] = {0};
    char* arrayMode[MAX_STR_LEN] = {0};
    char* tmp;
    int len;

    len = strlen(data->edid);
    tmp = data->edid;

    int i = 0;
    do {
        if (strlen(tmp) == 0)
            break;
        char* pos = strchr(tmp, 0x0a);
        *pos = 0;

        arrayMode[i] = tmp;
        if (!strncmp(arrayMode[i], data->ubootenv_hdmimode, strlen(data->ubootenv_hdmimode))) {
            strcpy(mode, data->ubootenv_hdmimode);
            return;
        }

        tmp = pos + 1;
        i++;
    } while (tmp <= data->edid + len -1);

    strcpy(mode, arrayMode[i-1]);
}

void DisplayMode::getHdmiMode(char* mode, mbox_data_t* data) {
    if (strstr(data->edid, "null") != NULL) {
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, DEFAULT_OUTPUT_MODE, mode);
        return;
    }

    if (pSysWrite->getPropertyBoolean(PROP_HDMIONLY, true)) {
        if (isBestOutputmode()) {
            getBestHdmiMode(mode, data);
        } else {
            filterHdmiMode(mode, data);
        }
    }
    SYS_LOGI("set HDMI mode to %s\n", mode);
}

void DisplayMode::getCurrentHdmiData(mbox_data_t* data){;
    if (strlen(data->hpd_state) == 0) {
        pSysWrite->readSysfs(DISPLAY_HPD_STATE, data->hpd_state);
    }

    if (!strcmp(data->hpd_state, "1")) {
        pSysWrite->readSysfsOriginal(DISPLAY_HDMI_EDID, data->edid);

        int count = 0;
        while (strlen(data->edid) == 0) {
            if (count >= 5) {
                strcpy(data->edid, "null edid");
                break;
            }

            pSysWrite->readSysfsOriginal(DISPLAY_HDMI_EDID, data->edid);
            count++;
            usleep(500000);
        }
    }
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, data->current_mode);
    getBootEnv(UBOOTENV_HDMIMODE, data->ubootenv_hdmimode);
}

void DisplayMode::startHdmiPlugDetectThread() {
    pthread_t id;
    int ret;
    ret = pthread_create(&id, NULL, startHdmiPlugDetectLoop, this);
    if (ret != 0) {
        SYS_LOGI("Create HdmiPlugDetectThread error!\n");
    }
}

// all the hdmi plug checking complete in this loop
void* DisplayMode::startHdmiPlugDetectLoop(void* data){
    DisplayMode *pThiz = (DisplayMode*)data;

    char oldHpdstate[MAX_STR_LEN] = {0};
    char currentHpdstate[MAX_STR_LEN] = {0};

    pThiz->pSysWrite->readSysfs(DISPLAY_HPD_STATE, oldHpdstate);
    while (1) {
        pThiz->pSysWrite->readSysfs(DISPLAY_HPD_STATE, currentHpdstate);
        if (strcmp(oldHpdstate, currentHpdstate)) {
            SYS_LOGI("HdmiPlugDetectLoop: detected HDMI plug: change state from %s to %s\n", oldHpdstate, currentHpdstate);

            pThiz->setMboxDisplay(currentHpdstate);
            strcpy(oldHpdstate, currentHpdstate);
        }
        usleep(2000000);
    }

    return NULL;
}

bool DisplayMode::isBestOutputmode() {
#if defined(ODROIDC)
	/*
	 * FIXME: Don't we discover best output mode from EDID?
	 * Currently return 'false' will force the output resolution as hdmi
	 * mode in 'boot.ini'
	 */
	return false;
#else
    char isBestMode[MAX_STR_LEN] = {0};
    return !getBootEnv(UBOOTENV_ISBESTMODE, isBestMode) || strcmp(isBestMode, "true") == 0;
#endif
}

void DisplayMode::setTVDisplay() {

}

void DisplayMode::setFbParameter(const char* fbdev, struct fb_var_screeninfo var_set) {
    struct fb_var_screeninfo var_old;

    int fh = open(fbdev, O_RDONLY);
	ioctl(fh, FBIOGET_VSCREENINFO, &var_old);

    copy_changed_values(&var_old, &var_set);
	ioctl(fh, FBIOPUT_VSCREENINFO, &var_old);
    close(fh);
}

static const char* DISPLAY_MODE_LIST[DISPLAY_MODE_TOTAL] = {
    "480i", "480p", "576i", "576p", "720p",
    "1080i", "1080p", "720p50hz", "1080i50hz", "1080p50hz", "480cvbs", "576cvbs",
    "4k2k24hz", "4k2k25hz", "4k2k30hz", "4k2ksmpte", "1080p24hz"
};

int DisplayMode::getBootenvInt(const char* key, int defaultVal) {
    int value = defaultVal;
    const char* p_value = bootenv_get(key);
    if (p_value) {
        value = atoi(p_value);
    }
    return value;
}

void DisplayMode::setOsdMouse(const char* curMode) {
    //SYS_LOGI("set osd mouse mode: %s", curMode);

    overscan_data_t overscan_data;
    memset(&overscan_data, 0, sizeof(overscan_data_t));
    getBootEnv(UBOOTENV_OVERSCAN_LEFT, overscan_data.left);
    getBootEnv(UBOOTENV_OVERSCAN_TOP, overscan_data.top);
    getBootEnv(UBOOTENV_OVERSCAN_RIGHT, overscan_data.right);
    getBootEnv(UBOOTENV_OVERSCAN_BOTTOM, overscan_data.bottom);

    setOsdMouse(0 + atoi(overscan_data.left), 0 + atoi(overscan_data.top),
        mDisplayWidth - atoi(overscan_data.right) - atoi(overscan_data.left) - 1,
        mDisplayHeight - atoi(overscan_data.bottom) - atoi(overscan_data.top) - 1);
}

void DisplayMode::setOsdMouse(int x, int y, int w, int h) {
    SYS_LOGI("set osd mouse x:%d y:%d w:%d h:%d", x, y, w, h);

    const char* displaySize = "1920 1080";
    if (!strncmp(mDefaultUI, "720", 3))
        displaySize = "1280 720";
    else if (!strncmp(mDefaultUI, "480", 3))
        displaySize = "720 480";
    else if (!strncmp(mDefaultUI, "576", 3))
        displaySize = "720 576";
    else if (!strncmp(mDefaultUI, "1080", 4))
        displaySize = "1920 1080";
    else if (!strncmp(mDefaultUI, "4k2k", 4))
        displaySize = "3840 2160";
    else if (!strncmp(mDefaultUI, "640x480", 7))
        displaySize = "640 480";
    else if (!strncmp(mDefaultUI, "800x600", 7))
        displaySize = "800 600";
    else if (!strncmp(mDefaultUI, "800x480", 7))
        displaySize = "800 480";
    else if (!strncmp(mDefaultUI, "1024x600", 8))
        displaySize = "1024 600";
    else if (!strncmp(mDefaultUI, "1024x768", 8))
        displaySize = "1024 768";
    else if (!strncmp(mDefaultUI, "800", 3))
        displaySize = "1280 800";
    else if (!strncmp(mDefaultUI, "1280x1024", 9))
        displaySize = "1280 1024";
    else if (!strncmp(mDefaultUI, "1360x768", 8))
        displaySize = "1360 768";
    else if (!strncmp(mDefaultUI, "1366x768", 8))
        displaySize = "1366 768";
    else if (!strncmp(mDefaultUI, "1440x900", 8))
        displaySize = "1440 900";
    else if (!strncmp(mDefaultUI, "1600x900", 8))
        displaySize = "1600 900";
    else if (!strncmp(mDefaultUI, "1680x1050", 9))
        displaySize = "1680 1050";
    else if (!strncmp(mDefaultUI, "1920x1200", 9))
        displaySize = "1920 1200";
    else if (!strncmp(mDefaultUI, "4k2k", 4))
        displaySize = "3840 2160";

    char cur_mode[MAX_STR_LEN] = {0};
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, cur_mode);
    if (!strcmp(cur_mode, DISPLAY_MODE_LIST[0]) || !strcmp(cur_mode, DISPLAY_MODE_LIST[2]) ||
        !strcmp(cur_mode, DISPLAY_MODE_LIST[5]) || !strcmp(cur_mode, DISPLAY_MODE_LIST[8]) ||
        !strcmp(cur_mode, DISPLAY_MODE_LIST[10]) || !strcmp(cur_mode, DISPLAY_MODE_LIST[11])) {
        y /= 2;
        h /= 2;
    }

    char axis[512] = {0};
    sprintf(axis, "%d %d %s %d %d 18 18", x, y, displaySize, x, y);
    pSysWrite->writeSysfs(SYSFS_DISPLAY_AXIS, axis);

    sprintf(axis, "%s %d %d", displaySize, w, h);
    pSysWrite->writeSysfs(DISPLAY_FB1_SCALE_AXIS, axis);
    pSysWrite->writeSysfs(DISPLAY_FB1_SCALE, "0x10001");
}

void DisplayMode::setOverscan(const char* curMode) {
    overscan_data_t data;
    memset(&data, 0, sizeof(overscan_data_t));
    getBootEnv(UBOOTENV_OVERSCAN_LEFT, data.left);
    getBootEnv(UBOOTENV_OVERSCAN_TOP, data.top);
    getBootEnv(UBOOTENV_OVERSCAN_RIGHT, data.right);
    getBootEnv(UBOOTENV_OVERSCAN_BOTTOM, data.bottom);

    if (strlen(data.left) == 0 || strlen(data.top) == 0 || strlen(data.right) == 0
            || strlen(data.bottom) == 0) {
        SYS_LOGI("overscan values is N/A");
        return;
    }

    char overscan[32] = {0};
    sprintf(overscan, "%d %d %d %d", 0 + atoi(data.left), 0 + atoi(data.top),
            mDisplayWidth - 1 - atoi(data.right), mDisplayHeight - 1 - atoi(data.bottom));

    SYS_LOGI("overscan value : %s\n", overscan);

    pSysWrite->writeSysfs(DISPLAY_FB0_WINDOW_AXIS, overscan);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
    return;
}

void DisplayMode::getPosition(const char* curMode, int *position) {
    int index = DISPLAY_MODE_720P;
    for (int i = 0; i < DISPLAY_MODE_TOTAL; i++) {
        if (!strcmp(curMode, DISPLAY_MODE_LIST[i]))
             index = i;
    }

    switch (index) {
    case DISPLAY_MODE_480I:
        position[0] = getBootenvInt(ENV_480I_X, 0);
        position[1] = getBootenvInt(ENV_480I_Y, 0);
        position[2] = getBootenvInt(ENV_480I_W, FULL_WIDTH_480);
        position[3] = getBootenvInt(ENV_480I_H, FULL_HEIGHT_480);
        break;
    case DISPLAY_MODE_480P: // 480p
        position[0] = getBootenvInt(ENV_480P_X, 0);
        position[1] = getBootenvInt(ENV_480P_Y, 0);
        position[2] = getBootenvInt(ENV_480P_W, FULL_WIDTH_480);
        position[3] = getBootenvInt(ENV_480P_H, FULL_HEIGHT_480);
        break;
    case DISPLAY_MODE_576I: // 576i
        position[0] = getBootenvInt(ENV_576I_X, 0);
        position[1] = getBootenvInt(ENV_576I_Y, 0);
        position[2] = getBootenvInt(ENV_576I_W, FULL_WIDTH_576);
        position[3] = getBootenvInt(ENV_576I_H, FULL_HEIGHT_576);
        break;
    case DISPLAY_MODE_576P: // 576p
        position[0] = getBootenvInt(ENV_576P_X, 0);
        position[1] = getBootenvInt(ENV_576P_Y, 0);
        position[2] = getBootenvInt(ENV_576P_W, FULL_WIDTH_576);
        position[3] = getBootenvInt(ENV_576P_H, FULL_HEIGHT_576);
        break;
    case DISPLAY_MODE_720P: // 720p
    case DISPLAY_MODE_720P50HZ: // 720p50hz
        position[0] = getBootenvInt(ENV_720P_X, 0);
        position[1] = getBootenvInt(ENV_720P_Y, 0);
        position[2] = getBootenvInt(ENV_720P_W, FULL_WIDTH_720);
        position[3] = getBootenvInt(ENV_720P_H, FULL_HEIGHT_720);
        break;

    case DISPLAY_MODE_1080I: // 1080i
    case DISPLAY_MODE_1080I50HZ: // 1080i50hz
        position[0] = getBootenvInt(ENV_1080I_X, 0);
        position[1] = getBootenvInt(ENV_1080I_Y, 0);
        position[2] = getBootenvInt(ENV_1080I_W, FULL_WIDTH_1080);
        position[3] = getBootenvInt(ENV_1080I_H, FULL_HEIGHT_1080);
        break;

    case DISPLAY_MODE_1080P: // 1080p
    case DISPLAY_MODE_1080P50HZ: // 1080p50hz
    case DISPLAY_MODE_1080P24HZ://1080p24hz
        position[0] = getBootenvInt(ENV_1080P_X, 0);
        position[1] = getBootenvInt(ENV_1080P_Y, 0);
        position[2] = getBootenvInt(ENV_1080P_W, FULL_WIDTH_1080);
        position[3] = getBootenvInt(ENV_1080P_H, FULL_HEIGHT_1080);
        break;
    case DISPLAY_MODE_480CVBS: // 480cvbs
        position[0] = getBootenvInt(ENV_480I_X, 0);
        position[1] = getBootenvInt(ENV_480I_Y, 0);
        position[2] = getBootenvInt(ENV_480I_W, FULL_WIDTH_480);
        position[3] = getBootenvInt(ENV_480I_H, FULL_HEIGHT_480);
        break;
    case DISPLAY_MODE_576CVBS: // 576cvbs
        position[0] = getBootenvInt(ENV_576I_X, 0);
        position[1] = getBootenvInt(ENV_576I_Y, 0);
        position[2] = getBootenvInt(ENV_576I_W, FULL_WIDTH_576);
        position[3] = getBootenvInt(ENV_576I_H, FULL_HEIGHT_576);
        break;
    case DISPLAY_MODE_4K2K24HZ: // 4k2k24hz
        position[0] = getBootenvInt(ENV_4K2K24HZ_X, 0);
        position[1] = getBootenvInt(ENV_4K2K24HZ_Y, 0);
        position[2] = getBootenvInt(ENV_4K2K24HZ_W, FULL_WIDTH_4K2K);
        position[3] = getBootenvInt(ENV_4K2K24HZ_H, FULL_HEIGHT_4K2K);
        break;
    case DISPLAY_MODE_4K2K25HZ: // 4k2k25hz
        position[0] = getBootenvInt(ENV_4K2K25HZ_X, 0);
        position[1] = getBootenvInt(ENV_4K2K25HZ_Y, 0);
        position[2] = getBootenvInt(ENV_4K2K25HZ_W, FULL_WIDTH_4K2K);
        position[3] = getBootenvInt(ENV_4K2K25HZ_H, FULL_HEIGHT_4K2K);
        break;
    case DISPLAY_MODE_4K2K30HZ: // 4k2k30hz
        position[0] = getBootenvInt(ENV_4K2K30HZ_X, 0);
        position[1] = getBootenvInt(ENV_4K2K30HZ_Y, 0);
        position[2] = getBootenvInt(ENV_4K2K30HZ_W, FULL_WIDTH_4K2K);
        position[3] = getBootenvInt(ENV_4K2K30HZ_H, FULL_HEIGHT_4K2K);
        break;
    case DISPLAY_MODE_4K2KSMPTE: // 4k2ksmpte
        position[0] = getBootenvInt(ENV_4K2KSMPTE_X, 0);
        position[1] = getBootenvInt(ENV_4K2KSMPTE_Y, 0);
        position[2] = getBootenvInt(ENV_4K2KSMPTE_W, FULL_WIDTH_4K2KSMPTE);
        position[3] = getBootenvInt(ENV_4K2KSMPTE_H, FULL_HEIGHT_4K2KSMPTE);
        break;
    default: // 720p
        position[0] = getBootenvInt(ENV_720P_X, 0);
        position[1] = getBootenvInt(ENV_720P_Y, 0);
        position[2] = getBootenvInt(ENV_720P_W, FULL_WIDTH_720);
        position[3] = getBootenvInt(ENV_720P_H, FULL_HEIGHT_720);
        break;
    }
}

void DisplayMode::setPosition(int left, int top, int width, int height) {
    char x[512] = {0};
    char y[512] = {0};
    char w[512] = {0};
    char h[512] = {0};
    sprintf(x, "%d", left);
    sprintf(y, "%d", top);
    sprintf(w, "%d", width);
    sprintf(h, "%d", height);

    char cur_mode[MAX_STR_LEN] = {0};
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, cur_mode);
    int index = DISPLAY_MODE_720P;
    for (int i = 0; i < DISPLAY_MODE_TOTAL; i++) {
        if (!strcmp(cur_mode, DISPLAY_MODE_LIST[i]))
             index = i;
    }

    switch (index) {
        case DISPLAY_MODE_480I: // 480i
        case DISPLAY_MODE_480CVBS: //480cvbs
            setBootEnv(ENV_480I_X, x);
            setBootEnv(ENV_480I_Y, y);
            setBootEnv(ENV_480I_W, w);
            setBootEnv(ENV_480I_H, h);
            break;
        case DISPLAY_MODE_480P: // 480p
            setBootEnv(ENV_480P_X, x);
            setBootEnv(ENV_480P_Y, y);
            setBootEnv(ENV_480P_W, w);
            setBootEnv(ENV_480P_H, h);
            break;
        case DISPLAY_MODE_576I: // 576i
        case DISPLAY_MODE_576CVBS:    //576cvbs
            setBootEnv(ENV_576I_X, x);
            setBootEnv(ENV_576I_Y, y);
            setBootEnv(ENV_576I_W, w);
            setBootEnv(ENV_576I_H, h);
            break;
        case DISPLAY_MODE_576P: // 576p
            setBootEnv(ENV_576P_X, x);
            setBootEnv(ENV_576P_Y, y);
            setBootEnv(ENV_576P_W, w);
            setBootEnv(ENV_576P_H, h);
            break;
        case DISPLAY_MODE_720P: // 720p
        case DISPLAY_MODE_720P50HZ: // 720p50hz
            setBootEnv(ENV_720P_X, x);
            setBootEnv(ENV_720P_Y, y);
            setBootEnv(ENV_720P_W, w);
            setBootEnv(ENV_720P_H, h);
            break;
        case DISPLAY_MODE_1080I: // 1080i
        case DISPLAY_MODE_1080I50HZ: // 1080i50hz
            setBootEnv(ENV_1080I_X, x);
            setBootEnv(ENV_1080I_Y, y);
            setBootEnv(ENV_1080I_W, w);
            setBootEnv(ENV_1080I_H, h);
            break;
        case DISPLAY_MODE_1080P: // 1080p
        case DISPLAY_MODE_1080P50HZ: // 1080p50hz
        case DISPLAY_MODE_1080P24HZ: //1080p24hz
            setBootEnv(ENV_1080P_X, x);
            setBootEnv(ENV_1080P_Y, y);
            setBootEnv(ENV_1080P_W, w);
            setBootEnv(ENV_1080P_H, h);
            break;
        case DISPLAY_MODE_4K2K24HZ:    //4k2k24hz
            setBootEnv(ENV_4K2K24HZ_X, x);
            setBootEnv(ENV_4K2K24HZ_Y, y);
            setBootEnv(ENV_4K2K24HZ_W, w);
            setBootEnv(ENV_4K2K24HZ_H, h);
            break;
        case DISPLAY_MODE_4K2K25HZ:    //4k2k25hz
            setBootEnv(ENV_4K2K25HZ_X, x);
            setBootEnv(ENV_4K2K25HZ_Y, y);
            setBootEnv(ENV_4K2K25HZ_W, w);
            setBootEnv(ENV_4K2K25HZ_H, h);
            break;
        case DISPLAY_MODE_4K2K30HZ:    //4k2k30hz
            setBootEnv(ENV_4K2K30HZ_X, x);
            setBootEnv(ENV_4K2K30HZ_Y, y);
            setBootEnv(ENV_4K2K30HZ_W, w);
            setBootEnv(ENV_4K2K30HZ_H, h);
            break;
        case DISPLAY_MODE_4K2KSMPTE:    //4k2ksmpte
            setBootEnv(ENV_4K2KSMPTE_X, x);
            setBootEnv(ENV_4K2KSMPTE_Y, y);
            setBootEnv(ENV_4K2KSMPTE_W, w);
            setBootEnv(ENV_4K2KSMPTE_H, h);
            break;
    }

    if (!strcmp(mSocType, "meson8")) {
        char axis[512] = {0};
        sprintf(axis, "%d %d %d %d", left, top, left + width - 1, top + height - 1);
        pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, axis);
    }
}

int DisplayMode::dump(char *result) {
    if (NULL == result)
        return -1;

    char buf[2048] = {0};
    sprintf(buf, "\ndisplay type: %d [0:none 1:tablet 2:mbox 3:tv], soc type:%s\n", mDisplayType, mSocType);
    strcat(result, buf);

    if (DISPLAY_TYPE_TABLET == mDisplayType) {
        sprintf(buf, "fb0 width:%d height:%d fbbits:%d triple buffer enable:%d\n",
            mFb0Width, mFb0Height, mFb0FbBits, (int)mFb0TripleEnable);
        strcat(result, buf);

        sprintf(buf, "fb1 width:%d height:%d fbbits:%d triple buffer enable:%d\n",
            mFb1Width, mFb1Height, mFb1FbBits, (int)mFb1TripleEnable);
        strcat(result, buf);
    }

    if (DISPLAY_TYPE_MBOX == mDisplayType) {
        sprintf(buf, "default ui:%s\n", mDefaultUI);
        strcat(result, buf);
    }
    return 0;
}


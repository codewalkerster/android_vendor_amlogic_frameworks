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
#include <cutils/properties.h>
#include "ubootenv.h"
#include "DisplayMode.h"
#include "SysTokenizer.h"

#define MODE_480I                   "480i60hz"
#define MODE_480P                   "480p60hz"
#define MODE_480CVBS                "480cvbs"
#define MODE_576I                   "576i50hz"
#define MODE_576P                   "576p50hz"
#define MODE_576CVBS                "576cvbs"
#define MODE_720P50HZ               "720p50hz"
#define MODE_720P                   "720p60hz"
#define MODE_1080P24HZ              "1080p24hz"
#define MODE_1080I50HZ              "1080i50hz"
#define MODE_1080P50HZ              "1080p50hz"
#define MODE_1080I                  "1080i60hz"
#define MODE_1080P                  "1080p60hz"
#define MODE_4K2K24HZ               "2160p24hz"
#define MODE_4K2K25HZ               "2160p25hz"
#define MODE_4K2K30HZ               "2160p30hz"
#define MODE_4K2K50HZ               "2160p50hz"
#define MODE_4K2K50HZ420            "2160p50hz420"
#define MODE_4K2K60HZ               "2160p60hz"
#define MODE_4K2K60HZ420            "2160p60hz420"
#define MODE_4K2KSMPTE              "smpte24hz"


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

    SYS_LOGI("display mode config path: %s", pConfigPath);

    pSysWrite = new SysWrite();
    initDisplay = true;
}

DisplayMode::~DisplayMode() {
}

void DisplayMode::init() {
    parseConfigFile();

    SYS_LOGI("display mode init type: %d [0:none 1:tablet 2:mbox 3:tv], soc type:%s, default UI:%s",
        mDisplayType, mSocType, mDefaultUI);
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

    SYS_LOGI("init mbox display hpdstate:%s, old outputmode:%s, new outputmode:%s\n",
            hpdstate,
            current_mode,
            outputmode);
    if (strlen(outputmode) == 0)
        strcpy(outputmode, mDefaultUI);

    if (!strncmp(mDefaultUI, "720", 3)) {
        mDisplayWidth= FULL_WIDTH_720;
        mDisplayHeight = FULL_HEIGHT_720;
        pSysWrite->setProperty(PROP_LCD_DENSITY, DESITY_720P);
        pSysWrite->setProperty(PROP_WINDOW_WIDTH, "1280");
        pSysWrite->setProperty(PROP_WINDOW_HEIGHT, "720");
    } else if (!strncmp(mDefaultUI, "1080", 4)) {
        mDisplayWidth = FULL_WIDTH_1080;
        mDisplayHeight = FULL_HEIGHT_1080;
        pSysWrite->setProperty(PROP_LCD_DENSITY, DESITY_1080P);
        pSysWrite->setProperty(PROP_WINDOW_WIDTH, "1920");
        pSysWrite->setProperty(PROP_WINDOW_HEIGHT, "1080");
    } else if (!strncmp(mDefaultUI, "4k2k", 4)) {
        mDisplayWidth = FULL_WIDTH_4K2K;
        mDisplayHeight = FULL_HEIGHT_4K2K;
        pSysWrite->setProperty(PROP_LCD_DENSITY, DESITY_2160P);
        pSysWrite->setProperty(PROP_WINDOW_WIDTH, "3840");
        pSysWrite->setProperty(PROP_WINDOW_HEIGHT, "2160");
    }


    if (strcmp(current_mode, outputmode)) {
        if (initDisplay) {
            //when change mode, need close uboot logo to avoid logo scaling wrong
            pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
            pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
            pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
        }else {
            //when change mode, it need time to set osd register,
            //so we disable osd 1 second to avoid screen flicker
            startDisableOsdThread();
        }
    }
    setMboxOutputMode(outputmode);

    free(data);
    data = NULL;
}

void DisplayMode::setMboxOutputMode(const char* outputmode){
    char value[MAX_STR_LEN] = {0};
    int outputx = 0;
    int outputy = 0;
    int outputwidth = 0;
    int outputheight = 0;
    int position[4] = { 0, 0, 0, 0 };

    if (!initDisplay) {
        pSysWrite->writeSysfs(DISPLAY_HDMI_AVMUTE, "1");
        usleep(30000);
    }

    getPosition(outputmode, position);
    outputx = position[0];
    outputy = position[1];
    outputwidth = position[2];
    outputheight = position[3];

    if ((!strcmp(outputmode, MODE_480I) || !strcmp(outputmode, MODE_576I)) &&
            (pSysWrite->getPropertyBoolean(PROP_HAS_CVBS_MODE, false))) {
        const char *mode = "";
        if (!strcmp(outputmode, MODE_480I)) {
            mode = MODE_480CVBS;
        }
        else if (!strcmp(outputmode, MODE_576I)) {
            mode = MODE_576CVBS;
        }

        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, mode);
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE2, "null");
    }
    else
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, outputmode);

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
            0, 0, mDisplayWidth - 1, mDisplayHeight - 1);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_AXIS, axis);

    sprintf(axis, "%d %d %d %d",
            outputx, outputy, outputx + outputwidth - 1, outputy + outputheight -1);
    pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, axis);
    pSysWrite->writeSysfs(DISPLAY_FB0_WINDOW_AXIS, axis);

    if (!initDisplay) {
        pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
        pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
        setOsdMouse(outputmode);
    } else {
        startBootanimDetectThread();
    }

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

    if (!initDisplay) {
        pSysWrite->writeSysfs(DISPLAY_HDMI_AVMUTE, "-1");
    }
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
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, mode, DEFAULT_OUTPUT_MODE);
    }
}

//check if the edid support current hdmi mode
void DisplayMode::filterHdmiMode(char* mode, mbox_data_t* data) {
    char currentHdmiMode[MAX_STR_LEN] = {0};
    char edid[MAX_STR_LEN] = {0};
    char* arrayMode[MAX_STR_LEN] = {0};
    char* tmp;
    int len;

    strcpy(edid, data->edid);
    len = strlen(edid);
    tmp = edid;

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
    } while (tmp <= edid + len -1);

    //old mode is not support in this TV, so switch to best mode.
    getBestHdmiMode(mode, data);
}

void DisplayMode::getHdmiMode(char* mode, mbox_data_t* data) {
    if (strstr(data->edid, "null") != NULL) {
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, mode, DEFAULT_OUTPUT_MODE);
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
    char status[PROPERTY_VALUE_MAX]={0};

    pThiz->pSysWrite->readSysfs(DISPLAY_HPD_STATE, oldHpdstate);
    while (1) {
        if (property_get("instaboot.status", status, "completed") &&
           !strcmp("booting", status)){
            usleep(2000000);
            continue;
        }

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

void DisplayMode::startBootanimDetectThread() {
    pthread_t id;
    int ret;
    ret = pthread_create(&id, NULL, bootanimDetect, this);
    if (ret != 0) {
        SYS_LOGI("Create BootanimDetect error!\n");
    }
}

//if detected bootanim is running, then close uboot logo
void* DisplayMode::bootanimDetect(void* data){
    DisplayMode *pThiz = (DisplayMode*)data;
    char state_bootanim[MAX_STR_LEN] = {0};
    char fs_mode[MAX_STR_LEN] = {0};
    char outputmode[MAX_STR_LEN] = {0};

    pThiz->pSysWrite->getPropertyString(PROP_FS_MODE, fs_mode, "android");
    pThiz->pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, outputmode);

    //some boot videos maybe need 2~3s to start playing, so if the bootamin prop
    //don't run after about 4s,  exit the dead loop.
    int timeout = 40;
    while (strcmp(fs_mode, "recovery") && strcmp(state_bootanim, "running") && timeout > 0) {
        pThiz->pSysWrite->getPropertyString(PROP_BOOTANIM, state_bootanim, "sleep");
        usleep(100000);
        timeout--;
    }

    if (strcmp(fs_mode, "recovery")) {
        char delay[MAX_STR_LEN] = {0};
        pThiz->pSysWrite->getPropertyString(PROP_BOOTANIM_DELAY, delay, "100");
        usleep(atoi(delay) * 1000);
    }

    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
    pThiz->setOsdMouse(outputmode);

    return NULL;
}

bool DisplayMode::isBestOutputmode() {
    char isBestMode[MAX_STR_LEN] = {0};
    return !getBootEnv(UBOOTENV_ISBESTMODE, isBestMode) || strcmp(isBestMode, "true") == 0;
}

void DisplayMode::startDisableOsdThread() {
    pthread_t id;
    int ret;
    ret = pthread_create(&id, NULL, tmpDisableOsd, this);
    if (ret != 0) {
        SYS_LOGI("Create DisableOsdThread error!\n");
    }
}

void* DisplayMode::tmpDisableOsd(void* data){
    DisplayMode *pThiz = (DisplayMode*)data;

    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
    usleep(1000000);
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");

    return NULL;
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
    MODE_480I,
    MODE_480P,
    MODE_480CVBS,
    MODE_576I,
    MODE_576P,
    MODE_576CVBS,
    MODE_720P,
    MODE_720P50HZ,
    MODE_1080P24HZ,
    MODE_1080I50HZ,
    MODE_1080P50HZ,
    MODE_1080I,
    MODE_1080P,
    MODE_4K2K24HZ,
    MODE_4K2K25HZ,
    MODE_4K2K30HZ,
    MODE_4K2K50HZ,
    MODE_4K2K50HZ420,
    MODE_4K2K60HZ,
    MODE_4K2K60HZ420,
    MODE_4K2KSMPTE
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

    int position[4] = { 0, 0, 0, 0 };
    getPosition(curMode, position);
    setOsdMouse(position[0], position[1], position[2], position[3]);
}

void DisplayMode::setOsdMouse(int x, int y, int w, int h) {
    SYS_LOGI("set osd mouse x:%d y:%d w:%d h:%d", x, y, w, h);

    const char* displaySize = "1920 1080";
    if (!strncmp(mDefaultUI, "720", 3)) {
        displaySize = "1280 720";
    } else if (!strncmp(mDefaultUI, "1080", 4)) {
        displaySize = "1920 1080";
    } else if (!strncmp(mDefaultUI, "4k2k", 4)) {
        displaySize = "3840 2160";
    }

    char cur_mode[MAX_STR_LEN] = {0};
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, cur_mode);
    if (!strcmp(cur_mode, MODE_480I) || !strcmp(cur_mode, MODE_576I) ||
            !strcmp(cur_mode, MODE_480CVBS) || !strcmp(cur_mode, MODE_576CVBS) ||
            !strcmp(cur_mode, MODE_1080I50HZ) || !strcmp(cur_mode, MODE_1080I)) {
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

void DisplayMode::getPosition(const char* curMode, int *position) {
    int index = DISPLAY_MODE_1080P;
    for (int i = 0; i < DISPLAY_MODE_TOTAL; i++) {
        if (!strcmp(curMode, DISPLAY_MODE_LIST[i])) {
             index = i;
             break;
        }
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
        case DISPLAY_MODE_4K2K50HZ: // 4k2k50hz
        case DISPLAY_MODE_4K2K50HZ420: // 4k2k50hz420
            position[0] = getBootenvInt(ENV_4K2K50HZ_X, 0);
            position[1] = getBootenvInt(ENV_4K2K50HZ_Y, 0);
            position[2] = getBootenvInt(ENV_4K2K50HZ_W, FULL_WIDTH_4K2K);
            position[3] = getBootenvInt(ENV_4K2K50HZ_H, FULL_HEIGHT_4K2K);
            break;
        case DISPLAY_MODE_4K2K60HZ: // 4k2k60hz
        case DISPLAY_MODE_4K2K60HZ420: // 4k2k60hz420
            position[0] = getBootenvInt(ENV_4K2K60HZ_X, 0);
            position[1] = getBootenvInt(ENV_4K2K60HZ_Y, 0);
            position[2] = getBootenvInt(ENV_4K2K60HZ_W, FULL_WIDTH_4K2K);
            position[3] = getBootenvInt(ENV_4K2K60HZ_H, FULL_HEIGHT_4K2K);
            break;
        case DISPLAY_MODE_4K2KSMPTE: // 4k2ksmpte
            position[0] = getBootenvInt(ENV_4K2KSMPTE_X, 0);
            position[1] = getBootenvInt(ENV_4K2KSMPTE_Y, 0);
            position[2] = getBootenvInt(ENV_4K2KSMPTE_W, FULL_WIDTH_4K2KSMPTE);
            position[3] = getBootenvInt(ENV_4K2KSMPTE_H, FULL_HEIGHT_4K2KSMPTE);
            break;
        default: //1080p
            position[0] = getBootenvInt(ENV_1080P_X, 0);
            position[1] = getBootenvInt(ENV_1080P_Y, 0);
            position[2] = getBootenvInt(ENV_1080P_W, FULL_WIDTH_1080);
            position[3] = getBootenvInt(ENV_1080P_H, FULL_HEIGHT_1080);
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
        case DISPLAY_MODE_4K2K50HZ:    //4k2k50hz
        case DISPLAY_MODE_4K2K50HZ420: //4k2k50hz420
            setBootEnv(ENV_4K2K50HZ_X, x);
            setBootEnv(ENV_4K2K50HZ_Y, y);
            setBootEnv(ENV_4K2K50HZ_W, w);
            setBootEnv(ENV_4K2K50HZ_H, h);
            break;
        case DISPLAY_MODE_4K2K60HZ:    //4k2k60hz
        case DISPLAY_MODE_4K2K60HZ420: //4k2k60hz420
            setBootEnv(ENV_4K2K60HZ_X, x);
            setBootEnv(ENV_4K2K60HZ_Y, y);
            setBootEnv(ENV_4K2K60HZ_W, w);
            setBootEnv(ENV_4K2K60HZ_H, h);
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


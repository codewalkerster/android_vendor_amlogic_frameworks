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
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>

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

#define UBOOTENV_PREFIX             "ubootenv.var."
#define UBOOTENV_DIGITAUDIO         "ubootenv.var.digitaudiooutput"
#define UBOOTENV_HDMIMODE           "ubootenv.var.hdmimode"
#define UBOOTENV_CVBSMODE           "ubootenv.var.cvbsmode"
#define UBOOTENV_OUTPUTMODE         "ubootenv.var.outputmode"


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
    mLogLevel(LOG_LEVEL_DEFAULT) {

    if (NULL == path) {
        pConfigPath = DISPLAY_CFG_FILE;
    }
    else {
        pConfigPath = path;
    }

    SYS_LOGI("display mode config path: %s", pConfigPath);

    pSysWrite = new SysWrite();
}

DisplayMode::~DisplayMode() {
}

void DisplayMode::init() {
    parseConfigFile();

    SYS_LOGI("display mode init type: %d [0:none 1:tablet 2:mbox 3:tv]", mDisplayType);
    if (DISPLAY_TYPE_TABLET == mDisplayType) {
        setTabletDisplay();
    }
    else if (DISPLAY_TYPE_MBOX == mDisplayType) {
        setMboxDisplay();
    }
    else if (DISPLAY_TYPE_TV == mDisplayType) {
        setTVDisplay();
    }
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

    var_set.xres = mFb1Width;
	var_set.yres = mFb1Height;
	var_set.xres_virtual = mFb1Width;
    if (mFb1TripleEnable)
	    var_set.yres_virtual = 3*mFb1Height;
    else
        var_set.yres_virtual = 2*mFb1Height;
	var_set.bits_per_pixel = mFb1FbBits;
    setFbParameter(DISPLAY_FB1, var_set);

    pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, "panel");

    char axis[512] = {0};
    sprintf(axis, "%d %d %d %d %d %d %d %d",
        0, 0, mFb0Width, mFb0Height, 0, 0, mFb1Width, mFb1Height);

    pSysWrite->writeSysfs(SYSFS_DISPLAY_AXIS, axis);

    pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
    pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
}

void DisplayMode::setMboxDisplay() {
    const char *prefix = UBOOTENV_PREFIX;
    const char *suffix_x = "_x";
    const char *suffix_y = "_y";
    const char *suffix_w = "_width";
    const char *suffix_h = "_height";

    const char *suffix_x2 = "outputx";
    const char *suffix_y2 = "outputy";
    const char *suffix_w2 = "outputwidth";
    const char *suffix_h2 = "outputheight";

    char hpdstate[MAX_STR_LEN] = {0};
    char current_mode[MAX_STR_LEN] = {0};
    char outputmode[MAX_STR_LEN] = {0};
    pSysWrite->readSysfs(DISPLAY_HPD_STATE, hpdstate);
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, current_mode);

    if (pSysWrite->getPropertyBoolean(PROP_HDMIONLY, false)) {
        if (!strcmp(hpdstate, "1")){
            if (!strcmp(current_mode, MODE_480CVBS) || !strcmp(current_mode, MODE_576CVBS)) {
                pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
                pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
            }

            getBootEnv(UBOOTENV_HDMIMODE, outputmode);
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

    SYS_LOGI("init mbox display hpdstate:%s, current mode:%s outputmode:%s\n",
        hpdstate,
        current_mode,
        outputmode);

    pSysWrite->setProperty(PROP_LCD_DENSITY, "240");

    char key_prefix[MAX_STR_LEN] = {0};
    char key[MAX_STR_LEN] = {0};
    char value[MAX_STR_LEN] = {0};
    int outputx = 0;
    int outputy = 0;
    int outputwidth = 0;
    int outputheight = 0;
    int defaultwidth = 0;
    int defaultheight = 0;
    bool hasRead = false;
    bool usedDefault = false;

    while (true) {
        if (!strcmp(outputmode, MODE_4K2K24HZ) ||
            !strcmp(outputmode, MODE_4K2K25HZ) ||
            !strcmp(outputmode, MODE_4K2K30HZ) ||
            !strcmp(outputmode, MODE_4K2KSMPTE)) {

            if (!strcmp(outputmode, MODE_4K2KSMPTE)) {
                defaultwidth = outputwidth = 4096;
            }
            else {
                defaultwidth = outputwidth = 3840;
            }

            defaultheight = outputheight = 2160;

            strcpy(key_prefix, prefix);
            strcat(key_prefix, outputmode);

            strcpy(key, key_prefix);
            if (getBootEnv(strcat(key, suffix_x), value))
                outputx = atoi(value);

            strcpy(key, key_prefix);
            if (getBootEnv(strcat(key, suffix_y), value))
                outputy = atoi(value);

            strcpy(key, key_prefix);
            if (getBootEnv(strcat(key, suffix_w), value))
                outputwidth = atoi(value);

            strcpy(key, key_prefix);
            if (getBootEnv(strcat(key, suffix_h), value))
                outputheight = atoi(value);

            hasRead = true;
            break;
        }
        else if (!strcmp(outputmode, MODE_480P) ||
            !strcmp(outputmode, MODE_480I) ||
            !strcmp(outputmode, MODE_480CVBS)) {
            defaultwidth = outputwidth = 720;
            defaultheight = outputheight = 480;
            if (!strcmp(outputmode, MODE_480CVBS)) {
                strcpy(key_prefix, prefix);
                strcat(key_prefix, MODE_480I);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_x2), value))
                    outputx = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_y2), value))
                    outputy = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_w2), value))
                    outputwidth = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_h2), value))
                    outputheight = atoi(value);

                hasRead = true;
            }
            break;
        }
        else if (!strcmp(outputmode, MODE_576P) ||
            !strcmp(outputmode, MODE_576I) ||
            !strcmp(outputmode, MODE_576CVBS)) {
            defaultwidth = outputwidth = 720;
            defaultheight = outputheight = 576;

            if (!strcmp(outputmode, MODE_576CVBS)) {
                strcpy(key_prefix, prefix);
                strcat(key_prefix, MODE_576I);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_x2), value))
                    outputx = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_y2), value))
                    outputy = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_w2), value))
                    outputwidth = atoi(value);

                strcpy(key, key_prefix);
                if (getBootEnv(strcat(key, suffix_h2), value))
                    outputheight = atoi(value);

                hasRead = true;
            }
            break;
        }
        else if (!strcmp(outputmode, MODE_720P) ||
            !strcmp(outputmode, MODE_720P50HZ)) {
            defaultwidth = outputwidth = 1280;
            defaultheight = outputheight = 720;
            break;
        }
        else if (!strcmp(outputmode, MODE_1080P) ||
            !strcmp(outputmode, MODE_1080I) ||
            !strcmp(outputmode, MODE_1080P24HZ) ||
            !strcmp(outputmode, MODE_1080I50HZ) ||
            !strcmp(outputmode, MODE_1080P50HZ)) {
            defaultwidth = outputwidth = 1920;
            defaultheight = outputheight = 1080;
            break;
        }
        else {
            if (!usedDefault) {
                SYS_LOGI("using default ui: %s", mDefaultUI);

                usedDefault = true;
                strcpy(outputmode, mDefaultUI);
                setBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
            }
            else {
                SYS_LOGI("because bootenv and config default are errors, so using system default ui: %s", MODE_1080P);

                defaultwidth = outputwidth = 1920;
                defaultheight = outputheight = 1080;

                strcpy(outputmode, MODE_1080P);
                setBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
                break;
            }
        }
    }

    if (!hasRead) {
        strcpy(key_prefix, prefix);
        strcat(key_prefix, outputmode);

        strcpy(key, key_prefix);
        if (getBootEnv(strcat(key, suffix_x2), value))
            outputx = atoi(value);

        strcpy(key, key_prefix);
        if (getBootEnv(strcat(key, suffix_y2), value))
            outputy = atoi(value);

        strcpy(key, key_prefix);
        if (getBootEnv(strcat(key, suffix_w2), value))
            outputwidth = atoi(value);

        strcpy(key, key_prefix);
        if (getBootEnv(strcat(key, suffix_h2), value))
            outputheight = atoi(value);
    }

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


    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_MODE, "1");
    pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE_MODE, "1");
    pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
        0, 0, SOURCE_OUTPUT_WIDTH - 1, SOURCE_OUTPUT_HEIGHT -1);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_AXIS, axis);

    sprintf(axis, "%d %d %d %d",
        outputx, outputy, outputx + outputwidth - 1, outputy + outputheight -1);
    pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, axis);
    pSysWrite->writeSysfs(DISPLAY_FB0_WINDOW_AXIS, axis);

    pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
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


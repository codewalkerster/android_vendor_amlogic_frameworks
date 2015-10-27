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
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
#include <poll.h>

#include <sys/socket.h>
#include <sys/types.h>
#include <linux/netlink.h>

#include <cutils/properties.h>
#include "ubootenv.h"
#include "DisplayMode.h"
#include "SysTokenizer.h"

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

/**
 * strstr - Find the first substring in a %NUL terminated string
 * @s1: The string to be searched
 * @s2: The string to search for
 */
char *_strstr(const char *s1, const char *s2)
{
    size_t l1, l2;

    l2 = strlen(s2);
    if (!l2)
        return (char *)s1;
    l1 = strlen(s1);
    while (l1 >= l2) {
        l1--;
        if (!memcmp(s1, s2, l2))
            return (char *)s1;
        s1++;
    }
    return NULL;
}

#if 0
/* locate a substring */
static char * _strstr(char *src, char *desStr)
{
    char srcChar, desChar;
    int sublen = strlen(desStr);

    desChar = *desStr++;
    do {
        do {
            srcChar = *src++;
            if ((0 == srcChar) || (strlen(src) < (unsigned int)(sublen - 1)))
                return NULL;
        } while (srcChar != desChar);
    } while (strncmp(src, desStr, sublen - 1));

    return --src;
}
#endif

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

static int uevent_init()
{
    struct sockaddr_nl addr;
    int sz = 64*1024;
    int s;

    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_pid = getpid();
    addr.nl_groups = 0xffffffff;

    s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (s < 0)
        return 0;

    setsockopt(s, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz));

    if (bind(s, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        close(s);
        return 0;
    }

    return s;
}

static int uevent_next_event(int fd, char* buffer, int buffer_length)
{
    while (1) {
        struct pollfd fds;
        int nr;

        fds.fd = fd;
        fds.events = POLLIN;
        fds.revents = 0;
        nr = poll(&fds, 1, -1);

        if (nr > 0 && (fds.revents & POLLIN)) {
            int count = recv(fd, buffer, buffer_length, 0);
            if (count > 0) {
                return count;
            }
        }
    }

    // won't get here
    return 0;
}

static bool isMatch(const char* buffer, size_t length, char* switch_state, char* switch_name) {
    bool matched = false;
    // Consider all zero-delimited fields of the buffer.
    const char* field = buffer;
    const char* end = buffer + length + 1;
    do {
        if (strstr(field, HDMI_UEVENT)) {
            SYS_LOGI("Matched uevent message with pattern: %s", HDMI_UEVENT);
            matched = true;
        }
        else if (strstr(field, HDMI_POWER_UEVENT)) {
            SYS_LOGI("Matched uevent message with pattern: %s", HDMI_POWER_UEVENT);
            matched = true;
        }
        //SWITCH_STATE=1, SWITCH_NAME=hdmi
        else if (strstr(field, "SWITCH_STATE=")) {
            strcpy(switch_state, field + strlen("SWITCH_STATE="));
        }
        else if (strstr(field, "SWITCH_NAME=")) {
            strcpy(switch_name, field + strlen("SWITCH_NAME="));
        }
        field += strlen(field) + 1;
    } while (field != end);

    return matched;
}

// all the hdmi plug checking complete in this loop
static void* HdmiPlugDetectThread(void* data) {
    DisplayMode *pThiz = (DisplayMode*)data;

    char status[PROPERTY_VALUE_MAX] = {0};
#if 0
    char oldHpdstate[MAX_STR_LEN] = {0};
    char currentHpdstate[MAX_STR_LEN] = {0};

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

            pThiz->setMboxDisplay(currentHpdstate, false);
            strcpy(oldHpdstate, currentHpdstate);
        }
        usleep(2000000);
    }
#endif

    //use uevent instead of usleep, because it's has some delay
    char buffer[1024] = {0};
    char switch_name[128] = {0};
    char switch_state[128] = {0};
    int fd = uevent_init();
    while (fd >= 0) {
        if (property_get("instaboot.status", status, "completed") &&
           !strcmp("booting", status)){
            usleep(2000000);
            continue;
        }

        int length = uevent_next_event(fd, buffer, sizeof(buffer) - 1);
        if (length <= 0)
            continue;

        buffer[length] = '\0';

    #if 0
        //change@/devices/virtual/switch/hdmi ACTION=change DEVPATH=/devices/virtual/switch/hdmi
        //SUBSYSTEM=switch SWITCH_NAME=hdmi SWITCH_STATE=0 SEQNUM=2791
        char printBuf[1024] = {0};
        memcpy(printBuf, buffer, length);
        for (int i = 0; i < length; i++) {
            if (printBuf[i] == 0x0)
                printBuf[i] = ' ';
        }
        SYS_LOGI("Received uevent message: %s", printBuf);
    #endif

        if (isMatch(buffer, length, switch_state, switch_name)) {
            SYS_LOGI("HDMI switch_state: %s switch_name: %s\n", switch_state, switch_name);
            if (!strcmp(switch_name, "hdmi") ||
                //0: hdmi suspend 1:hdmi resume
                (!strcmp(switch_name, "hdmi_power") && !strcmp(switch_state, "1"))) {
                pThiz->setMboxDisplay(switch_state, false);
            }
        }
    }

    return NULL;
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
    mVideoPlaying(false),
    mNativeWinX(0), mNativeWinY(0), mNativeWinW(0), mNativeWinH(0),
    mDisplayWidth(FULL_WIDTH_1080),
    mDisplayHeight(FULL_HEIGHT_1080),
    mLogLevel(LOG_LEVEL_DEFAULT) {

    if (NULL == path) {
        pConfigPath = DISPLAY_CFG_FILE;
    }
    else {
        pConfigPath = path;
    }

#if !defined(ODROIDC2)
    SYS_LOGI("display mode config path: %s", pConfigPath);
#endif

    pSysWrite = new SysWrite();
    mVideoAxisMap.clear();
}

DisplayMode::~DisplayMode() {
    mVideoAxisMap.clear();
    delete pSysWrite;
}

void DisplayMode::init() {
#if defined(ODROIDC2)
	setMboxDisplay(NULL, true);
#else
    parseConfigFile();

    SYS_LOGI("display mode init type: %d [0:none 1:tablet 2:mbox 3:tv], soc type:%s, default UI:%s",
        mDisplayType, mSocType, mDefaultUI);
    if (DISPLAY_TYPE_TABLET == mDisplayType) {
        setTabletDisplay();
    }
    else if (DISPLAY_TYPE_MBOX == mDisplayType) {
        setMboxDisplay(NULL, true);

        pthread_t id;
        int ret = pthread_create(&id, NULL, HdmiPlugDetectThread, this);
        if (ret != 0) {
            SYS_LOGE("Create HdmiPlugDetectThread error!\n");
        }
    }
    else if (DISPLAY_TYPE_TV == mDisplayType) {
        setTVDisplay();
    }
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
                    tokenizer->skipDelimiters(WHITESPACE);
                    strcpy(mDefaultUI, tokenizer->nextToken(WHITESPACE));
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

void DisplayMode::setMboxDisplay(char* hpdstate, bool initState) {
    hdmi_data_t data;
    char outputmode[MODE_LEN] = {0};
    memset(&data, 0, sizeof(hdmi_data_t));

    initHdmiData(&data, hpdstate);
    if (pSysWrite->getPropertyBoolean(PROP_HDMIONLY, true)) {
        if (!strcmp(data.hpd_state, "1")) {
            if ((!strcmp(data.current_mode, MODE_480CVBS) || !strcmp(data.current_mode, MODE_576CVBS))
                    && initState) {
                pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
                pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
            }

            getHdmiOutputMode(outputmode, &data);
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
    if (strcmp(data.hpd_state, "1")) {
        if (strcmp(outputmode, MODE_480CVBS) && strcmp(outputmode, MODE_576CVBS)) {
            strcpy(outputmode, MODE_576CVBS);
        }
    }

    SYS_LOGI("init mbox display hpdstate:%s, old outputmode:%s, new outputmode:%s\n",
            data.hpd_state,
            data.current_mode,
            outputmode);
    if (strlen(outputmode) == 0)
        strcpy(outputmode, mDefaultUI);

    if (initState) {
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
    }

    //output mode not the same
    if (strcmp(data.current_mode, outputmode)) {
        if (initState) {
            //when change mode, need close uboot logo to avoid logo scaling wrong
            pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
            pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
            pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");
        } else {
            #if 0
            //when change mode, it need time to set osd register,
            //so we disable osd 1 second to avoid screen flicker
            char bootvideo[MODE_LEN] = {0};
            char state_bootanim[MODE_LEN] = {"sleep"};
            pSysWrite->getPropertyString(PROP_BOOTVIDEO_SERVICE, bootvideo, "0");
            pSysWrite->getPropertyString(PROP_BOOTANIM, state_bootanim, "sleep");
            //boot video not start or not running, need close osd, then open osd
            if (strcmp(bootvideo, "1") || strcmp(state_bootanim, "running"))
                startDisableOsdThread();
            #endif
        }
    }
    setMboxOutputMode(outputmode, initState);
}

void DisplayMode::setMboxOutputMode(const char* outputmode){
    setMboxOutputMode(outputmode, false);
}

void DisplayMode::setMboxOutputMode(const char* outputmode, bool initState) {
    char value[MAX_STR_LEN] = {0};
    char preMode[MODE_LEN] = {0};
    int outputx = 0;
    int outputy = 0;
    int outputwidth = 0;
    int outputheight = 0;
    int position[4] = { 0, 0, 0, 0 };
    bool cvbsMode = false;

    //first close osd, after HDCP authenticate completely, then open osd
    pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");

    if (!initState) {
        pSysWrite->writeSysfs(DISPLAY_HDMI_AVMUTE, "1");
        usleep(30000);//30ms
    }

    memset(preMode, 0, sizeof(preMode));
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, preMode);

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

        cvbsMode = true;
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, mode);
        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE2, "null");
    }
    else {
        if (!strcmp(outputmode, MODE_480CVBS) || !strcmp(outputmode, MODE_576CVBS))
            cvbsMode = true;

        pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, outputmode);
    }

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
            0, 0, mDisplayWidth - 1, mDisplayHeight - 1);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_AXIS, axis);

    sprintf(axis, "%d %d %d %d",
            outputx, outputy, outputx + outputwidth - 1, outputy + outputheight -1);
    pSysWrite->writeSysfs(DISPLAY_FB0_WINDOW_AXIS, axis);
    setVideoAxis(preMode, outputmode);
    setNativeWindowRect(mNativeWinX, mNativeWinY, mNativeWinW, mNativeWinH);

    //only HDMI mode need HDCP authenticate
    if (!cvbsMode) {
        hdcpAuthenticate();
    }

    if (initState) {
        startBootanimDetectThread();
    } else {
        pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
        pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
        setOsdMouse(outputmode);
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

    if (!initState) {
        pSysWrite->writeSysfs(DISPLAY_HDMI_AVMUTE, "-1");
    }

    SYS_LOGI("set output mode:%s done\n", outputmode);
}

void DisplayMode::setNativeWindowRect(int x, int y, int w, int h) {
    char currMode[MODE_LEN] = {0};
    int currPos[4] = {0};

    if (!mVideoPlaying) {
        SYS_LOGI("video is not playing, don't need set video axis\n");
        return;
    }

    mNativeWinX = x;
    mNativeWinY = y;
    mNativeWinW = w;
    mNativeWinH = h;

    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, currMode);
    getPosition(currMode, currPos);

    //need base as display width and height
    float scaleW = (float)currPos[2]/mDisplayWidth;
    float scaleH = (float)currPos[3]/mDisplayHeight;

    //scale down or up the native window position
    int outputx = currPos[0] + x*scaleW;
    int outputy = currPos[1] + y*scaleH;
    int outputwidth = w*scaleW;
    int outputheight = h*scaleH;

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
            outputx, outputy, outputx + outputwidth - 1, outputy + outputheight - 1);
    SYS_LOGD("write %s: %s\n", SYSFS_VIDEO_AXIS, axis);
    pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, axis);
}

void DisplayMode::setVideoPlaying(bool playing) {
    mVideoPlaying = playing;
    SYS_LOGD("set video playing %d\n", playing?1:0);
}

bool DisplayMode::axisValid(const axis_t *axis) {
    return (axis->x >= 0) && (axis->y >= 0) && (axis->w > 0) && (axis->h > 0);
}

bool DisplayMode::axisEqual(int value1, int value2) {
    return (value2 >= (value1 - 1)) && (value2 <= (value1 + 1));
}

bool DisplayMode::checkAxisSame(const axis_t *axis1, const axis_t *axis2) {
    if (!axisValid(axis1))
        return false;

    if (!axisValid(axis2))
        return false;

    if (!axisEqual(axis1->x, axis2->x))
        return false;

    if (!axisEqual(axis1->y, axis2->y))
        return false;

    if (!axisEqual(axis1->w, axis2->w))
        return false;

    if (!axisEqual(axis1->h, axis2->h))
        return false;

    return true;
}

void DisplayMode::calcVideoAxis(const axis_t *prePosition, const axis_t *position,
        const axis_t *axis, axis_t *videoAxis) {
    videoAxis->x = (int)std::round(((axis->x - prePosition->x) * position->w  * 1.0f) / prePosition->w + position->x);
    videoAxis->y = (int)std::round(((axis->y - prePosition->y) * position->h * 1.0f) / prePosition->h + position->y);
    videoAxis->w = (int)std::round((axis->w * position->w * 1.0f) / prePosition->w);
    videoAxis->h = (int)std::round((axis->h * position->h * 1.0f) / prePosition->h);
}

void DisplayMode::axisStr(const axis_t *axis, char *str) {
    sprintf(str, "x(%d) y(%d) w(%d) h(%d)", axis->x, axis->y, axis->w, axis->h);
}

void DisplayMode::setVideoAxis(const char *preMode, const char *mode) {
    char str[MAX_STR_LEN] = {0,};
    std::string preModeStr = preMode;
    std::string modeStr = mode;
    axis_t axis;
    std::map<std::string, axis_t>::iterator it;
    int position[4] = {0, 0, 0, 0};
    axis_t prePositionAxis;
    axis_t positionAxis;
    axis_t preVideoPositionAxis;

    SYS_LOGD("[%s]preMode: %s\n", __FUNCTION__, preMode);
    SYS_LOGD("[%s]mode: %s\n", __FUNCTION__, mode);

    getPosition(preMode, position);
    prePositionAxis.x = position[0];
    prePositionAxis.y = position[1];
    prePositionAxis.w = position[2];
    prePositionAxis.h = position[3];
    memset(str, 0, sizeof(str));
    axisStr(&prePositionAxis, str);
    SYS_LOGD("[%s]prePositionAxis: %s\n", __FUNCTION__, str);

    getPosition(mode, position);
    positionAxis.x = position[0];
    positionAxis.y = position[1];
    positionAxis.w = position[2];
    positionAxis.h = position[3];
    memset(str, 0, sizeof(str));
    axisStr(&positionAxis, str);
    SYS_LOGD("[%s]positionAxis: %s\n", __FUNCTION__, str);

    memset(str, 0, sizeof(str));
    pSysWrite->readSysfs(SYSFS_VIDEO_AXIS, str);
    sscanf(str, "%d %d %d %d", position, position + 1, position + 2, position + 3);
    preVideoPositionAxis.x = position[0];
    preVideoPositionAxis.y = position[1];
    preVideoPositionAxis.w = position[2] - position[0] + 1;
    preVideoPositionAxis.h = position[3] - position[1] + 1;
    memset(str, 0, sizeof(str));
    axisStr(&preVideoPositionAxis, str);
    SYS_LOGD("[%s]preVideoPositionAxis: %s\n", __FUNCTION__, str);

    if (((preVideoPositionAxis.x == 0) && (preVideoPositionAxis.y == 0)
                && (preVideoPositionAxis.w == 0) && (preVideoPositionAxis.h == 0))
            || ((preVideoPositionAxis.x == 0) && (preVideoPositionAxis.y == 0)
                && (preVideoPositionAxis.w == -1) && (preVideoPositionAxis.h == -1))
            || ((preVideoPositionAxis.x <= prePositionAxis.x) && (preVideoPositionAxis.y <= prePositionAxis.y)
                && (preVideoPositionAxis.w >= prePositionAxis.w) && (preVideoPositionAxis.h >= prePositionAxis.h))) {
        memset(str, 0, sizeof(str));
        sprintf(str, "%d %d %d %d",
                positionAxis.x, positionAxis.y, positionAxis.w + positionAxis.x - 1, positionAxis.h + positionAxis.y - 1);
        SYS_LOGD("[%s:%d]write %s: %s\n", __FUNCTION__, __LINE__, SYSFS_VIDEO_AXIS, str);
        pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, str);
        return;
    }

    it = mVideoAxisMap.find(preModeStr);
    if (it != mVideoAxisMap.end()) {
        axis = it->second;
        if (checkAxisSame(&axis, &preVideoPositionAxis)) {
            mVideoAxisMap[preModeStr] = preVideoPositionAxis;
            it = mVideoAxisMap.find(modeStr);
            if (it == mVideoAxisMap.end()) {
                calcVideoAxis(&prePositionAxis, &positionAxis, &preVideoPositionAxis, &axis);
                mVideoAxisMap[modeStr] = axis;
            } else {
                axis = it->second;
            }
            memset(str, 0, sizeof(str));
            sprintf(str, "%d %d %d %d", axis.x, axis.y, axis.w + axis.x - 1, axis.h + axis.y - 1);
            SYS_LOGD("[%s:%d]write %s: %s\n", __FUNCTION__, __LINE__, SYSFS_VIDEO_AXIS, str);
            pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, str);
            return;
        }
    }
    mVideoAxisMap.clear();
    mVideoAxisMap[preModeStr] = preVideoPositionAxis;
    calcVideoAxis(&prePositionAxis, &positionAxis, &preVideoPositionAxis, &axis);
    mVideoAxisMap[modeStr] = axis;
    memset(str, 0, sizeof(str));
    sprintf(str, "%d %d %d %d", axis.x, axis.y, axis.w + axis.x - 1, axis.h + axis.y - 1);
    SYS_LOGD("[%s:%d]write %s: %s\n", __FUNCTION__, __LINE__, SYSFS_VIDEO_AXIS, str);
    pSysWrite->writeSysfs(SYSFS_VIDEO_AXIS, str);
}

//get the best hdmi mode by edid
void DisplayMode::getBestHdmiMode(char* mode, hdmi_data_t* data) {
    char* pos = strchr(data->edid, '*');
    if (pos != NULL) {
        char* findReturn = pos;
        while (*findReturn != 0x0a && findReturn >= data->edid) {
            findReturn--;
        }
        //*pos = 0;
        //strcpy(mode, findReturn + 1);

        findReturn = findReturn + 1;
        strncpy(mode, findReturn, pos - findReturn);
        SYS_LOGI("set HDMI to best edid mode: %s\n", mode);
    }

    if (strlen(mode) == 0) {
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, mode, DEFAULT_OUTPUT_MODE);
    }

  /*
    char* arrayMode[MAX_STR_LEN] = {0};
    char* tmp;

    int len = strlen(data->edid);
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
}

//check if the edid support current hdmi mode
void DisplayMode::filterHdmiMode(char* mode, hdmi_data_t* data) {
    char *pCmp = data->edid;
    while ((pCmp - data->edid) < (int)strlen(data->edid)) {
        char *pos = strchr(pCmp, 0x0a);
        if (NULL == pos)
            break;

        if (!strncmp(pCmp, data->ubootenv_hdmimode, pos - pCmp)) {
            strcpy(mode, data->ubootenv_hdmimode);
            return;
        }
        pCmp = pos + 1;
    }

    //old mode is not support in this TV, so switch to best mode.
    getBestHdmiMode(mode, data);
}

void DisplayMode::getHdmiOutputMode(char* mode, hdmi_data_t* data) {
    if (strstr(data->edid, "null") != NULL) {
        pSysWrite->getPropertyString(PROP_BEST_OUTPUT_MODE, mode, DEFAULT_OUTPUT_MODE);
        return;
    }

    bool edidChange = isEdidChange();
    if (pSysWrite->getPropertyBoolean(PROP_HDMIONLY, true)) {
        if (isBestOutputmode()) {
            getBestHdmiMode(mode, data);
        } else {
            //filterHdmiMode(mode, data);
            if (!edidChange && strlen(data->ubootenv_hdmimode) > 0) {
                strcpy(mode, data->ubootenv_hdmimode);
            } else {
                getBestHdmiMode(mode, data);
            }
        }
    }
    SYS_LOGI("set HDMI mode to %s\n", mode);
}

void DisplayMode::initHdmiData(hdmi_data_t* data, char* hpdstate){
    if (hpdstate == NULL) {
        pSysWrite->readSysfs(DISPLAY_HPD_STATE, data->hpd_state);
    } else {
        strcpy(data->hpd_state, hpdstate);
    }

    if (!strcmp(data->hpd_state, "1")) {
        int count = 0;
        while (true) {
            pSysWrite->readSysfsOriginal(DISPLAY_HDMI_EDID, data->edid);
            if (strlen(data->edid) > 0)
                break;

            if (count >= 5) {
                strcpy(data->edid, "null edid");
                break;
            }
            count++;
            usleep(500000);
        }
    }
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, data->current_mode);
    getBootEnv(UBOOTENV_HDMIMODE, data->ubootenv_hdmimode);
}

void DisplayMode::startBootanimDetectThread() {
    pthread_t id;
    int ret = pthread_create(&id, NULL, bootanimDetect, this);
    if (ret != 0) {
        SYS_LOGE("Create BootanimDetect error!\n");
    }
}

//if detected bootanim is running, then close uboot logo
void* DisplayMode::bootanimDetect(void* data) {
    DisplayMode *pThiz = (DisplayMode*)data;
    char state_bootanim[MODE_LEN] = {"sleep"};
    char fs_mode[MODE_LEN] = {0};
    char outputmode[MODE_LEN] = {0};
    char bootvideo[MODE_LEN] = {0};

    pThiz->pSysWrite->getPropertyString(PROP_FS_MODE, fs_mode, "android");
    pThiz->pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, outputmode);

    //not in the recovery mode
    if (strcmp(fs_mode, "recovery")) {
        //some boot videos maybe need 2~3s to start playing, so if the bootamin property
        //don't run after about 4s, exit the loop.
        int timeout = 40;
        while (timeout > 0) {
            pThiz->pSysWrite->getPropertyString(PROP_BOOTANIM, state_bootanim, "sleep");
            //boot animation or boot video is running
            if (!strcmp(state_bootanim, "running"))
                break;

            usleep(100000);
            timeout--;
        }

        int delayMs = pThiz->pSysWrite->getPropertyInt(PROP_BOOTANIM_DELAY, 100);
        usleep(delayMs * 1000);
    }

    pThiz->pSysWrite->writeSysfs(DISPLAY_LOGO_INDEX, "0");
    //pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
    //need close fb1, because uboot logo show in fb1
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB1_BLANK, "1");
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB1_FREESCALE, "0");

    pThiz->pSysWrite->getPropertyString(PROP_BOOTVIDEO_SERVICE, bootvideo, "0");
    SYS_LOGI("boot animation detect boot video:%s\n", bootvideo);
    //not boot video running, boot animation running
    if (strcmp(bootvideo, "1")) {
        //open fb0, let bootanimation show in it
        pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");
        if (DISPLAY_TYPE_TV == pThiz->mDisplayType && !strncmp(outputmode, "1080", 4)) {
            pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0");
        } else {
            pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
        }
    }

    pThiz->setOsdMouse(outputmode);
    return NULL;
}

#if defined(ODROIDC2)
/*
 * FIXME: What should we do for ODROID-C2?
 */
bool DisplayMode::isEdidChange() {
	return false;
}
#else
//get edid crc value to check edid change
bool DisplayMode::isEdidChange() {
    char edid[MAX_STR_LEN] = {0};
    char crcvalue[MAX_STR_LEN] = {0};
    unsigned int crcheadlength = strlen(DEFAULT_EDID_CRCHEAD);
    pSysWrite->readSysfs(DISPLAY_EDID_VALUE, edid);
    char *p = strstr(edid, DEFAULT_EDID_CRCHEAD);
    if (p != NULL && strlen(p) > crcheadlength) {
        p += crcheadlength;
        if (!getBootEnv(UBOOTENV_EDIDCRCVALUE, crcvalue) || strncmp(p, crcvalue, strlen(p))) {
            setBootEnv(UBOOTENV_EDIDCRCVALUE, p);
            return true;
        }
    }
    return false;
}
#endif

bool DisplayMode::isBestOutputmode() {
#if defined(ODROIDC2)
	/*
	 * FIXME: Don't we discover best output mode from EDID?
	 * Currently return 'false' will force the output resolution as hdmi
	 * mode in 'boot.ini'
	 */
	return false;
#else
    char isBestMode[MODE_LEN] = {0};
    return !getBootEnv(UBOOTENV_ISBESTMODE, isBestMode) || strcmp(isBestMode, "true") == 0;
#endif
}

void DisplayMode::startDisableOsdThread() {
    pthread_t id;
    int ret = pthread_create(&id, NULL, tmpDisableOsd, this);
    if (ret != 0) {
        SYS_LOGE("Create DisableOsdThread error!\n");
    }
}

void* DisplayMode::tmpDisableOsd(void* data){
    DisplayMode *pThiz = (DisplayMode*)data;

    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "1");
    usleep(1000000);
    pThiz->pSysWrite->writeSysfs(DISPLAY_FB0_BLANK, "0");

    return NULL;
}

void DisplayMode::setTVOutputMode(const char* outputmode) {
    int outputx = 0;
    int outputy = 0;
    int outputwidth = 0;
    int outputheight = 0;
    int position[4] = { 0, 0, 0, 0 };

    getPosition(outputmode, position);
    outputx = position[0];
    outputy = position[1];
    outputwidth = position[2];
    outputheight = position[3];

    pSysWrite->writeSysfs(SYSFS_DISPLAY_MODE, outputmode);

    char axis[MAX_STR_LEN] = {0};
    sprintf(axis, "%d %d %d %d",
            0, 0, mDisplayWidth - 1, mDisplayHeight - 1);
    pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_AXIS, axis);

    sprintf(axis, "%d %d %d %d",
            outputx, outputy, outputx + outputwidth - 1, outputy + outputheight -1);
    pSysWrite->writeSysfs(DISPLAY_FB0_WINDOW_AXIS, axis);

    if (outputwidth == FULL_WIDTH_4K2K) {
        pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE_MODE, "2");//super scale
        pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0x10001");
        //setOsdMouse(outputmode);
    } else {
        pSysWrite->writeSysfs(DISPLAY_FB0_FREESCALE, "0");
    }

    startBootanimDetectThread();
}

void DisplayMode::setTVDisplay() {
    char current_mode[MODE_LEN] = {0};
    char outputmode[MODE_LEN] = {0};

    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, current_mode);
    getBootEnv(UBOOTENV_OUTPUTMODE, outputmode);
    SYS_LOGD("init tv display old outputmode:%s, outputmode:%s\n", current_mode, outputmode);

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
        mDisplayWidth = FULL_WIDTH_1080;
        mDisplayHeight = FULL_HEIGHT_1080;
        pSysWrite->setProperty(PROP_LCD_DENSITY, DESITY_1080P);
        pSysWrite->setProperty(PROP_WINDOW_WIDTH, "1920");
        pSysWrite->setProperty(PROP_WINDOW_HEIGHT, "1080");
    }
    if (strcmp(current_mode, outputmode)) {
        char bootvideo[MODE_LEN] = {0};
        char state_bootanim[MODE_LEN] = {"sleep"};
        pSysWrite->getPropertyString(PROP_BOOTVIDEO_SERVICE, bootvideo, "0");
        pSysWrite->getPropertyString(PROP_BOOTANIM, state_bootanim, "sleep");
        if (!(!strcmp(bootvideo, "1") && !strcmp(state_bootanim, "running")))
            startDisableOsdThread();
    }

    setTVOutputMode(outputmode);
}

void DisplayMode::setFbParameter(const char* fbdev, struct fb_var_screeninfo var_set) {
    struct fb_var_screeninfo var_old;

    int fh = open(fbdev, O_RDONLY);
    ioctl(fh, FBIOGET_VSCREENINFO, &var_old);

    copy_changed_values(&var_old, &var_set);
    ioctl(fh, FBIOPUT_VSCREENINFO, &var_old);
    close(fh);
}

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

    char cur_mode[MODE_LEN] = {0};
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
    if (DISPLAY_TYPE_TV == mDisplayType && !strncmp(cur_mode, "1080", 4)) {
        pSysWrite->writeSysfs(DISPLAY_FB1_SCALE, "0");
    } else {
        pSysWrite->writeSysfs(DISPLAY_FB1_SCALE, "0x10001");
    }
}

void DisplayMode::getPosition(const char* curMode, int *position) {
    int index = modeToIndex(curMode);
    switch (index) {
        case DISPLAY_MODE_480I:
        case DISPLAY_MODE_480CVBS: // 480cvbs
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
        case DISPLAY_MODE_576CVBS: // 576cvbs
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

    char curMode[MODE_LEN] = {0};
    pSysWrite->readSysfs(SYSFS_DISPLAY_MODE, curMode);
    int index = modeToIndex(curMode);
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

int DisplayMode::modeToIndex(const char *mode) {
    int index = DISPLAY_MODE_1080P;
    for (int i = 0; i < DISPLAY_MODE_TOTAL; i++) {
        if (!strcmp(mode, DISPLAY_MODE_LIST[i])) {
            index = i;
            break;
        }
    }

    //SYS_LOGI("modeToIndex mode:%s index:%d", mode, index);
    return index;
}

void DisplayMode::hdcpAuthenticate() {
    bool useHdcp22 = false;
    bool useHdcp14 = false;
    char hdcpVer[MODE_LEN] = {0};
    char hdcpKey[MODE_LEN] = {0};

    //14 22 00 HDCP TX
    pSysWrite->readSysfs(DISPLAY_HDMI_HDCP_KEY, hdcpKey);
    SYS_LOGI("HDCP TX key:%s\n", hdcpKey);
    if ((strlen(hdcpKey) == 0) || !(strcmp(hdcpKey, "00")))
        return;

    //14 22 00 HDCP RX
    pSysWrite->readSysfs(DISPLAY_HDMI_HDCP_VER, hdcpVer);
    SYS_LOGI("HDCP RX version:%s\n", hdcpVer);
    if ((strlen(hdcpVer) == 0) || !(strcmp(hdcpVer, "00")))
        return;

#ifdef HDCP_AUTHENTICATION
    char cap[MAX_STR_LEN] = {0};
    pSysWrite->readSysfsOriginal(DISPLAY_HDMI_EDID, cap);
    if ((_strstr(cap, (char *)"2160p") != NULL) && (_strstr(hdcpVer, (char *)"22") != NULL) &&
        (_strstr(hdcpKey, (char *)"22") != NULL)) {
        useHdcp22 = true;
        pSysWrite->writeSysfs(DISPLAY_HDMI_HDCP_MODE, DISPLAY_HDMI_HDCP_22);

        SYS_LOGI("HDCP 2.2, stop hdcp_tx22, init will kill hdcp_tx22\n");
        pSysWrite->setProperty("ctl.stop", "hdcp_tx22");
        usleep(50*1000);
        SYS_LOGI("HDCP 2.2, start hdcp_tx22\n");
        pSysWrite->setProperty("ctl.start", "hdcp_tx22");
    }

    if (!useHdcp22 && (_strstr(hdcpVer, (char *)"14") != NULL) &&
        (_strstr(hdcpKey, (char *)"14") != NULL)) {
        useHdcp14 = true;
        SYS_LOGI("HDCP 1.4\n");
        pSysWrite->writeSysfs(DISPLAY_HDMI_HDCP_MODE, DISPLAY_HDMI_HDCP_14);
    }

    if (!useHdcp22 && !useHdcp14) {
        //do not support hdcp1.4 and hdcp2.2
        SYS_LOGE("device do not support hdcp1.4 or hdcp2.2\n");
        return;
    }

    SYS_LOGI("begin to authenticate\n");
    int count = 0;
    while (true) {
        usleep(500*1000);//sleep 500ms

        char auth[MODE_LEN] = {0};
        pSysWrite->readSysfs(DISPLAY_HDMI_HDCP_AUTH, auth);
        if (_strstr(auth, (char *)"1")) //Authenticate is OK
            break;

        count++;
        if (count > 10) { //max 5s it will authenticate completely
            if (useHdcp22) {
                SYS_LOGE("HDCP22 authenticate fail, 5s timeout\n");

                count = 0;
                useHdcp22 = false;
                useHdcp14 = true;
                //if support hdcp22, must support hdcp14
                pSysWrite->writeSysfs(DISPLAY_HDMI_HDCP_MODE, DISPLAY_HDMI_HDCP_14);
                continue;
            }
            else if (useHdcp14) {
                SYS_LOGE("HDCP14 authenticate fail, 5s timeout\n");

                pSysWrite->writeSysfs(DISPLAY_HDMI_HDCP_CONF, DISPLAY_HDMI_HDCP_STOP);
            }
            break;
        }
    }
    SYS_LOGI("authenticate finish\n");
#endif
}

void DisplayMode::hdcpSwitch() {
    SYS_LOGI("hdcpSwitch for debug hdcp authenticate\n");
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


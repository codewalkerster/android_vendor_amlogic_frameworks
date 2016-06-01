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
 *  @version  1.0
 *  @date     2014/10/22
 *  @par function description:
 *  - 1 control system sysfs proc env & property
 */

#ifndef ANDROID_SYSTEM_CONTROL_H
#define ANDROID_SYSTEM_CONTROL_H

#include <utils/Errors.h>
#include <utils/String8.h>
#include <utils/String16.h>
#include <utils/Mutex.h>
#include <ISystemControlService.h>

#include "SysWrite.h"
#include "common.h"
#include "DisplayMode.h"

//should sync with vendor\amlogic\frameworks\av\LibPlayer\amcodec\include\amports\Amstream.h
#define VIDEO_PATH              "/dev/amvideo"
#define DI_BYPASS_ALL           "/sys/module/di/parameters/bypass_all"
#define DI_BYPASS_POST          "/sys/module/di/parameters/bypass_post"
#define DET3D_MODE_SYSFS        "/sys/module/di/parameters/det3d_mode"
#define PROG_PROC_SYSFS         "/sys/module/di/parameters/prog_proc_config"

#define AMSTREAM_IOC_MAGIC   'S'
#define AMSTREAM_IOC_SET_3D_TYPE                    _IOW((AMSTREAM_IOC_MAGIC), 0x3c, unsigned int)
#define AMSTREAM_IOC_GET_3D_TYPE                    _IOW((AMSTREAM_IOC_MAGIC), 0x3d, unsigned int)
#define AMSTREAM_IOC_GET_SOURCE_VIDEO_3D_TYPE       _IOW((AMSTREAM_IOC_MAGIC), 0x3e, unsigned int)

//should sync with common\drivers\amlogic\amports\Vpp.h

/*when the output mode is field alterlative*/
/* LRLRLRLRL mode */
#define MODE_3D_OUT_FA_L_FIRST      0x00001000
#define MODE_3D_OUT_FA_R_FIRST      0x00002000
/* LBRBLBRB */
#define MODE_3D_OUT_FA_LB_FIRST     0x00004000
#define MODE_3D_OUT_FA_RB_FIRST     0x00008000

#define MODE_3D_OUT_FA_MASK \
    (MODE_3D_OUT_FA_L_FIRST | \
    MODE_3D_OUT_FA_R_FIRST|MODE_3D_OUT_FA_LB_FIRST|MODE_3D_OUT_FA_RB_FIRST)

#define MODE_3D_DISABLE             0x00000000
#define MODE_3D_ENABLE              0x00000001
#define MODE_3D_AUTO                (0x00000002 | MODE_3D_ENABLE)
#define MODE_3D_LR                  (0x00000004 | MODE_3D_ENABLE)
#define MODE_3D_TB                  (0x00000008 | MODE_3D_ENABLE)
#define MODE_3D_LA                  (0x00000010 | MODE_3D_ENABLE)
#define MODE_3D_FA                  (0x00000020 | MODE_3D_ENABLE)
#define MODE_3D_LR_SWITCH           (0x00000100 | MODE_3D_ENABLE)
#define MODE_3D_TO_2D_L             (0x00000200 | MODE_3D_ENABLE)
#define MODE_3D_TO_2D_R             (0x00000400 | MODE_3D_ENABLE)
#define MODE_3D_MVC                 (0x00000800 | MODE_3D_ENABLE)
#define MODE_3D_OUT_TB              (0x00010000 | MODE_3D_ENABLE)
#define MODE_3D_OUT_LR              (0x00020000 | MODE_3D_ENABLE)
#define MODE_FORCE_3D_TO_2D_LR      (0x00100000 | MODE_3D_ENABLE)
#define MODE_FORCE_3D_TO_2D_TB      (0x00200000 | MODE_3D_ENABLE)

#define VPP_3D_MODE_NULL 0x0
#define VPP_3D_MODE_LR 0x1
#define VPP_3D_MODE_TB 0x2
#define VPP_3D_MODE_LA	 0x3
#define VPP_3D_MODE_FA 0x4

#define RETRY_MAX 5

enum {
    FORMAT_3D_OFF                           = 0,
    FORMAT_3D_AUTO                          = 1,
    FORMAT_3D_SIDE_BY_SIDE                  = 2,
    FORMAT_3D_TOP_AND_BOTTOM                = 3,
    FORMAT_3D_LINE_ALTERNATIVE              = 4,
    FORMAT_3D_FRAME_ALTERNATIVE             = 5,
    FORMAT_3D_TO_2D_LEFT_EYE                = 6,
    FORMAT_3D_TO_2D_RIGHT_EYE               = 7,
    FORMAT_3D_SIDE_BY_SIDE_FORCE            = 8,
    FORMAT_3D_TOP_AND_BOTTOM_FORCE          = 9
};

extern "C" int vdc_loop(int argc, char **argv);

namespace android {
// ----------------------------------------------------------------------------

class SystemControl : public BnISystemControlService
{
public:
    SystemControl(const char *path);
    virtual ~SystemControl();

    virtual bool getProperty(const String16& key, String16& value);
    virtual bool getPropertyString(const String16& key, String16& value, String16& def);
    virtual int32_t getPropertyInt(const String16& key, int32_t def);
    virtual int64_t getPropertyLong(const String16& key, int64_t def);

    virtual bool getPropertyBoolean(const String16& key, bool def);
    virtual void setProperty(const String16& key, const String16& value);

    virtual bool readSysfs(const String16& path, String16& value);
    virtual bool writeSysfs(const String16& path, const String16& value);

    virtual void setBootEnv(const String16& key, const String16& value);
    virtual bool getBootEnv(const String16& key, String16& value);

    virtual void getDroidDisplayInfo(int &type, String16& socType, String16& defaultUI,
        int &fb0w, int &fb0h, int &fb0bits, int &fb0trip,
        int &fb1w, int &fb1h, int &fb1bits, int &fb1trip);

    virtual void loopMountUnmount(int &isMount, String16& path);

    virtual void setMboxOutputMode(const String16& mode);
    virtual int32_t set3DMode(const String16& mode3d);
    virtual void setDigitalMode(const String16& mode);
    virtual void setListener(const sp<ISystemControlNotify>& listener);
    virtual void setOsdMouseMode(const String16& mode);
    virtual void setOsdMousePara(int x, int y, int w, int h);
    virtual void setPosition(int left, int top, int width, int height);
    virtual void getPosition(const String16& mode, int &x, int &y, int &w, int &h);
    virtual void reInit();
    virtual void instabootResetDisplay(void);
    virtual void setNativeWindowRect(int x, int y, int w, int h);
    virtual void setVideoPlayingAxis();
    virtual void init3DSetting(void);
    virtual int32_t getVideo3DFormat(void);
    virtual int32_t getDisplay3DTo2DFormat(void);
    virtual bool setDisplay3DTo2DFormat(int format);
    virtual bool setDisplay3DFormat(int format);
    virtual int32_t getDisplay3DFormat(void);
    virtual bool setOsd3DFormat(int format);
    virtual bool switch3DTo2D(int format);
    virtual bool switch2DTo3D(int format);
    virtual void autoDetect3DForMbox();
    static void* detect3DThread(void* data);

    static void instantiate(const char *cfgpath);

    virtual status_t dump(int fd, const Vector<String16>& args);

    int getLogLevel();

private:
    int permissionCheck();
    void setLogLevel(int level);
    void traceValue(const String16& type, const String16& key, const String16& value);
    int getProcName(pid_t pid, String16& procName);
    void get3DFormatStr(int format, char *str);
    unsigned int get3DOperationByFormat(int format);
    int get3DFormatByOperation(unsigned int operation);
    int get3DFormatByVpp(int vpp3Dformat);
    void setDiBypassAll(int format);

    mutable Mutex mLock;

    int mLogLevel;

    SysWrite *pSysWrite;
    DisplayMode *pDisplayMode;

    int mDisplay3DFormat;
};

// ----------------------------------------------------------------------------

} // namespace android
#endif // ANDROID_SYSTEM_CONTROL_H

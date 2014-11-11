
#include <jni.h>
#include <JNIHelp.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <syslog.h>
#include <string.h>

#include <dirent.h>
#include <android/log.h>
#include "HDMIIN/audio_global_cfg.h"
#include "HDMIIN/audiodsp_control.h"
#include "HDMIIN/audio_utils_ctl.h"
#include "HDMIIN/mAlsa.h"
#include "cutils/properties.h"

#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
//#include <android_runtime/android_view_Surface.h>
#include <linux/videodev2.h>
#include <hardware/hardware.h>
#include <hardware/aml_screen.h>

//#define LOG  printf
#define DISABLE_VIDEO_PATH          "/sys/class/video/disable_video"
#define VFM_MAP_PATH                "/sys/class/vfm/map"
#define FB0_FREE_SCALE_PATH         "/sys/class/graphics/fb0/free_scale"
#define FB0_FREE_SCALE_AXIS_PATH    "/sys/class/graphics/fb0/free_scale_axis"
#define FB1_FREE_SCALE_PATH         "/sys/class/graphics/fb1/free_scale"
#define FB1_FREE_SCALE_AXIS_PATH    "/sys/class/graphics/fb1/free_scale_axis"
#define DI_BYPASS_PROG_PATH         "/sys/module/di/parameters/bypass_prog"

namespace android
{

static unsigned char audio_state = 0;
static unsigned char audio_enable = 1;
static unsigned char audio_ready = 0;
static unsigned char video_enable = 0;
static char audio_rate[32];
static int rate = 0;

static char class_path[PATH_MAX] = {0,};
static char param_path[PATH_MAX] = {0,};
static bool sysfsChecked = false;
static bool useSii9293 = false;
static bool useSii9233a = false;

static char vfm_tvpath[PATH_MAX] = {0,};
static char vfm_default_ext[PATH_MAX] = {0,};
static char vfm_default_amlvideo2[PATH_MAX] = {0,};
static char vfm_hdmiin[PATH_MAX] = {0,};
static bool rmPathFlag = false;
static int mInputSource = 0;
static sp<ANativeWindow> window = NULL;

//check use ppmgr
#define PROP_HDMIIN_PPMGR ("sys.hdmiin.ppmgr")
static bool usePpmgr = false;
#define PROP_HDMIIN_VIDEOLAYER ("mbx.hdmiin.videolayer")
static bool mUseVideoLayer = true;
static bool mIsFullscreen = false;

enum State{
    START,
    PAUSE,
    STOPING,
    STOP,
};
aml_screen_module_t* mScreenModule;
aml_screen_device_t* mScreenDev;
int mWidth = 0;
int mHeight = 0;
int mState = STOP;

typedef struct output_mode_info_
{
    char name[16];
    int width;
    int height;
    bool iMode;
}
output_mode_info_t;

output_mode_info_t output_mode_info[] =
{
    {"1080p", 1920, 1080, false},
    {"1080p24hz", 1920, 1080, false},
    {"1080p50hz", 1920, 1080, false},
    {"1080i", 1920, 1080, true},
    {"1080i50hz", 1920, 1080, true},
    {"720p", 1280, 720, false},
    {"720p50hz", 1280, 720, false},
    {"480p", 720, 480, false},
    {"480i", 720, 480, true},
    {"576p", 720, 576, false},
    {"576i", 720, 576, true},
};

#define OUTPUT_MODE_NUM (sizeof(output_mode_info) / sizeof(output_mode_info[0]))

#define DEFAULT_HACTIVE (1920)
#define DEFAULT_VACTIVE (1080)

static int send_command(const char* cmd_file, const char* cmd_string)
{
    int fd = open(cmd_file, O_RDWR);
    int len = strlen(cmd_string);
    int ret = -1;

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s, cmd: %s\n", cmd_file, cmd_string);
	  if(fd >= 0){
        write(fd, cmd_string, len);
        close(fd);

         ret = 0;
	  }
	  if(ret<0){
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","fail to %s file %s\n", ret==-1?"open":"write", cmd_file);
	  }
    return ret;
}

static int read_value(const char* prop_file)
{
    int fd = open(prop_file, O_RDONLY);
    int len = 0;
    char tmp_buf[32];
    int ret = 0;
    // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s\n", prop_file);
	  if(fd >= 0){
        len = read(fd, tmp_buf, 32 );
        close(fd);
        if(len > 0){
            // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s, %s\n", prop_file, tmp_buf);
            ret = atoi(tmp_buf); 
           // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","read %s => %d\n", prop_file, ret);
        }
	  }
	  else{
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","fail to open file %s\n", prop_file);
	  }
    return ret;

}

static int read_value(const char* prop_file, const int pos)
{
    int fd = open(prop_file, O_RDONLY);
    int len = 0;
    char tmp_buf[128];
    int ret = 0;

    memset(tmp_buf, 0, sizeof(tmp_buf));
    // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s\n", prop_file);
	  if(fd >= 0){
        len = read(fd, tmp_buf, 128);
        close(fd);
        if(len > 0){
            // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s, %s\n", prop_file, tmp_buf);
            ret = (tmp_buf[pos] == '0') ? 0 : 1;
           // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","read %s => %d\n", prop_file, ret);
        }
	  }
	  else{
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","fail to open file %s\n", prop_file);
	  }
    return ret;

}

static int read_value(const char* prop_file, char* buf)
{
    int fd = open(prop_file, O_RDONLY);
    int len = 0;
    char tmp_buf[128];
    int ret = -1;

    memset(tmp_buf, 0, sizeof(tmp_buf));
    // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s\n", prop_file);
    if (fd >= 0) {
        len = read(fd, tmp_buf, 128);
        close(fd);
        if (len > 0) {
            // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","file: %s, %s, len: %d\n", prop_file, tmp_buf, strlen(tmp_buf));
            // if (!strcmp(tmp_buf, "invalid\n"))
                // return -1;

            strncpy(buf, tmp_buf, strlen(tmp_buf) - 1);
            buf[strlen(tmp_buf)] = '\0';
            ret = 0;
            // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","buf: %s, len: %d\n", buf, strlen(buf));
        }
    }

    return ret;
}

enum {
    HDMI_TYPE,
    DISPLAY_TYPE
};

static int getInputInfo(char* mode, const int type, char* buf)
{
    int ret = -1;
    char* context = NULL;
    char* ptr = NULL;

    // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","getInputInfo: %s, len: %d, type: %d\n", mode, strlen(mode), type);
    ptr = strtok_r(mode, ":", &context);
    if (ptr != NULL) {
        if (type == HDMI_TYPE) {
            strcpy(buf, ptr);
            // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","buf: %s\n", buf);
            ret = 0;
        } else if (type == DISPLAY_TYPE) {
            ptr = strtok_r(NULL, ":", &context);
            if (ptr != NULL) {
                strcpy(buf, ptr);
                // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","buf: %s\n", buf);
                ret = 0;
            }
        }
    }

    return ret;
}

static int read_output_display_mode(void)
{
    int fd = open("/sys/class/display/mode", O_RDONLY);
    int len = 0;
    unsigned int i;
    char tmp_buf[32];
    int ret = -1;
	  if(fd >= 0){
        len = read(fd, tmp_buf, 32 );
        close(fd);
        if(len > 0){
            for(i=0; i<OUTPUT_MODE_NUM; i++){
                if(strncmp(tmp_buf, output_mode_info[i].name, strlen(output_mode_info[i].name))==0){
                    ret = i;
                    break;
                }
            }
            if(i==OUTPUT_MODE_NUM){
                __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","output mode not supported: %s\n", tmp_buf);
            }
        }
	  }
	  else{
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","fail to open file %s\n", "/sys/class/display/mode");
	  }
    return ret;

}

static char* getFs(const char* path, const char* key)
{
    char fsPath[PATH_MAX] = {0,};
    strncpy(fsPath, path, strlen(path));
    strcat(fsPath, key);
    return fsPath;
}

static void disp_android(void)
{
    send_command(getFs(class_path, "enable") , "0"); // disable "hdmi in"
    //send_command("/sys/class/ppmgr/ppscaler" , "0"); // disable pscaler  for "hdmi in"

    send_command("/sys/class/vfm/map", "rm default_ext" );
    send_command("/sys/class/vfm/map", "add default_ext vdin0 vm amvideo" );
    send_command("/sys/module/di/parameters/bypass_prog", "0" );

     /* set and enable freescale */
    send_command("/sys/class/graphics/fb0/free_scale", "0");
    send_command("/sys/class/graphics/fb0/free_scale_axis", "0 0 1279 719 ");
    send_command("/sys/class/graphics/fb0/free_scale", "1");

    send_command("/sys/class/graphics/fb1/free_scale", "0");
    send_command("/sys/class/graphics/fb1/free_scale_axis", "0 0 1279 719 ");
    send_command("/sys/class/graphics/fb1/free_scale", "1");
    /**/

     /* turn on OSD layer */
    send_command("/sys/class/graphics/fb0/blank", "0");
    //send_command("/sys/class/graphics/fb1/blank", "0");
     /**/

}

static void disp_hdmi(void)
{
    send_command(getFs(class_path, "enable") , "0"); // disable "hdmi in"
    send_command("/sys/class/video/disable_video"           , "2"); // disable video layer, video layer will be enabled after "hdmi in" is enabled

    send_command("/sys/class/vfm/map", "rm default_ext" );
    send_command("/sys/class/vfm/map", "add default_ext vdin0 deinterlace amvideo" );
    send_command("/sys/module/di/parameters/bypass_prog", "1" );

     /* disable OSD layer */
    send_command("/sys/class/graphics/fb0/blank"            , "1");
    //send_command("/sys/class/graphics/fb1/blank"            , "1");
     /**/

    //send_command("/sys/class/ppmgr/ppscaler"                , "0"); // disable pscaler  for "hdmi in"

     /* disable freescale */
    send_command("/sys/class/graphics/fb0/free_scale"       , "0");
    send_command("/sys/class/graphics/fb1/free_scale"       , "0");
     /**/

    send_command(getFs(class_path, "enable") , "1"); // enable "hdmi in"

//    system("stop media");
//    system("alsa_aplay -C -Dhw:0,0 -r 48000 -f S16_LE -t raw -c 2 | alsa_aplay -Dhw:0,0 -r 48000 -f S16_LE -t raw -c 2");

}


static void disp_pip(int x, int y, int width, int height)
{
    char buf[32];
    int idx = 0;
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "disp_pip(), x: %d, w: %d, w: %d, h: %d\n", x, y, width, height);
    // mWidth = width - x;
    // mHeight = height - y;
    /* if((mWidth < 0) || (mHeight < 0))
    {
        mWidth = x - width;
        mHeight = y - height;
    } */
    mWidth = width;
    mHeight = height;

    if(usePpmgr)
    {
		__android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "disp_pip(), usePpmgr");
        /* set and enable pscaler */
        idx = read_output_display_mode();
        if(idx < 0){
            idx = 0;
        }
        snprintf(buf, 31, "%d %d %d %d", 0, 0, output_mode_info[idx].width-1, output_mode_info[idx].height-1);
        send_command("/sys/class/video/axis", buf);
        send_command("/sys/class/ppmgr/ppscaler", "1");
        snprintf(buf, 31, "%d %d %d %d 1", x, y, x+width-1, y+height-1);
        send_command("/sys/class/ppmgr/ppscaler_rect", buf);
    }
    else
    {
        /* send_command("/sys/class/ppmgr/ppscaler", "0");
        send_command("/sys/class/graphics/fb0/free_scale", "0");
        send_command("/sys/class/graphics/fb1/free_scale", "0");
        send_command("/sys/class/video/screen_mode", "1"); // 1:full stretch
        snprintf(buf, 31, "%d %d %d %d 1", x, y, x+width-1, y+height-1);
        send_command("/sys/class/video/axis", buf); */
    }
}

/* static int checkExist(char* path)
{
    int ret = -1;
    struct stat buf;
    char sysfs_path[PATH_MAX] = {0,};
    strncpy(sysfs_path, path, strlen(path));
    if (stat(strcat(sysfs_path, "poweron"), &buf) == 0) {
        ret = 0;
    }
    else
    {
        ret = -1;
    }
    return ret;
} */

static int checkExistFile(const char* path, const char* name)
{
    struct stat buf;
    char sysfs_path[PATH_MAX] = {0, };

    strncpy(sysfs_path, path, strlen(path));
    if (stat(strcat(sysfs_path, name), &buf) == 0)
        return 0;

    return -1;
}

static int checkExist(const char* path)
{
    return checkExistFile(path, "poweron");
}

static void checkSysfs()
{
    //for it660x
    const char* it660x_class_path = "/sys/class/it660x/it660x_hdmirx0/";
    const char* it660x_param_path = "/sys/module/tvin_it660x/parameters/";

    //for mt box
    const char* mt_class_path = "/sys/class/vdin_ctrl/vdin_ctrl0/";
    const char* mt_param_path = "/sys/module/tvin_it660x/parameters/";

    // for sii9233a
    const char* sii9233a_class_path = "/sys/class/sii9233a/";
    const char* sii9233a_param_path = "/sys/class/sii9233a/";

    // for sii9293
    const char* sii9293_class_path = "/sys/class/sii9293/";
    const char* sii9293_param_path = "/sys/class/sii9293/";

    if(sysfsChecked)
    {
        return;
    }
    else
    {
        sysfsChecked = true;
    }

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","checkSysfs\n");
    if (checkExist(it660x_class_path) == 0)
    {
        strncpy(class_path, it660x_class_path, strlen(it660x_class_path));
        strncpy(param_path, it660x_param_path, strlen(it660x_param_path));
    }
    else if (checkExist(mt_class_path) == 0)
    {
        strncpy(class_path, mt_class_path, strlen(mt_class_path));
        strncpy(param_path, mt_param_path, strlen(mt_class_path));
    }
    else if (checkExistFile(sii9233a_class_path, "port") == 0)
    {
        useSii9233a = true;
        strncpy(class_path, sii9233a_class_path, strlen(sii9233a_class_path));
        strncpy(param_path, sii9233a_param_path, strlen(sii9233a_param_path));
    }
    else if (checkExistFile(sii9293_class_path, "enable") == 0)
    {
        useSii9293 = true;
        strncpy(class_path, sii9293_class_path, strlen(sii9293_class_path));
        strncpy(param_path, sii9293_param_path, strlen(sii9293_param_path));
    }
    else
    {
        // TODO: add more
    }

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","class_path %s \n", class_path);
    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","param_path %s \n", param_path);
}

static void getVfmPath()
{
    int len = 0;
    int offset = 0;
    char* ptr;
    char* starptr;
    //int size=1024*4;
    char buf[1024*4]={0,};
    int fd = open("/sys/class/vfm/map", O_RDWR);
    if(fd >= 0)
    {
        len = read(fd, buf, 1024*4);
        close(fd);
        if(len > 0)
        {
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","[getVfmPath] :%s.\n", buf);
        }
    }
}


static void getPath(const char *name, char *vfm_path)
{
    int len = 0;
    int offset = 0;
    char* ptr;
    char* starptr;
    //int size=1024*4;
    char buf[1024*4]={0,};
    int fd = open("/sys/class/vfm/map", O_RDWR);
    if(fd >= 0)
    {
        len = read(fd, buf, 1024*4);
        close(fd);
        if(len > 0)
        {
            ptr = strstr(buf, name);
            if(ptr != NULL)
            {
                starptr = ptr;
                ptr = strchr(starptr, '}');
                if(ptr != NULL)
                {
                    offset = ptr - starptr + 1;
                    strncpy(vfm_path, starptr, offset);
                    // __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","[getPath] name:%s, vfm_path:%s.\n", name, vfm_path);
                }
            }
        }
    }
}

static void resumePath(const char *name, char *vfm_path)
{
    char* ptr;
    char* endptr;
    int offset = 0;
    char path[512]={0,};
    char buf[512]={0,};
    char *bufptr = NULL;
    char *context = NULL;
    char cmd[32];

    if(strlen(vfm_path) > 0)
    {
        memset(cmd, 0, sizeof(cmd));
        sprintf(cmd, "rm %s\n", name);
        send_command("/sys/class/vfm/map", cmd);


        endptr = strchr(vfm_path, '}');
        ptr = strchr(vfm_path, '{');
        strncpy(buf, ptr + 1, endptr - ptr - 1);
        bufptr = buf;
        snprintf(path, sizeof(path), "add %s ", name);
        while ((ptr = strtok_r(bufptr, " ", &context)) != NULL) {
            if (strlen(ptr) > 0) {
                endptr = strchr(ptr, '(');
                if (endptr != NULL) {
                    offset = endptr - ptr;
                    strncat(path, ptr, offset);
                    strcat(path, " ");
                } else
                    strcat(path, ptr);
            }
            bufptr = NULL;
        }

        send_command("/sys/class/vfm/map", path);
    }
}

static bool searchPath(const char *vfm_path, const char *module) {
    char *ptr;
    char *endptr;
    char buf[512] = {0, };
    char *bufptr = NULL;
    char *context = NULL;
    char node[128] = {0, };

    if (strlen(vfm_path) > 0) {
        endptr = strchr(vfm_path, '}');
        ptr = strchr(vfm_path, '{');
        strncpy(buf, ptr + 1, endptr - ptr - 1);
        bufptr = buf;
        while ((ptr = strtok_r(bufptr, " ", &context)) != NULL) {
            if (strlen(ptr) > 0) {
                endptr = strchr(ptr, '(');
                memset(node, 0, sizeof(node));
                if (endptr != NULL) {
                    strncpy(node, ptr, endptr - ptr);
                } else
                    strcpy(node, ptr);
                if (!strcmp(node, module))
                    return true;
            }
            bufptr = NULL;
        }
    }

    return false;
}

static void safeRmPath(const char* path) {
    const char* checkNode = "/sys/module/amvideo/parameters/new_frame_count";
    int retry = 20; //retry 20 times
    int len = 0;
    int size = 512;
    int readRlt = 0;
    char buf[512]={0,};
    char cmd[512]={0,};

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","[safeRmPath] \n");

    if (!strcmp(path, "hdmiin") && strlen(vfm_hdmiin) > 0) {
        if (!searchPath(vfm_hdmiin, "amvideo")) {
            send_command(VFM_MAP_PATH, "rm hdmiin");
            return;
        }
    }

    strcpy(cmd, "rm ");
    while(retry > 0) {
        int fd = open(checkNode, O_RDONLY);
        if(fd >= 0)
        {
            memset(buf, 0, size);
            len = read(fd, buf, size);
            close(fd);
            if(len > 0)
            {
                readRlt = atoi(buf);
                __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","[safeRmPath] readRlt:%d.\n", readRlt);
                if(readRlt > 0)
                {
                    sleep(1);  // sleep 10 ms
                }
                else if(readRlt == 0)
                {
                    strcat(cmd, path);
                    send_command("/sys/class/vfm/map", cmd);
                    break;
                }
                else
                {
                    // readRlt < 0
                    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","[safeRmPath] readRlt < 0.\n");
                    break;
                }
            }
        }
        retry--;

        if(retry == 0) {
            strcat(cmd, path);
            send_command("/sys/class/vfm/map", cmd);
        }
    }
}

static bool checkBoolProp(char *name, char *def) {
    char prop[PROPERTY_VALUE_MAX] = {0,};

    property_get(name, prop, def);
    if (!strcmp(prop, "true"))
        return true;

    return false;
}

static void getScreenDev() {
    if (mScreenDev)
        return;

    if (!mScreenModule)
        hw_get_module(AML_SCREEN_HARDWARE_MODULE_ID, (const hw_module_t **)&mScreenModule);

    if (mScreenModule)
        mScreenModule->common.methods->open((const hw_module_t *)mScreenModule, AML_SCREEN_SOURCE,
                (struct hw_device_t **)&mScreenDev);
}

static void init(int source, bool isFullscreen)
{
    usePpmgr = checkBoolProp(PROP_HDMIIN_PPMGR, "false");
    mUseVideoLayer = checkBoolProp(PROP_HDMIIN_VIDEOLAYER, "true");
    mIsFullscreen = isFullscreen;
    mInputSource = source;
    int retry = 5;
    if (useSii9293) {
        while (retry > 0) {
            if (read_value(getFs(class_path, "drv_init_flag"), 16) == 1)
                break;
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","retry: %d, sleep 100ms\n", retry);
            retry--;
            usleep(100000);
        }
    }

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","Version 0.1\n");
    // send_command("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "performance");
    if (useSii9233a || useSii9293) {
        send_command(getFs(class_path, "enable"), "0\n");
    } else {
        send_command(getFs(class_path, "enable")  , "0"); // disable "hdmi in"
    }

    if (mIsFullscreen && mUseVideoLayer)
        send_command("/sys/class/video/disable_video", "1"); // disable video layer

    if (!mIsFullscreen || !mUseVideoLayer) {
        getScreenDev();
        if (mScreenDev != NULL) {
            mScreenDev->ops.set_source_type(mScreenDev, HDMI_IN);
        } else
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","mScreenDev == NULL\n");
    }

    //rm tvpath for conflict
    memset(vfm_tvpath, 0, sizeof(vfm_tvpath));
    memset(vfm_default_ext, 0, sizeof(vfm_default_ext));
    memset(vfm_default_amlvideo2, 0, sizeof(vfm_default_amlvideo2));
    memset(vfm_hdmiin, 0, sizeof(vfm_hdmiin));
    getPath("tvpath ", vfm_tvpath);
    send_command("/sys/class/vfm/map", "rm tvpath");
    getPath("default_ext ", vfm_default_ext);
    send_command("/sys/class/vfm/map", "rm default_ext");
    if (!mIsFullscreen || !mUseVideoLayer) {
        getPath("default_amlvideo2 ", vfm_default_amlvideo2);
        send_command("/sys/class/vfm/map", "rm default_amlvideo2");
    }

    safeRmPath("hdmiin");
    if(usePpmgr)
    {
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","usePpmgr\n");
        // send_command("/sys/class/vfm/map", "rm default_amlvideo2" );
        send_command("/sys/class/vfm/map", "add hdmiin vdin0 amlvideo2 ppmgr amvideo" );

        /* set and enable freescale */
        send_command("/sys/class/graphics/fb0/free_scale", "0");
        send_command("/sys/class/graphics/fb0/free_scale_axis", "0 0 1279 719");
        send_command("/sys/class/graphics/fb0/free_scale", "1");
        send_command("/sys/class/graphics/fb1/free_scale", "0");
        send_command("/sys/class/graphics/fb1/free_scale_axis", "0 0 1279 719 ");
        send_command("/sys/class/graphics/fb1/free_scale", "1");
    }
    else
    {
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","!usePpmgr\n");

        if (mIsFullscreen && mUseVideoLayer)
            send_command("/sys/class/vfm/map", "add hdmiin vdin0 amvideo" );
        else
            send_command("/sys/class/vfm/map", "add hdmiin vdin0 amlvideo2" );
    }

    if (useSii9233a) {
        char port[8];
        memset(port, 0, sizeof(port));
        sprintf(port, "%d\n", source);
        send_command(getFs(class_path, "port"), port);
        send_command(getFs(class_path, "enable"), "1\n");
    } else if (useSii9293)
        send_command(getFs(class_path, "enable") , "1\n");
    else {
        send_command(getFs(class_path, "poweron") , "1");
        send_command(getFs(class_path, "enable")  , "1");
    }
}

static void uninit(void)
{
    if (useSii9233a || useSii9293) {
        send_command(getFs(class_path, "enable")  , "0\n"); // disable "hdmi in"
    } else {
        send_command(getFs(class_path, "enable")  , "0"); // disable "hdmi in"
        send_command(getFs(class_path, "poweron"), "0"); //power off "hdmi in"
    }

    if (mIsFullscreen && mUseVideoLayer)
        send_command("/sys/class/video/disable_video", "2");

    memset(vfm_hdmiin, 0, sizeof(vfm_hdmiin));
    getPath("hdmiin ", vfm_hdmiin);
    safeRmPath("hdmiin");
    resumePath("tvpath", vfm_tvpath);
    resumePath("default_ext", vfm_default_ext);
    if (!mIsFullscreen || !mUseVideoLayer)
        resumePath("default_amlvideo2", vfm_default_amlvideo2);
    sysfsChecked = false;
}

static void deinit(void)
{
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN>"," deinit Version 0.1\n");
	uninit();
}

static void audio_start(int track_rate)
{
	//__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","audio passthrough test code start\n");
	send_command("/sys/class/amaudio/record_type", "2");
	AudioUtilsInit();
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","init audioutil finished, track_rate: %d\n", track_rate);
	mAlsaInit(0, CC_FLAG_CREATE_TRACK | CC_FLAG_START_TRACK|CC_FLAG_CREATE_RECORD|CC_FLAG_START_RECORD, track_rate);
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","start audio track finished\n");
	// audiodsp_start();
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","audiodsp start finished\n");
	//AudioUtilsStartLineIn();
	//__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","start line in finished\n");

}

static void audio_stop(void)
{
	AudioUtilsStopLineIn();
	send_command("/sys/class/amaudio/record_type", "0");
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","stop line in finished\n");
	AudioUtilsUninit();
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","uninit audioutil finished\n");
	mAlsaUninit(0);
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","stop audio track finished\n");
	// audiodsp_stop();
	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","audiodsp stop finished, sys.hdmiin.mute false\n");
	property_set("sys.hdmiin.mute", "false");

}

static void setStateCB(int state)
{
    mState = state;
}

static int getState()
{
    return mState;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _init
 * Signature: ()V
 */
static void Java_com_android_server_OverlayViewService__1init
  (JNIEnv *env, jobject obj, jint source, jboolean isFullscreen)
{
    checkSysfs();
    init(source, isFullscreen);
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _deinit
 * Signature: ()V
 */
static void Java_com_android_server_OverlayViewService__1deinit
  (JNIEnv *env, jobject obj)
{
	video_enable = 0;

	deinit();
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _displayHdmi
 * Signature: ()I
 */
static jint Java_com_android_server_OverlayViewService__1displayHdmi
  (JNIEnv *env, jobject obj)
{
    disp_hdmi();
    video_enable = 1;
    return 0;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _displayAndroid
 * Signature: ()I
 */
static jint Java_com_android_server_OverlayViewService__1displayAndroid
  (JNIEnv *env, jobject obj)
{
    video_enable = 0;
    disp_android();
    return 0;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _displayPip
 * Signature: (IIII)I
 */
static jint Java_com_android_server_OverlayViewService__1displayPip
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
    char mode[20];

    disp_pip( x, y, width, height);

    memset((void*)mode, 0, 20);
    sprintf(mode, "%d %d %d %d", x, y, width, height);

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","mode is: %s\n", mode);

    // send_command("/sys/class/graphics/fb0/video_hole", mode);

    video_enable = 1;

    return 0;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _getHActive
 * Signature: ()I
 */
static jint Java_com_android_server_OverlayViewService__1getHActive
  (JNIEnv *env, jobject obj)
{
    char value[128];
    char buf[16];
    int ret = 0;
    unsigned int i = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return ret;

        if (!strcmp(value, "invalid\n"))
            return -1;

        memset(buf, 0, sizeof(buf));
        ret = getInputInfo(value, DISPLAY_TYPE, buf);
        if (ret == -1)
            return ret;

        for (i = 0; i < OUTPUT_MODE_NUM; i++) {
            if (!strcmp(buf, output_mode_info[i].name)) {
                return output_mode_info[i].width;
            }
        }
    }

	return read_value(getFs(param_path, "horz_active"));
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _getHdmiInSize
 * Signature: ()Ljava/lang/String;
 */

static jstring Java_com_android_server_OverlayViewService__1getHdmiInSize
  (JNIEnv *env, jobject obj)
{
    char value[128];
    int ret = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return NULL;

        if (!strcmp(value, "invalid\n"))
            return NULL;

        return env->NewStringUTF(value);
    }

    return NULL;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _getVActive
 * Signature: ()I
 */
static jint Java_com_android_server_OverlayViewService__1getVActive
  (JNIEnv *env, jobject obj)
{
    char value[128];
    char buf[16];
    int ret = 0;
    unsigned int i = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return ret;

        if (!strcmp(value, "invalid\n"))
            return -1;

        memset(buf, 0, sizeof(buf));
        ret = getInputInfo(value, DISPLAY_TYPE, buf);
        if (ret == -1)
            return ret;

        for (i = 0; i < OUTPUT_MODE_NUM; i++) {
            if (!strcmp(buf, output_mode_info[i].name)) {
                return output_mode_info[i].height;
            }
        }
    }

	return read_value(getFs(param_path, "vert_active"));
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _isDvi
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1isDvi
  (JNIEnv *env, jobject obj)
{
    char value[128];
    char buf[16];
    int ret = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return JNI_FALSE;

        if (!strcmp(value, "invalid\n"))
            return JNI_FALSE;

        memset(buf, 0, sizeof(buf));
        ret = getInputInfo(value, HDMI_TYPE, buf);
        if (ret == -1)
            return JNI_FALSE;

        return (strcmp(buf, "DVI") == 0);
    }

	return (read_value(getFs(param_path, "is_hdmi_mode"))==0);
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _isPowerOn
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1isPowerOn
  (JNIEnv *env, jobject obj)
{
    char value[128];
    int ret = 0;

	checkSysfs();
    if (useSii9293) {
        if (read_value(getFs(class_path, "enable"), 22) == 1)
            return JNI_TRUE;

        return JNI_FALSE;
    } else if (useSii9233a)
        return (read_value(getFs(class_path, "port"), 30) == 1);

	return (read_value(getFs(class_path, "poweron"))==1);
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _isEnable
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1isEnable
  (JNIEnv *env, jobject obj)
{
    char value[128];
    int ret = 0;

	checkSysfs();
    if (useSii9233a || useSii9293) {
        if (useSii9293) {
            if (read_value(getFs(class_path, "enable"), 22) == 1)
                return JNI_TRUE;
        } else if (useSii9233a) {
            if (read_value(getFs(class_path, "enable"), 23) == 1)
                return JNI_TRUE;
        }

        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return JNI_FALSE;

        return JNI_TRUE;
    }

	return (read_value(getFs(class_path, "enable"))==1);
}


/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _isInterlace
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1isInterlace
  (JNIEnv *env, jobject obj)
{
    char value[128];
    char buf[16];
    int ret = 0;
    unsigned int i = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "input_mode"), value);
        if (ret == -1)
            return JNI_FALSE;

        if (!strcmp(value, "invalid\n"))
            return JNI_FALSE;

        memset(buf, 0, sizeof(buf));
        ret = getInputInfo(value, DISPLAY_TYPE, buf);
        if (ret == -1)
            return JNI_FALSE;

        for (i = 0; i < OUTPUT_MODE_NUM; i++) {
            if (!strcmp(buf, output_mode_info[i].name)) {
                return output_mode_info[i].iMode;
            }
        }
    }

	return (read_value(getFs(param_path, "is_interlace"))==1);
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _hdmiPlugged
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1hdmiPlugged
  (JNIEnv *env, jobject obj)
{
    char value[128];
    int ret = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "cable_status"), value);
        if (ret == -1)
            return JNI_FALSE;

        if ('1' == value[0])
            return JNI_TRUE;

        return JNI_FALSE;
    }

	return JNI_FALSE;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _hdmiSignal
 * Signature: ()Z
 */
static jboolean Java_com_android_server_OverlayViewService__1hdmiSignal
  (JNIEnv *env, jobject obj)
{
    char value[128];
    int ret = 0;

    if (useSii9233a || useSii9293) {
        memset(value, 0, sizeof(value));
        ret = read_value(getFs(param_path, "signal_status"), value);
        if (ret == -1)
            return JNI_FALSE;

        if ('1' == value[0])
            return JNI_TRUE;

        return JNI_FALSE;
    }

	return JNI_FALSE;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _enableAudio
 * Signature: (I)I
 */
static jint Java_com_android_server_OverlayViewService__1enableAudio
  (JNIEnv *env, jobject obj, jint flag)
{
		__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","%s, flag=%d\n", __func__,flag);
		#if 1
    memset(audio_rate, 0, sizeof(audio_rate));
    if(flag == 1){
        audio_enable = 1;
    }
    else{
        audio_enable = 0;
        // audio_ready = 0;
    }
    #endif
    return 0;
}

/*
 * Class:     com_android_server_OverlayViewService
 * Method:    _handleAudio
 * Signature: ()I
 */
static jint Java_com_android_server_OverlayViewService__1handleAudio
  (JNIEnv *env, jobject obj)
{
#if 1
// __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","%s: video_enable:%d,audio_enable:%d,audio_state:%d\n", __func__,video_enable,audio_enable,audio_state);
    char value[32];
    int ret = 0;
    bool rate_changed = false;

    audio_ready = 0;
    memset(value, 0, sizeof(value));
	ret = read_value(getFs(param_path, "audio_sample_rate"), value);
    if (ret == -1)
        audio_ready = 0;
    else if (strstr(value, "kHz") != NULL)
        audio_ready = 1;
    if (ret != -1 && strcmp(audio_rate, value)) {
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","audio_sample_rate: %s\n", value);
        rate_changed = true;
        strcpy(audio_rate, value);
        double val = atof(audio_rate);
        val *= 1000;
        rate = (int)val;
        if (rate == 0) {
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","sys.hdmiin.mute true\n");
            property_set("sys.hdmiin.mute", "true");
        } else {
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","sys.hdmiin.mute false\n");
            property_set("sys.hdmiin.mute", "false");
        }
    }

	if(audio_ready != 0 && video_enable&&audio_enable){
        if(audio_state == 0){
            audio_start(rate);
            audio_state = 1;
        } else if (audio_state == 1 && rate_changed) {
            audio_start(rate);
        }

        if(0/*(read_value(getFs(param_path, "vert_active"))==0)
            ||(read_value(getFs(param_path, "is_hdmi_mode"))==0)*/){
            if(audio_state == 2){
                AudioUtilsStopLineIn();
                audio_state = 1;
    	          __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","%s: stop line in finished\n", __func__);
    	      }
        }
        else{
            if(audio_state == 1){
    	        AudioUtilsStartLineIn();
                if (!useSii9233a && !useSii9293)
                  send_command(getFs(class_path, "enable") , "257"); //0x101: bit16, event of "mailbox_send_audiodsp"
    	        audio_state = 2;
    	        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","%s: start line in finished\n", __func__);	
            }
        }
    }
    else if (!audio_enable) {
        if(audio_state!=0){
			__android_log_print(ANDROID_LOG_INFO, "<HDMI IN AUD>","%s, audio_stop\n", __func__);
            audio_stop();
            audio_state = 0;
        }
    }
    #endif
    return audio_ready;
}

/*
* Class:     com_android_server_OverlayViewService
* Method:    _setEnable
* Signature: ()I
*/
static void Java_com_android_server_OverlayViewService__1setEnable
(JNIEnv *env, jobject obj, jboolean enable)
{
    if (enable) {
        if (!mIsFullscreen || !mUseVideoLayer) {
            getScreenDev();
            if (mScreenDev != NULL) {
                mScreenDev->ops.set_source_type(mScreenDev, HDMI_IN);
            } else
                __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>","mScreenDev == NULL\n");
        }

        if (useSii9233a) {
            char port[8];
            memset(port, 0, sizeof(port));
            sprintf(port, "%d\n", mInputSource);
            send_command(getFs(class_path, "port"), port);
            send_command(getFs(class_path, "enable"), "1\n");
        } else if (useSii9293)
            send_command(getFs(class_path, "enable") , "1\n");
        else {
            send_command(getFs(class_path, "poweron") , "1");
            send_command(getFs(class_path, "enable")  , "1"); // disable "hdmi in"
        }
    } else {
        if (useSii9233a || useSii9293) {
            send_command(getFs(class_path, "enable")  , "0\n"); // disable "hdmi in"
        } else {
            send_command(getFs(class_path, "enable")  , "0"); // disable "hdmi in"
            send_command(getFs(class_path, "poweron"), "0"); //power off "hdmi in"
        }
    }
}

/*
* Class:     com_android_server_OverlayViewService
* Method:    _setSourceType
* Signature: ()I
*/
static jint Java_com_android_server_OverlayViewService__1setSourceType
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        return -1;

    if (mScreenDev != NULL)
        mScreenDev->ops.set_source_type(mScreenDev, HDMI_IN);
    return 0;
}

/*
* Class:     com_android_server_OverlayViewService
* Method:    _isSurfaceAvailable
* Signature: (Landroid/view/Surface;)Z
*/
static jboolean Java_com_android_server_OverlayViewService__isSurfaceAvailable
(JNIEnv *env, jobject obj, jobject jsurface)
{
	if (jsurface)
	{
		//sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));
		sp<Surface> surface = NULL;
		if (surface == NULL) {
			__android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "isSurfaceAvailable(), surface == NULL\n");
			return JNI_FALSE;
		}

		return JNI_TRUE;
	}

	__android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "isSurfaceAvailable(), jsurface == NULL\n");
	return JNI_FALSE;
}

/*
* Class:     com_android_server_OverlayViewService
* Method:    _setPreviewWindow
* Signature: (Landroid/view/Surface;)V
*/
static jboolean Java_com_android_server_OverlayViewService__setPreviewWindow
(JNIEnv *env, jobject obj, jobject jsurface)
{
    if (mIsFullscreen && mUseVideoLayer)
        return JNI_FALSE;

    /* if (!mScreenModule)
    {
        hw_get_module(AML_SCREEN_HARDWARE_MODULE_ID, (const hw_module_t **)&mScreenModule);
        mScreenModule->common.methods->open((const hw_module_t *)mScreenModule, AML_SCREEN_SOURCE, 
            (struct hw_device_t**)&mScreenDev);
    } */

    sp<IGraphicBufferProducer> gbp;
    if(jsurface)
    {
        //sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));
        sp<Surface> surface = NULL;
        if (surface == NULL) {
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "setPreviewWindow(), surface == NULL\n");
            return JNI_FALSE;
        }
        gbp = surface->getIGraphicBufferProducer();
        if(gbp != NULL)
        {
            window = new Surface(gbp);
        }
    }

    if(window != NULL)
    {
        if (window != NULL) {
            window ->incStrong((void*)ANativeWindow_acquire);
        }
        __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "setPreviewWindow(), mWidth: %d, mHeight: %d\n", mWidth, mHeight);
        getScreenDev();
        if(mScreenDev != NULL) {
            mScreenDev->ops.setPreviewWindow(mScreenDev, window.get());
            mScreenDev->ops.set_format(mScreenDev, mWidth, mHeight, V4L2_PIX_FMT_NV21);
            mScreenDev->ops.setStateCallBack(mScreenDev, setStateCB);
        }
        else {
            __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "AML_SCREEN_HARDWARE_MODULE is busy!!\n");
            return JNI_FALSE;
        }
    }

    return JNI_TRUE;
}

/*
 * Class:       com_android_server_OverlayViewService
 * Method:      setCrop
 * Signature:   (IIII)I
 */
static jint Java_com_android_server_OverlayViewService__1setCrop
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
    if (mIsFullscreen && mUseVideoLayer)
        return -1;

    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "setCrop(), x: %d, y: %d, width: %d, height: %d\n", x, y, width, height);
    if (mScreenDev != NULL)
        mScreenDev->ops.set_crop(mScreenDev, x, y, width, height);
    return 0;
}

/*
* Class:     com_android_server_startMov
* Method:    _startMov
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__startMov
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        return;

    int state = getState();
    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "startMov() state:%d\n",state);
    if(mScreenDev != NULL)
    {
        mScreenDev->ops.start(mScreenDev);
    }
}

/*
* Class:     com_android_server_stopMov
* Method:    _stopMov
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__stopMov
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        return;

    int state = getState();
    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "stopMov() state:%d\n",state);
    if(mScreenDev != NULL && state != STOP)
    {
        mScreenDev->ops.stop(mScreenDev);
        mScreenDev->common.close((struct hw_device_t *)mScreenDev);
        mScreenDev = NULL;
        mScreenModule = NULL;

        if (window != NULL) {
            window ->decStrong((void*)ANativeWindow_acquire);
        }
    }
}

/*
* Class:     com_android_server_pauseMov
* Method:    _pauseMov
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__pauseMov
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        return;

    int state = getState();
    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "pauseMov() state:%d\n",state);
    if(mScreenDev != NULL)
    {
        //mScreenDev->ops.pause(mScreenDev);
    }
}

/*
* Class:     com_android_server_resumeMov
* Method:    _resumeMov
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__resumeMov
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        return;

    int state = getState();
    __android_log_print(ANDROID_LOG_INFO, "<HDMI IN>", "resumeMov() state:%d\n",state);
    if(mScreenDev != NULL)
    {
        //mScreenDev->ops.resume(mScreenDev);
    }
}

/*
* Class:     com_android_server_startVideo
* Method:    _startVideo
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__startVideo
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer) {
        int ret = read_value("/sys/class/video/disable_video");
        if (ret != 2 && ret != 0)
            send_command("/sys/class/video/disable_video", "2");
    }
}

/*
* Class:     com_android_server_stopVideo
* Method:    _stopVideo
* Signature: ()V
*/
static void Java_com_android_server_OverlayViewService__stopVideo
(JNIEnv *env, jobject obj)
{
    if (mIsFullscreen && mUseVideoLayer)
        send_command("/sys/class/video/disable_video", "1");
}

static JNINativeMethod sMethods[] =
{
		{"_init", "(IZ)V",
			(void*)Java_com_android_server_OverlayViewService__1init},
		{"_deinit", "()V",
			(void*)Java_com_android_server_OverlayViewService__1deinit},
		{"_displayHdmi", "()I",
			(void*)Java_com_android_server_OverlayViewService__1displayHdmi},
		{"_displayAndroid", "()I",
			(void*)Java_com_android_server_OverlayViewService__1displayAndroid},
		{"_displayPip", "(IIII)I",
			(void*)Java_com_android_server_OverlayViewService__1displayPip},
		{"_getHActive", "()I",
			(void*)Java_com_android_server_OverlayViewService__1getHActive},
		{"_getVActive", "()I",
			(void*)Java_com_android_server_OverlayViewService__1getVActive},
		{"_getHdmiInSize", "()Ljava/lang/String;",
			(void*)Java_com_android_server_OverlayViewService__1getHdmiInSize},
		{"_isDvi", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1isDvi},
		{"_isPowerOn", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1isPowerOn},
		{"_isEnable", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1isEnable},
		{"_isInterlace", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1isInterlace},
		{"_hdmiPlugged", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1hdmiPlugged},
		{"_hdmiSignal", "()Z",
			(void*)Java_com_android_server_OverlayViewService__1hdmiSignal},
		{"_enableAudio", "(I)I",
			(void*)Java_com_android_server_OverlayViewService__1enableAudio},
		{"_handleAudio", "()I",
			(void*)Java_com_android_server_OverlayViewService__1handleAudio},
		{"_setEnable", "(Z)V",
			(void*)Java_com_android_server_OverlayViewService__1setEnable},
		{"_setSourceType", "()I",
			(void*)Java_com_android_server_OverlayViewService__1setSourceType},
		{"_isSurfaceAvailable", "(Landroid/view/Surface;)Z",
			(void*)Java_com_android_server_OverlayViewService__isSurfaceAvailable},
		{"_setPreviewWindow", "(Landroid/view/Surface;)Z",
			(void*)Java_com_android_server_OverlayViewService__setPreviewWindow},
		{"_setCrop", "(IIII)I",
			(void*)Java_com_android_server_OverlayViewService__1setCrop},
		{"_startMov", "()V",
			(void*)Java_com_android_server_OverlayViewService__startMov},
		{"_stopMov", "()V",
			(void*)Java_com_android_server_OverlayViewService__stopMov},
		{"_pauseMov", "()V",
			(void*)Java_com_android_server_OverlayViewService__pauseMov},
		{"_resumeMov", "()V",
			(void*)Java_com_android_server_OverlayViewService__resumeMov},
		{"_startVideo", "()V",
			(void*)Java_com_android_server_OverlayViewService__startVideo},
		{"_stopVideo", "()V",
			(void*)Java_com_android_server_OverlayViewService__stopVideo},
};

int register_android_server_HDMIIN(JNIEnv* env)
{
	return jniRegisterNativeMethods(env, "com/droidlogic/app/HdmiInManager",
	                        sMethods, NELEM(sMethods));
}

}  // end namespace android

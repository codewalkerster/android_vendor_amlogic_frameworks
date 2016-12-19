/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * frank.chen@amlogic.com
 */

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>

#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/un.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <linux/netlink.h>
#include <linux/kd.h>
#include <linux/loop.h>
#include <linux/if.h>
#include <cutils/partition_utils.h>
#include <cutils/properties.h>
#include <cutils/android_reboot.h>
#include <selinux/selinux.h>
#include <selinux/label.h>

#define LOG_TAG "Dig"
#include "log.h"
#include "md5.h"

#include "DigManager.h"
#include "make_ext4fs.h"
#include "ResponseCode.h"


DigManager *DigManager::sInstance = NULL;

DigManager *DigManager::Instance() {
    if (!sInstance)
        sInstance = new DigManager();
    return sInstance;
}

DigManager::DigManager(): mBootCompleted(false),
	mSupportSysBak(false),
	mDataRoCountMax(0),
	mCheckInterval(INTERVAL_IN_BOOT),
	mCheckSystemCount(CHECK_SYSTEM_COUNT) {
    mBroadcaster = NULL;
}

DigManager::~DigManager() {
}

void* DigManager::_workThread(void *cookie) {
    DigManager* pThis = (DigManager*)cookie;
    return pThis->workThread();
}

void* DigManager::workThread() {

    for (;;) {
        if (!mBootCompleted) {
            if (isBootCompleted() == 1) {
                mBootCompleted = true;
                mCheckInterval = INTERVAL_AFTER_BOOT;
                setDataRoCount(0);

                if (isInitMountDataFail() == 1) {
                    handleInitMountDataFail();
                }
            }
        }

        //check cache ro
        if (isVolumeRo("/cache")) {
            handleCacheRo();
        }

        //check data ro
        if (isVolumeRo("/data")) {
            handleDataRo();
        }

        //check system partition
        if (( mSupportSysBak != 0) && (mCheckSystemCount++ == CHECK_SYSTEM_COUNT)) {
            mCheckSystemCount = 1;
            char error_file_path[512];
            int sys_check = checkSystemPartition(error_file_path);
            if ( sys_check != 0 ) {
                HanldeSysChksumError(error_file_path);
            }
        }

        if (isRebooting())
            break;

        //sleep
        sleep(mCheckInterval);
    }

    return NULL;
}

int DigManager::start() {

    ERROR("dig start!\n");

    struct selinux_opt seopts[] = {
       { SELABEL_OPT_PATH, "/file_contexts" }
    };

    mSehandle = selabel_open(SELABEL_CTX_FILE, seopts, 1);

    if (!mSehandle) {
        ERROR("No file_contexts\n");
    }

    mSupportSysBak = isSupportSystemBak();
    mDataRoCountMax = getDataRoCountMax();

    pthread_create(&mTread, NULL, _workThread, this);

    return 0;
}

int DigManager::stop() {
    return 0;
}


int DigManager::isFileExist(const char* path) {
    return (access(path, F_OK) == 0);
}

int DigManager::isSupportSystemBak() {
    int sup_sys_bak = 0;
    char system_bak_enable[PROPERTY_VALUE_MAX];
    property_get("ro.system_backup_enable", system_bak_enable, "0");
    if (strcmp(system_bak_enable, "1") == 0) {
        sup_sys_bak = isFileExist(SYSTEM_BAK_NODE);
    }
    return sup_sys_bak;
}

int DigManager::isInitMountDataFail() {
    int mount_fail = 0;
    char prop_mountdata[PROPERTY_VALUE_MAX];
    property_get("ro.init.mountdatafail", prop_mountdata, "false");
    if (strcmp( prop_mountdata, "true" ) == 0) {
        mount_fail = 1;
    }
    return mount_fail;
}

void DigManager::doRestoreSystem()
{
    ERROR("do_restore_system!\n");

    mkdir("/cache/recovery", 0666);

    //write command
    FILE *fCommand = NULL;
    if ((fCommand = fopen("/cache/recovery/command", "wt")) == NULL) {
        ERROR("create /cache/recovery/command fail!\n");
        return;
    }

    fprintf(fCommand, "--restore_system\n");

    fclose(fCommand);

    //android_reboot(ANDROID_RB_RESTART2, 0, "recovery");
    property_set(ANDROID_RB_PROPERTY, "reboot,recovery");

    sleep(20);
}

void DigManager::HanldeSysChksumError(char* error_file_path) {
    if (isRebooting())
        return;

    if (!mBootCompleted) {
        //reboot into recovery to restore system
        ERROR("HanldeSysChksumError doRestoreSystem\n");
        doRestoreSystem();
    } else {
        //start notify activity,RestoreSystemActivity
        ERROR("HanldeSysChksumError start notify activity,RestoreSystemActivity\n");
#ifdef DIG_TEST
        char* cmd = NULL;
        asprintf(&cmd, "/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RestoreSystemActivity -e error_file_path %s", error_file_path);
        if (cmd != NULL) {
            system(cmd);
            free(cmd);
        } else {
            system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RestoreSystemActivity");
        }
#else
        if (mBroadcaster != NULL) {
            char msg[256] = {0};
            if (error_file_path != NULL) {
                snprintf(msg, sizeof(msg), "system crash %s", error_file_path);
            } else {
                snprintf(msg, sizeof(msg), "system crash");
            }
            mBroadcaster->sendBroadcast(ResponseCode::DigReport_SystemChanged,
                    msg, false);
        }
#endif
    }
}

void DigManager::doReboot()
{
    ERROR("doReboot!\n");
    //android_reboot(ANDROID_RB_RESTART, 0, 0);
    property_set(ANDROID_RB_PROPERTY, "reboot");
    sleep(20);
}

void DigManager::doRebootRecoveryTipDataro() {
    ERROR("doRebootRecoveryTipDataro!\n");

    mkdir("/cache/recovery", 0666);

    //write command
    FILE *fCommand = NULL;
    if ((fCommand = fopen("/cache/recovery/command", "wt")) == NULL) {
        ERROR("create /cache/recovery/command fail!\n");
        return;
    }

    fprintf(fCommand, "--data_ro_wipe\n");

    fflush(fCommand);
    fsync(fileno(fCommand));

    fclose(fCommand);

    //android_reboot(ANDROID_RB_RESTART2, 0, "recovery");
    property_set(ANDROID_RB_PROPERTY, "reboot,recovery");

    sleep(20);
}

int DigManager::getDataRoCount() {
    int nCount = 0;
    FILE *fCount = NULL;
    if ((fCount = fopen(DIG_DATA_RO_COUNT_FILE, "r")) != NULL) {
        fscanf(fCount, "%d", &nCount);

        fclose(fCount);
    } else {
        ERROR("getDataRoCount open %s fail!\n", DIG_DATA_RO_COUNT_FILE);
    }

    ERROR("getDataRoCount nCount:%d!\n", nCount);
    return nCount;
}

void DigManager::setDataRoCount(int count) {
    FILE *fCount = NULL;
    if ((fCount = fopen(DIG_DATA_RO_COUNT_FILE, "w")) != NULL) {
        ERROR("setDataRoCount count:%d!\n", count);

        fprintf(fCount, "%d", count);

        fflush( fCount );
        fsync( fileno(fCount) );

        fclose(fCount);
    }
}

void DigManager::handleDataRo() {
    if (isRebooting())
        return;

    if (!mBootCompleted) {
        int count = getDataRoCount();
        ERROR("handleDataRo count:%d\n", count);
        if (count < mDataRoCountMax) {
            //reboot
            ERROR("handleDataRo do_reboot\n");
            setDataRoCount(count+1);
            doReboot();
        } else {
            //data ro too much time,run into recovery mode
            setDataRoCount(0);
            doRebootRecoveryTipDataro();
        }
    } else {
        //start notify activity,RebootActivity
        ERROR("handleDataRo start notify activity,RebootActivity\n");
#ifdef DIG_TEST
        system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RebootActivity");
#else
        if (mBroadcaster != NULL) {
            char msg[256];
            snprintf(msg, sizeof(msg), "data mount ro");
            mBroadcaster->sendBroadcast(ResponseCode::DigReport_DataReadOnly,
                    msg, false);
        }
#endif
    }
}

void DigManager::handleCacheRo() {
    if (isRebooting())
        return;

    const char cache_dev[] = "/dev/block/cache";
    const char target[] = "/cache";
    const char sys[] = "ext4";
    int flags = MS_NOATIME | MS_NODIRATIME | MS_NOSUID | MS_NODEV;
    const char options[] = "noauto_da_alloc";

    if (isFileExist(cache_dev) == 0) {
        ERROR("handleCacheRo cache_dev:%s not exist", cache_dev);
        return;
    }

    if (umount(target) == 0) {
        int result = -1;

        ERROR("make_ext4fs cache_dev:%s,target:%s ", cache_dev, target);
        result = make_ext4fs(cache_dev, 0, target, mSehandle);
        if (result != 0) {
            ERROR("handleCacheRo, format cache make_extf4fs err[%s]\n", strerror(errno) );
        }

        //mount ext4 /dev/block/cache /cache noatime nodiratime norelatime nosuid nodev noauto_da_alloc
        result = mount(cache_dev, target, sys, flags, options);
        if (result) {
            ERROR("handleCacheRo, check cache ro,re-mount failed on err[%s]\n", strerror(errno) );
        }
    } else {
        ERROR("handleCacheRo, check cache ro,umount cache fail");
    }
}

void DigManager::doRemount( char* dev, char* target, char* system, int readonly ) {
    ERROR("doRemount!\n");
    int flags = MS_REMOUNT | MS_NOATIME | MS_NODIRATIME | MS_NOSUID | MS_NODEV;
    if (readonly == 1) {
        flags |= MS_RDONLY;
    }
    char options[] = "noauto_da_alloc";
    mount( dev, target, system, flags, options);
}

int DigManager::uRead(int  fd, void*  buff, int  len)
{
    int  ret;
    do {
       ret = read(fd, buff, len);
    } while (ret < 0 && errno == EINTR);
    return ret;
}

int DigManager::fRead(const char*  filename, char* buff, size_t  buffsize)
{
    int  len = 0;
    int  fd  = open(filename, O_RDONLY);
    if (fd >= 0) {
        len = uRead(fd, buff, buffsize-1);
        close(fd);
    }
    buff[len > 0 ? len : 0] = 0;
    return len;
}


bool DigManager::isVolumeRo(char *device)
{
    bool ro = false;
    char mounts[4096], *start, *end, *line;
    fRead("/proc/mounts", mounts, sizeof(mounts));
    start = mounts;

    while ((end = strchr(start, '\n'))) {
        line = start;
        *end++ = 0;
        start = end;

        if (strstr(line, device) != NULL) {
            if (strstr(line, "ro,") != NULL) {
                ERROR("%s became read-only!\n", device);
                ro = true;
            }
            break;
        }
    }

    return ro;
}

int DigManager::isBootCompleted() {
    int ret = 0;

    //check if system is complete
    char flag[PROPERTY_VALUE_MAX];
    property_get("sys.boot_completed", flag, "");
    if (strcmp(flag, "1") == 0) {
        ERROR("isBootCompleted:%s!\n", flag);
        ret = 1;
    }

    return ret;
}

void DigManager::hextoa(char *szBuf, unsigned char nData[], int len)
{
    int i;
    for (i = 0; i < len; i++,szBuf+=2) {
        sprintf(szBuf,"%02x",nData[i]);
    }
}

int DigManager::getMd5(const char *path, unsigned char* md5)
{
    unsigned int i;
    int fd;
    MD5_CTX md5_ctx;

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        fprintf(stderr,"could not open %s, %s\n", path, strerror(errno));
        return -1;
    }

    /* Note that bionic's MD5_* functions return void. */
    MD5_Init(&md5_ctx);

    while (1) {
        char buf[4096];
        ssize_t rlen;
        rlen = read(fd, buf, sizeof(buf));
        if (rlen == 0)
            break;
        else if (rlen < 0) {
            fprintf(stderr,"could not read %s, %s\n", path, strerror(errno));
            return -1;
        }
        MD5_Update(&md5_ctx, buf, rlen);
    }
    if (close(fd)) {
        fprintf(stderr,"could not close %s, %s\n", path, strerror(errno));
    }

    MD5_Final(md5, &md5_ctx);
/*
    for (i = 0; i < (int)sizeof(md5); i++)
        printf("%02x", md5[i]);
    printf("  %s\n", path);
*/
    return 0;
}

int DigManager::checkSystemPartition(char* error_file_path) {
    FILE* f_chk_sum = NULL;
    if ((f_chk_sum = fopen(CHECKSUM_LIST_PATH, "r")) == NULL) {
        ERROR("checkSystemPartition fopen chksum_list fail!\n");
        return 0;
    }

    char chksum[256], filepath[256];
    while (fscanf(f_chk_sum,"%s  %s\n", chksum, filepath) == 2) {
        //ERROR("checkSystemPartition chksum:%s -> filepath:%s\n", chksum, filepath);
        unsigned char md5[MD5_DIGEST_LENGTH];
        char md5_str[2*MD5_DIGEST_LENGTH+1] = {0};
        if (getMd5(filepath, md5) == 0) {
                        hextoa( md5_str, md5, MD5_DIGEST_LENGTH );
        } else {
            ERROR("checkSystemPartition get md5sum fail filepath:%s\n", filepath);
            sprintf( error_file_path, "%s", filepath);
                        return 1;
        }

        if (strcmp(chksum, md5_str)) {
            ERROR("checkSystemPartition chksum is wrong filepath:%s !\n", filepath);
            sprintf( error_file_path, "%s", filepath);
            return 1;
        }

        usleep(CHECK_SYSTEM_FILE_SLEEP_TIME);
    }

    fclose(f_chk_sum);

    return 0;
}

void DigManager::handleInitMountDataFail() {
    ERROR("handle init mount data fail, start notify activity,WipeConfirmActivity\n");

#ifdef DIG_TEST
    system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.WipeConfirmActivity");
#else
    if (mBroadcaster != NULL) {
        char msg[256] = {0};
        snprintf(msg, sizeof(msg), "data mount fail");
        mBroadcaster->sendBroadcast(ResponseCode::DigReport_DataCrash,
                    msg, false);
    }
#endif
}

int DigManager::getDataRoCountMax() {
    char value[PROPERTY_VALUE_MAX];
    property_get(DATA_RO_COUNT_MAX_PROP, value, DATA_RO_COUNT_MAX_DEFAULT);
    int count_max = atoi(value);
    //ERROR("getDataRoCountMax return:%d!\n", count_max);
    return count_max;
}

bool DigManager::isRebooting() {
    char powerctl[PROPERTY_VALUE_MAX] = {0};
    property_get(ANDROID_RB_PROPERTY, powerctl, "");
    if (strstr(powerctl, "reboot") != NULL) {
        return true;
    }
    return false;
}

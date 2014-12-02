#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <linux/kd.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <linux/if.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <sys/mount.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <linux/loop.h>
#include <cutils/partition_utils.h>
#include <cutils/properties.h>
#include <cutils/android_reboot.h>
#include "log.h"
#include "md5.h"

#ifndef MD5_DIGEST_LENGTH
#define MD5_DIGEST_LENGTH 16
#endif

//data only check time in bootup:
#define INTERVAL_IN_BOOT 1

//data only check time in normal:
#define INTERVAL_AFTER_BOOT 180

//system partition check time: CHECK_SYSTEM_COUNT * "data only check time"
#define CHECK_SYSTEM_COUNT 5

//sleep between system file check, unit is us
#define CHECK_SYSTEM_FILE_SLEEP_TIME 5000

#define CHECKSUM_LIST_PATH "/system/chksum_list"

#define SYSTEM_BAK_NODE "/dev/block/backup"

#define DIG_DATA_RO_COUNT_FILE "/cache/dig_data_ro_count"

//could overwrite the g_data_ro_count_max value via DATA_RO_COUNT_MAX_PROP
#define DATA_RO_COUNT_MAX_PROP "ro.dig.dataro_count_max"
#define DATA_RO_COUNT_MAX_DEFAULT "5"

static int g_bootCompleted = 0;
static int g_supportSysBak = 0;
static int g_data_ro_count_max = 0;

int is_file_exist(const char* path) {
    return ( access( path, F_OK ) == 0 );
}

int is_support_system_bak() {
    int sup_sys_bak = 0;
    char system_bak_enable[PROPERTY_VALUE_MAX];
    property_get("ro.system_backup_enable", system_bak_enable, "0");
    if ( strcmp( system_bak_enable, "1" ) == 0 ) {
        sup_sys_bak = is_file_exist( SYSTEM_BAK_NODE );
    }
    return sup_sys_bak;
}

int is_init_mount_data_fail() {
    int mount_fail = 0;
    char prop_mountdata[PROPERTY_VALUE_MAX];
    property_get("ro.init.mountdatafail", prop_mountdata, "false");
    if ( strcmp( prop_mountdata, "true" ) == 0 ) {
        mount_fail = 1;
    }
    return mount_fail;
}

void do_restore_system()
{
    ERROR("data_integrity_guard do_restore_system!\n");

    mkdir("/cache/recovery", 0666);

    //write command
    FILE *fCommand = NULL;
    if ( (fCommand = fopen("/cache/recovery/command", "wt")) == NULL ) {
        ERROR("data_integrity_guard create /cache/recovery/command fail!\n");
        return;
    }

    fprintf(fCommand, "--restore_system\n");

    fclose(fCommand);

    android_reboot(ANDROID_RB_RESTART2, 0, "recovery");

    sleep(20);
}

void HanldeSysChksumError(char* error_file_path) {
    if ( g_bootCompleted == 0 ) {
        //reboot into recovery to restore system
        ERROR("HanldeSysChksumError do_restore_system\n");
        do_restore_system();
    } else {
        //start notify activity,RestoreSystemActivity
        ERROR("HanldeSysChksumError start notify activity,RestoreSystemActivity\n");
        char* cmd = NULL;
        asprintf(&cmd, "/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RestoreSystemActivity -e error_file_path %s", error_file_path);
        if ( cmd != NULL ) {
            system(cmd);
            free(cmd);
        } else {
            system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RestoreSystemActivity");
        }
    }
}

void do_reboot()
{
    ERROR("data_integrity_guard do_reboot!\n");
    android_reboot(ANDROID_RB_RESTART, 0, 0);
    sleep(20);
}

void do_reboot_recovery_tip_dataro() {
    ERROR("data_integrity_guard do_reboot_recovery_tip_dataro!\n");

    mkdir("/cache/recovery", 0666);

    //write command
    FILE *fCommand = NULL;
    if ( (fCommand = fopen("/cache/recovery/command", "wt")) == NULL ) {
        ERROR("data_integrity_guard create /cache/recovery/command fail!\n");
        return;
    }

    fprintf(fCommand, "--data_ro_wipe\n");

    fflush( fCommand );
    fsync( fileno(fCommand) );

    fclose(fCommand);

    android_reboot(ANDROID_RB_RESTART2, 0, "recovery");

    sleep(20);
}

int get_data_ro_count() {
    int nCount = 0;
    FILE *fCount = NULL;
        if ( (fCount = fopen(DIG_DATA_RO_COUNT_FILE, "r")) != NULL ) {
        fscanf(fCount, "%d", &nCount);

        fclose(fCount);
    } else {
        ERROR("dig get_data_ro_count open %s fail!\n", DIG_DATA_RO_COUNT_FILE);
    }

    ERROR("dig get_data_ro_count nCount:%d!\n", nCount);
    return nCount;
}

void set_data_ro_count(int count) {
    FILE *fCount = NULL;
    if ( (fCount = fopen(DIG_DATA_RO_COUNT_FILE, "w")) != NULL ) {
        ERROR("dig set_data_ro_count count:%d!\n", count);

        fprintf(fCount, "%d", count);

        fflush( fCount );
        fsync( fileno(fCount) );

        fclose(fCount);
    }
}

void handleDataRo() {
    if ( g_bootCompleted == 0 ) {
        int count = get_data_ro_count();
        ERROR("handleDataRo count:%d\n", count);
        if ( count < g_data_ro_count_max ) {
            //reboot
            ERROR("handleDataRo do_reboot\n");
            set_data_ro_count(count+1);
            do_reboot();
        } else {
            //data ro too much time,run into recovery mode
            set_data_ro_count(0);
            do_reboot_recovery_tip_dataro();
        }
    } else {
        //start notify activity,RebootActivity
        ERROR("handleDataRo start notify activity,RebootActivity\n");
        system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.RebootActivity");
    }
}

void handleCacheRo() {
    const char cache_dev[] = "/dev/block/cache";
    const char target[] = "/cache";
    const char sys[] = "ext4";
    int flags = MS_NOATIME | MS_NODIRATIME | MS_NOSUID | MS_NODEV;
    const char options[] = "noauto_da_alloc";

    if ( is_file_exist(cache_dev) == 0 ) {
        ERROR("handleCacheRo cache_dev:%s not exist", cache_dev);
        return;
    }

    if ( umount(target) == 0 ) {
        int result = -1;
        ERROR("dig make_ext4fs cache_dev:%s,target:%s ", cache_dev, target);
        result = make_ext4fs(cache_dev, 0, target, NULL);
        if (result != 0) {
            ERROR("handleCacheRo, format cache make_extf4fs err[%s]\n", strerror(errno) );
        }
/*
        char* cmd = NULL;
        asprintf(&cmd, "/system/xbin/mkfs.ext2 %s", cache_dev);
        if ( cmd != NULL ) {
            ERROR("format cmd:%s", cmd);
            system(cmd);
            free(cmd);
        }
*/
        //mount ext4 /dev/block/cache /cache noatime nodiratime norelatime nosuid nodev noauto_da_alloc
        result = mount(cache_dev, target, sys, flags, options);
        if (result) {
            ERROR("handleCacheRo, check cache ro,re-mount failed on err[%s]\n", strerror(errno) );
        }
    } else {
        ERROR("handleCacheRo, check cache ro,umount cache fail");
    }
}

void do_remount( char* dev, char* target, char* system, int readonly ) {
    ERROR("data_integrity_guard do_remount!\n");
    int flags = MS_REMOUNT | MS_NOATIME | MS_NODIRATIME | MS_NOSUID | MS_NODEV;
    if (readonly == 1) {
        flags |= MS_RDONLY;
    }
    char options[] = "noauto_da_alloc";
    mount( dev, target, system, flags, options);
}

int u_read(int  fd, void*  buff, int  len)
{
    int  ret;
    do { ret = read(fd, buff, len); } while (ret < 0 && errno == EINTR);
    return ret;
}

int f_read(const char*  filename, char* buff, size_t  buffsize)
{
    int  len = 0;
    int  fd  = open(filename, O_RDONLY);
    if (fd >= 0) {
        len = u_read(fd, buff, buffsize-1);
        close(fd);
    }
    buff[len > 0 ? len : 0] = 0;
    return len;
}


int is_data_ro()
{
    int ro = 0;
    char mounts[2048], *start, *end, *line;
    f_read("/proc/mounts", mounts, sizeof(mounts));
    start = mounts;

    while ( (end = strchr(start, '\n')))
    {
        line = start;
        *end++ = 0;
        start = end;

        if ( strstr( line, "/data" ) != NULL )
        {
            if ( strstr( line, "ro," ) != NULL )
            {
                ERROR("data partition is read-only!\n");
                ro = 1;
            }
            break;
        }
    }

    return ro;
}

int is_cache_ro()
{
    int ro = 0;
    char mounts[2048], *start, *end, *line;
    f_read("/proc/mounts", mounts, sizeof(mounts));
    start = mounts;

    while ( (end = strchr(start, '\n')))
    {
        line = start;
        *end++ = 0;
        start = end;

        if ( strstr( line, "/cache" ) != NULL )
        {
            if ( strstr( line, "ro," ) != NULL )
            {
                ERROR("dig is_cache_ro, cache partition is read-only!\n");
                ro = 1;
            }
            break;
        }
    }

    //ERROR("dig is_cache_ro, is_cache_ro ret:%d\n",ro);

    return ro;
}

int isBootCompleted() {
    int ret = 0;

    //check if system is complete
    char flag[PROPERTY_VALUE_MAX];
    property_get("sys.boot_completed", flag, "");
    if (strcmp(flag, "1") == 0) {
        ERROR("data_integrity_guard isBootCompleted:%s!\n", flag);
        ret = 1;
    }

    return ret;
}

void hextoa(char *szBuf, unsigned char nData[], int len)
{
    int i;
    for ( i = 0; i < len; i++,szBuf+=2 ) {
        sprintf(szBuf,"%02x",nData[i]);
    }
}

int get_md5(const char *path, unsigned char* md5)
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

int check_system_partition(char* error_file_path) {
    FILE* f_chk_sum = NULL;
    if ( (f_chk_sum = fopen(CHECKSUM_LIST_PATH, "r")) == NULL ) {
        ERROR("check_system_partition fopen chksum_list fail!\n");
        return 0;
    }

    char chksum[256], filepath[256];
    while ( fscanf(f_chk_sum,"%s  %s\n", chksum, filepath) == 2 )
    {
        //ERROR("check_system_partition chksum:%s -> filepath:%s\n", chksum, filepath);
        unsigned char md5[MD5_DIGEST_LENGTH];
        char md5_str[2*MD5_DIGEST_LENGTH+1] = {0};
        if ( get_md5(filepath, md5) == 0 ) {
                        hextoa( md5_str, md5, MD5_DIGEST_LENGTH );
        } else {
            ERROR("check_system_partition get md5sum fail filepath:%s\n", filepath);
            sprintf( error_file_path, "%s", filepath);
                        return 1;
        }

        if ( strcmp(chksum, md5_str) ) {
            ERROR("check_system_partition chksum is wrong filepath:%s !\n", filepath);
            sprintf( error_file_path, "%s", filepath);
            return 1;
        }

        usleep(CHECK_SYSTEM_FILE_SLEEP_TIME);
    }

    fclose(f_chk_sum);

    return 0;
}

void handle_init_mount_data_fail() {
    ERROR("handle init mount data fail, start notify activity,WipeConfirmActivity\n");
    system("/system/bin/am start -n com.droidlogic.promptuser/com.droidlogic.promptuser.WipeConfirmActivity");
}

int get_data_ro_count_max() {
    char value[PROPERTY_VALUE_MAX];
    property_get(DATA_RO_COUNT_MAX_PROP, value, DATA_RO_COUNT_MAX_DEFAULT);
    int count_max = atoi(value);
    //ERROR("dig get_data_ro_count_max return:%d!\n", count_max);
    return count_max;
}

int main()
{
    int nCheckInterval = INTERVAL_IN_BOOT;
    int nCheckSystemCount = CHECK_SYSTEM_COUNT;
    //klog_init();

    ERROR("data_integrity_guard main!\n");

    g_supportSysBak = is_support_system_bak();
    g_data_ro_count_max = get_data_ro_count_max();

    while ( 1 ) {
        if ( g_bootCompleted == 0 ) {
            if ( isBootCompleted() == 1 )
            {
                g_bootCompleted = 1;
                nCheckInterval = INTERVAL_AFTER_BOOT;
                set_data_ro_count(0);

                if ( is_init_mount_data_fail() == 1 ) {
                    handle_init_mount_data_fail();
                }
            }
        }

        //check data ro
        int data_ro = is_data_ro();
        if ( data_ro == 1 )
        {
            handleDataRo();
        }

        //check cache ro
        int cache_ro = is_cache_ro();
        if ( cache_ro == 1 )
        {
            handleCacheRo();
        }

        //check system partition
        if ( ( g_supportSysBak != 0 ) && ( nCheckSystemCount++ == CHECK_SYSTEM_COUNT ) ) {
            nCheckSystemCount = 1;
            //ERROR("check_system_partition before\n");
            char error_file_path[512];
            int sys_check = check_system_partition(error_file_path);
            //ERROR("check_system_partition after\n");
            if ( sys_check != 0 ) {
                HanldeSysChksumError(error_file_path);
            }
        }

        //sleep
        sleep(nCheckInterval);
    }

    ERROR("data_integrity_guard exit!\n");

    return 0;
}



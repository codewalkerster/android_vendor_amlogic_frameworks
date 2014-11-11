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

#define LOG_TAG "SystemControl"
//#define LOG_NDEBUG 0

#include <fcntl.h>
#include <utils/Log.h>
#include <cutils/properties.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/PermissionCache.h>
#include <stdint.h>
#include <sys/types.h>

#include "SystemControl.h"
#include "ubootenv.h"

namespace android {

void SystemControl::instantiate() {
    android::status_t ret = defaultServiceManager()->addService(
            String16("system_control"), new SystemControl());

    if (ret != android::OK) {
        ALOGE("Couldn't register system control service!");
    }
    ALOGI("instantiate add system_control service result:%d", ret);
}

SystemControl::SystemControl()
    : mLogLevel(LOG_LEVEL_DEFAULT){

    bootenv_init();

    pSysWrite = new SysWrite();

    pDisplayMode = new DisplayMode();
    pDisplayMode->init();
}

SystemControl::~SystemControl() {
    delete pSysWrite;
    delete pDisplayMode;
}

int SystemControl::permissionCheck(){

    // codes that require permission check
    IPCThreadState* ipc = IPCThreadState::self();
    const int pid = ipc->getCallingPid();
    const int uid = ipc->getCallingUid();
    #if 0
    if ((uid != AID_MEDIA) &&
            !PermissionCache::checkPermission("android.permission.SYSTEM_CONTROL", pid, uid)) {
        ALOGE("Permission Denial: "
                "can't use system control service pid=%d, uid=%d", pid, uid);
        return PERMISSION_DENIED;
    }
    #endif

    ALOGV("system_control service permissionCheck pid=%d, uid=%d", pid, uid);
    return NO_ERROR;
}

//read write property and sysfs
bool SystemControl::getProperty(const String16& key, String16& value){
    char buf[PROPERTY_VALUE_MAX] = {0};
    //property_get(key, buf, "");
    bool ret = pSysWrite->getProperty(String8(key).string(), buf);
    value.setTo(String16(buf));

    return ret;
}

bool SystemControl::getPropertyString(const String16& key, String16& def, String16& value){
    char buf[PROPERTY_VALUE_MAX] = {0};
    //property_get(key, buf, "");
    bool ret = pSysWrite->getPropertyString(String8(key).string(), String8(def).string(), (char *)buf);
    value.setTo(String16(buf));

    //bool ret = pSysWrite->getPropertyString(key, def, value);
    return ret;
}

int32_t SystemControl::getPropertyInt(const String16& key, int32_t def){
    return pSysWrite->getPropertyInt(String8(key).string(), def);
}

int64_t SystemControl::getPropertyLong(const String16& key, int64_t def){
    return pSysWrite->getPropertyLong(String8(key).string(), def);
}

bool SystemControl::getPropertyBoolean(const String16& key, bool def){
    return pSysWrite->getPropertyBoolean(String8(key).string(), def);
}

void SystemControl::setProperty(const String16& key, const String16& value){
    if(NO_ERROR == permissionCheck()){
        pSysWrite->setProperty(String8(key).string(), String8(value).string());
        traceValue(String16("setProperty"), key, value);
    }
}

bool SystemControl::readSysfs(const String16& path, String16& value){
    if(NO_ERROR == permissionCheck()){
        traceValue(String16("readSysfs"), path, value);

        char buf[PROPERTY_VALUE_MAX] = {0};
        bool ret = pSysWrite->readSysfs(String8(path).string(), buf);
        value.setTo(String16(buf));
        return ret;
        //return pSysWrite->readSysfs(path, value);
    }

    return false;
}

bool SystemControl::writeSysfs(const String16& path, const String16& value){
    if(NO_ERROR == permissionCheck()){
        traceValue(String16("writeSysfs"), path, value);

        return pSysWrite->writeSysfs(String8(path).string(), String8(value).string());
    }

    return false;
}

//set or get uboot env
bool SystemControl::getBootEnv(const String16& key, String16& value){
    //bool ret = pSysWrite->getProperty(key, value);
    const char* p_value = bootenv_get(String8(key).string());
	if (p_value) {
        value.setTo(String16(p_value));
        return true;
	}
    return false;
}

void SystemControl::setBootEnv(const String16& key, const String16& value){
    if(NO_ERROR == permissionCheck()){
        bootenv_update(String8(key).string(), String8(value).string());
        traceValue(String16("setBootEnv"), key, value);
    }
}

void SystemControl::traceValue(const String16& type, const String16& key, const String16& value) {
    if(mLogLevel > LOG_LEVEL_0){
        ALOGI("%s key=%s value=%s from pid=%d, uid=%d",
            String8(type).string(), String8(key).string(), String8(value).string(),
            IPCThreadState::self()->getCallingPid(),
            IPCThreadState::self()->getCallingUid());
    }
}

void SystemControl::setLogLevel(int level) {
    if(level > (LOG_LEVEL_TOTAL - 1)){
        ALOGE("out of range level=%d, max=%d", level, LOG_LEVEL_TOTAL);
        return;
    }

    mLogLevel = level;
    pSysWrite->setLogLevel(level);
    pDisplayMode->setLogLevel(level);
}

int SystemControl::getLogLevel() {
    return mLogLevel;
}

status_t SystemControl::dump(int fd, const Vector<String16>& args){
    const size_t SIZE = 256;
    char buffer[SIZE];
    String8 result;
    if (checkCallingPermission(String16("android.permission.DUMP")) == false) {
        snprintf(buffer, SIZE, "Permission Denial: "
                "can't dump system_control from pid=%d, uid=%d\n",
                IPCThreadState::self()->getCallingPid(),
                IPCThreadState::self()->getCallingUid());
        result.append(buffer);
    } else {
        Mutex::Autolock lock(mLock);

        int len = args.size();
        for (int i = 0; i < len; i ++){
            String16 debugLevel("-l");
            String16 bootenv("-b");
            String16 display("-d");
            String16 help("-h");
            if (args[i] == debugLevel) {
                if(i + 1 < len){
                    String8 levelStr(args[i+1]);
                    int level = atoi(levelStr.string());
                    setLogLevel(level);

                    result.appendFormat(String8::format("Setting log level to %d.\n", level));
                    break;
                }
            }
            else if (args[i] == bootenv) {
                if((i + 3 < len) && (args[i + 1] == String16("set"))){
                    setBootEnv(args[i+2], args[i+3]);

                    result.appendFormat(String8::format("set bootenv key:[%s] value:[%s]\n",
                        String8(args[i+2]).string(), String8(args[i+3]).string()));
                    break;
                }
                else if (((i + 2) <= len) && (args[i + 1] == String16("get"))){
                    if((i + 2) == len){
                        result.appendFormat("get all bootenv\n");
                        bootenv_print();
                    }
                    else{
                        String16 value;
                        getBootEnv(args[i+2], value);

                        result.appendFormat(String8::format("get bootenv key:[%s] value:[%s]\n",
                            String8(args[i+2]).string(), String8(value).string()));
                    }
                    break;
                }
                else{
                    result.appendFormat(
                        "dump bootenv format error!! should use:\n"
                        "dumpsys system_control -b [set |get] key value \n");
                }
            }
            else if (args[i] == display) {
                /*
                String8 displayInfo;
                pDisplayMode->dump(displayInfo);
                result.append(displayInfo);*/

                char buf[2048] = {0};
                pDisplayMode->dump(buf);
                result.append(String8(buf));
                break;
            }
            else if (args[i] == help) {
                result.appendFormat(
                    "system_control service use to control the system sysfs property and boot env \n"
                    "in multi-user mode, normal process will have not system privilege \n"
                    "usage: \n"
                    "dumpsys system_control -l value \n"
                    "dumpsys system_control -b [set |get] key value \n"
                    "-l: debug level \n"
                    "-b: set or get bootenv \n"
                    "-d: dump display mode info \n"
                    "-h: help \n");
            }
        }
    }
    write(fd, result.string(), result.size());
    return NO_ERROR;
}

} // namespace android


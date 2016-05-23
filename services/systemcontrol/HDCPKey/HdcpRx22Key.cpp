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
 *  @date     2016/5/17
 *  @par function description:
 *  - 1 read key from storage, then generate a firmware
 */

#define LOG_TAG "SystemControl"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "HdcpRx22Key.h"
#include "HdcpKeyDecrypt.h"

#define HDCP_RX_KEY_ENCRYPT_TEST        "/param/extractedKey"

HdcpRx22Key::HdcpRx22Key() {
}

HdcpRx22Key::~HdcpRx22Key() {
}

int HdcpRx22Key::readSys(const char *path, char *buf, int count) {
    int fd, len = -1;

    if ( NULL == buf ) {
        SYS_LOGE("buf is NULL");
        return len;
    }

    if ((fd = open(path, O_RDONLY)) < 0) {
        SYS_LOGE("readSysFs, open %s error(%s)", path, strerror (errno));
        return len;
    }

    len = read(fd, buf, count);
    if (len < 0) {
        SYS_LOGE("read error: %s, %s\n", path, strerror(errno));
    }

    close(fd);
    return len;
}

int HdcpRx22Key::writeSys(const char *path, const char *val) {
    int fd, len = -1;

    if ((fd = open(path, O_RDWR)) < 0) {
        SYS_LOGE("writeSysFs, open %s error(%s)", path, strerror(errno));
        return len;
    }

    len = write(fd, val, strlen(val));
    close(fd);
    return len;
}

int HdcpRx22Key::saveFile(const char *path, const char *buf, int bufLen) {
    int fd, len = -1;

    if ((fd = open(path, O_CREAT | O_RDWR | O_TRUNC, 0644)) < 0) {
        SYS_LOGE("saveFile, open %s error(%s)", path, strerror(errno));
        return len;
    }

    len = write(fd, buf, bufLen);
    close(fd);
    return len;
}

bool HdcpRx22Key::genKeyImg() {
    char cmd[512] = {0};
    sprintf(cmd, "%s -t 0 %s %s -s 0 -e 1 -o %s",
        HDCP_RX_TOOL_KEYS, HDCP_RX_KEY_FORMAT, HDCP_RX_KEY_PATH, HDCP_RX_OUT_KEY_IMG);

    SYS_LOGI("hdcpkeys cmd:%s\n", cmd);
    int ret = system (cmd);
    if (0 != ret) {
        SYS_LOGE("exec cmd:%s error\n", cmd);
        return false;
    }

    if (access(HDCP_RX_OUT_KEY_IMG, F_OK)) {
        SYS_LOGE("generate %s fail \n", HDCP_RX_OUT_KEY_IMG);
        return false;
    }

    SYS_LOGI("generate %s success \n", HDCP_RX_OUT_KEY_IMG);
    return true;
}

bool HdcpRx22Key::esmSwap() {
    char cmd[512] = {0};
    sprintf(cmd, "%s -i %s -o %s -s 1616 -e %s -t 0",
        HDCP_RX_TOOL_ESM_SWAP, HDCP_RX_OUT_KEY_IMG, HDCP_RX_OUT_KEY_LE, HDCP_RX_CFG_AIC_DES);

    SYS_LOGI("esm swap cmd:%s\n", cmd);
    int ret = system (cmd);
    if (0 != ret) {
        SYS_LOGE("exec cmd:%s error\n", cmd);
        return false;
    }

    if (access(HDCP_RX_OUT_KEY_LE, F_OK)) {
        SYS_LOGE("generate %s fail \n", HDCP_RX_OUT_KEY_LE);
        return false;
    }

    SYS_LOGI("generate %s success \n", HDCP_RX_OUT_KEY_LE);
    return true;
}

bool HdcpRx22Key::aicTool() {
    char cmd[512] = {0};
    sprintf(cmd, "%s --format=binary-le -o %s -f %s",
        HDCP_RX_TOOL_AIC, HDCP_RX_OUT_FW_LE, HDCP_RX_CFG_AIC_DES);

    SYS_LOGI("aic tool cmd:%s\n", cmd);
    int ret = system (cmd);
    if (0 != ret) {
        SYS_LOGE("exec cmd:%s error\n", cmd);
        return false;
    }

    if (access(HDCP_RX_OUT_FW_LE, F_OK)) {
        SYS_LOGE("generate %s fail \n", HDCP_RX_OUT_FW_LE);
        return false;
    }

    SYS_LOGI("generate %s success \n", HDCP_RX_OUT_FW_LE);
    return true;
}

bool HdcpRx22Key::generateHdcpRxFw()
{
    int ret = false;
    int keyLen = 0;

    char keyCrcData[HDCP_RX_KEY_CRC_LEN] = {0};
    long keyCrcValue = 0;
    char lastCrcData[HDCP_RX_KEY_CRC_LEN] = {0};
    long lastCrcValue = 0;

    char existKey[10] = {0};

    char *pKeyBuf = new char[HDCP_RX_STORAGE_KEY_SIZE];
    if (!pKeyBuf) {
        SYS_LOGE("Exception: fail to alloc buffer size:%d\n", HDCP_RX_STORAGE_KEY_SIZE);
        goto _exit;
    }
    memset(pKeyBuf, 0, HDCP_RX_STORAGE_KEY_SIZE);

    writeSys("/sys/class/unifykeys/attach", "1");
    writeSys("/sys/class/unifykeys/name", "hdcp22_rx_fw");

    readSys("/sys/class/unifykeys/exist", (char*)existKey, 10);
    if (0 == strcmp(existKey, "0")) {
        SYS_LOGE("do not write key to the storage");
        goto _exit;
    }

#if 1
    //the first 4 bytes are the crc values
    keyLen = readSys("/sys/class/unifykeys/read", pKeyBuf, HDCP_RX_STORAGE_KEY_SIZE);
    if (keyLen < HDCP_RX_KEY_CRC_LEN) {
        SYS_LOGE("read key length fail, at least %d bytes, but read len = %d\n", HDCP_RX_KEY_CRC_LEN, keyLen);
        goto _exit;
    }
#else
    keyLen = readSys(HDCP_RX_KEY_ENCRYPT_TEST, pKeyBuf, HDCP_RX_STORAGE_KEY_SIZE);
    if (keyLen < HDCP_RX_KEY_CRC_LEN) {
        SYS_LOGE("read key length fail, at least %d bytes, but read len = %d\n", HDCP_RX_KEY_CRC_LEN, keyLen);
        goto _exit;
    }
#endif

    //for (int i = 0; i < keyLen; i++)
    //    SYS_LOGI("read key [%d] = 0x%x\n", i, pKeyBuf[i]);

    memcpy(keyCrcData, pKeyBuf, HDCP_RX_KEY_CRC_LEN);
    keyCrcValue = ((keyCrcData[3]<<24)|(keyCrcData[2]<<16)|(keyCrcData[1]<<8)|(keyCrcData[0]&0xff));

    readSys(HDCP_RX_KEY_CRC_PATH, lastCrcData, HDCP_RX_KEY_CRC_LEN);
    lastCrcValue = ((lastCrcData[3]<<24)|(lastCrcData[2]<<16)|(lastCrcData[1]<<8)|(lastCrcData[0]&0xff));

    if (access(HDCP_RX_OUT_FW_LE, F_OK) || keyCrcValue != lastCrcValue) {
        SYS_LOGI("HDCP RX 2.2 firmware don't exist or crc different, need create it, last crc value:0x%x, cur crc value:0x%x\n",
            lastCrcValue, keyCrcValue);

        //1. unpack random number and key to the files
        bool decryptRet = hdcpKeyUnpack(pKeyBuf, keyLen,
            HDCP_RX_CFG_AIC_SRC, HDCP_RX_CFG_AIC_DES, HDCP_RX_KEY_PATH);
        if (!decryptRet) {
            SYS_LOGE("unpack hdcp key fail\n");
            goto _exit;
        }

        //2. then generate hdcp firmware
        if (genKeyImg() && esmSwap() && aicTool()) {
            //3. generate firmware success, save the key's crc value
            saveFile(HDCP_RX_KEY_CRC_PATH, keyCrcData, HDCP_RX_KEY_CRC_LEN);
            SYS_LOGI("HDCP RX 2.2 firmware generate success, save crc value:0x%x -> %s\n", keyCrcValue, HDCP_RX_KEY_CRC_PATH);

            //remove temporary files
            remove(HDCP_RX_KEY_PATH);
            remove(HDCP_RX_OUT_KEY_IMG);
            remove(HDCP_RX_OUT_KEY_LE);

            ret = true;
        }
    }

_exit:
    if (pKeyBuf) delete[] pKeyBuf;
    return ret;
}


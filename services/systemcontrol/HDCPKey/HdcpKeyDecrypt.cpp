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
 *  - 1 decrypt key from storage, generate 2 files
 *  - 2 one is random number, another is key file
 */

#define LOG_TAG "SystemControl"
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include "common.h"
#include "HdcpRx22Key.h"
#include "HdcpKeyDecrypt.h"

#define debugP(fmt...) printf("[Debug]"fmt)
#define errorP(fmt...) printf("[Error]"fmt)

#define _KEY_MAX_SIZE   (1024 * 2)

unsigned add_sum(const void* pBuf, const unsigned size)
{
    unsigned sum		 =	0;
    const unsigned* data = (const unsigned*)pBuf;
    unsigned wordLen 	 = size>>2;
    unsigned rest 		 = size & 3;

    for (; wordLen/4; wordLen -= 4) {
        sum += *data++;
        sum += *data++;
        sum += *data++;
        sum += *data++;
    }

    while (wordLen--) {
        sum += *data++;
    }

    if (rest == 0) {
        ;
    }
    else if(rest == 1) {
        sum += (*data) & 0xff;
    }
    else if(rest == 2) {
        sum += (*data) & 0xffff;
    }
    else if(rest == 3) {
        sum += (*data) & 0xffffff;
    }

    return sum;
}

#if 0
static int _save_buf_as_files(const u8* data, const unsigned dataLen, const char* filePath)
{
    FILE* pFile = fopen(filePath, "wb");
    if (!pFile) {
        errorP("Fail in open (%s) in wb\n", filePath);
        return __LINE__;
    }
    fwrite(data, sizeof(char), dataLen / sizeof(char), pFile);
    fclose(pFile);

    return 0;
}

/*
 * Return value, 0 if no error
 * */
static int hdcp22_key_unpack(const char* keyPath, const char* unPackDir)
{
    int ret = __LINE__;
    char* keyBuf = NULL;
    unsigned nread = 0;
    AmlResImgHead_t*  packedImgHead = NULL;
    AmlResItemHead_t* packedImgItem = NULL;
    unsigned gensum = 0;
    int i = 0;

    FILE* pHdcpKey = fopen(keyPath, "rb");
    if (!pHdcpKey) {
        errorP("Fail in fopen(%s)\n", keyPath);
        return __LINE__;
    }

    fseeko(pHdcpKey, 0, SEEK_END);
    const unsigned keyFileSz = (unsigned)ftell(pHdcpKey);
    if ( keyFileSz > _KEY_MAX_SIZE ) {
        errorP("FileSz %zd > max(%d)\n", keyFileSz, _KEY_MAX_SIZE);
        ret = __LINE__; goto _exit;
    }

    keyBuf = new char [_KEY_MAX_SIZE / sizeof(char)];
    if ( !keyBuf ) {
        errorP("Fail in alloc buf for key read\n");
        return __LINE__;
    }

    fseeko(pHdcpKey, 0, SEEK_SET);
    nread = fread(keyBuf, 1, keyFileSz, pHdcpKey);
    if ( nread != keyFileSz ) {
        errorP("Fail in read key to buf, nread=%d\n", nread);
        ret = __LINE__; goto _exit;
    }

    packedImgHead = (AmlResImgHead_t*)keyBuf;
    packedImgItem = (AmlResItemHead_t*)(packedImgHead + 1);

    gensum = add_sum(keyBuf + 4, keyFileSz - 4);
    if (packedImgHead->crc != gensum) {
        errorP("crc chcked failed, origsum[%8x] != gensum[%8x]\n", packedImgHead->crc, gensum);
        ret = __LINE__; goto _exit;
    }

    for (i = 0; i < packedImgHead->imgItemNum; ++i)
    {
        const AmlResItemHead_t* pItem = packedImgItem + i;
        u8*         itembuf         = (u8*)keyBuf + pItem->dataOffset;
        const int   itemSz          = pItem->dataSz;
        const char* itemName =      pItem->name;
        char  fPath[128];

        const int   dirPathLen  = strlen(unPackDir);
        const int   itemPathLen = strlen(itemName);
        if ( 128 - 1 < dirPathLen + itemPathLen ) {
            errorP("dirPathLen(%d) + itemPathLen(%d) > maxLen(%d)\n", dirPathLen, itemPathLen, 127);
            ret = __LINE__; goto _exit;
        }
        memcpy(fPath, unPackDir, dirPathLen);
        memcpy(fPath + dirPathLen, itemName, itemPathLen);
        fPath[dirPathLen + itemPathLen] = '\0';

        ret = _save_buf_as_files(itembuf, itemSz, fPath);
        if (ret) {
            errorP("Fail in _save_buf_as_files\n");
            ret = __LINE__; goto _exit;
        }
    }


    ret = 0;
_exit:
    delete[] keyBuf;
    if (pHdcpKey) fclose(pHdcpKey);
    return ret;
}

int main(int argc, char* argv[])
{
    int ret = 0;
    if (argc < 3) {
        errorP("usages: %s keyPath unpackDirPath\n", argv[0]);
        return __LINE__;
    }
    const char* keyPath     = argv[1];
    const char* unpackDir   = argv[2];

    ret = hdcp22_key_unpack(keyPath, unpackDir);

    return ret;
}
#endif

/*
 * inBuf: include random number and hdcp key
 * */
bool hdcpKeyUnpack(const char* inBuf, int inBufLen,
    const char *srcAicPath, const char *desAicPath, const char *keyPath)
{
    AmlResImgHead_t*  packedImgHead = NULL;
    AmlResItemHead_t* packedImgItem = NULL;
    unsigned gensum = 0;
    int i = 0;

    if ( inBufLen > _KEY_MAX_SIZE ) {
        SYS_LOGE("key size %d > max(%d)\n", inBufLen, _KEY_MAX_SIZE);
        return false;
    }

    if (access(srcAicPath, F_OK)) {
        SYS_LOGE("do not exist path:%s\n", srcAicPath);
        return false;
    }

    packedImgHead = (AmlResImgHead_t*)inBuf;
    packedImgItem = (AmlResItemHead_t*)(packedImgHead + 1);

    gensum = add_sum(inBuf + 4, inBufLen - 4);
    if (packedImgHead->crc != gensum) {
        SYS_LOGE("crc chcked failed, origsum[%8x] != gensum[%8x]\n", packedImgHead->crc, gensum);
        return false;
    }

    for (i = 0; i < (int)packedImgHead->imgItemNum; ++i) {
        const AmlResItemHead_t* pItem = packedImgItem + i;
        u8 *itembuf = (u8*)inBuf + pItem->dataOffset;
        const int itemSz = pItem->dataSz;
        const char* itemName = pItem->name;

        //this item is random number
        if (!strcmp(itemName, "firmware")) {
            int desFd;
            if ((desFd = open(desAicPath, O_CREAT | O_RDWR | O_TRUNC, 0644)) < 0) {
                SYS_LOGE("unpack dhcp key, open %s error(%s)", desAicPath, strerror(errno));
                return false;
            }
            //write random number to destination aic file
            write(desFd, itembuf, itemSz);

            //origin firmware.aic append the end
            int srcFd = open(srcAicPath, O_RDONLY);
            int srcSize = lseek(srcFd, 0, SEEK_END);
            lseek(srcFd, 0, SEEK_SET);
            char *pSrcData = (char *)malloc(srcSize + 1);
            if (NULL == pSrcData) {
                SYS_LOGE("unpack dhcp key, can not malloc:%d memory\n", srcSize);
                close(desFd);
                close(srcFd);
                return false;
            }
            memset((void*)pSrcData, 0, srcSize + 1);
            read(srcFd, (void*)pSrcData, srcSize);
            //write source aic file data to destination aic file
            write(desFd, pSrcData, srcSize);

            close(srcFd);
            free(pSrcData);
            close(desFd);

            SYS_LOGI("unpack dhcp key, write random number -> (%s) done\n", desAicPath);
        }
        //this item is hdcp rx 22 key
        else if (!strcmp(itemName, "hdcp2_rx")) {
            int keyFd;
            if ((keyFd = open(keyPath, O_CREAT | O_RDWR | O_TRUNC, 0644)) < 0) {
                SYS_LOGE("unpack dhcp key, open %s error(%s)", keyPath, strerror(errno));
                return false;
            }

            //write key to file
            write(keyFd, itembuf, itemSz);
            close(keyFd);

            SYS_LOGI("unpack dhcp key, write key -> (%s) done\n", keyPath);
        }
    }

    return true;
}
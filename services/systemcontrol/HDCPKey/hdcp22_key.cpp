/*
 * \file        hdcp22_key.c
 * \brief
 *
 * \version     1.0.0
 * \date        16/03/9
 * \author      Sam.Wu <yihui.wu@amlgic.com>
 *
 * Copyright (c) 2016 Amlogic. All Rights Reserved.
 *
 */
#define LOG_TAG "SystemControl"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <utils/Log.h>
//#include "crc32.h"
#include "hdcp22_key.h"


#define HDCP_LOGD(...)  ALOGD(__VA_ARGS__)
#define HDCP_LOGE(x, ...)   ALOGE("[%s, %d] " x, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#define MAX_PATH 256

#define errorP HDCP_LOGE

#define COMPILE_TYPE_CHK(expr, t)       typedef char t[(expr) ? 1 : -1]

COMPILE_TYPE_CHK(AML_RES_IMG_HEAD_SZ == sizeof(AmlResImgHead_t), a);//assert the image header size 64
COMPILE_TYPE_CHK(AML_RES_ITEM_HEAD_SZ == sizeof(AmlResItemHead_t), b);//assert the item head size 64

#define IMG_HEAD_SZ     sizeof(AmlResImgHead_t)
#define ITEM_HEAD_SZ    sizeof(AmlResItemHead_t)
//#define ITEM_READ_BUF_SZ    (1U<<20)//1M
#define ITEM_READ_BUF_SZ    (64U<<10)//64K to test

static unsigned add_sum(const void* pBuf, const unsigned size, unsigned int sum)
{
    const unsigned* data = (const unsigned*)pBuf;
    unsigned wordLen 	 = size>>2;
    unsigned rest 		 = size & 3;

    for (; wordLen/4; wordLen -= 4)
    {
        sum += *data++;
        sum += *data++;
        sum += *data++;
        sum += *data++;
    }
    while (wordLen--)
    {
        sum += *data++;
    }

    if (rest == 0)
    {
        ;
    }
    else if(rest == 1)
    {
        sum += (*data) & 0xff;
    }
    else if(rest == 2)
    {
        sum += (*data) & 0xffff;
    }
    else if(rest == 3)
    {
        sum += (*data) & 0xffffff;
    }

    return sum;
}

//Generate crc32 value with file steam, which from 'offset' to end if checkSz==0
unsigned calc_img_crc(FILE* fp, off_t offset, unsigned checkSz)
{
    unsigned char* buf = NULL;
    unsigned MaxCheckLen = 0;
    unsigned totalLenToCheck = 0;
    const int oneReadSz = 12 * 1024;
    unsigned int crc = 0;

    if (fp == NULL) {
        fprintf(stderr,"bad param!!\n");
        return 0;
    }

    buf = (unsigned char*)malloc(oneReadSz);
    if (!buf) {
        errorP("Fail in malloc for sz %d\n", oneReadSz);
        return 0;
    }

    fseeko(fp, 0, SEEK_END);
    MaxCheckLen  = ftell(fp);
    MaxCheckLen -= offset;
    if (!checkSz) {
        checkSz = MaxCheckLen;
    }
    else if(checkSz > MaxCheckLen){
        fprintf(stderr, "checkSz %u > max %u\n", checkSz, MaxCheckLen);
        free(buf);
        return 0;
    }
    fseeko(fp,offset,SEEK_SET);

    while (totalLenToCheck < checkSz)
    {
        int nread;
        unsigned leftLen = checkSz - totalLenToCheck;
        int thisReadSz = leftLen > oneReadSz ? oneReadSz : leftLen;

        nread = fread(buf,1, thisReadSz, fp);
        if (nread < 0) {
            fprintf(stderr,"%d:read %s.\n", __LINE__, strerror(errno));
            free(buf);
            return 0;
        }
        crc = add_sum(buf, thisReadSz, crc);

        totalLenToCheck += thisReadSz;
    }

    free(buf);
    return crc;
}

static size_t get_filesize(const char *fpath)
{
    struct stat buf;
    if (stat(fpath, &buf) < 0) {
        HDCP_LOGE("Can't stat %s : %s\n", fpath, strerror(errno));
        return 0;
    }
    return buf.st_size;
}

/* *
 * @packedImg is /impdata/hdcp22_key2.2.bin
 * buffer size for @pbuf >= 10k
 * */
int aml_extract_one_item_to_buf(const char* packedImg, const char* itemName,
    char* datBuf, const unsigned dataBufSz, int* theItemSz)
{
    int ret = -1;
    const size_t fileSize = get_filesize(packedImg);
    if (!fileSize || fileSize > 11 * 1024) {
        HDCP_LOGE("file sz (%zd) of (%s) illegal\n", fileSize, packedImg);
        return ret;
    }

    AmlResImgHead_t *aAmlResImgHead;
    AmlResItemHead_t *pFirstItemHeadInfo;
    //size_t wantLen = IMG_HEAD_SZ + 4 * ITEM_HEAD_SZ; // one image head + 4 item size
    size_t readLen = 0;
    unsigned genCrc = 0;
    int i = 0;
    AmlResItemHead_t *pItemHeadInfo = NULL;
    char *imgFileBuf = NULL;
    char *itemBuf = NULL;
    FILE* fd = NULL;

    //read img file to buffer
    imgFileBuf = new char[fileSize + 1];
    if (!imgFileBuf) {
        HDCP_LOGE("Exception: fail to alloc buffer size:%d\n", fileSize + 1);
        goto _exit3;
    }

    fd = fopen(packedImg, "rb");
    if (!fd) {
        HDCP_LOGE("Fail in open packedImg(%s)\n", packedImg);
        goto _exit3;
    }

    readLen = fread(imgFileBuf, 1, fileSize, fd);
    if (readLen != fileSize) {
        HDCP_LOGE("Fail to read head in sz %d\n", fileSize);
        goto _exit3;
    }

    itemBuf = new char[ITEM_READ_BUF_SZ * 2];
    if (!itemBuf) {
        HDCP_LOGE("Exception: fail to alloc buffer size:\n", ITEM_READ_BUF_SZ * 2);
        goto _exit3;
    }
    memset(itemBuf, 0, ITEM_READ_BUF_SZ*2);

    aAmlResImgHead = (AmlResImgHead_t*)(itemBuf + ITEM_READ_BUF_SZ);
    pFirstItemHeadInfo = (AmlResItemHead_t*)(aAmlResImgHead + 1);
    /*
    retLen = fread(aAmlResImgHead, 1, wantLen, fd_src);
    if (retLen != wantLen) {
        HDCP_LOGE("Fail to read head in sz %d\n", wantLen);
        iRet = __LINE__; goto _exit3;
    }
    */

    // one image head + 4 item size = (24 + 4*48 = 216)
    memcpy(aAmlResImgHead, imgFileBuf, IMG_HEAD_SZ + 4 * ITEM_HEAD_SZ);

    //check crc value
    genCrc = calc_img_crc(fd, 4, 0);//Gen crc32
    if (genCrc != aAmlResImgHead->crc) {
        HDCP_LOGE("genCrc 0x%8x != oriCrc 0x%8x\n", genCrc, aAmlResImgHead->crc);
        goto _exit3;
    }
    fclose(fd);
    fd = NULL;
    //read img file end

    for (; i < (int)aAmlResImgHead->imgItemNum; ++i) {
        pItemHeadInfo = pFirstItemHeadInfo + i;
        if (0 == strcmp(itemName, pItemHeadInfo->name))
            break;
    }

    if (i == (int)aAmlResImgHead->imgItemNum) {
        HDCP_LOGE("Fail to find item name[%s]\n", itemName);
        goto _exit3;
    }

    if (pItemHeadInfo->dataSz > dataBufSz) {
        HDCP_LOGE("item size(%d) > buffer size (%d)\n", pItemHeadInfo->dataSz, dataBufSz);
        goto _exit3;
    }
    /*
    fseek(fd, pItemHeadInfo->dataOffset, SEEK_SET);
    retLen = fread(datBuf, 1, itemSz, fd);
    if (retLen != itemSz) {
        HDCP_LOGE("retLen %d != itemSz %d\n", retLen, itemSz);
        iRet = __LINE__; goto _exit3;
    }*/

    memcpy(datBuf, imgFileBuf + pItemHeadInfo->dataOffset, pItemHeadInfo->dataSz);
    *theItemSz = pItemHeadInfo->dataSz;
    ret = 0;

_exit3:
    if (imgFileBuf) delete[] imgFileBuf;
    if (itemBuf) delete[] itemBuf;
    if (fd) fclose(fd);
    return ret;
}

int generateHdcpFw(const char* firmwarele, const char* packedImg, const char* newFw)
{
    int iRet = -1;
    FILE* fd_src        = NULL;
    FILE* fd_dest       = NULL;

    //Copy file
    int rwLen = 0;
    int wantLen = 0;
    int itemSz = 0;

    HDCP_LOGD("generate dhcp rx2.2 firmware, le path:%s, packed image path:%s, des path:%s",
        firmwarele, packedImg, newFw);

    const size_t packedImgSz = get_filesize(packedImg);
    if (!packedImgSz || packedImgSz > 11 * 1024) {
        HDCP_LOGE("file sz (%zd) of (%s) illegal\n", packedImgSz, packedImg);
        return __LINE__;
    }

    char *itemBuf = new char[ITEM_READ_BUF_SZ];
    if (!itemBuf) {
        ALOGE("[%d] Exception: fail to alloc buuffer\n", __LINE__);
        return __LINE__;
    }

    fd_src = fopen(firmwarele, "rb");
    fd_dest = fopen(newFw, "wb");
    if (!fd_src || !fd_dest) {
        HDCP_LOGE("fail in open file(%s) or (%s)\n", firmwarele, newFw);
        goto _exit4;
    }

    do {
        rwLen = fread(itemBuf, 1, ITEM_READ_BUF_SZ, fd_src);
        if (!rwLen) break;
        wantLen = fwrite(itemBuf, 1, rwLen, fd_dest);
        if (wantLen != rwLen) {
            HDCP_LOGE("wantLen %d != rwLen %d\n", wantLen, rwLen);
            goto _exit4;
        }
    } while (rwLen);

    iRet = aml_extract_one_item_to_buf(packedImg, "extractedKey", itemBuf, ITEM_READ_BUF_SZ, &itemSz);
    if (iRet) {
        HDCP_LOGE("fail in extract item, ret =%d\n", iRet);
        goto _exit4;
    }

    fseek(fd_dest, 0x2800, SEEK_SET);
    wantLen = fwrite(itemBuf, 1, itemSz, fd_dest);
    if (wantLen != itemSz) {
        HDCP_LOGE("wantLen %d != itemSz %d\n", wantLen, itemSz);
        goto _exit4;
    }

_exit4:
    if (itemBuf) delete[] itemBuf;
    if (fd_src) fclose(fd_src);
    if (fd_dest) fclose(fd_dest);

    return 0;
}

int readSys(const char *path, char *buf, int count) {
    int fd, len = -1;

    if ( NULL == buf ) {
        HDCP_LOGE("buf is NULL");
        return len;
    }

    if ((fd = open(path, O_RDONLY)) < 0) {
        HDCP_LOGE("readSysFs, open %s fail.", path);
        return len;
    }

    len = read(fd, buf, count);
    if (len < 0) {
        HDCP_LOGE("read error: %s, %s\n", path, strerror(errno));
    }

    close(fd);
    return len;
}

void writeSys(const char *path, const char *val) {
    int fd;

    if ((fd = open(path, O_RDWR)) < 0) {
        HDCP_LOGE("writeSysFs, open %s fail.", path);
        return;
    }

    write(fd, val, strlen(val));
    close(fd);
}

//Generate crc32 value with file steam, which from 'offset' to end if checkSz==0
unsigned storage_calc_img_crc(char *pbuf, int bufLen, unsigned checkSz)
{
    unsigned int crc = 0;
    if (checkSz == 0) {
        checkSz = bufLen;
    }
    else if ((int)checkSz > bufLen) {
        HDCP_LOGE("checkSz %u > max %u\n", checkSz, bufLen);
        return 0;
    }

    crc = add_sum(pbuf, checkSz, crc);
    return crc;
}

/* *
 * @packedImg is /impdata/hdcp22_key2.2.bin
 * buffer size for @pbuf >= 10k
 * */
int storage_extract_one_item_to_buf(const char* itemName,
    char* datBuf, const unsigned dataBufSz, int* theItemSz)
{
    int KEY_SIZE = 2080;
    int ret = -1;

    AmlResImgHead_t *aAmlResImgHead;
    AmlResItemHead_t *pFirstItemHeadInfo;
    //size_t wantLen = IMG_HEAD_SZ + 4 * ITEM_HEAD_SZ; // one image head + 4 item size
    unsigned genCrc = 0;
    int index = 0;
    AmlResItemHead_t *pItemHeadInfo = NULL;
    char *keyBuf = NULL;
    char *itemBuf = NULL;

    //read key storage to buffer
    keyBuf = new char[KEY_SIZE + 1];
    if (!keyBuf) {
        HDCP_LOGE("Exception: fail to alloc buffer size:%d\n", KEY_SIZE + 1);
        goto _exit3;
    }

    writeSys("/sys/class/unifykeys/attach", "1");
    writeSys("/sys/class/unifykeys/name", "hdcp22_rx_fw");
    readSys("/sys/class/unifykeys/read", keyBuf, KEY_SIZE);
    //read key storage end

    itemBuf = new char[ITEM_READ_BUF_SZ * 2];
    if (!itemBuf) {
        HDCP_LOGE("Exception: fail to alloc buffer size:\n", ITEM_READ_BUF_SZ * 2);
        goto _exit3;
    }
    memset(itemBuf, 0, ITEM_READ_BUF_SZ*2);

    aAmlResImgHead = (AmlResImgHead_t*)(itemBuf + ITEM_READ_BUF_SZ);
    pFirstItemHeadInfo = (AmlResItemHead_t*)(aAmlResImgHead + 1);

    // one image head + 4 item size = (24 + 4*48 = 216)
    memcpy(aAmlResImgHead, keyBuf, IMG_HEAD_SZ + 4 * ITEM_HEAD_SZ);

    //check crc value
    genCrc = storage_calc_img_crc(keyBuf + 4, KEY_SIZE-4, 0);//Gen crc32
    if (genCrc != aAmlResImgHead->crc) {
        HDCP_LOGE("genCrc 0x%8x != oriCrc 0x%8x\n", genCrc, aAmlResImgHead->crc);
        goto _exit3;
    }

    for (; index < (int)aAmlResImgHead->imgItemNum; ++index) {
        pItemHeadInfo = pFirstItemHeadInfo + index;
        if (0 == strcmp(itemName, pItemHeadInfo->name))
            break;
    }

    if (index == (int)aAmlResImgHead->imgItemNum) {
        HDCP_LOGE("Fail to find item name[%s]\n", itemName);
        goto _exit3;
    }

    if (pItemHeadInfo->dataSz > dataBufSz) {
        HDCP_LOGE("item size(%d) > buffer size (%d)\n", pItemHeadInfo->dataSz, dataBufSz);
        goto _exit3;
    }

    memcpy(datBuf, keyBuf + pItemHeadInfo->dataOffset, pItemHeadInfo->dataSz);
    *theItemSz = pItemHeadInfo->dataSz;
    ret = 0;

_exit3:
    if (keyBuf) delete[] keyBuf;
    if (itemBuf) delete[] itemBuf;
    return ret;
}

int generateHdcpFwFromStorage(const char* firmwarele, const char* newFw)
{
    int iRet = 0;
    FILE* fd_src        = NULL;
    FILE* fd_dest       = NULL;

    //Copy file
    int keyLen = 0;
    int rwLen = 0;
    int wantLen = 0;
    int itemSz = 0;

    HDCP_LOGD("generate dhcp rx2.2 firmware, le path:%s, des path:%s", firmwarele, newFw);

    char *itemBuf = new char[ITEM_READ_BUF_SZ];
    if (!itemBuf) {
        ALOGE("[%d] Exception: fail to alloc buffer\n", __LINE__);
        return -1;
    }

    fd_src = fopen(firmwarele, "rb");
    if (!fd_src) {
        HDCP_LOGE("fail in open file(%s)\n", firmwarele);
        iRet = -1;
        goto _exit4;
    }

    fd_dest = fopen(newFw, "wb");
    if (!fd_dest) {
        HDCP_LOGE("fail in open file(%s)\n", newFw);
        iRet = -1;
        goto _exit4;
    }

    do {
        rwLen = fread(itemBuf, 1, ITEM_READ_BUF_SZ, fd_src);
        if (!rwLen) break;
        wantLen = fwrite(itemBuf, 1, rwLen, fd_dest);
        if (wantLen != rwLen) {
            HDCP_LOGE("wantLen %d != rwLen %d\n", wantLen, rwLen);
            iRet = -1;
            goto _exit4;
        }
    } while (rwLen);

#if 0
    iRet = storage_extract_one_item_to_buf("extractedKey", itemBuf, ITEM_READ_BUF_SZ, &itemSz);
    if (iRet) {
        HDCP_LOGE("fail in extract item, ret =%d\n", iRet);
        goto _exit4;
    }
#else
    itemSz = 2080;
    writeSys("/sys/class/unifykeys/attach", "1");
    writeSys("/sys/class/unifykeys/name", "hdcp22_rx_fw");
    keyLen = readSys("/sys/class/unifykeys/read", itemBuf, itemSz);
    if (keyLen < itemSz) {
        HDCP_LOGE("read key length fail, request len = %d, read len = %d\n", itemSz, keyLen);
        iRet = -1;
        goto _exit4;
    }
    //for (int i = 0; i < itemSz; i++)
    //    HDCP_LOGD("read key [%d] = 0x%x\n", i, itemBuf[i]);
    //read key storage end
#endif//#ifdef IMPDATA_HDCP_RX_KEY

    fseek(fd_dest, 0x2800, SEEK_SET);
    wantLen = fwrite(itemBuf, 1, itemSz, fd_dest);
    if (wantLen != itemSz) {
        HDCP_LOGE("wantLen %d != itemSz %d\n", wantLen, itemSz);
        iRet = -1;
        goto _exit4;
    }

_exit4:
    if (itemBuf) delete[] itemBuf;
    if (fd_src) fclose(fd_src);
    if (fd_dest) fclose(fd_dest);

    return iRet;
}


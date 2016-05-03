/*
 * \file        hdcp22_key.h
 * \brief
 *
 * \version     1.0.0
 * \date        16/03/9
 * \author      Sam.Wu <yihui.wu@amlgic.com>
 *
 * Copyright (c) 2016 Amlogic. All Rights Reserved.
 *
 */

#ifndef __HDCP22_HEY_H__
#define __HDCP22_HEY_H__

//old path use for tcl
#define HDCP_FW_LE_OLD_PATH         "/system/etc/firmware.le"
#define HDCP_PACKED_IMG_PATH        "/impdata/hdcp_key2.0.bin"
#define HDCP_NEW_KEY_CREATED        "/impdata/newkeyCreated"

#define HDCP_RX_SRC_FW_PATH         "/system/etc/firmware/hdcp_rx22/firmware.le"
#define HDCP_RX_DES_FW_PATH         "/param/firmware.le"

typedef unsigned int        __u32;
typedef signed int          __s32;
typedef unsigned char       __u8;
typedef signed char         __s8;

#define IH_MAGIC	0x27051956	/* Image Magic Number		*/
#define IH_NMLEN		32	/* Image Name Length		*/


#define AML_RES_IMG_ITEM_ALIGN_SZ   16
#define AML_RES_IMG_V1_MAGIC_LEN    8
#define AML_RES_IMG_V1_MAGIC        "AML_HDK!"//8 chars
#define AML_RES_IMG_HEAD_SZ         (24)//64
#define AML_RES_ITEM_HEAD_SZ        (48)//64

#define AML_RES_IMG_VERSION_V1      (0x01)

#pragma pack(push, 1)
typedef struct pack_header{
	unsigned int	totalSz;/* Item Data total Size*/
	unsigned int	dataSz;	/* Item Data used  Size*/
	unsigned int	dataOffset;	/* Item data offset*/
	unsigned char   type;	/* Image Type, not used yet*/
	unsigned char 	comp;	/* Compression Type	*/
    unsigned short  reserv;
	char 	name[IH_NMLEN];	/* Image Name		*/
}AmlResItemHead_t;
#pragma pack(pop)

//typedef for amlogic resource image
#pragma pack(push, 4)
typedef struct {
    __u32   crc;    //crc32 value for the resouces image
    __s32   version;//0x01 means 'AmlResItemHead_t' attach to each item , 0x02 means all 'AmlResItemHead_t' at the head

    __u8    magic[AML_RES_IMG_V1_MAGIC_LEN];  //resources images magic

    __u32   imgSz;  //total image size in byte
    __u32   imgItemNum;//total item packed in the image

}AmlResImgHead_t;
#pragma pack(pop)

/*The Amlogic resouce image is consisted of a AmlResImgHead_t and many
 *
 * |<---AmlResImgHead_t-->|<--AmlResItemHead_t-->---...--|<--AmlResItemHead_t-->---...--|....
 *
 */

int aml_hdcp22_key_pack(const char** const path_src, const int totalFileNum, const char* const packedImg);

int aml_hdcp22_extract_firmwarele(const char* firmwarele, const char* extractedKey);

int generateHdcpFw(const char* firmwarele, const char* packedImg, const char* newFw);
int generateHdcpFwFromStorage(const char* firmwarele, const char* newFw);

#endif// #ifndef __HDCP22_HEY_H__


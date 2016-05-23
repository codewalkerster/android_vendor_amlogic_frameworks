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

#ifndef HDCP_RX22_KEY_H
#define HDCP_RX22_KEY_H

#include "common.h"

#define HDCP_RX_TOOL_KEYS               "hdcprxkeys"
#define HDCP_RX_TOOL_ESM_SWAP           "esm_swap"
#define HDCP_RX_TOOL_AIC                "aictool"
#define HDCP_RX_KEY_FORMAT              "-b"

//system/etc/firmware/hdcp_rx22/esm_config.i
//system/etc/firmware/hdcp_rx22/firmware.rom
//system/etc/firmware/hdcp_rx22/firmware.aic
//esm_config.i, firmware.rom, hdcp_keys.le, used by firmware.aic
#define HDCP_RX_CFG_AIC_SRC             "/system/etc/firmware/hdcp_rx22/firmware.aic"
#define HDCP_RX_CFG_AIC_DES             "/param/firmware.aic"
#define HDCP_RX_KEY_PATH                "/param/hdcp2_rx.bin"

#define HDCP_RX_OUT_KEY_IMG             "/param/dcp_rx.out"
#define HDCP_RX_OUT_KEY_LE              "/param/hdcp_keys.le"
#define HDCP_RX_OUT_FW_LE               "/param/firmware.le"

#define HDCP_RX_KEY_CRC_PATH            "/param/hdcprx22.crc"
#define HDCP_RX_KEY_CRC_LEN             4

#define HDCP_RX_STORAGE_KEY_SIZE        (10U<<10)//10K

class HdcpRx22Key
{
public:
    HdcpRx22Key();
    virtual ~HdcpRx22Key();

    bool generateHdcpRxFw();

private:
    bool aicTool();
    bool esmSwap();
    bool genKeyImg();
    int writeSys(const char *path, const char *val);
    int readSys(const char *path, char *buf, int count);
    int saveFile(const char *path, const char *buf, int bufLen);
};

// ----------------------------------------------------------------------------
#endif // HDCP_RX22_KEY_H
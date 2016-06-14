/*
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#ifndef __CSERIAL_STREAM__
#define __CSERIAL_STREAM__

#include "CFile.h"
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <pthread.h>
#include <termios.h>
#include <errno.h>


static const int speed_arr[] = {B115200, B38400, B19200, B9600, B4800, B2400, B1200, B300, B38400, B19200, B9600, B4800, B2400, B1200, B300};
static const int name_arr[] = { 115200, 38400, 19200, 9600, 4800, 2400, 1200, 300, 38400, 19200, 9600, 4800, 2400, 1200, 300};
static const char *DEV_PATH_S0 = "/dev/ttyS0";
static const char *DEV_PATH_S1 = "/dev/ttyS1";
static const char *DEV_PATH_S2 = "/dev/ttyS2";

enum SerialDeviceID {
    SERIAL_A = 0,
    SERIAL_B,
    SERIAL_C,
};

class CSerialPort: public CFile {
public:
    CSerialPort();
    ~CSerialPort();

    int OpenDevice(int serial_dev_id);
    int CloseDevice();

    int writeFile(const unsigned char *pData, unsigned int uLen);
    int readFile(unsigned char *pBuf, unsigned int uLen);
    int set_opt(int speed, int db, int sb, char pb, int overtime, bool raw_mode);
    int setup_serial();
    unsigned int Calcrc32(unsigned int crc, const unsigned char *ptr, unsigned int buf_len);
    int getDevId()
    {
        return mDevId;
    };

private:
    int setdatabits(struct termios *s, int db);
    int setstopbits(struct termios *s, int sb);
    int setparity(struct termios *s, char pb);
    int set_Parity (int fd, int databits, int stopbits, int parity);
    void set_speed (int fd, int speed);

    int mDevId;
};

#endif

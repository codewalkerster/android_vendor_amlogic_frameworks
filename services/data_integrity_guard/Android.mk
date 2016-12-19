# Copyright 2005 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	DigManager.cpp \
        main.cpp


LOCAL_CFLAGS += -DUSE_KERNEL_LOG -DDIG_TEST

LOCAL_MODULE:= digtest

LOCAL_FORCE_STATIC_EXECUTABLE := true

LOCAL_STATIC_LIBRARIES := libcutils libc\
	libcrypto_static \
	libext4_utils_static \
	libsparse_static \
	libz\
        libselinux\
        liblog

LOCAL_C_INCLUDES += system/extras/ext4_utils \
                    system/vold

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
        DigManager.cpp

LOCAL_CFLAGS += -DUSE_KERNEL_LOG

LOCAL_MODULE:= libdig

LOCAL_C_INCLUDES += system/extras/ext4_utils \
                    system/vold

LOCAL_SHARED_LIBRARIES := libcutils libc\
        libcrypto \
        libext4_utils \
        libsparse \
        libz\
        libselinux \
        libsysutils

LOCAL_STATIC_LIBRARIES:= libvold

include $(BUILD_SHARED_LIBRARY)

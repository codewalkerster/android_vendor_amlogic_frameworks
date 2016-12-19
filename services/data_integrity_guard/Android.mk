# Copyright 2005 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	data_integrity_guard.c

LOCAL_CFLAGS += -DUSE_KERNEL_LOG

LOCAL_MODULE:= dig

LOCAL_FORCE_STATIC_EXECUTABLE := true

LOCAL_STATIC_LIBRARIES := libcutils libc\
	libcrypto_static \
	libext4_utils_static \
	libsparse_static \
	libz\
        libselinux

include $(BUILD_EXECUTABLE)


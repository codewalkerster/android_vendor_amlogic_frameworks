LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	ISystemControlService.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libbinder

LOCAL_MODULE:= libsystemcontrolservice

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

ifeq ($(TARGET_BOARD_PLATFORM), meson8)
LOCAL_CFLAGS += -DMESON8_ENVSIZE
endif

ifeq ($(TARGET_BOARD_PLATFORM), gxbaby)
LOCAL_CFLAGS += -DGXBABY_ENVSIZE
endif

LOCAL_SRC_FILES:= \
  main_systemcontrol.cpp \
  ubootenv.c \
  VdcLoop.c \
  SysWrite.cpp \
  SystemControl.cpp \
  DisplayMode.cpp \
  SysTokenizer.cpp

LOCAL_SHARED_LIBRARIES := \
  libsystemcontrolservice \
  libcutils \
  libutils \
  liblog \
  libbinder \
  libm

LOCAL_C_INCLUDES := \
  external/zlib \
  external/libcxx/include

LOCAL_MODULE:= systemcontrol

LOCAL_STATIC_LIBRARIES := \
	libz

include $(BUILD_EXECUTABLE)


# build for recovery mode
# =========================================================
include $(CLEAR_VARS)

ifeq ($(TARGET_BOARD_PLATFORM), meson8)
LOCAL_CFLAGS += -DMESON8_ENVSIZE
endif

ifeq ($(TARGET_BOARD_PLATFORM), gxbaby)
LOCAL_CFLAGS += -DGXBABY_ENVSIZE
endif

LOCAL_CFLAGS += -DRECOVERY_MODE

LOCAL_SRC_FILES:= \
	main_recovery.cpp \
	ubootenv.c \
	SysWrite.cpp \
	DisplayMode.cpp \
	SysTokenizer.cpp

LOCAL_STATIC_LIBRARIES := \
	libcutils \
	liblog \
	libz \
	libc \
	libm

LOCAL_C_INCLUDES := \
    external/zlib \
    external/libcxx/include

LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_MODULE_PATH := $(PRODUCT_OUT)/utilities
LOCAL_MODULE:= systemcontrol_static

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

ifeq ($(TARGET_BOARD_PLATFORM), meson8)
LOCAL_CFLAGS += -DMESON8_ENVSIZE
endif

ifeq ($(TARGET_BOARD_PLATFORM), gxbaby)
LOCAL_CFLAGS += -DGXBABY_ENVSIZE
endif

LOCAL_CFLAGS += -DRECOVERY_MODE

LOCAL_SRC_FILES:= \
	main_recovery.cpp \
	ubootenv.c \
	SysWrite.cpp \
	DisplayMode.cpp \
	SysTokenizer.cpp

LOCAL_STATIC_LIBRARIES := \
	libcutils \
	liblog \
	libz \
	libc \
	libm

LOCAL_C_INCLUDES := \
    external/zlib \
    external/libcxx/include

LOCAL_MODULE:= libsystemcontrol_static

include $(BUILD_STATIC_LIBRARY)

LOCAL_PATH:= $(call my-dir)

#
# libimageplayerservice
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
  IImagePlayerService.cpp

LOCAL_SHARED_LIBRARIES := \
  libutils \
  libcutils \
  libbinder

LOCAL_MODULE:= libimageplayerservice

include $(BUILD_SHARED_LIBRARY)

# build for image server
# =========================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
  main_imageserver.cpp \
  ImagePlayerService.cpp  \
  RGBPicture.c

LOCAL_SHARED_LIBRARIES := \
  libimageplayerservice \
  libbinder                   \
  libskia                     \
  libcutils                   \
  libutils                    \
  liblog                      \
  libdl                       \
  libstagefright              \
  libsystemcontrolservice

LOCAL_C_INCLUDES += \
  external/skia/include/core \
  external/skia/include/effects \
  external/skia/include/images \
  external/skia/src/ports \
  external/skia/include/utils \
  frameworks/av/include \
  vendor/amlogic/frameworks/services/systemcontrol

LOCAL_MODULE:= imageserver

include $(BUILD_EXECUTABLE)

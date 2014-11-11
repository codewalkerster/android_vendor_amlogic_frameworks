LOCAL_PATH:= $(call my-dir)

#
# libimageplayerservice
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=               \
    IImagePlayerService.cpp \
    ImagePlayerService.cpp  \
    RGBPicture.c

LOCAL_SHARED_LIBRARIES :=       \
    libbinder                   \
    libskia                     \
    libcutils                   \
    libutils                    \
    liblog                      \
    libdl                       \
    libstagefright

LOCAL_C_INCLUDES += \
	external/skia/include/core \
	external/skia/include/effects \
	external/skia/include/images \
	external/skia/src/ports \
	external/skia/include/utils \
	frameworks/av/include

LOCAL_MODULE:= libimageplayerservice

include $(BUILD_SHARED_LIBRARY)

# build for image server
# =========================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main_imageserver.cpp

LOCAL_SHARED_LIBRARIES := \
	libimageplayerservice \
	libutils \
	liblog \
	libbinder

LOCAL_C_INCLUDES := \
  external/skia/include/core \
	external/skia/include/effects \
	external/skia/include/images \
  frameworks/av/media/libimageplayerservice

LOCAL_MODULE:= imageserver

include $(BUILD_EXECUTABLE)

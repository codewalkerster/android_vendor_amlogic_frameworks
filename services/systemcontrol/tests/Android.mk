LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	systemcontroltest.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils  \
	libbinder \
	libsystemcontrolservice

LOCAL_MODULE:= test-systemcontrol

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

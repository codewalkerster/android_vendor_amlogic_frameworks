
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_PACKAGE_NAME := droidlogic-res
LOCAL_CERTIFICATE := platform

# Tell aapt to create "extending (non-application)" resource IDs,
# since these resources will be used by many apps.
LOCAL_AAPT_FLAGS := -x

LOCAL_MODULE_TAGS := optional

# Install this alongside the libraries.
# Install to the system\framework\droidlogic-res
#LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)

# Install thie to system/priv-app
LOCAL_PRIVILEGED_MODULE := true

# Create package-export.apk, which other packages can use to get
# PRODUCT-agnostic resource data like IDs and type definitions.
LOCAL_EXPORT_PACKAGE_RESOURCES := true

include $(BUILD_PACKAGE)



LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# Include res dir from chips
chips_dir := ../../../frameworks/ex/chips/res
res_dirs := $(chips_dir) res

LOCAL_EMMA_COVERAGE_FILTER := +io.n7.calendar.*

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under,src)

# bundled
#LOCAL_STATIC_JAVA_LIBRARIES += \
#               android-common \
#               android-common-chips \
#               calendar-common

# unbundled
LOCAL_STATIC_JAVA_LIBRARIES := \
                android-common \
                android-common-chips \
                calendar-common \
                libccodec \
                libclang \
                libchttpc \
                libwebdav \
                libbkport \
                libehcache
                
LOCAL_SDK_VERSION := current

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_PACKAGE_NAME := CalendarN7

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.ex.chips

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libccodec:lib/commons-codec-1.3.jar \
                                        libclang:lib/commons-lang-2.4.jar \
                                        libchttpc:lib/commons-httpclient-3.0.jar \
                                        libwebdav:lib/jakarta-slide-webdavlib-2.2pre1-httpclient-3.0.jar \
                                        libbkport:lib/backport-util-concurrent.jar \
                                        libehcache:lib/ehcache-1.2.jar \

#include $(BUILD_MULTI_PREBUILT)

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

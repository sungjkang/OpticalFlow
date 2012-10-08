LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

OPENCV_LIB_TYPE:=STATIC
OPENCV_INSTALL_MODULES:=on
include /home/sjk/opencv-android/sdk/native/jni/OpenCV.mk
LOCAL_STATIC_LIBRARIES += openc_core


LOCAL_MODULE    := opticalflow
LOCAL_CFLAGS    := -Werror -Wno-psabi
LOCAL_SRC_FILES := optical_flow.cpp
LOCAL_LDLIBS    += -llog -ldl

include $(BUILD_SHARED_LIBRARY)
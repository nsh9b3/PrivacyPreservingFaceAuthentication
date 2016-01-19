LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# OpenCV
OPENCV_CAMERA_MODULES   := on
OPENCV_INSTALL_MODULES  := on
include /home/nick/Android/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
# OpenCV

LOCAL_LDLIBS    := -llog
LOCAL_MODULE    := face_detection
LOCAL_SRC_FILES := DetectionBasedTracker.cpp LBPHistogram.cpp

include $(BUILD_SHARED_LIBRARY)
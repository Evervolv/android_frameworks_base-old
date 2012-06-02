LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	ISensorEventConnection.cpp \
	ISensorServer.cpp \
	ISurfaceTexture.cpp \
	Sensor.cpp \
	SensorChannel.cpp \
	SensorEventQueue.cpp \
	SensorManager.cpp \
	SurfaceTexture.cpp \
	SurfaceTextureClient.cpp \
	ISurfaceComposer.cpp \
	ISurface.cpp \
	ISurfaceComposerClient.cpp \
	IGraphicBufferAlloc.cpp \
	LayerState.cpp \
	Surface.cpp \
	SurfaceComposerClient.cpp \

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder \
	libhardware \
	libhardware_legacy \
	libui \
	libEGL \
	libGLESv2

ifeq ($(BOARD_USES_QCOM_HARDWARE),true)

ifeq ($(BOARD_USES_LEGACY_QCOM),true)
	# Legacy gralloc cannot handle this flag: undefine it
	LOCAL_CFLAGS += -UQCOM_HARDWARE
else
	LOCAL_SHARED_LIBRARIES += libQcomUI
	LOCAL_C_INCLUDES := hardware/qcom/display/libqcomui
endif

ifeq ($(TARGET_QCOM_HDMI_OUT),true)
	LOCAL_CFLAGS += -DQCOM_HDMI_OUT
endif

endif # QCOM_HARDWARE

LOCAL_MODULE:= libgui

ifeq ($(TARGET_BOARD_PLATFORM), tegra)
	LOCAL_CFLAGS += -DALLOW_DEQUEUE_CURRENT_BUFFER
endif

ifeq ($(BOARD_ADRENO_DECIDE_TEXTURE_TARGET),true)
    LOCAL_CFLAGS += -DDECIDE_TEXTURE_TARGET
    ifeq ($(BOARD_ADRENO_AVOID_EXTERNAL_TEXTURE),true)
        LOCAL_CFLAGS += -DCHECK_FOR_EXTERNAL_FORMAT
    endif
endif

include $(BUILD_SHARED_LIBRARY)

ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call first-makefiles-under,$(LOCAL_PATH))
endif

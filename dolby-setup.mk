# Default path to the Dolby directory
DOLBY_PATH := vendor/dolby

# SEPolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DOLBY_PATH)/sepolicy/vendor

# HIDL
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += $(DOLBY_PATH)/hidl/dolby_framework_matrix.xml
PRODUCT_PACKAGES += \
	dms-service.xml \
	vendor.dolby.media.c2@1.0-service.xml \
	vendor.dolby.hardware.dms.xml

# Dolby Audio media codecs (AC3, EAC3, EAC3-JOC, AC4)
ifeq ($(TARGET_SUPPORTS_DOLBY_CODECS),)
TARGET_SUPPORTS_DOLBY_CODECS := true
endif

ifeq ($(TARGET_SUPPORTS_DOLBY_CODECS),true)
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/media/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml
endif

# Inherit proprietary targets
$(call inherit-product, $(DOLBY_PATH)/prebuilts/prebuilts-vendor.mk)

# Dolby props
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.dolby.dax.version=DAX3_3.11.0.10_r2 \
	persist.vendor.audio_fx.current=dolby

# DAX config
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/soundfx/dax-default.xml:$(TARGET_COPY_OUT_VENDOR)/etc/dolby/dax-default.xml

# Dolby Vision
ifeq ($(TARGET_SUPPORTS_DOVI),true)
# SEPolicy
BOARD_VENDOR_SEPOLICY_DIRS += \
    $(DOLBY_PATH)/sepolicy/vendor/vision
# HIDL
PRODUCT_PACKAGES += \
    vendor.dovi.media.c2@1.0-service.xml \
    vendor.dolby.media.dvs-service.xml
# Override supported HDR types to include Dolby Vision
PRODUCT_PACKAGES += \
    DoviParts
# Dolby Vision media codecs
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/media/media_codecs_dolby_vision.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_vision.xml
# Supress spam logs
PRODUCT_VENDOR_PROPERTIES +=  \
    persist.log.tag.qdgralloc=S \
    persist.log.tag.DisplayManagementConfig=S
endif

# Media codecs (Includes Dolby Audio & Vision codecs)
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/media/media_codecs_vendor.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby.xml

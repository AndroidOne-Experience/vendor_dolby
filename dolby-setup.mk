# Default path to the Dolby directory
DOLBY_PATH := vendor/dolby

# SEPolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DOLBY_PATH)/sepolicy/vendor

# HIDL
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += $(DOLBY_PATH)/hidl/dolby_framework_matrix.xml
PRODUCT_PACKAGES += \
	dms-service.xml \
	vendor.dolby.media.c2@1.0-service.xml

# Dolby Audio media codecs (AC3, EAC3, EAC3-JOC, AC4)
ifeq ($(TARGET_SUPPORTS_DOLBY_CODECS),)
TARGET_SUPPORTS_DOLBY_CODECS := true
endif

ifeq ($(TARGET_SUPPORTS_DOLBY_CODECS),true)
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/media/media_codecs_vendor.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs.xml
endif

# Inherit proprietary targets
$(call inherit-product, $(DOLBY_PATH)/prebuilts/prebuilts-vendor.mk)

# Dolby props
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.dolby.dax.version=DAX3_3.11.0.10_r2


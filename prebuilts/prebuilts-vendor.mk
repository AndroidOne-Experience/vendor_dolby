#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/dolby/prebuilts

PRODUCT_COPY_FILES += \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/dms-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/dms-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.c2@1.0-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml

PRODUCT_PACKAGES += \
    libdapparamstorage \
    libdmshal \
    vendor.dolby.dms-V1-ndk \
    vendor.dolby.hardware.dms@2.0 \
    vendor.dolby.hardware.dms@2.1 \
    libcodec2_soft_ac4dec \
    libcodec2_soft_ddpdec \
    libcodec2_store_dolby \
    libdeccfg \
    libdlbdsservice \
    vendor.dolby.dms.service \
    vendor.dolby.media.c2@1.0-service

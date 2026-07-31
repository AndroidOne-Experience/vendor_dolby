#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/dolby/prebuilts

PRODUCT_COPY_FILES += \
    vendor/dolby/prebuilts/proprietary/vendor/etc/dolby_vision.cfg:$(TARGET_COPY_OUT_VENDOR)/etc/dolby_vision.cfg \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/dms-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/dms-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.hardware.dms@2.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.hardware.dms@2.0-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.c2@1.0-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.media.dvs-service-vision.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.dvs-service-vision.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolbyvision.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolbyvision.media.c2@1.0-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml

PRODUCT_PACKAGES += \
    libdapparamstorage-sony \
    libdapparamstorage \
    libdlbpreg \
    libdmshal \
    libdlbvol \
    libswdap \
    libswgamedap \
    libswvqe \
    vendor.dolby.dms-V1-ndk \
    vendor.dolby.hardware.dms@2.0-sony \
    vendor.dolby.hardware.dms@2.0 \
    vendor.dolby.hardware.dms@2.1 \
    c2.dolby.avc.dec \
    c2.dolby.avc.sec.dec \
    c2.dolby.client \
    c2.dolby.egl \
    c2.dolby.hevc.dec \
    c2.dolby.hevc.enc \
    c2.dolby.hevc.sec.dec \
    c2.dolby.store \
    libcodec2_soft_ac4dec \
    libcodec2_soft_ddpdec \
    libcodec2_store_dolby \
    libdeccfg \
    libdlbdsservice-sony \
    libdlbdsservice \
    libdolbyottcameracontrol \
    libdolbyvision \
    vendor.dolby.dvs@1.0 \
    vendor.dolby.hardware.dms@2.0-impl \
    dolbycodec2 \
    dvs-hal-service \
    vendor.dolby.dms.service \
    vendor.dolby.hardware.dms@2.0-service \
    vendor.dolby.media.c2@1.0-service

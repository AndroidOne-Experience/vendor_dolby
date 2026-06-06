#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/dolby/prebuilts

PRODUCT_COPY_FILES += \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/com.motorola.frameworks.core.addon.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/com.motorola.frameworks.core.addon.xml \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/com.motorola.motosignature.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/com.motorola.motosignature.xml \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/moto-checkin.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/moto-checkin.xml \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/moto-core_services.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/moto-core_services.xml \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/moto-settings.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/moto-settings.xml \
    vendor/dolby/prebuilts/proprietary/system/etc/permissions/motoaudioeffectsdk.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/permissions/motoaudioeffectsdk.xml \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/dms-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/dms-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.hardware.dms@2.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.hardware.dms@2.0-service.rc \
    vendor/dolby/prebuilts/proprietary/vendor/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.c2@1.0-service.rc \
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
    libcodec2_soft_ac4dec \
    libcodec2_soft_ddpdec \
    libcodec2_store_dolby \
    libdeccfg \
    libdlbdsservice-sony \
    libdlbdsservice \
    vendor.dolby.hardware.dms@2.0-impl \
    MotoSignature2App \
    MotoSignatureApp \
    MotorolaSettingsProvider \
    com.motorola.frameworks.core.addon \
    com.motorola.motosignature \
    moto-checkin \
    moto-core_services \
    moto-settings \
    motoaudioeffectsdk \
    vendor.dolby.dms.service \
    vendor.dolby.hardware.dms@2.0-service \
    vendor.dolby.media.c2@1.0-service

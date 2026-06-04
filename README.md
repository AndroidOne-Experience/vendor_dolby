<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/DLB_Atms_UI_rgb_wht_@2x.png">
    <source media="(prefers-color-scheme: light)" srcset="assets/DLB_Atms_UI_rgb_blk_@2x.png">
    <img alt="Dolby Atmos" src="assets/DLB_Atms_UI_rgb_blk_@2x.png" width="260">
  </picture>

  <br>
  <br>
  
  Bring Dolby EQ & Dolby Audio codec support to your Android build.
</div>

---

## Clone the Repository

Clone the Dolby vendor repository into your source tree:

```bash
git clone https://github.com/AndroidOne-Experience/vendor_dolby.git vendor/dolby --depth=1 -b v1.1-motorola
```

---

## Device Tree Integration

Inherit Dolby Atmos in your device configuration:

```makefile
$(call inherit-product, vendor/dolby/dolby-setup.mk)
```

---

## Audio Effects Configuration

Add the following entries to your `audio_effects.xml`.

### Load the Libraries

```xml
<!-- DOLBY DAP -->
<library name="dap" path="libswdap.so"/>
<library name="dvl" path="libdlbvol.so"/>
<!-- DOLBY GAME -->
<library name="gamedap" path="libswgamedap.so"/>
<!-- DOLBY VQE -->
<library name="vqe" path="libswvqe.so"/>
<!-- DOLBY END -->
```

### Load the Effects

```xml
<!-- DOLBY DAP -->
<effect name="dap" library="dap" uuid="9d4921da-8225-4f29-aefa-39537a04bcaa"/>
<effect name="dlb_music_listener" library="dvl" uuid="40f66c8b-5aa5-4345-8919-53ec431aaa98"/>
<effect name="dlb_ring_listener" library="dvl" uuid="21d14087-558a-4f21-94a9-5002dce64bce"/>
<effect name="dlb_alarm_listener" library="dvl" uuid="6aff229c-30c6-4cc8-9957-dbfe5c1bd7f6"/>
<effect name="dlb_system_listener" library="dvl" uuid="874db4d8-051d-4b7b-bd95-a3bebc837e9e"/>
<effect name="dlb_notification_listener" library="dvl" uuid="1f0091e3-6ad8-40fe-9b09-5948f9a26e7e"/>
<effect name="dlb_voice_call_listener" library="dvl" uuid="58d13383-b41d-05df-d94e-bb23db293260"/>
<!-- DOLBY GAME -->
<effect name="gamedap" library="gamedap" uuid="3783c334-d3a0-4d13-874f-0032e5fb80e2"/>
<!-- DOLBY VQE -->
<effect name="vqe" library="vqe" uuid="64a0f614-7fa4-48b8-b081-d59dc954616f"/>
<!-- DOLBY END -->
```

---

## Dolby Codec Support

By default, this repository includes support for **Dolby Audio codecs**.

If you do not want Dolby Audio media codecs support, add the following flag to your device configuration:

```makefile
TARGET_SUPPORTS_DOLBY_CODECS := false
```

<div align="center">

**Supported Codecs**

AC3 • EAC3 • EAC3-JOC • AC4

> Dolby Audio media codecs require Android 15 or later. Binaries for Android versions below 15 are not compatible.

</div>

---

## Credits

* [Dolby Laboratories](https://www.dolby.com/en-in/)

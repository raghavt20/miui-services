package com.android.server.audio.dolbyeffect;

/* loaded from: classes.dex */
public class DeviceId {
    public static final String BLUETOOTH_AIRDOTS_3_PRO = "bluetooth_AirDots_3_Pro";
    public static final String BLUETOOTH_AIR_2_SE = "bluetooth_Air_2_SE";
    public static final String BLUETOOTH_DEFAULT = "default_bluetooth";
    public static final String BLUETOOTH_REDMI_BUDS_4_PRO = "bluetooth_Redmi_Buds_4_Pro";
    public static final String BLUETOOTH_SPATIALIZER = "bluetooth_spatializer";
    public static final String SPK_DEFAULT = "default_internal_speaker";
    public static final String SPK_SPATIALIZER = "speaker_spatializer_90";
    public static final String SPK_SPATIALIZER_270 = "speaker_spatilizer_270";
    public static final String SPK_SPATIALIZER_270_VOLUME_HIGH = "speaker_spatilizer_270_volume_high";
    public static final String SPK_SPATIALIZER_270_VOLUME_LOW = "speaker_spatilizer_270_volume_low";
    public static final String SPK_SPATIALIZER_90 = "speaker_spatilizer_90";
    public static final String SPK_SPATIALIZER_90_VOLUME_HIGH = "speaker_spatilizer_90_volume_high";
    public static final String SPK_SPATIALIZER_90_VOLUME_LOW = "speaker_spatilizer_90_volume_low";
    public static final String SPK_VOLUME_HIGH = "speaker_volume_high";
    public static final String SPK_VOLUME_LOW = "speaker_volume_low";
    public static final String USB_DEFAULT = "default_usb";
    public static final String USB_SPATIALIZER = "headphone_spatializer";
    public static final String WIRED_DEFAULT = "default_headphone";
    public static final String WIRED_SPATIALIZER = "headphone_spatializer";

    public static String getBtDeviceId(int majorId, int minorId) {
        if (majorId == 65808 && (minorId == 1 || minorId == 2 || minorId == 3 || minorId == 7)) {
            return BLUETOOTH_REDMI_BUDS_4_PRO;
        }
        if (majorId == 65801 && minorId == 6) {
            return BLUETOOTH_AIRDOTS_3_PRO;
        }
        if (majorId == 65793 && minorId == 0) {
            return BLUETOOTH_AIR_2_SE;
        }
        return BLUETOOTH_DEFAULT;
    }

    public static String getUsbDeviceId(int vendorId, int productId) {
        return USB_DEFAULT;
    }

    public static String getWiredDeviceId(int vendorId, int productId) {
        return WIRED_DEFAULT;
    }
}

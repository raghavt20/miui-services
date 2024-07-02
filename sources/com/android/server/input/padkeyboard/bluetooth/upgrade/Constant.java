package com.android.server.input.padkeyboard.bluetooth.upgrade;

import java.util.UUID;

/* loaded from: classes.dex */
public class Constant {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    public static final byte DFU_CTRL_CAL_CHECKSUM = 3;
    public static final byte DFU_CTRL_CREATE = 1;
    public static final byte DFU_CTRL_EXECTUE = 4;
    public static final byte DFU_CTRL_RESERVE = 5;
    public static final byte DFU_CTRL_RESPONSE = 96;
    public static final byte DFU_CTRL_SELECT = 6;
    public static final byte DFU_CTRL_SET_PRN = 2;
    public static final byte DFU_FILE_BIN = 2;
    public static final byte DFU_FILE_DAT = 1;
    public static final byte DFU_SUCCESS = 1;
    public static String DFU_SERVICE = "0000fe59-0000-1000-8000-00805f9b34fb";
    public static String DFU_CONTROL_CHARACTER = "8ec90001-f315-4f60-9fb8-838830daea50";
    public static String DFU_PACKET_CHARACTER = "8ec90002-f315-4f60-9fb8-838830daea50";
    public static String DFU_VERSION_CHARACTER = "00000003-0000-1000-8000-00805f9b34fb";
    public static String HID_SERVICE = "00001812-0000-1000-8000-00805f9b34fb";
    public static String HID_BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String HID_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_DFU_SERVICE = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DFU_CONTROL_CHARACTER = UUID.fromString(DFU_CONTROL_CHARACTER);
    public static final UUID UUID_DFU_PACKET_CHARACTER = UUID.fromString(DFU_PACKET_CHARACTER);
    public static final UUID UUID_DFU_VERSION_CHARACTER = UUID.fromString(DFU_VERSION_CHARACTER);
    public static final UUID UUID_HID_SERVICE = UUID.fromString(HID_SERVICE);
    public static final UUID UUID_BATTERY_SERVICE = UUID.fromString(HID_BATTERY_SERVICE);
    public static final UUID UUID_BATTERY_LEVEL = UUID.fromString(HID_BATTERY_LEVEL);
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = new UUID(45088566677504L, -9223371485494954757L);
    public static int GATT_DFU_PROFILE = 9;
    public static int GATT_HID_PROFILE = 10;
    public static byte[] CMD_CTRL_SELECT_DAT = {6, 1};
    public static byte[] CMD_CTRL_SELECT_BIN = {6, 2};
    public static byte[] CMD_CTRL_SET_PRN = {2, 0, 0};
    public static byte[] CMD_CTRL_CREATE_DAT = {1, 1, 0, 0, 0, 0};
    public static byte[] CMD_CTRL_CREATE_BIN = {1, 2, 0, 0, 0, 0};
    public static byte[] CMD_DFU_CTRL_CAL_CHECKSUM = {3};
    public static byte[] CMD_DFU_CTRL_EXECUTE = {4};
}

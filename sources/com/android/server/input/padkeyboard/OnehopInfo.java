package com.android.server.input.padkeyboard;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import miui.util.ReflectionUtils;

/* loaded from: classes.dex */
public class OnehopInfo {
    private static final String ACTION = "TAG_DISCOVERED";
    private static final String DEFAULT_BLUETOOTH_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final int DEVICE_TYPE = 8;
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private static final String TAG = "OnehopInfo";
    private static final int TYPE_ABILITY_BASE = 120;
    private static final int TYPE_ACTION_BASE = 100;
    public static final int TYPE_CUSTOM_ACTION = 101;
    private static final int TYPE_DEVICEINFO_BASE = 0;
    public static final int TYPE_DEVICEINFO_BT_MAC = 1;
    public static final int TYPE_EXT_ABILITY = 121;
    private String action_suffix;
    private String bt_mac;
    private int device_type;
    private byte[] ext_ability;
    private static final byte[] PROTOCOL_ID = {39, 23};
    private static String ACTION_BASE = "com.miui.onehop.action";
    public static String ACTION_SUFFIX_MIRROR = "MIRROR";
    private static final byte[] PREFIX_PROTOCOL = {8, 1, MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH, 13, 34, 1, 3, 42, 9, 77, 73, 45, CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, 70, 67, 84, CommunicationUtil.KEYBOARD_COLOR_BLACK, 71, CommunicationUtil.KEYBOARD_ADDRESS, 15, 74};
    private static final byte[] SUFFIX_PROTOCOL = {106, 2, -6, Byte.MAX_VALUE};

    private OnehopInfo(Builder builder) {
        this.action_suffix = builder.action_suffix;
        this.device_type = builder.device_type;
        this.bt_mac = builder.bt_mac;
        this.ext_ability = builder.ext_ability;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private String action_suffix;
        private String bt_mac;
        private int device_type;
        private byte[] ext_ability;

        public Builder setActionSuffix(String actionSuffix) {
            this.action_suffix = actionSuffix;
            return this;
        }

        public Builder setBtMac(String btMac) {
            this.bt_mac = btMac;
            return this;
        }

        public Builder setExtAbility(byte[] extAbility) {
            this.ext_ability = extAbility;
            return this;
        }

        public OnehopInfo build() {
            return new OnehopInfo(this);
        }
    }

    public byte[] toPayloadByteArray() {
        int totalLenth = 0;
        try {
            int act_l = getTLVLength(this.action_suffix);
            if (act_l > 0) {
                totalLenth = 0 + 2 + act_l;
            }
            int bt_mac_l = getTLVLength(this.bt_mac);
            if (bt_mac_l > 0) {
                totalLenth = totalLenth + 2 + bt_mac_l;
            }
            int ext_ability_l = getTLVLength(this.ext_ability);
            if (ext_ability_l > 0) {
                totalLenth = totalLenth + 2 + ext_ability_l;
            }
            ByteBuffer totalData = ByteBuffer.allocate(totalLenth);
            if (act_l > 0) {
                totalData.put((byte) 101);
                totalData.put((byte) act_l);
                totalData.put(this.action_suffix.getBytes(StandardCharsets.UTF_8));
            }
            if (bt_mac_l > 0) {
                totalData.put((byte) 1);
                totalData.put((byte) bt_mac_l);
                totalData.put(this.bt_mac.getBytes(StandardCharsets.UTF_8));
            }
            if (ext_ability_l > 0) {
                totalData.put((byte) 121);
                totalData.put((byte) ext_ability_l);
                totalData.put(this.ext_ability);
            }
            byte[] totalDataArray = totalData.array();
            return totalDataArray;
        } catch (Exception e) {
            Log.e(TAG, "" + e);
            return null;
        }
    }

    private int getTLVLength(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        return length;
    }

    private int getTLVLength(byte[] byteValue) {
        if (byteValue == null) {
            return 0;
        }
        int length = byteValue.length;
        return length;
    }

    public byte[] getPayload(OnehopInfo info) {
        byte[] payload = info.toPayloadByteArray();
        byte[] protocolData = toProtocolByteArray(PROTOCOL_ID, 8, null, ACTION, payload);
        byte[] nfcProtocolValue = generateNFCProtocol(protocolData);
        return nfcProtocolValue;
    }

    public static byte[] toProtocolByteArray(byte[] protocolId, int deviceType, Map<Integer, byte[]> tlvCountNum, String action, byte[] payload) {
        int totalLenth = 0 + 2 + 4 + 1;
        if (tlvCountNum != null) {
            try {
                Iterator<Map.Entry<Integer, byte[]>> it = tlvCountNum.entrySet().iterator();
                while (it.hasNext()) {
                    totalLenth = totalLenth + 2 + it.next().getValue().length;
                }
            } catch (Exception e) {
                Slog.e(TAG, "toProtocolByteArray error : " + e.getMessage());
                return null;
            }
        }
        if (!TextUtils.isEmpty(action)) {
            byte[] actionConntent = action.getBytes(StandardCharsets.UTF_8);
            ByteBuffer totalData = ByteBuffer.allocate(totalLenth + 1 + actionConntent.length + payload.length);
            totalData.put(protocolId);
            totalData.putInt(deviceType);
            if (tlvCountNum != null && tlvCountNum.size() > 1) {
                totalData.put((byte) tlvCountNum.size());
                for (Map.Entry<Integer, byte[]> entryData : tlvCountNum.entrySet()) {
                    int entryKey = entryData.getKey().intValue();
                    byte[] entryValue = entryData.getValue();
                    totalData.putInt(entryKey);
                    totalData.put((byte) entryValue.length);
                    totalData.put(entryValue);
                }
            } else {
                totalData.put((byte) 0);
            }
            if (actionConntent != null) {
                totalData.put((byte) actionConntent.length);
                totalData.put(actionConntent);
                if (payload != null) {
                    totalData.put(payload);
                    byte[] totalDataArray = totalData.array();
                    return totalDataArray;
                }
                Slog.e(TAG, "toProtocolByteArray mPayload is null");
                return null;
            }
            Slog.e(TAG, "toProtocolByteArray actionConntent is null");
            return null;
        }
        Slog.e(TAG, "toProtocolByteArray mAction is null");
        return null;
    }

    public static byte[] getExtAbility() {
        int i = 0 + (0 | 1);
        byte result = (byte) (i & 255);
        return new byte[]{result};
    }

    public static String getBtMacAddress(Context context) {
        if (context == null) {
            Slog.e(TAG, "mContext is null");
            return null;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Slog.e(TAG, "bluetoothAdapter is null");
            return null;
        }
        String address = bluetoothAdapter.getAddress();
        if (address != null && !address.isEmpty() && !address.equals(DEFAULT_BLUETOOTH_MAC_ADDRESS)) {
            return address;
        }
        String address2 = Settings.Secure.getString(context.getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        if (address2 == null || address2.isEmpty() || address2.equals(DEFAULT_BLUETOOTH_MAC_ADDRESS)) {
            return getBtMacAddressByReflection();
        }
        return address2;
    }

    private static String getBtMacAddressByReflection() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return null;
            }
            Object bluetoothManagerService = ReflectionUtils.getObjectField(bluetoothAdapter, "mService", Object.class);
            if (bluetoothManagerService == null) {
                Slog.w(TAG, "getBtMacAdressByReflection: bluetooth manager service is null");
                return null;
            }
            Object address = bluetoothManagerService.getClass().getMethod("getAddress", new Class[0]).invoke(bluetoothManagerService, new Object[0]);
            if (address != null && (address instanceof String)) {
                return (String) address;
            }
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "occur exception:" + e.getLocalizedMessage());
            return null;
        }
    }

    private static final byte[] generateNFCProtocol(byte[] appDataProtocol) {
        if (appDataProtocol == null) {
            Slog.e(TAG, "generateNFCProtocol appDataProtocol is null");
            return new byte[0];
        }
        byte[] bArr = PREFIX_PROTOCOL;
        int length = bArr.length + 2 + 1 + appDataProtocol.length;
        byte[] bArr2 = SUFFIX_PROTOCOL;
        int totoleLength = length + bArr2.length;
        ByteBuffer totalData = ByteBuffer.allocate(totoleLength);
        totalData.put((byte) 10);
        totalData.put((byte) (totoleLength - 2));
        totalData.put(bArr);
        totalData.put((byte) appDataProtocol.length);
        totalData.put(appDataProtocol);
        totalData.put(bArr2);
        byte[] protocolData = totalData.array();
        return protocolData;
    }

    public String toString() {
        return "OnehopInfo{, action_suffix='" + this.action_suffix + "', device_type=" + this.device_type + ", bt_mac='" + this.bt_mac + "', ext_ability='" + this.ext_ability + "'}";
    }
}

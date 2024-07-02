package com.android.server.input.padkeyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.LongSparseArray;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.android.server.policy.MiuiKeyInterceptExtend;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.xiaomi.NetworkBoost.StatusManager;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import miui.hardware.input.InputFeature;
import miui.hardware.input.MiuiKeyboardHelper;

/* loaded from: classes.dex */
public class MiuiKeyboardUtil {
    public static final String ARABIC = "ar_EG";
    private static final Map<Integer, Float> BRIGHTNESS_SETTINGS_RELATION;
    public static final String FRENCH = "fr_FR";
    public static final String GERMAN = "de_DE";
    public static final String ITALIA = "it_IT";
    public static final String KEYBOARD_LAYOUT_ARABIC = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_arabic";
    public static final String KEYBOARD_LAYOUT_FRENCH = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_french";
    public static final String KEYBOARD_LAYOUT_GERMAN = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_german";
    public static final String KEYBOARD_LAYOUT_ITALIA = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_italian";
    public static final String KEYBOARD_LAYOUT_RUSSIAN = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_russian";
    public static final String KEYBOARD_LAYOUT_SPAINISH = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_spanish";
    public static final String KEYBOARD_LAYOUT_THAI = "com.miui.miinput/com.miui.miinput.keyboardlayout.InputDeviceReceiver/keyboard_layout_thai";
    public static final String KEYBOARD_LAYOUT_UK = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_english_uk";
    public static final String KEYBOARD_LAYOUT_US = "com.android.inputdevices/com.android.inputdevices.InputDeviceReceiver/keyboard_layout_english_us";
    public static final int KEYBOARD_LEVEL_HIGH = 512;
    public static final int KEYBOARD_LEVEL_LOW = 256;
    public static final String KEYBOARD_TYPE_LEVEL = "keyboard_type_level";
    public static final byte KEYBOARD_TYPE_LQ_BLE = 33;
    public static final byte KEYBOARD_TYPE_LQ_HIGH = 32;
    public static final byte KEYBOARD_TYPE_LQ_LOW = 2;
    public static final byte KEYBOARD_TYPE_UCJ = 1;
    public static final byte KEYBOARD_TYPE_UCJ_HIGH = 16;
    public static final String RUSSIA = "ru_RU";
    public static final String SPAINISH = "es_US";
    private static final String TAG = "CommonUtil";
    public static final String THAI = "th_TH";
    public static final String UK = "en_GB";
    public static final String US = "en_US";
    public static final int VALUE_KB_TYPE_HIGH = 1;
    public static final int VALUE_KB_TYPE_LOW = 0;
    private static String mMcuVersion = null;
    private static final float[] angle_cos = {1.0f, 0.999848f, 0.999391f, 0.99863f, 0.997564f, 0.996195f, 0.994522f, 0.992546f, 0.990268f, 0.987688f, 0.984808f, 0.981627f, 0.978148f, 0.97437f, 0.970296f, 0.965926f, 0.961262f, 0.956305f, 0.951057f, 0.945519f, 0.939693f, 0.93358f, 0.927184f, 0.920505f, 0.913545f, 0.906308f, 0.898794f, 0.891007f, 0.882948f, 0.87462f, 0.866025f, 0.857167f, 0.848048f, 0.838671f, 0.829038f, 0.819152f, 0.809017f, 0.798635f, 0.788011f, 0.777146f, 0.766044f, 0.75471f, 0.743145f, 0.731354f, 0.71934f, 0.707107f, 0.694658f, 0.681998f, 0.669131f, 0.656059f, 0.642788f, 0.62932f, 0.615661f, 0.601815f, 0.587785f, 0.573576f, 0.559193f, 0.544639f, 0.529919f, 0.515038f, 0.5f, 0.48481f, 0.469472f, 0.453991f, 0.438371f, 0.422618f, 0.406737f, 0.390731f, 0.374607f, 0.358368f, 0.34202f, 0.325568f, 0.309017f, 0.292372f, 0.275637f, 0.258819f, 0.241922f, 0.224951f, 0.207912f, 0.190809f, 0.173648f, 0.156434f, 0.139173f, 0.121869f, 0.104528f, 0.087156f, 0.069756f, 0.052336f, 0.034899f, 0.017452f, -0.0f};
    private static final float[] angle_sin = {MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 0.017452f, 0.034899f, 0.052336f, 0.069756f, 0.087156f, 0.104528f, 0.121869f, 0.139173f, 0.156434f, 0.173648f, 0.190809f, 0.207912f, 0.224951f, 0.241922f, 0.258819f, 0.275637f, 0.292372f, 0.309017f, 0.325568f, 0.34202f, 0.358368f, 0.374607f, 0.390731f, 0.406737f, 0.422618f, 0.438371f, 0.453991f, 0.469472f, 0.48481f, 0.5f, 0.515038f, 0.529919f, 0.544639f, 0.559193f, 0.573576f, 0.587785f, 0.601815f, 0.615662f, 0.62932f, 0.642788f, 0.656059f, 0.669131f, 0.681998f, 0.694658f, 0.707107f, 0.71934f, 0.731354f, 0.743145f, 0.75471f, 0.766044f, 0.777146f, 0.788011f, 0.798636f, 0.809017f, 0.819152f, 0.829038f, 0.838671f, 0.848048f, 0.857167f, 0.866025f, 0.87462f, 0.882948f, 0.891007f, 0.898794f, 0.906308f, 0.913545f, 0.920505f, 0.927184f, 0.93358f, 0.939693f, 0.945519f, 0.951057f, 0.956305f, 0.961262f, 0.965926f, 0.970296f, 0.97437f, 0.978148f, 0.981627f, 0.984808f, 0.987688f, 0.990268f, 0.992546f, 0.994522f, 0.996195f, 0.997564f, 0.99863f, 0.999391f, 0.999848f, 1.0f};

    static {
        HashMap hashMap = new HashMap();
        BRIGHTNESS_SETTINGS_RELATION = hashMap;
        hashMap.put(0, Float.valueOf(0.6f));
        Float valueOf = Float.valueOf(0.8f);
        hashMap.put(1, valueOf);
        hashMap.put(2, Float.valueOf(1.0f));
        hashMap.put(3, valueOf);
        hashMap.put(4, Float.valueOf(0.7f));
        hashMap.put(5, Float.valueOf(0.5f));
        hashMap.put(6, Float.valueOf(0.4f));
        hashMap.put(7, Float.valueOf(0.3f));
        hashMap.put(8, Float.valueOf(0.2f));
        hashMap.put(9, Float.valueOf(0.1f));
        hashMap.put(10, Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
    }

    private MiuiKeyboardUtil() {
    }

    public static synchronized byte getSum(byte[] data, int start, int length) {
        byte sum;
        synchronized (MiuiKeyboardUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum = (byte) (data[i] + sum);
            }
        }
        return sum;
    }

    public static synchronized boolean checkSum(byte[] data, int start, int length, byte sum) {
        boolean z;
        synchronized (MiuiKeyboardUtil.class) {
            byte dsum = getSum(data, start, length);
            z = dsum == sum;
        }
        return z;
    }

    public static float invSqrt(float num) {
        float xHalf = 0.5f * num;
        int temp = Float.floatToIntBits(num);
        float num2 = Float.intBitsToFloat(1597463007 - (temp >> 1));
        return num2 * (1.5f - ((xHalf * num2) * num2));
    }

    public static float calculatePKAngle(float x1, float x2, float z1, float z2) {
        float angle1;
        float angle2;
        float resultAngle;
        float quadrant = x1 * x2;
        float cos1 = z1 / ((float) Math.sqrt((x1 * x1) + (z1 * z1)));
        float cos2 = z2 / ((float) Math.sqrt((x2 * x2) + (z2 * z2)));
        if (Math.abs(cos1 - 1.0d) < 0.01d) {
            angle1 = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        } else if (Math.abs(cos1 + 1.0d) < 0.01d) {
            angle1 = 180.0f;
        } else {
            angle1 = (float) ((Math.acos(cos1) * 180.0d) / 3.141592653589793d);
        }
        if (Math.abs(cos2 - 1.0d) < 0.01d) {
            angle2 = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        } else if (Math.abs(cos2 + 1.0d) < 0.01d) {
            angle2 = 180.0f;
        } else {
            angle2 = (float) ((Math.acos(cos2) * 180.0d) / 3.141592653589793d);
        }
        Slog.i(TAG, "angle1 = " + angle1 + " angle2 = " + angle2);
        float angleOffset = angle1 - angle2;
        float angleSum = angle1 + angle2;
        float angleOffset2 = angle1 - (180.0f - angle2);
        if (quadrant > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            resultAngle = ((x1 >= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || angleOffset <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) && (x1 <= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || angleOffset >= MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)) ? 180.0f + Math.abs(angleOffset) : 180.0f - Math.abs(angleOffset);
        } else if (x1 < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            if (angleOffset2 > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                resultAngle = 360.0f - (angleSum - 180.0f);
            } else {
                resultAngle = 180.0f - angleSum;
            }
        } else if (angleOffset2 > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            resultAngle = (-180.0f) + angleSum;
        } else {
            resultAngle = 180.0f + angleSum;
        }
        Slog.i(TAG, "result Angle = " + resultAngle);
        return resultAngle;
    }

    public static int calculatePKAngleV2(float kX, float kY, float kZ, float pX, float pY, float pZ) {
        float deviation2;
        float deviation1;
        float recipNorm = invSqrt((kX * kX) + (kY * kY) + (kZ * kZ));
        float kX2 = kX * recipNorm;
        float kY2 = kY * recipNorm;
        float kZ2 = kZ * recipNorm;
        float recipNorm2 = invSqrt((pX * pX) + (pY * pY) + (pZ * pZ));
        float pX2 = pX * recipNorm2;
        float pY2 = pY * recipNorm2;
        float pZ2 = pZ * recipNorm2;
        float min_deviation = 100.0f;
        int min_deviation_angle = 0;
        for (int i = 0; i <= 360; i++) {
            if (i > 90) {
                if (i > 90 && i <= 180) {
                    float[] fArr = angle_cos;
                    float f = (-fArr[180 - i]) * kX2;
                    float[] fArr2 = angle_sin;
                    float deviation12 = (f - (fArr2[180 - i] * kZ2)) + pX2;
                    deviation2 = ((-fArr[180 - i]) * kZ2) + (fArr2[180 - i] * kX2) + pZ2;
                    deviation1 = deviation12;
                } else if (i > 180 && i <= 270) {
                    float[] fArr3 = angle_cos;
                    float f2 = (-fArr3[i - 180]) * kX2;
                    float[] fArr4 = angle_sin;
                    float deviation13 = (f2 - ((-fArr4[i - 180]) * kZ2)) + pX2;
                    deviation2 = ((-fArr3[i - 180]) * kZ2) + ((-fArr4[i - 180]) * kX2) + pZ2;
                    deviation1 = deviation13;
                } else {
                    float[] fArr5 = angle_cos;
                    float f3 = fArr5[360 - i] * kX2;
                    float[] fArr6 = angle_sin;
                    float deviation14 = (f3 - ((-fArr6[360 - i]) * kZ2)) + pX2;
                    deviation2 = (fArr5[360 - i] * kZ2) + ((-fArr6[360 - i]) * kX2) + pZ2;
                    deviation1 = deviation14;
                }
            } else {
                float f4 = angle_cos[i];
                float f5 = angle_sin[i];
                float deviation15 = ((f4 * kX2) - (f5 * kZ2)) + pX2;
                deviation2 = (f4 * kZ2) + (f5 * kX2) + pZ2;
                deviation1 = deviation15;
            }
            if (Math.abs(deviation1) + Math.abs(deviation2) < min_deviation) {
                float min_deviation2 = Math.abs(deviation1) + Math.abs(deviation2);
                min_deviation_angle = i;
                min_deviation = min_deviation2;
            }
        }
        float accel_angle_error = Math.abs(pY2) > Math.abs(kY2) ? Math.abs(pY2) : Math.abs(kY2);
        if (accel_angle_error > 0.98f) {
            return -1;
        }
        return min_deviation_angle;
    }

    public static String Bytes2Hex(byte[] data, int len) {
        return Bytes2HexString(data, len, ",");
    }

    public static String Bytes2HexString(byte[] data, int len, String s) {
        StringBuilder stringBuilder = new StringBuilder();
        if (len > data.length) {
            len = data.length;
        }
        for (int i = 0; i < len; i++) {
            stringBuilder.append(String.format("%02x", Byte.valueOf(data[i]))).append(s);
        }
        return stringBuilder.toString();
    }

    public static String Bytes2RevertHexString(byte[] data) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : data) {
            hexBuilder.append(String.format("%02x", Byte.valueOf(b)));
        }
        String hexStr = hexBuilder.toString();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            result.append(hexStr.substring((hexStr.length() - 2) - i, hexStr.length() - i));
        }
        return result.toString();
    }

    public static String Bytes2String(byte[] data) {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String Hex2String(String hex) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, i + 2);
            int decimal = Integer.parseInt(output, 16);
            stringBuilder.append((char) decimal);
        }
        return stringBuilder.toString();
    }

    public static String String2HexString(String str) {
        char[] chars = str.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) {
            stringBuilder.append(Integer.toHexString(c));
        }
        return stringBuilder.toString();
    }

    public static byte[] int2Bytes(int data) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (data >> (24 - (i * 8)));
        }
        return b;
    }

    public static int[] getYearMonthDayByTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int[] ret = {calendar.get(1), calendar.get(2) + 1, calendar.get(5)};
        return ret;
    }

    public static void operationWait(int ms) {
        try {
            Thread.currentThread();
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static int byte2int(byte data) {
        return data & 255;
    }

    public static byte[] commandMiDevAuthInitForIIC() {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        temp[5] = 49;
        temp[6] = CommunicationUtil.PAD_ADDRESS;
        temp[7] = CommunicationUtil.KEYBOARD_ADDRESS;
        temp[8] = CommunicationUtil.AUTH_COMMAND.AUTH_START.getCommand();
        temp[9] = 6;
        temp[10] = 77;
        temp[11] = 73;
        temp[12] = CommunicationUtil.KEYBOARD_COLOR_BLACK;
        temp[13] = 85;
        temp[14] = 84;
        temp[15] = 72;
        temp[16] = getSum(temp, 4, 13);
        Slog.i(TAG, "send init auth = " + Bytes2Hex(temp, temp.length));
        return temp;
    }

    public static byte[] commandMiAuthStep3Type1ForIIC(byte[] keyMeta, byte[] challenge) {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        temp[5] = 49;
        temp[6] = CommunicationUtil.PAD_ADDRESS;
        temp[7] = CommunicationUtil.KEYBOARD_ADDRESS;
        temp[8] = CommunicationUtil.AUTH_COMMAND.AUTH_STEP3.getCommand();
        temp[9] = 20;
        if (keyMeta != null && keyMeta.length == 4) {
            System.arraycopy(keyMeta, 0, temp, 10, 4);
        }
        if (challenge == null || challenge.length != 16) {
            temp[14] = getSum(temp, 4, 11);
        } else {
            System.arraycopy(challenge, 0, temp, 14, 16);
            temp[30] = getSum(temp, 4, 27);
        }
        Slog.i(TAG, "send 3-1 auth = " + Bytes2Hex(temp, temp.length));
        return temp;
    }

    public static byte[] commandMiAuthStep5Type1ForIIC(byte[] token) {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        temp[5] = 49;
        temp[6] = CommunicationUtil.PAD_ADDRESS;
        temp[7] = CommunicationUtil.KEYBOARD_ADDRESS;
        temp[8] = CommunicationUtil.AUTH_COMMAND.AUTH_STEP5_1.getCommand();
        temp[9] = KEYBOARD_TYPE_UCJ_HIGH;
        if (token == null || token.length != 16) {
            temp[10] = getSum(temp, 4, 10);
        } else {
            System.arraycopy(token, 0, temp, 10, 16);
            temp[26] = getSum(temp, 4, 26);
        }
        Slog.i(TAG, "send 5-1 auth = " + Bytes2Hex(temp, temp.length));
        return temp;
    }

    public static byte[] commandMiDevAuthInitForUSB() {
        byte[] bytes = {CommunicationUtil.SEND_REPORT_ID_LONG_DATA, 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, 49, 6, 77, 73, CommunicationUtil.KEYBOARD_COLOR_BLACK, 85, 84, 72, getSum(bytes, 0, 12)};
        return bytes;
    }

    public static byte[] commandMiAuthStep3Type1ForUSB(byte[] keyMeta, byte[] challenge) {
        byte[] bytes = new byte[27];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 49;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[4] = 50;
        bytes[5] = 20;
        if (keyMeta != null && keyMeta.length == 4) {
            System.arraycopy(keyMeta, 0, bytes, 6, 4);
        }
        if (challenge != null && challenge.length == 16) {
            System.arraycopy(challenge, 0, bytes, 10, 16);
        }
        bytes[26] = getSum(bytes, 0, 26);
        return bytes;
    }

    public static int getBackLightBrightnessWithSensor(float brightness) {
        int level = Math.round(Math.min(Math.max(brightness, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X), 50.0f) / 5.0f);
        float result = BRIGHTNESS_SETTINGS_RELATION.get(Integer.valueOf(Math.min(Math.max(level, 0), 10))).floatValue();
        if (result > 0.5f && KeyboardInteraction.INTERACTION.isConnectBleLocked() && !KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
            result = 0.5f;
        }
        return Math.round(100.0f * result);
    }

    public static void notifySettingsKeyboardStatusChanged(Context context, Bundle data) {
        Intent intent = new Intent("com.xiaomi.input.action.keyboard.KEYBOARD_STATUS_CHANGED");
        intent.setPackage("com.miui.securitycore");
        intent.putExtras(data);
        context.sendBroadcastAsUser(intent, UserHandle.OWNER);
    }

    public static boolean isKeyboardSupportMuteLight(int type) {
        return type == 33 || type == 1 || type == 16;
    }

    public static boolean hasTouchpad(byte type) {
        return type == 32 || type == 33 || type == 16;
    }

    /* loaded from: classes.dex */
    public static class KeyBoardShortcut {
        public static final String ADJUST_SPLIT_SCREEN = "调整分屏比例";
        public static final String BACK_ESC = "返回";
        public static final String BACK_HOME = "回到桌面";
        public static final String BRIGHTNESS_DOWN = "亮度减";
        public static final String BRIGHTNESS_UP = "亮度加";
        public static final String CLOSE_WINDOW = "关闭当前窗口";
        public static final String DELETE_WORD = "删除";
        public static final String FULL_SCREEN = "进入全屏模式";
        public static final String LAUNCH_CONTROL_CENTER = "打开控制中心";
        public static final String LAUNCH_NOTIFICATION_CENTER = "打开通知中心";
        public static final String LAUNCH_RECENTS_TASKS = "显示最近任务";
        public static final String LOCK_SCREEN = "锁屏";
        public static final String MEDIA_NEXT = "下一曲";
        public static final String MEDIA_PLAY_PAUSE = "暂停播放";
        public static final String MEDIA_PREVIOUS = "上一曲";
        public static final String MUTE_MODE = "麦克风开关";
        public static final String OPEN_APP = "打开自定义应用";
        public static final String SCREENSHOT = "截全屏";
        public static final String SCREEN_SHOT_PARTIAL = "区域截屏";
        public static final String SHOW_DOCK = "显示dock";
        public static final String SHOW_SHORTCUT_LIST = "显示快捷键列表";
        public static final String SMALL_WINDOW = "进入小窗模式";
        public static final String TOGGLE_RECENT_APP = "快切最近应用";
        public static final int TYPE_APP = 0;
        public static final int TYPE_CLOSEAPP = 8;
        public static final int TYPE_CONTROLPANEL = 1;
        public static final int TYPE_FULLSCREEN = 10;
        public static final int TYPE_HOME = 4;
        public static final int TYPE_LEFTSPLITSCREEN = 11;
        public static final int TYPE_LOCKSCREEN = 5;
        public static final int TYPE_NOTIFICATIONPANLE = 2;
        public static final int TYPE_RECENT = 3;
        public static final int TYPE_RIGHTSPLITSCREEN = 12;
        public static final int TYPE_SCREENSHOT = 6;
        public static final int TYPE_SCREENSHOTPARTIAL = 7;
        public static final int TYPE_SMALLWINDOW = 9;
        public static final String VOICE_ASSISTANT = "小爱同学";
        public static final String VOICE_TO_WORD = "快捷语音";
        public static final String VOLUME_DOWN = "音量下";
        public static final String VOLUME_MUTE = "静音";
        public static final String VOLUME_UP = "音量上";
        public static final String ZEN_MODE = "勿扰";

        public static String getShortCutNameByType(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
            switch (info.getType()) {
                case 0:
                    return OPEN_APP;
                case 1:
                    return LAUNCH_CONTROL_CENTER;
                case 2:
                    return LAUNCH_NOTIFICATION_CENTER;
                case 3:
                    return LAUNCH_RECENTS_TASKS;
                case 4:
                    return BACK_HOME;
                case 5:
                    return LOCK_SCREEN;
                case 6:
                    return SCREENSHOT;
                case 7:
                    return SCREEN_SHOT_PARTIAL;
                case 8:
                    return CLOSE_WINDOW;
                case 9:
                    return SMALL_WINDOW;
                case 10:
                    return FULL_SCREEN;
                case 11:
                case 12:
                    return ADJUST_SPLIT_SCREEN;
                default:
                    return null;
            }
        }

        public static String getShortcutNameByKeyCode(int keyCode) {
            switch (keyCode) {
                case 4:
                    if (MiuiKeyboardHelper.support6FKeyboard()) {
                        return BACK_ESC;
                    }
                    return "";
                case 24:
                    return VOLUME_UP;
                case 25:
                    return VOLUME_DOWN;
                case 85:
                    return MEDIA_PLAY_PAUSE;
                case 87:
                    return MEDIA_NEXT;
                case 88:
                    return MEDIA_PREVIOUS;
                case StatusManager.CALLBACK_VPN_STATUS /* 112 */:
                    if (MiuiKeyboardHelper.support6FKeyboard()) {
                        return DELETE_WORD;
                    }
                    return "";
                case 164:
                    return VOLUME_MUTE;
                case 220:
                    return BRIGHTNESS_DOWN;
                case 221:
                    return BRIGHTNESS_UP;
                case 9996:
                    return MUTE_MODE;
                case 9997:
                    return VOICE_ASSISTANT;
                case 9998:
                    return ZEN_MODE;
                case 9999:
                    return LOCK_SCREEN;
                default:
                    return "";
            }
        }

        public static String getShortcutNameByKeyCodeWithAction(int keyCode, boolean isLongPress) {
            if (!MiuiKeyboardHelper.support6FKeyboard()) {
                return "";
            }
            switch (keyCode) {
                case 9994:
                    if (isLongPress) {
                        return SCREEN_SHOT_PARTIAL;
                    }
                    return SCREENSHOT;
                default:
                    return "";
            }
        }
    }

    public static boolean interceptShortCutKeyIfCustomDefined(Context context, KeyEvent event, MiuiKeyInterceptExtend.INTERCEPT_STAGE stage) {
        int keyCode = event.getKeyCode();
        if (stage == MiuiKeyInterceptExtend.INTERCEPT_STAGE.BEFORE_DISPATCHING) {
            long shortCutKey = keyCode;
            if ((event.getMetaState() & 50) != 0) {
                if ((event.getMetaState() & 32) != 0) {
                    shortCutKey |= 137438953472L;
                } else if ((event.getMetaState() & 16) != 0) {
                    shortCutKey |= 68719476736L;
                }
                shortCutKey |= 8589934592L;
            }
            if (event.isMetaPressed()) {
                shortCutKey |= 281474976710656L;
            }
            if (event.isCtrlPressed()) {
                shortCutKey |= 17592186044416L;
            }
            if (event.isShiftPressed()) {
                shortCutKey |= 4294967296L;
            }
            LongSparseArray<MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo> shortCutInfos = MiuiCustomizeShortCutUtils.getInstance(context).getMiuiKeyboardShortcutInfo();
            return (shortCutInfos == null || shortCutInfos.get(shortCutKey) == null || shortCutInfos.get(shortCutKey).isEnable()) ? false : true;
        }
        return false;
    }

    public static boolean isXM2022MCU() {
        String str = mMcuVersion;
        return str != null && str.startsWith("XM2022");
    }

    public static void setMcuVersion(String version) {
        mMcuVersion = version;
    }

    public static boolean supportCalculateAngle() {
        boolean iicConnected = KeyboardInteraction.INTERACTION.isConnectIICLocked();
        boolean bleConnected = InputFeature.isSingleBleKeyboard() && KeyboardInteraction.INTERACTION.isConnectBleLocked();
        return iicConnected || bleConnected;
    }
}

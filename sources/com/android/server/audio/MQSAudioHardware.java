package com.android.server.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;

/* loaded from: classes.dex */
public class MQSAudioHardware {
    private static final String AUDIO_HW = "{\"onetrack_count\":\"1\",\"name\":\"audio_hw\",\"audio_event\":{\"audio_hw_changes\":[%s],\"detect_time\":\"%s\", \"time_zone\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\"}";
    private static final String HW_CHANGE = "{\"audio_hw_type\":\"%s\",\"audio_hw_state\":\"%s\"}";
    private static final String TAG = "MQSAudioHardware";
    private static volatile MQSAudioHardware sInstance;
    private Context mContext;

    private MQSAudioHardware() {
    }

    public static MQSAudioHardware getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MQSAudioHardware.class) {
                if (sInstance == null) {
                    sInstance = new MQSAudioHardware();
                    sInstance.mContext = context;
                }
            }
        }
        return sInstance;
    }

    public void onetrack() {
        if (!canCheck()) {
            Slog.d(TAG, "shouldn't check");
            return;
        }
        String newAudioHwState = SystemProperties.get("vendor.audio.hw.state");
        if (newAudioHwState == null) {
            return;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String oldAudioHwState = Settings.Global.getString(contentResolver, "audio_hw_state");
        if (oldAudioHwState == null) {
            oldAudioHwState = "00000000000000";
        }
        Hashtable<String, Integer> hwState = parseHwState(oldAudioHwState, newAudioHwState);
        Enumeration<String> e = hwState.keys();
        ArrayList<String> changes = new ArrayList<>();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            changes.add(String.format(HW_CHANGE, key, hwState.get(key)));
        }
        if (changes.size() > 0) {
            String changesStr = String.join(",", changes);
            String message = String.format(AUDIO_HW, changesStr, getTimeStamp(), getTimeZoneId());
            MQSserver mqs = MQSserver.getInstance(this.mContext);
            if (mqs.onetrack_report(message)) {
                Settings.Global.putString(contentResolver, "audio_hw_state", newAudioHwState);
            }
        }
    }

    private boolean canCheck() {
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            int userSetUpComplete = Settings.Secure.getInt(contentResolver, "user_setup_complete", 0);
            boolean userExperienceEnable = MiuiSettings.Secure.isUserExperienceProgramEnable(contentResolver);
            Slog.d(TAG, "userSetUpComplete = " + userSetUpComplete + ", userExperienceEnable = " + userExperienceEnable);
            return userSetUpComplete != 0 && userExperienceEnable;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Hashtable<String, Integer> parseHwState(String oldAudioHwState, String newAudioHwState) {
        Hashtable hws = new Hashtable();
        if (newAudioHwState.equals(oldAudioHwState)) {
            return hws;
        }
        Slog.d(TAG, String.format("audio hw state changed from %s to %s", oldAudioHwState, newAudioHwState));
        try {
            int hapticNum = Integer.parseInt(SystemProperties.get("ro.vendor.audio.haptic.number", "0"));
            addStateChange(hws, "haptic", oldAudioHwState, newAudioHwState, 1, hapticNum);
            int paNum = Integer.parseInt(SystemProperties.get("ro.vendor.audio.smartPA.number", "0"));
            addStateChange(hws, "pa", oldAudioHwState, newAudioHwState, 3, paNum);
            if (addStateChange(hws, "adsp", oldAudioHwState, newAudioHwState, 11, 1) == 0) {
                return hws;
            }
            addStateChange(hws, "codec", oldAudioHwState, newAudioHwState, 12, 1);
            int audioswitchNum = Integer.parseInt(SystemProperties.get("ro.vendor.audio.audioswitch.number", "0"));
            addStateChange(hws, "audioswitch", oldAudioHwState, newAudioHwState, 13, audioswitchNum);
            return hws;
        } catch (Exception e) {
            e.printStackTrace();
            return hws;
        }
    }

    private static int addStateChange(Hashtable<String, Integer> hws, String hwType, String oldHwState, String newHwState, int posStart, int len) {
        String oldState = oldHwState.substring(posStart, posStart + len);
        String newState = newHwState.substring(posStart, posStart + len);
        if (!newState.equals(oldState) && !(newState.contains("0") & oldState.contains("0"))) {
            if (newState.contains("0")) {
                hws.put(hwType, 0);
                return 0;
            }
            hws.put(hwType, 1);
            return 1;
        }
        return -1;
    }

    private static String getTimeStamp() {
        long time = System.currentTimeMillis();
        return String.valueOf(time / 1000);
    }

    private static String getTimeZoneId() {
        TimeZone tz = TimeZone.getDefault();
        return tz.getID();
    }
}

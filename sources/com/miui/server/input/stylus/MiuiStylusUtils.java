package com.miui.server.input.stylus;

import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import miui.util.FeatureParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiStylusUtils {
    private static final String DEFAULT_STYLUS_PAGE_KEY_BW_CONFIG_FILE_PATH = "/system_ext/etc/input/stylus_page_key_bw_config.json";
    private static final String SUPPORT_STYLUS_PALM_REJECT = "persist.stylus.palm.reject";
    private static final String TAG = MiuiStylusUtils.class.getSimpleName();

    private MiuiStylusUtils() {
    }

    public static boolean supportStylusGesture() {
        return FeatureParser.getBoolean("support_stylus_gesture", false);
    }

    public static boolean isSupportOffScreenQuickNote() {
        return FeatureParser.getBoolean("stylus_quick_note", false);
    }

    public static boolean isSupportStylusPalmReject() {
        return SystemProperties.getBoolean(SUPPORT_STYLUS_PALM_REJECT, false);
    }

    private static String readFileToString(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            Slog.e(TAG, "where is my file (" + filePath + "), I can't find it :(");
            return "";
        }
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    result.append(line);
                } finally {
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            Slog.e(TAG, "file(" + filePath + ") load fail", e);
        }
        return result.toString();
    }

    public static StylusPageKeyConfig getDefaultStylusPageKeyConfig() {
        String configString;
        StylusPageKeyConfig config = new StylusPageKeyConfig();
        try {
            configString = readFileToString(DEFAULT_STYLUS_PAGE_KEY_BW_CONFIG_FILE_PATH);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed!!!", e);
        }
        if (configString.length() == 0) {
            return config;
        }
        parseJsonToStylusPageKeyConfigInternal(config, new JSONObject(configString));
        return config;
    }

    public static StylusPageKeyConfig parseJsonToStylusPageKeyConfig(JSONObject object) {
        StylusPageKeyConfig config = new StylusPageKeyConfig();
        try {
            parseJsonToStylusPageKeyConfigInternal(config, object);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed!!!", e);
        }
        return config;
    }

    private static void parseJsonToStylusPageKeyConfigInternal(StylusPageKeyConfig config, JSONObject jsonObject) throws JSONException {
        config.setVersion(jsonObject.getInt(AmapExtraCommand.VERSION_KEY));
        config.setEnable(jsonObject.getBoolean(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE));
        JSONArray appWhiteList = jsonObject.getJSONArray("appWhiteList");
        JSONArray activityBlackList = jsonObject.getJSONArray("activityBlackList");
        Set<String> appWhiteSet = new HashSet<>();
        Set<String> activityBlackSet = new HashSet<>();
        int size = appWhiteList.length();
        for (int i = 0; i < size; i++) {
            appWhiteSet.add(appWhiteList.getString(i));
        }
        int size2 = activityBlackList.length();
        for (int i2 = 0; i2 < size2; i2++) {
            activityBlackSet.add(activityBlackList.getString(i2));
        }
        config.setAppWhiteSet(appWhiteSet);
        config.setActivityBlackSet(activityBlackSet);
    }
}

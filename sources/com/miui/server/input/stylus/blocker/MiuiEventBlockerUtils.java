package com.miui.server.input.stylus.blocker;

import android.util.Slog;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiEventBlockerUtils {
    private static final String DEFAULT_STYLUS_BLOCKER_CONFIG_FILE_PATH = "/system_ext/etc/input/stylus_palm_reject_config.json";
    private static final String TAG = MiuiEventBlockerUtils.class.getSimpleName();

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

    public static StylusBlockerConfig getDefaultStylusBlockerConfig() {
        String configString;
        StylusBlockerConfig config = new StylusBlockerConfig();
        try {
            configString = readFileToString(DEFAULT_STYLUS_BLOCKER_CONFIG_FILE_PATH);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed!!!", e);
        }
        if (configString.length() == 0) {
            return config;
        }
        parseJsonToStylusBlockerConfigInternal(config, new JSONObject(configString));
        return config;
    }

    public static StylusBlockerConfig parseJsonToStylusBlockerConfig(JSONObject jsonObject) {
        StylusBlockerConfig config = new StylusBlockerConfig();
        try {
            parseJsonToStylusBlockerConfigInternal(config, jsonObject);
        } catch (JSONException e) {
            Slog.e(TAG, "parse config failed!!!", e);
        }
        return config;
    }

    private static void parseJsonToStylusBlockerConfigInternal(StylusBlockerConfig config, JSONObject jsonObject) throws JSONException {
        config.setVersion(jsonObject.getInt(AmapExtraCommand.VERSION_KEY));
        config.setEnable(jsonObject.getBoolean(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE));
        config.setWeakModeDelayTime(jsonObject.getInt("weakModeDelayTime"));
        config.setWeakModeMoveThreshold((float) jsonObject.getDouble("weakModeMoveThreshold"));
        config.setStrongModeDelayTime(jsonObject.getInt("strongModeDelayTime"));
        config.setStrongModeMoveThreshold((float) jsonObject.getDouble("strongModeMoveThreshold"));
        JSONArray blockList = jsonObject.getJSONArray("blockList");
        Set<String> set = new HashSet<>();
        int size = blockList.length();
        for (int i = 0; i < size; i++) {
            set.add(blockList.getString(i));
        }
        config.setBlockSet(set);
    }
}

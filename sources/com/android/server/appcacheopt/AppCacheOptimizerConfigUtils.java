package com.android.server.appcacheopt;

import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import miui.app.StorageRestrictedPathManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AppCacheOptimizerConfigUtils {
    private static final String OPTIMIZE_ENABLE = "isOptimizeEnable";
    private static final String OPTIMIZE_PATH = "optimizePath";
    private static final String PACKAGE_NAME = "packageName";
    private static final String TAG = "AppConfigUtils";
    private static final String WATERMARK = "watermark";

    public static ArrayList<AppCacheOptimizerConfig> parseAppConfig(String configString) {
        if (TextUtils.isEmpty(configString)) {
            return null;
        }
        ArrayList<AppCacheOptimizerConfig> appConfigs = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(configString);
            if (jsonObject.has("apps")) {
                JSONArray apps = jsonObject.getJSONArray("apps");
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject appJsonObject = apps.getJSONObject(i);
                    AppCacheOptimizerConfig appConfig = new AppCacheOptimizerConfig();
                    if (appJsonObject.has("packageName")) {
                        String packageName = appJsonObject.getString("packageName");
                        appConfig.setPackageName(packageName);
                    }
                    if (appJsonObject.has(OPTIMIZE_ENABLE)) {
                        int isOptimizeEnable = appJsonObject.getInt(OPTIMIZE_ENABLE);
                        appConfig.setIsOptimizeEnable(isOptimizeEnable);
                    }
                    if (appJsonObject.has(OPTIMIZE_PATH)) {
                        String[] optimizePath = appJsonObject.getString(OPTIMIZE_PATH).split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
                        appConfig.setOptimizePath(optimizePath);
                    }
                    appConfigs.add(appConfig);
                }
            }
        } catch (JSONException e) {
            Log.i(TAG, e.toString());
        }
        return appConfigs;
    }

    public static long parseWatermarkConfig(String configString) {
        if (TextUtils.isEmpty(configString)) {
            return -1L;
        }
        try {
            JSONObject object = new JSONObject(configString);
            return object.optLong(WATERMARK, -1L);
        } catch (JSONException e) {
            return -1L;
        }
    }
}

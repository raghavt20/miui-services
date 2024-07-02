package com.android.server.input.overscroller;

import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ScrollerOptimizationConfigProviderUtils {
    public static final String APP_LIST_NAME = "appList";
    private static final String FLING_VELOCITY_MAXIMUM = "flingVelocityMaximum";
    private static final String FLING_VELOCITY_SCALED = "flingVelocityScaled";
    private static final String FLING_VELOCITY_THRESHOLD = "flingVelocityThreshold";
    private static final String FLY_WHEEL = "flywheel";
    private static final String FLY_WHEEL_PARAM_1 = "flywheelParam1";
    private static final String FLY_WHEEL_PARAM_2 = "flywheelParam2";
    private static final String FLY_WHEEL_PARAM_3 = "flywheelParam3";
    private static final String FLY_WHEEL_TIME_INTERVAL_THRESHOLD = "flywheelTimeIntervalThreshold";
    private static final String FLY_WHEEL_VELOCITY_THRESHOLD_1 = "flywheelVelocityThreshold1";
    private static final String FLY_WHEEL_VELOCITY_THRESHOLD_2 = "flywheelVelocityThreshold2";
    private static final String FRICTION = "friction";
    private static final String OPTIMIZE_ENABLE = "isOptimizeEnable";
    public static final String PACKAGE_NAME = "packageName";
    private static final String TAG = ScrollerOptimizationConfigProviderUtils.class.getSimpleName();
    private static final String VELOCITY_THRESHOLD = "velocityThreshold";

    public static String parseGeneralConfig(JSONObject jsonAll) throws JSONException {
        if (jsonAll.length() == 0) {
            return null;
        }
        JSONObject jsonGeneralConfig = new JSONObject();
        if (jsonAll.has(OPTIMIZE_ENABLE)) {
            int isOptimizeEnable = jsonAll.getInt(OPTIMIZE_ENABLE);
            jsonGeneralConfig.put(OPTIMIZE_ENABLE, isOptimizeEnable);
        }
        if (jsonAll.has(FRICTION)) {
            double friction = jsonAll.getDouble(FRICTION);
            jsonGeneralConfig.put(FRICTION, friction);
        }
        if (jsonAll.has(VELOCITY_THRESHOLD)) {
            int velocityThreshold = jsonAll.getInt(VELOCITY_THRESHOLD);
            jsonGeneralConfig.put(VELOCITY_THRESHOLD, velocityThreshold);
        }
        if (jsonAll.has(FLY_WHEEL)) {
            int flywheel = jsonAll.getInt(FLY_WHEEL);
            jsonGeneralConfig.put(FLY_WHEEL, flywheel);
        }
        if (jsonAll.has(FLY_WHEEL_TIME_INTERVAL_THRESHOLD)) {
            int flywheelTimeIntervalThreshold = jsonAll.getInt(FLY_WHEEL_TIME_INTERVAL_THRESHOLD);
            jsonGeneralConfig.put(FLY_WHEEL_TIME_INTERVAL_THRESHOLD, flywheelTimeIntervalThreshold);
        }
        if (jsonAll.has(FLY_WHEEL_VELOCITY_THRESHOLD_1)) {
            int flywheelVelocityThreshold1 = jsonAll.getInt(FLY_WHEEL_VELOCITY_THRESHOLD_1);
            jsonGeneralConfig.put(FLY_WHEEL_VELOCITY_THRESHOLD_1, flywheelVelocityThreshold1);
        }
        if (jsonAll.has(FLY_WHEEL_VELOCITY_THRESHOLD_2)) {
            int flywheelVelocityThreshold2 = jsonAll.getInt(FLY_WHEEL_VELOCITY_THRESHOLD_2);
            jsonGeneralConfig.put(FLY_WHEEL_VELOCITY_THRESHOLD_2, flywheelVelocityThreshold2);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_1)) {
            double flywheelParam1 = jsonAll.getDouble(FLY_WHEEL_PARAM_1);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_1, flywheelParam1);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_2)) {
            double flywheelParam2 = jsonAll.getDouble(FLY_WHEEL_PARAM_2);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_2, flywheelParam2);
        }
        if (jsonAll.has(FLY_WHEEL_PARAM_3)) {
            int flywheelParam3 = jsonAll.getInt(FLY_WHEEL_PARAM_3);
            jsonGeneralConfig.put(FLY_WHEEL_PARAM_3, flywheelParam3);
        }
        if (jsonAll.has(FLING_VELOCITY_THRESHOLD)) {
            int flingVelocityThreshold = jsonAll.getInt(FLING_VELOCITY_THRESHOLD);
            jsonGeneralConfig.put(FLING_VELOCITY_THRESHOLD, flingVelocityThreshold);
        }
        if (jsonAll.has(FLING_VELOCITY_SCALED)) {
            double flingVelocityScaled = jsonAll.getDouble(FLING_VELOCITY_SCALED);
            jsonGeneralConfig.put(FLING_VELOCITY_SCALED, flingVelocityScaled);
        }
        if (jsonAll.has(FLING_VELOCITY_MAXIMUM)) {
            int flingVelocityMaximum = jsonAll.getInt(FLING_VELOCITY_MAXIMUM);
            jsonGeneralConfig.put(FLING_VELOCITY_MAXIMUM, flingVelocityMaximum);
        }
        return jsonGeneralConfig.toString();
    }

    public static String readLocalFile(String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        File file = new File(filePath);
        if (file.exists()) {
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int lenth = inputStream.read(buffer);
                        if (lenth == -1) {
                            break;
                        }
                        stringBuilder.append(new String(buffer, 0, lenth));
                    }
                    inputStream.close();
                } finally {
                }
            } catch (IOException e) {
                Slog.e(TAG, "exception when readLocalFile: ", e);
            }
        }
        return stringBuilder.toString();
    }

    public static JSONObject getLocalFileConfig(String filePath) {
        JSONObject jsonObject = new JSONObject();
        String configString = readLocalFile(filePath);
        try {
            JSONObject jsonObject2 = new JSONObject(configString);
            return jsonObject2;
        } catch (JSONException e) {
            Slog.e(TAG, "exception when getLocalFileConfig: ", e);
            return jsonObject;
        }
    }
}

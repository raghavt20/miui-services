package com.android.server.app;

import android.os.Build;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.app.GameManagerServiceStubImpl;
import com.android.server.wm.WindowManagerServiceStub;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class GmsUtil {
    private static final String TAG = "GameManagerServiceStub";

    public static boolean isContainDevice(HashSet<String> devices) {
        if (devices != null) {
            return devices.contains(Build.DEVICE);
        }
        return false;
    }

    public static String calcuRatio(int targetWidth, int currentWidth) {
        BigDecimal bigDecimal = new BigDecimal((targetWidth * 1.0f) / currentWidth);
        float ratio = bigDecimal.setScale(2, RoundingMode.UP).floatValue();
        if (ratio >= 1.0f) {
            Slog.d(TAG, "ratio >= 1  targetWidth = " + targetWidth + "  currentWidth = " + currentWidth);
            return "disable";
        }
        if (ratio > 0.6f) {
            int raTemp = (int) (ratio * 100.0f);
            int lastDigital = raTemp % 10;
            int firstDigital = raTemp / 10;
            if (lastDigital != 0) {
                lastDigital = 5;
            }
            ratio = (((firstDigital * 10) + lastDigital) * 1.0f) / 100.0f;
        }
        return Math.max(ratio, 0.6f) + "";
    }

    public static String getTargetRatioForPad(GameManagerServiceStubImpl.AppItem appItem, boolean mPowerSaving, GameManagerServiceStubImpl.DownscaleCloudData mDownscaleCloudData, boolean isDebugPowerSave) {
        int systemVersion;
        int mode = appItem.mode;
        if (mode == 0 || (systemVersion = appItem.systemVersion) == 0) {
            return "disable";
        }
        int appVersion = appItem.appVersion;
        if (appVersion > 1) {
            return "disable";
        }
        boolean saveBatteryEnable = (mode & 2) != 0;
        boolean containDevice = isDebugPowerSave || isContainDevice(mDownscaleCloudData.devices);
        boolean preVersionEnable = (systemVersion & 1) != 0;
        boolean devVersionEnable = (systemVersion & 2) != 0;
        boolean stableVersionEnable = (systemVersion & 4) != 0;
        if (((!preVersionEnable || !miui.os.Build.IS_PRE_VERSION) && ((!devVersionEnable || !miui.os.Build.IS_DEV_VERSION) && (!stableVersionEnable || !miui.os.Build.IS_STABLE_VERSION))) || !mPowerSaving || !saveBatteryEnable || !containDevice) {
            return "disable";
        }
        return mDownscaleCloudData.scenes.padSaveBattery;
    }

    public static boolean isWQHD(int currentWidth) {
        int[] resolutionArray = FeatureParser.getIntArray("screen_resolution_supported");
        boolean screenCompatSupported = FeatureParser.getBoolean("screen_compat_supported", false);
        if (miui.os.Build.IS_TABLET || currentWidth <= 1080 || (resolutionArray == null && !screenCompatSupported)) {
            return false;
        }
        Slog.d(TAG, "2K need downscale ");
        return true;
    }

    public static boolean needDownscale(String packageName, GameManagerServiceStubImpl.DownscaleCloudData downscaleCloudData, int currentWidth) {
        if (TextUtils.isEmpty(packageName) || !checkValidApp(packageName, downscaleCloudData)) {
            return false;
        }
        int[] resolutionArray = FeatureParser.getIntArray("screen_resolution_supported");
        boolean screenCompatSupported = FeatureParser.getBoolean("screen_compat_supported", false);
        if (!miui.os.Build.IS_TABLET && currentWidth > 1080 && (resolutionArray != null || screenCompatSupported)) {
            Slog.d(TAG, "2K need downscale ");
            return true;
        }
        if (downscaleCloudData != null) {
            boolean contained = isContainDevice(downscaleCloudData.devices);
            if (contained) {
                Slog.d(TAG, "power need downscale ");
                return true;
            }
            float currentScale = WindowManagerServiceStub.get().getCompatScale(packageName, 0);
            if (currentScale != 1.0f) {
                Slog.d(TAG, "need downscale ");
                return true;
            }
        }
        return false;
    }

    public static boolean checkValidApp(String packageName, GameManagerServiceStubImpl.DownscaleCloudData downscaleCloudData) {
        if (TextUtils.isEmpty(packageName)) {
            return true;
        }
        if (downscaleCloudData != null && downscaleCloudData.apps != null) {
            GameManagerServiceStubImpl.AppItem app = downscaleCloudData.apps.get(packageName);
            return app != null;
        }
        return false;
    }
}

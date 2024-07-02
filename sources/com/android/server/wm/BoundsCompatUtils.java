package com.android.server.wm;

import android.app.WindowConfiguration;
import android.app.servertransaction.BoundsCompat;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import com.android.server.wm.DisplayPolicy;

/* loaded from: classes.dex */
public class BoundsCompatUtils {
    private static volatile BoundsCompatUtils sSingleInstance = null;
    private static final Object M_LOCK = new Object();

    public static BoundsCompatUtils getInstance() {
        if (sSingleInstance == null) {
            synchronized (M_LOCK) {
                if (sSingleInstance == null) {
                    sSingleInstance = new BoundsCompatUtils();
                }
            }
        }
        return sSingleInstance;
    }

    public Configuration getCompatConfiguration(Configuration globalConfig, float aspectRatio, DisplayContent dp, float scale) {
        Configuration compatConfig = new Configuration(globalConfig);
        if (aspectRatio < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            Slog.w("BoundsCompatUtils", "aspectRatio =" + aspectRatio + ",skip");
            return compatConfig;
        }
        Rect globalBounds = globalConfig.windowConfiguration.getBounds();
        globalConfig.windowConfiguration.getAppBounds();
        Point displaySize = new Point(globalBounds.width(), globalBounds.height());
        Rect compatBounds = BoundsCompat.getInstance().computeCompatBounds(aspectRatio, displaySize, getCompatGravity(globalConfig.windowConfiguration, 17), globalConfig.orientation, 0, scale);
        compatConfig.windowConfiguration.setBounds(compatBounds);
        compatConfig.windowConfiguration.setAppBounds(compatBounds);
        compatConfig.windowConfiguration.setMaxBounds(compatBounds);
        adaptCompatBounds(compatConfig, dp);
        return compatConfig;
    }

    public void adaptCompatBounds(Configuration config, DisplayContent dc) {
        Rect bounds = config.windowConfiguration.getBounds();
        if (bounds.isEmpty() || config.densityDpi == 0) {
            return;
        }
        float density = config.densityDpi / 160.0f;
        int width = bounds.width();
        int height = bounds.height();
        int i = (int) ((width / density) + 0.5d);
        config.compatScreenWidthDp = i;
        config.screenWidthDp = i;
        config.orientation = width <= height ? 1 : 2;
        config.windowConfiguration.setRotation(width <= height ? 0 : config.windowConfiguration.getRotation());
        if (config.orientation == 1 && dc != null) {
            DisplayPolicy.DecorInsets.Info info = dc.getDisplayPolicy().getDecorInsetsInfo(0, width, height);
            config.screenHeightDp = (int) ((info.mConfigFrame.height() / density) + 0.5d);
        } else {
            int i2 = (int) (height / density);
            config.compatScreenHeightDp = i2;
            config.screenHeightDp = i2;
        }
        int shortSizeDp = width <= height ? config.screenWidthDp : config.screenHeightDp;
        int longSizeDp = width >= height ? config.screenWidthDp : config.screenHeightDp;
        config.compatSmallestScreenWidthDp = shortSizeDp;
        config.smallestScreenWidthDp = shortSizeDp;
        int sl = Configuration.resetScreenLayout(config.screenLayout);
        config.screenLayout = Configuration.reduceScreenLayout(sl, longSizeDp, shortSizeDp);
    }

    public float getGlobalScaleByName(String pkgName, int flipCompatMode, Rect bounds) {
        int scaleMode = ActivityTaskManagerServiceStub.get().getScaleMode(pkgName);
        if (scaleMode == 3 || bounds.isEmpty()) {
            return -1.0f;
        }
        if (MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            if (flipCompatMode == 1) {
                float scale = MiuiSizeCompatService.FLIP_UNSCALE;
                return scale;
            }
            if (flipCompatMode != 2 && flipCompatMode != -1) {
                return -1.0f;
            }
            float scale2 = MiuiSizeCompatService.FLIP_DEFAULT_SCALE;
            return scale2;
        }
        int longSide = Math.max(bounds.width(), bounds.height());
        int shortSide = Math.min(bounds.width(), bounds.height());
        float scale3 = (shortSide * 1.0f) / longSide;
        return scale3;
    }

    public static int getCompatGravity(WindowConfiguration windowConfiguration, int mGravity) {
        if (MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            if (windowConfiguration.getDisplayRotation() == 0) {
                return 5;
            }
            if (windowConfiguration.getDisplayRotation() == 2) {
                return 3;
            }
            if (windowConfiguration.getDisplayRotation() == 1) {
                return 48;
            }
            if (windowConfiguration.getDisplayRotation() == 1) {
                return 80;
            }
            return mGravity;
        }
        return mGravity;
    }

    public int getFlipCompatModeByApp(ActivityTaskManagerService atms, String packageName) {
        try {
            ApplicationInfo applicationInfo = atms.getPackageManager().getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            if (applicationInfo != null && applicationInfo.metaData != null && applicationInfo.metaData.containsKey("miui.supportFlipFullScreen")) {
                return applicationInfo.metaData.getInt("miui.supportFlipFullScreen");
            }
            return -1;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public int getFlipCompatModeByActivity(ActivityRecord activityRecord) {
        try {
            ActivityInfo activityInfo = activityRecord.mAtmService.getPackageManager().getActivityInfo(activityRecord.mActivityComponent, 128L, UserHandle.myUserId());
            if (activityInfo != null && activityInfo.metaData != null && activityInfo.metaData.containsKey("miui.supportFlipFullScreen")) {
                return activityInfo.metaData.getInt("miui.supportFlipFullScreen");
            }
            return -1;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}

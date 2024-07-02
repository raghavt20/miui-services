package com.android.server.tof;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Slog;
import com.miui.tof.gesture.TofGestureAppData;
import com.miui.tof.gesture.TofGestureAppDetailInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class AppFeatureHelper {
    private static final String TAG = "AppFeatureHelper";
    private static volatile AppFeatureHelper mInstance;
    private String mCloudGestureConfig;
    private Context mContext;
    private PackageManager mPackageManager;
    private String mResGestureConfig;
    private final HashMap<String, List<TofGestureComponent>> mTofGestureAppConfigs = new HashMap<>();

    private AppFeatureHelper() {
    }

    public static AppFeatureHelper getInstance() {
        if (mInstance == null) {
            synchronized (AppFeatureHelper.class) {
                if (mInstance == null) {
                    mInstance = new AppFeatureHelper();
                }
            }
        }
        return mInstance;
    }

    public String getResGestureConfig() {
        return this.mResGestureConfig;
    }

    public void setResGestureConfig(String mResGestureConfig) {
        this.mResGestureConfig = mResGestureConfig;
    }

    public String getCloudGestureConfig() {
        return this.mCloudGestureConfig;
    }

    public void setCloudGestureConfig(String mCloudGestureConfig) {
        this.mCloudGestureConfig = mCloudGestureConfig;
    }

    public int getSupportFeature(ComponentName componentName, boolean vertical) {
        if (componentName != null) {
            return getSupportFeature(componentName.getPackageName(), componentName.getClassName(), vertical);
        }
        return 0;
    }

    public int getSupportFeature(ComponentName componentName) {
        return getSupportFeature(componentName, false);
    }

    public int getAppCategory(ComponentName componentName) {
        if (componentName != null) {
            return getAppCategory(componentName.getPackageName(), componentName.getClassName());
        }
        return 0;
    }

    private int getSupportFeature(String pkg, String activity, boolean vertical) {
        TofGestureComponent tofGestureComponent = getSupportComponentFromConfig(pkg, activity, false);
        if (tofGestureComponent != null) {
            return vertical ? tofGestureComponent.getPortraitFeature() : tofGestureComponent.getLandscapeFeature();
        }
        return 0;
    }

    private int getAppCategory(String pkg, String activity) {
        TofGestureComponent tofGestureComponent = getSupportComponentFromConfig(pkg, activity, false);
        if (tofGestureComponent == null) {
            return 0;
        }
        return tofGestureComponent.getCategory();
    }

    public TofGestureComponent getSupportComponentFromConfig(ComponentName componentName) {
        if (componentName != null) {
            return getSupportComponentFromConfig(componentName.getPackageName(), componentName.getClassName(), false);
        }
        return null;
    }

    public TofGestureComponent getSupportComponentFromConfig(String pkg, String activity, boolean includeDisable) {
        boolean enabled;
        synchronized (this.mTofGestureAppConfigs) {
            List<TofGestureComponent> tofGestureComponents = this.mTofGestureAppConfigs.get(pkg);
            if (tofGestureComponents != null) {
                if (AppStatusManager.statusHasChanged(this.mContext, pkg)) {
                    enabled = AppStatusManager.isAppEnabled(this.mContext, pkg);
                } else {
                    enabled = tofGestureComponents.get(0).isEnable();
                }
                for (TofGestureComponent tofGestureComponent : tofGestureComponents) {
                    PackageInfo info = getPackageInfo(pkg);
                    if (info != null && tofGestureComponent.isVersionSupported(info.versionCode) && (enabled || includeDisable)) {
                        String configActivity = tofGestureComponent.getActivityName();
                        String configPackageName = tofGestureComponent.getPackageName();
                        if (activity.equals(configActivity) || (configActivity.isEmpty() && configPackageName.equals(pkg))) {
                            return tofGestureComponent;
                        }
                    }
                }
            }
            return null;
        }
    }

    public void initTofComponentConfig(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        parseTofGestureComponentFromRes(context);
    }

    public void parseContactlessGestureComponentFromCloud(List<String> componentFeatureList) {
        if (componentFeatureList == null || componentFeatureList.size() == 0) {
            return;
        }
        setCloudGestureConfig(componentFeatureList.toString());
        this.mTofGestureAppConfigs.clear();
        for (String item : componentFeatureList) {
            TofGestureComponent gestureApp = parseTofGestureComponent(item);
            addTofGestureAppConfig(gestureApp);
        }
    }

    private void parseTofGestureComponentFromRes(Context context) {
        String[] array = context.getResources().getStringArray(285409367);
        if (array == null) {
            Slog.e(TAG, "get config_tofComponentSupport resource error.");
            return;
        }
        setResGestureConfig(Arrays.toString(array));
        for (String str : array) {
            TofGestureComponent gestureApp = parseTofGestureComponent(str);
            addTofGestureAppConfig(gestureApp);
        }
    }

    private TofGestureComponent parseTofGestureComponent(String item) {
        boolean enabled;
        String[] appConfig = item.split(",");
        if (appConfig.length < 8) {
            Slog.e(TAG, "gesture component [" + item + "] length error!");
            return null;
        }
        String pkgName = appConfig[0];
        String activity = appConfig[1];
        try {
            int category = Integer.parseInt(appConfig[2]);
            if (!appConfig[3].isEmpty()) {
                int feature = hexStringToInt(appConfig[3]);
                int featureVertical = appConfig[4].isEmpty() ? feature : hexStringToInt(appConfig[4]);
                int minVersionCode = Integer.parseInt(appConfig[5]);
                int maxVersionCode = Integer.parseInt(appConfig[6]);
                if (Integer.parseInt(appConfig[7]) != 1) {
                    enabled = false;
                } else {
                    enabled = true;
                }
                return new TofGestureComponent(pkgName, activity, category, feature, featureVertical, minVersionCode, maxVersionCode, enabled);
            }
            Slog.e(TAG, "gesture component [" + item + "] feature config error!");
            return null;
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            Slog.e(TAG, e.toString());
            return null;
        }
    }

    private int hexStringToInt(String config) {
        if (config == null || config.isEmpty()) {
            return 0;
        }
        if (config.matches("\\d+")) {
            return Integer.parseInt(config);
        }
        if (!config.toLowerCase().startsWith("0x")) {
            return 0;
        }
        try {
            return Integer.parseInt(config.substring(2), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addTofGestureAppConfig(TofGestureComponent gestureApp) {
        if (gestureApp != null) {
            String pkgName = gestureApp.getPackageName();
            List<TofGestureComponent> tofGestureComponents = this.mTofGestureAppConfigs.get(pkgName);
            if (tofGestureComponents == null) {
                tofGestureComponents = new ArrayList();
            }
            tofGestureComponents.add(gestureApp);
            this.mTofGestureAppConfigs.put(pkgName, tofGestureComponents);
        }
    }

    public TofGestureAppData getTofGestureAppData() {
        TofGestureAppDetailInfo appDetailInfo;
        TofGestureAppData gestureAppData = new TofGestureAppData();
        List<TofGestureAppDetailInfo> tofGestureAppDetailInfos = new ArrayList<>();
        for (String pkgName : this.mTofGestureAppConfigs.keySet()) {
            PackageInfo info = getPackageInfo(pkgName);
            if (info != null && (appDetailInfo = getSupportTofGestureAppDetailInfo(info)) != null) {
                tofGestureAppDetailInfos.add(appDetailInfo);
            }
        }
        gestureAppData.setTofGestureAppDetailInfoList(tofGestureAppDetailInfos);
        return gestureAppData;
    }

    private PackageInfo getPackageInfo(String pkgName) {
        try {
            return this.mPackageManager.getPackageInfoAsUser(pkgName, 64, ActivityManager.getCurrentUser());
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, e.toString());
            return null;
        }
    }

    public boolean changeTofGestureAppConfig(String pkg, boolean enable) {
        List<TofGestureComponent> tofGestureComponents = this.mTofGestureAppConfigs.get(pkg);
        if (tofGestureComponents != null) {
            AppStatusManager.setAppEnable(this.mContext, pkg, enable);
            return true;
        }
        return false;
    }

    private TofGestureAppDetailInfo getSupportTofGestureAppDetailInfo(PackageInfo packageInfo) {
        boolean isEnable;
        if (packageInfo != null) {
            String pkgName = packageInfo.packageName;
            String appName = getAppName(packageInfo);
            int versionCode = packageInfo.versionCode;
            List<TofGestureComponent> tofGestureComponents = this.mTofGestureAppConfigs.get(pkgName);
            if (tofGestureComponents != null) {
                TofGestureComponent tofGestureComponent = tofGestureComponents.get(0);
                if (tofGestureComponent.isVersionSupported(versionCode)) {
                    String supportSceneDes = getSupportSceneDes(tofGestureComponent.getCategory(), tofGestureComponent.getPortraitFeature());
                    if (AppStatusManager.statusHasChanged(this.mContext, pkgName)) {
                        isEnable = AppStatusManager.isAppEnabled(this.mContext, pkgName);
                    } else {
                        isEnable = tofGestureComponent.isEnable();
                    }
                    TofGestureAppDetailInfo appDetailInfo = new TofGestureAppDetailInfo(pkgName, appName, isEnable, supportSceneDes);
                    return appDetailInfo;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private String getSupportSceneDes(int type, int portraitFeature) {
        switch (type) {
            case 1:
                if (portraitFeature > 0) {
                    String des = this.mContext.getString(286195783);
                    return des;
                }
                String des2 = this.mContext.getString(286196682);
                return des2;
            case 2:
                String des3 = this.mContext.getString(286196681);
                return des3;
            case 3:
                String des4 = this.mContext.getString(286196679);
                return des4;
            case 4:
                String des5 = this.mContext.getString(286196680);
                return des5;
            case 5:
            default:
                return "";
            case 6:
                String des6 = this.mContext.getString(286195782);
                return des6;
        }
    }

    private String getAppName(PackageInfo info) {
        if (info == null) {
            return "";
        }
        ApplicationInfo appInfo = info.applicationInfo;
        String appName = appInfo == null ? "" : appInfo.loadLabel(this.mPackageManager).toString();
        return appName;
    }
}

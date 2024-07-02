package com.android.server.wm;

import android.appcompat.ApplicationCompatUtilsStub;
import android.content.Context;
import android.content.res.Resources;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
class FoldablePackagePolicy extends PolicyImpl {
    private static final String ACTIVITY_NAME = "activity";
    private static final String ACTIVITY_NAME_PRINT_WRITER = "activity:activity:...";
    private static final String APP_CONTINUITY_BLACK_LIST_KEY = "app_continuity_blacklist";
    private static final String APP_CONTINUITY_ID_KEY = "id";
    private static final String APP_CONTINUITY_WHILTE_LIST_KEY = "app_continuity_whitelist";
    private static final String APP_INTERCEPT_ALLOWLIST_KEY = "app_intercept_allowlist";
    private static final String APP_INTERCEPT_BLACKLIST_KEY = "app_intercept_blacklist";
    private static final String APP_INTERCEPT_COMPONENT_ALLOWLIST_KEY = "app_intercept_component_allowlist";
    private static final String APP_INTERCEPT_COMPONENT_BLACKLIST_KEY = "app_intercept_component_blacklist";
    private static final String APP_RELAUNCH_BLACKLIST_KEY = "app_relaunch_blacklist";
    private static final String APP_RESTART_BLACKLIST_KEY = "app_restart_blacklist";
    private static final String CALLBACK_NAME_DISPLAY_COMPAT = "displayCompat";
    private static final String CALLBACK_NAME_FULLSCREEN = "fullscreenpackage";
    private static final String CALLBACK_NAME_FULLSCREEN_COMPONENT = "fullscreencomponent";
    private static final String COMMAND_OPTION_ALLOW_LIST = "allowlist";
    private static final String COMMAND_OPTION_BLOCK_LIST = "blocklist";
    private static final String COMMAND_OPTION_INTERCEPT_COMPONENT_LIST = "interceptcomlist";
    private static final String COMMAND_OPTION_INTERCEPT_LIST = "interceptlist";
    private static final String COMMAND_OPTION_RELAUNCH_LIST = "relaunchlist";
    private static final String COMMAND_OPTION_RESTART_LIST = "restartlist";
    private static final int DEFAULT_CONTINUITY_VERSION = 1199910;
    private static final String FORCE_RESIZABLE_ACTIVITIES = "force_resizable_activities";
    private static final String FULLSCREEN_DEMO_MODE = "demoMode";
    public static final String MIUI_SMART_ROTATION_ACTIVITY_KEY = "fullscreen_activity_list";
    public static final String MIUI_SMART_ROTATION_FULLSCREEN_KEY = "fullscreen_landscape_list";
    private static final String PACKAGE_NAME = "packageName";
    private static final String PACKAGE_NAME_PRINT_WRITER = "packageName:packageName:...";
    private static final String POLICY_NAME = "FoldablePackagePolicy";
    public static final String POLICY_VALUE_ALLOW_LIST = "w";
    public static final String POLICY_VALUE_BLOCK_LIST = "b";
    public static final String POLICY_VALUE_INTERCEPT_ALLOW_LIST = "ia";
    public static final String POLICY_VALUE_INTERCEPT_COMPONENT_ALLOW_LIST = "ica";
    public static final String POLICY_VALUE_INTERCEPT_COMPONENT_LIST = "ic";
    public static final String POLICY_VALUE_INTERCEPT_LIST = "i";
    public static final String POLICY_VALUE_RELAUNCH_LIST = "c";
    public static final String POLICY_VALUE_RESTART_LIST = "r";
    private static final String PULL_APP_FLAG_COMMAND = "-pullAppFlag";
    private static final String PULL_CLOUD_DATA_MODULE_COMMAND = "-pullCloudDataModule";
    private static final String SET_FIXED_ASPECT_RATIO = "setFixedAspectRatio";
    private static final String SET_FIXED_ASPECT_RATIO_COMMAND = "-setFixedAspectRatio";
    private static final String SET_FORCE_DISPLAY_COMPAT_COMMAND = "-setForceDisplayCompatMode";
    private static final String SET_FULLSCREEN_COMPONENT_COMMAND = "-setFullscreenActivity";
    private static final String SET_FULLSCREEN_LANDSCAPE_COMMAND = "-setFullscreenPackage";
    private static final String SET_FULLSCREEN_LANDSCAPE_COMMAND_OLD = "-setFullscreenlandscape";
    private static final String SET_FULLSCREEN_USER_COMMAND = "-setFullscreenUser";
    private static final String TAG = "FoldablePackagePolicy";
    private final ActivityTaskManagerServiceImpl mAtmServiceImpl;
    private final String mCompatModeModuleName;
    private final Context mContext;
    private final String mContinuityModuleName;
    private long mContinuityVersion;
    private boolean mDevelopmentForceResizable;
    private final List<String> mDisplayCompatAllowCloudAppList;
    private final List<String> mDisplayCompatBlockCloudAppList;
    private final List<String> mDisplayInterceptCloudAppAllowList;
    private final List<String> mDisplayInterceptCloudAppList;
    private final List<String> mDisplayInterceptComponentCloudAppAllowList;
    private final List<String> mDisplayInterceptComponentCloudAppList;
    private final List<String> mDisplayRelaunchCloudAppList;
    private final List<String> mDisplayRestartCloudAppList;
    private final List<String> mFullScreenActivityCloudList;
    private final List<String> mFullScreenPackageCloudAppList;
    private final Resources mResources;
    private final String mSmartRotationNModuleName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FoldablePackagePolicy(ActivityTaskManagerServiceImpl atmServiceImpl) {
        super(atmServiceImpl.mPackageConfigurationController, "FoldablePackagePolicy");
        this.mContinuityVersion = 1199910L;
        this.mAtmServiceImpl = atmServiceImpl;
        Context context = atmServiceImpl.mContext;
        this.mContext = context;
        Resources resources = context.getResources();
        this.mResources = resources;
        this.mContinuityModuleName = resources.getString(286195894);
        this.mCompatModeModuleName = resources.getString(286196569);
        this.mSmartRotationNModuleName = resources.getString(286196589);
        if (MiuiAppSizeCompatModeStub.get().isFoldScreenDevice()) {
            registerCallback(CALLBACK_NAME_DISPLAY_COMPAT, atmServiceImpl.mPackageSettingsManager.mDisplayCompatPackages.mCallback);
            registerCallback(CALLBACK_NAME_FULLSCREEN, atmServiceImpl.mFullScreenPackageManager.mFullScreenPackagelistCallback);
            registerCallback(CALLBACK_NAME_FULLSCREEN_COMPONENT, atmServiceImpl.mFullScreenPackageManager.mFullScreenComponentCallback);
        }
        this.mDisplayCompatAllowCloudAppList = new ArrayList();
        this.mDisplayCompatBlockCloudAppList = new ArrayList();
        this.mDisplayRestartCloudAppList = new ArrayList();
        this.mDisplayInterceptCloudAppList = new ArrayList();
        this.mDisplayInterceptComponentCloudAppList = new ArrayList();
        this.mDisplayInterceptCloudAppAllowList = new ArrayList();
        this.mDisplayInterceptComponentCloudAppAllowList = new ArrayList();
        this.mDisplayRelaunchCloudAppList = new ArrayList();
        this.mFullScreenPackageCloudAppList = new ArrayList();
        this.mFullScreenActivityCloudList = new ArrayList();
    }

    List<String> getListFromCloud(Context context, String moduleName, String key) {
        String data;
        List<String> arrayList = new ArrayList<>();
        if (TextUtils.isEmpty(moduleName)) {
            return arrayList;
        }
        try {
            data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), moduleName, key, (String) null);
            Slog.d("FoldablePackagePolicy", "getListFromCloud: data: " + data + " moduleName=" + moduleName + " key=" + key);
        } catch (JSONException e) {
            Slog.e("FoldablePackagePolicy", "exception when getListFromCloud: ", e);
        }
        if (TextUtils.isEmpty(data)) {
            return arrayList;
        }
        JSONArray apps = new JSONArray(data);
        for (int i = 0; i < apps.length(); i++) {
            arrayList.add(apps.getString(i));
        }
        Slog.d("FoldablePackagePolicy", "getListFromCloud: mCloudList: " + arrayList);
        return arrayList;
    }

    Map<String, List<String>> getMapFromCloud(Context context, String key, String jsonObjectName, String jsonArrayName) {
        MiuiSettings.SettingsCloudData.CloudData data;
        List<String> activityList;
        Map<String, List<String>> map = new HashMap<>();
        try {
            data = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), key, key, (String) null, false);
            Slog.d("FoldablePackagePolicy", "getMayFromCloud: data: " + data);
            activityList = new ArrayList<>();
        } catch (JSONException e) {
            Slog.e("FoldablePackagePolicy", "exception when getMayFromCloud: ", e);
        }
        if (data == null) {
            return map;
        }
        JSONArray apps = data.json().getJSONArray(key);
        if (apps == null) {
            return map;
        }
        for (int i = 0; i < apps.length(); i++) {
            activityList.clear();
            JSONObject jsonObj = apps.getJSONObject(i);
            JSONArray array = jsonObj.getJSONArray(jsonArrayName);
            for (int j = 0; j < array.length(); j++) {
                activityList.add(array.getString(j));
            }
            map.put(jsonObj.getString(jsonObjectName), activityList);
        }
        Slog.d("FoldablePackagePolicy", "getMayFromCloud: mCloudList: " + map);
        return map;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.wm.PolicyImpl
    public boolean executeShellCommandLocked(String command, String[] args, PrintWriter pw) {
        char c;
        switch (command.hashCode()) {
            case -1413282293:
                if (command.equals(SET_FULLSCREEN_LANDSCAPE_COMMAND_OLD)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1214887717:
                if (command.equals(PULL_APP_FLAG_COMMAND)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -177769610:
                if (command.equals(SET_FULLSCREEN_LANDSCAPE_COMMAND)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 672242143:
                if (command.equals(SET_FULLSCREEN_COMPONENT_COMMAND)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 989887131:
                if (command.equals(SET_FULLSCREEN_USER_COMMAND)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1486265465:
                if (command.equals(PULL_CLOUD_DATA_MODULE_COMMAND)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
            case 3:
                executeShellCommandLockedForSmartRotation(command, args, pw);
                return true;
            case 4:
                pw.println(getListFromCloud(this.mContext, args[0], args[1]));
                return true;
            case 5:
                if (ApplicationCompatUtilsStub.MIUI_SUPPORT_APP_CONTINUITY.equals(args[1]) || "android.supports_size_changes".equals(args[1])) {
                    if (this.mAtmServiceImpl.hasMetaData(args[0], args[1])) {
                        boolean continuityFlag = this.mAtmServiceImpl.getMetaDataBoolean(args[0], args[1]);
                        pw.println(args[1] + " = " + continuityFlag);
                    } else {
                        pw.println(args[1] + " not added!");
                    }
                }
                if ("android.max_aspect".equals(args[1])) {
                    if (this.mAtmServiceImpl.hasMetaData(args[0], args[1])) {
                        pw.println(args[1] + " = " + this.mAtmServiceImpl.getMetaDataFloat(args[0], args[1]));
                    } else {
                        pw.println(args[1] + " not added!");
                    }
                }
                return true;
            default:
                return false;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00cc, code lost:
    
        if (r12.equals(com.android.server.wm.FoldablePackagePolicy.SET_FULLSCREEN_COMPONENT_COMMAND) != false) goto L37;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void executeShellCommandLockedForSmartRotation(java.lang.String r12, java.lang.String[] r13, java.io.PrintWriter r14) {
        /*
            Method dump skipped, instructions count: 302
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.FoldablePackagePolicy.executeShellCommandLockedForSmartRotation(java.lang.String, java.lang.String[], java.io.PrintWriter):void");
    }

    @Override // com.android.server.wm.PolicyImpl
    int getLocalVersion() {
        return this.mResources.getInteger(285933618);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.wm.PolicyImpl
    void getPolicyDataMapFromLocal(String configurationName, ConcurrentHashMap<String, String> outPolicyDataMap) {
        char c;
        switch (configurationName.hashCode()) {
            case 746659883:
                if (configurationName.equals(CALLBACK_NAME_FULLSCREEN)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1373610914:
                if (configurationName.equals(CALLBACK_NAME_FULLSCREEN_COMPONENT)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 2022711012:
                if (configurationName.equals(CALLBACK_NAME_DISPLAY_COMPAT)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                String[] allowPackages = this.mResources.getStringArray(285409385);
                for (String str : allowPackages) {
                    outPolicyDataMap.put(str, POLICY_VALUE_ALLOW_LIST);
                }
                String[] blockPackages = this.mResources.getStringArray(285409386);
                for (String str2 : blockPackages) {
                    outPolicyDataMap.put(str2, POLICY_VALUE_BLOCK_LIST);
                }
                return;
            case 1:
                String[] landscapePackages = this.mResources.getStringArray(285409406);
                for (String str3 : landscapePackages) {
                    String[] split = str3.split(",");
                    String str4 = split.length == 2 ? split[1] : "";
                    outPolicyDataMap.put(split[0], str4);
                }
                return;
            case 2:
                String[] packages = this.mResources.getStringArray(285409405);
                for (String str5 : packages) {
                    String[] split2 = str5.split(",");
                    String str6 = split2.length == 2 ? split2[1] : "";
                    outPolicyDataMap.put(split2[0], str6);
                }
                return;
            default:
                return;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.wm.PolicyImpl
    void getPolicyDataMapFromCloud(String configurationName, ConcurrentHashMap<String, String> outPolicyDataMap) {
        char c;
        switch (configurationName.hashCode()) {
            case 746659883:
                if (configurationName.equals(CALLBACK_NAME_FULLSCREEN)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1373610914:
                if (configurationName.equals(CALLBACK_NAME_FULLSCREEN_COMPONENT)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 2022711012:
                if (configurationName.equals(CALLBACK_NAME_DISPLAY_COMPAT)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                try {
                    this.mContinuityVersion = MiuiSettings.SettingsCloudData.getCloudDataInt(this.mContext.getContentResolver(), this.mContinuityModuleName, APP_CONTINUITY_ID_KEY, 0);
                } catch (Exception e) {
                    Slog.w("FoldablePackagePolicy", "Something is wrong.", e);
                }
                this.mDisplayCompatAllowCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_CONTINUITY_WHILTE_LIST_KEY));
                if (!this.mDisplayCompatAllowCloudAppList.isEmpty()) {
                    String[] allowPackages = (String[]) this.mDisplayCompatAllowCloudAppList.toArray(new String[0]);
                    for (String str : allowPackages) {
                        outPolicyDataMap.put(str, POLICY_VALUE_ALLOW_LIST);
                    }
                }
                this.mDisplayCompatBlockCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_CONTINUITY_BLACK_LIST_KEY));
                if (!this.mDisplayCompatBlockCloudAppList.isEmpty()) {
                    for (String str2 : (String[]) this.mDisplayCompatBlockCloudAppList.toArray(new String[0])) {
                        outPolicyDataMap.put(str2, POLICY_VALUE_BLOCK_LIST);
                    }
                }
                this.mDisplayRestartCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_RESTART_BLACKLIST_KEY));
                if (!this.mDisplayRestartCloudAppList.isEmpty()) {
                    for (String str3 : (String[]) this.mDisplayRestartCloudAppList.toArray(new String[0])) {
                        outPolicyDataMap.put(str3, POLICY_VALUE_RESTART_LIST);
                    }
                }
                this.mDisplayInterceptCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_INTERCEPT_BLACKLIST_KEY));
                if (!this.mDisplayInterceptCloudAppList.isEmpty()) {
                    for (String str4 : (String[]) this.mDisplayInterceptCloudAppList.toArray(new String[0])) {
                        outPolicyDataMap.put(str4, POLICY_VALUE_INTERCEPT_LIST);
                    }
                }
                this.mDisplayInterceptComponentCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_INTERCEPT_COMPONENT_BLACKLIST_KEY));
                if (!this.mDisplayInterceptComponentCloudAppList.isEmpty()) {
                    for (String str5 : (String[]) this.mDisplayInterceptComponentCloudAppList.toArray(new String[0])) {
                        outPolicyDataMap.put(str5, POLICY_VALUE_INTERCEPT_COMPONENT_LIST);
                    }
                }
                this.mDisplayInterceptCloudAppAllowList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_INTERCEPT_ALLOWLIST_KEY));
                if (!this.mDisplayInterceptCloudAppAllowList.isEmpty()) {
                    for (String str6 : (String[]) this.mDisplayInterceptCloudAppAllowList.toArray(new String[0])) {
                        outPolicyDataMap.put(str6, POLICY_VALUE_INTERCEPT_ALLOW_LIST);
                    }
                }
                this.mDisplayInterceptComponentCloudAppAllowList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_INTERCEPT_COMPONENT_ALLOWLIST_KEY));
                if (!this.mDisplayInterceptComponentCloudAppAllowList.isEmpty()) {
                    for (String str7 : (String[]) this.mDisplayInterceptComponentCloudAppAllowList.toArray(new String[0])) {
                        outPolicyDataMap.put(str7, POLICY_VALUE_INTERCEPT_COMPONENT_ALLOW_LIST);
                    }
                }
                this.mDisplayRelaunchCloudAppList.addAll(getListFromCloud(this.mContext, this.mContinuityModuleName, APP_RELAUNCH_BLACKLIST_KEY));
                if (!this.mDisplayRelaunchCloudAppList.isEmpty()) {
                    String[] blockPackages = (String[]) this.mDisplayRelaunchCloudAppList.toArray(new String[0]);
                    for (String str8 : blockPackages) {
                        outPolicyDataMap.put(str8, POLICY_VALUE_RELAUNCH_LIST);
                    }
                    return;
                }
                return;
            case 1:
                this.mFullScreenPackageCloudAppList.addAll(getListFromCloud(this.mContext, this.mSmartRotationNModuleName, MIUI_SMART_ROTATION_FULLSCREEN_KEY));
                if (!this.mFullScreenPackageCloudAppList.isEmpty()) {
                    outPolicyDataMap.clear();
                    for (String str9 : (String[]) this.mFullScreenPackageCloudAppList.toArray(new String[0])) {
                        String[] split = str9.split(",");
                        String str10 = split.length == 2 ? split[1] : "";
                        outPolicyDataMap.put(split[0], str10);
                    }
                    return;
                }
                return;
            case 2:
                this.mFullScreenActivityCloudList.addAll(getListFromCloud(this.mContext, this.mSmartRotationNModuleName, MIUI_SMART_ROTATION_ACTIVITY_KEY));
                List<String> list = this.mFullScreenActivityCloudList;
                if (list != null && !list.isEmpty()) {
                    outPolicyDataMap.clear();
                    String[] packages = (String[]) this.mFullScreenActivityCloudList.toArray(new String[0]);
                    for (String str11 : packages) {
                        String[] split2 = str11.split(",");
                        String str12 = split2.length == 2 ? split2[1] : "";
                        outPolicyDataMap.put(split2[0], str12);
                    }
                    return;
                }
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.wm.PolicyImpl
    boolean isDisabledConfiguration(String configurationName) {
        return Settings.Global.getInt(this.mContext.getContentResolver(), FORCE_RESIZABLE_ACTIVITIES, 0) != 0 && CALLBACK_NAME_DISPLAY_COMPAT.equals(configurationName);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public List<String> getContinuityList(String policyName) {
        char c;
        if (TextUtils.isEmpty(policyName)) {
            return new ArrayList();
        }
        switch (policyName.hashCode()) {
            case -1955062848:
                if (policyName.equals(APP_INTERCEPT_COMPONENT_BLACKLIST_KEY)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1909974378:
                if (policyName.equals(APP_CONTINUITY_WHILTE_LIST_KEY)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -715471604:
                if (policyName.equals(APP_INTERCEPT_ALLOWLIST_KEY)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -633210206:
                if (policyName.equals(APP_RELAUNCH_BLACKLIST_KEY)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -456574897:
                if (policyName.equals(APP_RESTART_BLACKLIST_KEY)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 244803266:
                if (policyName.equals(APP_INTERCEPT_BLACKLIST_KEY)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1076888428:
                if (policyName.equals(APP_CONTINUITY_BLACK_LIST_KEY)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1379629578:
                if (policyName.equals(APP_INTERCEPT_COMPONENT_ALLOWLIST_KEY)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return this.mDisplayCompatAllowCloudAppList;
            case 1:
                return this.mDisplayCompatBlockCloudAppList;
            case 2:
                return this.mDisplayRestartCloudAppList;
            case 3:
                return this.mDisplayInterceptCloudAppList;
            case 4:
                return this.mDisplayInterceptComponentCloudAppList;
            case 5:
                return this.mDisplayRelaunchCloudAppList;
            case 6:
                return this.mDisplayInterceptCloudAppAllowList;
            case 7:
                return this.mDisplayInterceptComponentCloudAppAllowList;
            default:
                return new ArrayList();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getContinuityVersion() {
        return this.mContinuityVersion;
    }

    void printOptionsRequires(PrintWriter pw, String cmd) {
        pw.println(cmd + " options requires:");
    }

    void printCommandHelp(PrintWriter pw, String cmd, String[] options) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String option : options) {
            stringBuilder.append(" [");
            stringBuilder.append(option);
            stringBuilder.append("]");
        }
        pw.println(cmd + stringBuilder.toString());
    }
}

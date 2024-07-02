package com.miui.server.multisence;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class MultiSenceConfig {
    private static final int DEBUG_BIT_CONFIG = 8;
    private static final int DEBUG_BIT_DYNAMIC_CONTROLLER = 16;
    private static final int DEBUG_BIT_DYN_FPS = 2;
    private static final int DEBUG_BIT_FW = 64;
    private static final int DEBUG_BIT_POLICY_BASE = 1;
    private static final int DEBUG_BIT_TRACE = 32;
    private static final int DEBUG_BIT_VRS = 4;
    private static final int NONSUPPORT_DYNAMIC_FPS = 1;
    private static final int NONSUPPORT_FRAMEPREDICT = 4;
    private static final int NONSUPPORT_FW = 32;
    private static final int NONSUPPORT_MULTITASK_ANIM_SCHED = 2;
    private static final int NONSUPPORT_POLICY_WINDOWSIZE = 16;
    private static final int NONSUPPORT_VRS = 8;
    public static final String PROPERTY_PREFIX = "persist.multisence.";
    public static final String SERVICE_JAVA_NAME = "MultiSence";
    private static final String TAG = "MultiSenceConfig";
    public Set<String> floatingWindowWhiteList;
    private Context mContext;
    private boolean mReady;
    MultiSenceService mService;
    private Set<String> mWhiteListVRS;
    public static final String PROP_SUB_FUNC_NONSUPPORT = "persist.multisence.nonsupport";
    private static int SUB_FUNC_NONSUPPORT = SystemProperties.getInt(PROP_SUB_FUNC_NONSUPPORT, 0);
    public static final String PROP_MULTISENCE_ENABLE = "persist.multisence.enable";
    public static final boolean SERVICE_ENABLED = SystemProperties.getBoolean(PROP_MULTISENCE_ENABLE, true);
    public static boolean DYN_FPS_ENABLED = false;
    public static boolean MULTITASK_ANIM_SCHED_ENABLED = false;
    public static boolean FREMAPREDICT_ENABLE = false;
    public static boolean VRS_ENABLE = false;
    public static boolean POLICY_WINDOWSIZE_ENABLE = false;
    public static boolean FW_ENABLE = false;
    public static boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    public static boolean DEBUG_POLICY_BASE = false;
    public static boolean DEBUG_DYN_FPS = false;
    public static boolean DEBUG_VRS = false;
    public static boolean DEBUG_CONFIG = false;
    public static boolean DEBUG_DYNAMIC_CONTROLLER = false;
    public static boolean DEBUG_TRACE = false;
    public static boolean DEBUG_FW = false;

    /* loaded from: classes.dex */
    public static class MultiSenceConfigHolder {
        private static final MultiSenceConfig INSTANCE = new MultiSenceConfig();
    }

    private MultiSenceConfig() {
        this.mReady = false;
        this.mWhiteListVRS = new HashSet();
        this.floatingWindowWhiteList = new HashSet();
    }

    public static MultiSenceConfig getInstance() {
        return MultiSenceConfigHolder.INSTANCE;
    }

    public void init(MultiSenceService service, Context context) {
        this.mService = service;
        this.mContext = context;
        updateDebugInfo();
        if (this.mService != null) {
            this.mReady = true;
        }
    }

    public void updateDebugInfo() {
        boolean z = false;
        int debugCode = SystemProperties.getInt("persist.multisence.debug.code", 0);
        boolean z2 = SystemProperties.getBoolean("persist.multisence.debug.on", false);
        DEBUG = z2;
        DEBUG_POLICY_BASE = z2 && (debugCode & 1) != 0;
        DEBUG_DYN_FPS = z2 && (debugCode & 2) != 0;
        DEBUG_VRS = z2 && (debugCode & 4) != 0;
        DEBUG_CONFIG = z2 && (debugCode & 8) != 0;
        DEBUG_DYNAMIC_CONTROLLER = z2 && (debugCode & 16) != 0;
        DEBUG_TRACE = z2 && (debugCode & 32) != 0;
        if (z2 && (debugCode & 64) != 0) {
            z = true;
        }
        DEBUG_FW = z;
    }

    public void updateSubFuncStatus() {
        boolean z = false;
        SUB_FUNC_NONSUPPORT = SystemProperties.getInt(PROP_SUB_FUNC_NONSUPPORT, 0);
        boolean z2 = SERVICE_ENABLED;
        DYN_FPS_ENABLED = z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 1) == 0;
        MULTITASK_ANIM_SCHED_ENABLED = z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 2) == 0;
        FREMAPREDICT_ENABLE = z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 4) == 0;
        VRS_ENABLE = z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 8) == 0;
        POLICY_WINDOWSIZE_ENABLE = z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 16) == 0;
        if (z2 && MultiSenceDynamicController.IS_CLOUD_ENABLED && (SUB_FUNC_NONSUPPORT & 32) == 0) {
            z = true;
        }
        FW_ENABLE = z;
        if (this.mReady && !DYN_FPS_ENABLED && this.mService.getPolicy() != null) {
            this.mService.getPolicy().resetDynFps();
        }
        if (this.mReady && !VRS_ENABLE && this.mService.getPolicy() != null) {
            this.mService.getPolicy().resetVrs();
        }
        MultiSenceServiceUtils.msLogD(DEBUG_CONFIG, showSubFunctionStatus());
    }

    public void initVRSWhiteList() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        String cloudInfo = Settings.System.getStringForUser(context.getContentResolver(), MultiSenceDynamicController.VRS_WHITELIST, -2);
        updateVRSWhiteListAll(cloudInfo);
    }

    public void updateVRSWhiteListAll(String whitelistinfo) {
        if (whitelistinfo == null || whitelistinfo.length() == 0) {
            return;
        }
        MultiSenceServiceUtils.msLogD(DEBUG_VRS, "vrs whitelist: " + whitelistinfo);
        StringBuilder whitelistinfoModify = new StringBuilder();
        for (char c : whitelistinfo.toCharArray()) {
            if (c != '[' && c != ']' && c != '\"') {
                whitelistinfoModify.append(c);
            }
        }
        MultiSenceServiceUtils.msLogD(DEBUG_VRS, "mofidied: " + whitelistinfoModify.toString());
        String[] nameWhiteList = whitelistinfoModify.toString().split(",");
        int count = 0;
        for (String name : nameWhiteList) {
            MultiSenceServiceUtils.msLogD(DEBUG_VRS, "[" + count + "]" + name);
            updateVRSWhiteListByApp(name, true);
            count++;
        }
    }

    public void updateVRSWhiteListByApp(String name, boolean isAdd) {
        if (name == null) {
            Slog.w(TAG, "invalid operation when add app to support VRS");
            return;
        }
        boolean isContained = this.mWhiteListVRS.contains(name);
        if (isAdd && !isContained) {
            this.mWhiteListVRS.add(name);
        } else if (!isAdd && isContained) {
            this.mWhiteListVRS.remove(name);
        }
    }

    public boolean checkVrsSupport(String name) {
        if (!this.mWhiteListVRS.contains(name)) {
            return false;
        }
        return true;
    }

    public void initFWWhiteList() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        String cloudInfo = Settings.System.getStringForUser(context.getContentResolver(), MultiSenceDynamicController.FW_WHITELIST, -2);
        if (cloudInfo == null || cloudInfo.length() == 0) {
            String[] fwPackageNames = this.mContext.getResources().getStringArray(285409393);
            this.floatingWindowWhiteList.addAll(Arrays.asList(fwPackageNames));
            MultiSenceServiceUtils.msLogD(DEBUG_FW, "local floating window whitelist: " + this.floatingWindowWhiteList);
            return;
        }
        updateFWWhiteListAll(cloudInfo);
    }

    public void updateFWWhiteListAll(String whitelistinfo) {
        MultiSenceServiceUtils.msLogD(DEBUG_FW, "floating window whitelist: " + whitelistinfo);
        StringBuilder whitelistinfoModify = new StringBuilder();
        for (char c : whitelistinfo.toCharArray()) {
            if (c != '[' && c != ']' && c != '\"') {
                whitelistinfoModify.append(c);
            }
        }
        MultiSenceServiceUtils.msLogD(DEBUG_FW, "modified: " + whitelistinfoModify.toString());
        synchronized (this.floatingWindowWhiteList) {
            this.floatingWindowWhiteList.clear();
            this.floatingWindowWhiteList.addAll(Arrays.asList(whitelistinfoModify.toString().split(",")));
            MultiSenceServiceUtils.msLogD(DEBUG_FW, "cloud floating window whitelist: " + this.floatingWindowWhiteList);
        }
    }

    public void dumpConfig(PrintWriter pw) {
        StringBuilder details = new StringBuilder("Config:\n");
        details.append(showSubFunctionStatus());
        pw.print(details);
    }

    public String showSubFunctionStatus() {
        StringBuilder subFunctionStatus = new StringBuilder("  Subfunction support: {\n");
        subFunctionStatus.append("    dyn-fps: " + DYN_FPS_ENABLED + ",\n");
        subFunctionStatus.append("    multi-task-animation: " + MULTITASK_ANIM_SCHED_ENABLED + ",\n");
        subFunctionStatus.append("    framepredict: " + FREMAPREDICT_ENABLE + ",\n");
        subFunctionStatus.append("    VRS: " + VRS_ENABLE + ",\n");
        subFunctionStatus.append("    policy[window-size]: " + POLICY_WINDOWSIZE_ENABLE + ",\n");
        subFunctionStatus.append("    floatingwindow: " + FW_ENABLE + ",\n");
        subFunctionStatus.append("  }\n");
        return subFunctionStatus.toString();
    }
}

package com.miui.server.multisence;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;

/* loaded from: classes.dex */
public class MultiSenceDynamicController {
    private static final String CLOUD_MULTISENCE_ENABLE = "cloud_multisence_enable";
    private static final String CLOUD_MULTISENCE_SUBFUNC_NONSUPPORT = "cloud_multisence_subfunc_nonsupport";
    public static final String FW_WHITELIST = "support_fw_app";
    private static final String KEY_POWER_PERFORMANCE_MODE_OPEN = "POWER_PERFORMANCE_MODE_OPEN";
    public static final String VRS_WHITELIST = "support_common_vrs_app";
    private String TAG = "MultiSenceDynamicController";
    private Context mContext;
    private Handler mHandler;
    private MultiSenceService mService;
    private static final String PROPERTY_CLOUD_MULTISENCE_ENABLE = "persist.multisence.cloud.enable";
    public static boolean IS_CLOUD_ENABLED = SystemProperties.getBoolean(PROPERTY_CLOUD_MULTISENCE_ENABLE, false);

    public MultiSenceDynamicController(MultiSenceService service, Handler handler, Context context) {
        this.mService = service;
        this.mHandler = handler;
        this.mContext = context;
    }

    public void registerCloudObserver() {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.miui.server.multisence.MultiSenceDynamicController.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_DYNAMIC_CONTROLLER, "onChange");
                if (uri != null && uri.equals(Settings.System.getUriFor(MultiSenceDynamicController.CLOUD_MULTISENCE_ENABLE))) {
                    MultiSenceDynamicController.IS_CLOUD_ENABLED = Boolean.parseBoolean(Settings.System.getStringForUser(MultiSenceDynamicController.this.mContext.getContentResolver(), MultiSenceDynamicController.CLOUD_MULTISENCE_ENABLE, -2));
                    SystemProperties.set(MultiSenceDynamicController.PROPERTY_CLOUD_MULTISENCE_ENABLE, String.valueOf(MultiSenceDynamicController.IS_CLOUD_ENABLED));
                    Slog.i(MultiSenceDynamicController.this.TAG, "cloud control set received: " + MultiSenceDynamicController.IS_CLOUD_ENABLED);
                    if (!MultiSenceDynamicController.IS_CLOUD_ENABLED) {
                        Slog.i(MultiSenceDynamicController.this.TAG, "unregister listner due to cloud controller");
                        MultiSenceDynamicController.this.mService.unregisterMultiTaskActionListener();
                        MultiSenceDynamicController.this.mService.clearServiceStatus();
                    } else {
                        Slog.i(MultiSenceDynamicController.this.TAG, "register listner due to cloud controller");
                        MultiSenceDynamicController.this.mService.registerMultiTaskActionListener();
                        SystemProperties.set(MultiSenceConfig.PROP_MULTISENCE_ENABLE, String.valueOf(MultiSenceDynamicController.IS_CLOUD_ENABLED));
                        MultiSenceDynamicController.this.mService.notifyMultiSenceEnable(MultiSenceDynamicController.IS_CLOUD_ENABLED);
                    }
                    MultiSenceConfig.getInstance().updateSubFuncStatus();
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(MultiSenceDynamicController.KEY_POWER_PERFORMANCE_MODE_OPEN))) {
                    try {
                        boolean z = true;
                        boolean isPowerMode = Settings.System.getIntForUser(MultiSenceDynamicController.this.mContext.getContentResolver(), MultiSenceDynamicController.KEY_POWER_PERFORMANCE_MODE_OPEN, 0, -2) != 0;
                        if (MultiSenceDynamicController.this.mService.getPolicy() != null) {
                            MultiSenceDynamicController.this.mService.getPolicy().updatePowerMode(!isPowerMode);
                        }
                        boolean z2 = MultiSenceConfig.DEBUG_DYNAMIC_CONTROLLER;
                        StringBuilder append = new StringBuilder().append("updatePowerMode isPowerMode: ");
                        if (isPowerMode) {
                            z = false;
                        }
                        MultiSenceServiceUtils.msLogD(z2, append.append(z).toString());
                    } catch (Exception e) {
                        Slog.e(MultiSenceDynamicController.this.TAG, "updatePowerModeParas fail");
                    }
                }
                if (uri != null && uri.equals(Settings.System.getUriFor(MultiSenceDynamicController.CLOUD_MULTISENCE_SUBFUNC_NONSUPPORT))) {
                    try {
                        int nonSupportCode = Integer.parseInt(Settings.System.getStringForUser(MultiSenceDynamicController.this.mContext.getContentResolver(), MultiSenceDynamicController.CLOUD_MULTISENCE_SUBFUNC_NONSUPPORT, -2));
                        int oldNonSupportCode = SystemProperties.getInt(MultiSenceConfig.PROP_SUB_FUNC_NONSUPPORT, 0);
                        if (nonSupportCode != oldNonSupportCode) {
                            SystemProperties.set(MultiSenceConfig.PROP_SUB_FUNC_NONSUPPORT, String.valueOf(nonSupportCode));
                            MultiSenceConfig.getInstance().updateSubFuncStatus();
                        } else {
                            MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_DYNAMIC_CONTROLLER, "sub-function is not changed.");
                        }
                    } catch (Exception e2) {
                        Slog.e(MultiSenceDynamicController.this.TAG, "update sub-function nonSupport parameter fail.");
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MULTISENCE_ENABLE), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_POWER_PERFORMANCE_MODE_OPEN), false, observer, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MULTISENCE_SUBFUNC_NONSUPPORT), false, observer, -2);
        MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_DYNAMIC_CONTROLLER, "isCloudEnable: " + IS_CLOUD_ENABLED);
    }

    public void registerVRSCloudObserver() {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.miui.server.multisence.MultiSenceDynamicController.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MultiSenceDynamicController.VRS_WHITELIST))) {
                    String cloudInfo = Settings.System.getStringForUser(MultiSenceDynamicController.this.mContext.getContentResolver(), MultiSenceDynamicController.VRS_WHITELIST, -2);
                    MultiSenceServiceUtils.msLogD(MultiSenceConfig.DEBUG_VRS, "vrs-whitelist from cloud: " + cloudInfo);
                    MultiSenceConfig.getInstance().updateVRSWhiteListAll(cloudInfo);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(VRS_WHITELIST), false, observer, -2);
    }

    public void registerFWCloudObserver() {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.miui.server.multisence.MultiSenceDynamicController.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MultiSenceDynamicController.FW_WHITELIST))) {
                    MultiSenceConfig.getInstance().initFWWhiteList();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(FW_WHITELIST), false, observer, -2);
    }
}

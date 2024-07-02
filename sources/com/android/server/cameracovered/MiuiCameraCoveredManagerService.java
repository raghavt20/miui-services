package com.android.server.cameracovered;

import android.cameracovered.IMiuiCameraCoveredManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiBgThread;
import com.android.server.SystemService;
import com.android.server.wm.WindowManagerInternal;
import com.miui.app.MiuiCameraCoveredManagerServiceInternal;
import java.util.ArrayList;
import miui.os.DeviceFeature;
import org.json.JSONArray;
import org.json.JSONException;

/* loaded from: classes.dex */
public final class MiuiCameraCoveredManagerService extends IMiuiCameraCoveredManager.Stub implements MiuiCameraCoveredManagerServiceInternal {
    private static final String SCREEN_ANTI_BURN_DEFAULT_DATA = "{\"com.ss.android.ugc.aweme\": {    \"mode\": 1000,    \"layout\": [\"MainBottomTab\"],    \"enable_offset\": false,    \"dim_ratio\": \"0.5f\"},\"com.ss.android.ugc.aweme.lite\": {    \"mode\": 1000,    \"layout\": [\"MainBottomTab\"],    \"enable_offset\": false,    \"dim_ratio\": \"0.5f\"}}";
    public static final String SERVICE_NAME = "camera_covered_service";
    private static final String SRCREEN_ANTI_BURN_MODULE = "screen_anti_burn";
    public static final String TAG = "CameraCoveredManager";
    private CameraBlackCoveredManager mCameraBlackCoveredManager;
    private Context mContext;
    private MiuiSettings.SettingsCloudData.CloudData mScreenAntiBurnData;
    private WindowManagerInternal mWindowManagerService;
    private static String Test_Dynamic_Cutout = "testDynamicCutout";
    private static String Packages = "packages";

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiCameraCoveredManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiuiCameraCoveredManagerService(context);
        }

        public void onStart() {
            publishBinderService(MiuiCameraCoveredManagerService.SERVICE_NAME, this.mService);
        }
    }

    public MiuiCameraCoveredManagerService(Context context) {
        this.mContext = context;
        LocalServices.addService(MiuiCameraCoveredManagerServiceInternal.class, this);
        this.mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        initScreenAntiBurnData();
        registerScreenAntiBurnDataObserver();
    }

    @Override // com.miui.app.MiuiCameraCoveredManagerServiceInternal
    public void systemBooted() {
        try {
            if (DeviceFeature.SUPPORT_FRONTCAMERA_CIRCLE_BLACK) {
                CameraBlackCoveredManager cameraBlackCoveredManager = new CameraBlackCoveredManager(this.mContext, this.mWindowManagerService);
                this.mCameraBlackCoveredManager = cameraBlackCoveredManager;
                cameraBlackCoveredManager.systemReady();
                Slog.d(TAG, "systemBooted: current device need camera covered feature!");
            }
        } catch (Exception e) {
            Slog.d(TAG, e.toString());
        }
    }

    public void setCoveredPackageName(String packageName) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.setCoverdPackageName(packageName);
        }
    }

    public void hideCoveredBlackView() {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.hideCoveredBlackView();
        }
    }

    public void cupMuraCoveredAnimation(int type) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.cupMuraCoveredAnimation(type);
        }
    }

    public void hbmCoveredAnimation(int type) {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.hbmCoveredAnimation(type);
        }
    }

    public void addCoveredBlackView() {
        CameraBlackCoveredManager cameraBlackCoveredManager = this.mCameraBlackCoveredManager;
        if (cameraBlackCoveredManager != null) {
            cameraBlackCoveredManager.startCameraAnimation(true);
        }
    }

    public boolean needDisableCutout(String packageName) {
        new ArrayList();
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (JSONException e) {
                Slog.e(TAG, "exception when updateForceRestartAppList: ", e);
            }
            if (this.mContext.getContentResolver() != null && packageName != null) {
                String data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), Test_Dynamic_Cutout, Packages, (String) null);
                if (!TextUtils.isEmpty(data)) {
                    JSONArray apps = new JSONArray(data);
                    Slog.d(TAG, "needDisableCutout: packageName: " + packageName);
                    for (int i = 0; i < apps.length(); i++) {
                        if (apps.getString(i).equals(packageName)) {
                            Binder.restoreCallingIdentity(ident);
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
            Slog.d(TAG, "mContext.getContentResolver() or packageName is null");
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void registerScreenAntiBurnDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(MiuiBgThread.getHandler()) { // from class: com.android.server.cameracovered.MiuiCameraCoveredManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.i(MiuiCameraCoveredManagerService.TAG, "initScreenAntiBurnData onChange--");
                MiuiCameraCoveredManagerService.this.initScreenAntiBurnData();
            }
        });
    }

    public void initScreenAntiBurnData() {
        MiuiSettings.SettingsCloudData.CloudData cloudDataSingle = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), SRCREEN_ANTI_BURN_MODULE, (String) null, (String) null, false);
        this.mScreenAntiBurnData = cloudDataSingle;
        if (cloudDataSingle == null) {
            Slog.d(TAG, "without screen_anti_burn data.");
            this.mScreenAntiBurnData = new MiuiSettings.SettingsCloudData.CloudData(SCREEN_ANTI_BURN_DEFAULT_DATA);
        }
    }

    public String getScreenAntiBurnData(String packageName) {
        return this.mScreenAntiBurnData.getString(packageName, (String) null);
    }
}

package com.xiaomi.vkmode.service;

import android.content.Context;
import android.os.Binder;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiCommonCloudServiceStub;
import com.android.server.SystemService;
import com.android.server.am.ProcessUtils;
import com.android.server.wm.OneTrackVulkanHelper;
import com.xiaomi.vkmode.IMiuiForceVkService;
import com.xiaomi.vkmode.VkRulesData;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class MiuiForceVkService extends IMiuiForceVkService.Stub implements MiuiForceVkServiceInternal {
    private static String APP_NE_CRASH = null;
    private static final int QUEUE_MAX_SIZE = 30;
    public static final String SERVICE_NAME = "MiuiForceVkService";
    public static final String TAG = "APP_CRASHINFO_TRACK";
    private static final long TARGET_CRASH_MILLIS = 60000;
    private static final int TARGET_CRASH_NUM = 3;
    private static MiuiForceVkService instance;
    private final LinkedList<CrashPkgInfo> mCrashedQueue = new LinkedList<>();
    private final ArraySet<String> mCrashedBlackList = new ArraySet<>();
    private boolean crash_msg = false;

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiForceVkService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = MiuiForceVkService.getInstance();
        }

        public void onStart() {
            publishBinderService(MiuiForceVkService.SERVICE_NAME, this.mService);
        }
    }

    private MiuiForceVkService() {
        LocalServices.addService(MiuiForceVkServiceInternal.class, this);
    }

    public static synchronized MiuiForceVkService getInstance() {
        MiuiForceVkService miuiForceVkService;
        synchronized (MiuiForceVkService.class) {
            if (instance == null) {
                instance = new MiuiForceVkService();
                Slog.d(SERVICE_NAME, "MiuiForceVkService initialized");
            }
            miuiForceVkService = instance;
        }
        return miuiForceVkService;
    }

    public boolean shouldUseVk() {
        String packageName = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (packageName == null) {
            Slog.w(SERVICE_NAME, "request packageName is null");
            return false;
        }
        if (this.mCrashedBlackList.contains(packageName)) {
            Slog.d(SERVICE_NAME, "target ne false, pkg=" + packageName);
            return false;
        }
        VkRulesData vkRulesData = (VkRulesData) MiuiCommonCloudServiceStub.getInstance().getDataByModuleName("miui_vk_mode");
        if (vkRulesData == null) {
            Slog.w(SERVICE_NAME, "vkRulesData is null");
            return false;
        }
        ArraySet<String> pkgSet = vkRulesData.getPackages();
        if (pkgSet == null || !pkgSet.contains(packageName)) {
            return false;
        }
        Slog.d(SERVICE_NAME, "shouldUseVk true, pkg=" + packageName);
        return true;
    }

    @Override // com.xiaomi.vkmode.service.MiuiForceVkServiceInternal
    public void onAppCrashed(String packageName, boolean inNative, String stackTrace) {
        VkRulesData vkRulesData;
        boolean z = false;
        boolean vkModeEnabled = SystemProperties.getBoolean("persist.sys.vk_mode_enabled", false);
        if (!vkModeEnabled) {
            return;
        }
        String lowerCase = stackTrace.toLowerCase();
        APP_NE_CRASH = lowerCase;
        if (lowerCase.contains("renderthread") && APP_NE_CRASH.contains("grvkgpu")) {
            z = true;
        }
        this.crash_msg = z;
        if (packageName == null || !inNative || stackTrace == null || !z || (vkRulesData = (VkRulesData) MiuiCommonCloudServiceStub.getInstance().getDataByModuleName("miui_vk_mode")) == null) {
            return;
        }
        ArraySet<String> pkgSet = vkRulesData.getPackages();
        if (!this.mCrashedBlackList.contains(packageName) && pkgSet != null && pkgSet.contains(packageName)) {
            OneTrackVulkanHelper.getInstance().getCrashInfo(stackTrace);
            OneTrackVulkanHelper.getInstance().reportOneTrack(packageName);
            recordCrash(packageName);
            Slog.d(SERVICE_NAME, "target crash recorded, pkg=" + packageName);
        }
    }

    private void recordCrash(String packageName) {
        cleanTimeOut();
        if (existTargetCrash(packageName)) {
            this.mCrashedBlackList.add(packageName);
            return;
        }
        CrashPkgInfo info = new CrashPkgInfo();
        info.pkgName = packageName;
        info.reportTime = System.currentTimeMillis();
        if (this.mCrashedQueue.size() < 30) {
            this.mCrashedQueue.addLast(info);
        } else {
            this.mCrashedQueue.removeFirst();
            this.mCrashedQueue.addLast(info);
        }
    }

    private boolean existTargetCrash(String packageName) {
        if (this.mCrashedQueue.size() < 2) {
            return false;
        }
        int count = 0;
        Iterator<CrashPkgInfo> itr = this.mCrashedQueue.iterator();
        while (itr.hasNext()) {
            CrashPkgInfo info = itr.next();
            if (info != null && packageName.equals(info.pkgName)) {
                count++;
            }
            if (count == 2) {
                return true;
            }
        }
        return false;
    }

    private void cleanTimeOut() {
        long currentTime = System.currentTimeMillis();
        Iterator<CrashPkgInfo> itr = this.mCrashedQueue.iterator();
        while (itr.hasNext()) {
            CrashPkgInfo info = itr.next();
            if (info == null || currentTime - info.reportTime > 60000) {
                itr.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class CrashPkgInfo {
        String pkgName;
        long reportTime;

        private CrashPkgInfo() {
        }
    }
}

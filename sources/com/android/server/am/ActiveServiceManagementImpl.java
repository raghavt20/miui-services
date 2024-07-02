package com.android.server.am;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.whetstone.WhetstonePackageState;
import com.miui.whetstone.server.WhetstoneActivityManagerService;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ActiveServiceManagementImpl implements ActiveServiceManagementStub {
    String activeWallpaperPackageName;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActiveServiceManagementImpl> {

        /* compiled from: ActiveServiceManagementImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActiveServiceManagementImpl INSTANCE = new ActiveServiceManagementImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ActiveServiceManagementImpl m299provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ActiveServiceManagementImpl m298provideNewInstance() {
            return new ActiveServiceManagementImpl();
        }
    }

    ActiveServiceManagementImpl() {
    }

    public boolean canRestartServiceLocked(ServiceRecord record) {
        if ((record.appInfo.flags & 8) != 0 || UserHandle.getAppId(record.appInfo.uid) <= 2000 || record.packageName.equals(this.activeWallpaperPackageName)) {
            return true;
        }
        if (!AutoStartManagerServiceStub.getInstance().canRestartServiceLocked(record.packageName, record.appInfo.uid, "ActiveServiceManagementImpl#canRestartServiceLocked", record.getComponentName(), !record.stopIfKilled)) {
            return false;
        }
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService singletonService = WhetstoneActivityManagerService.getSingletonService();
            String str = record.packageName;
            int callingUserId = UserHandle.getCallingUserId();
            String className = record.name != null ? record.name.getClassName() : "";
            String str2 = record.processName;
            Object[] objArr = new Object[1];
            objArr[0] = record.intent != null ? record.intent.getIntent() : null;
            if (singletonService.checkPackageState(str, "Restart: AMS", 2, callingUserId, className, str2, objArr) != 1) {
                Slog.w("WhetstonePackageState", "Permission denied by Whetstone, cannot re-start service from " + record.packageName + "/" + (record.name != null ? record.name.getClassName() : "") + " in " + record.processName + ", UserId: " + UserHandle.getCallingUserId());
                return false;
            }
        }
        if (WhetstonePackageState.DEBUG) {
            Slog.d("WhetstonePackageState", "restart service from " + record.packageName + "/" + (record.name != null ? record.name.getClassName() : "") + " in " + record.processName + ", UserId: " + UserHandle.getCallingUserId());
            return true;
        }
        return true;
    }

    public boolean canBindService(Context context, Intent service, int userId) {
        return AutoStartManagerServiceStub.getInstance().isAllowStartService(context, service, userId);
    }

    public void updateWallPaperPackageName(String packageName) {
        this.activeWallpaperPackageName = packageName;
    }
}

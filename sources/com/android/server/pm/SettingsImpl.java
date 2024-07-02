package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.miui.AppOpsUtils;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miui.securityspace.XSpaceConstant;
import miui.securityspace.XSpaceUserHandle;

/* loaded from: classes.dex */
public class SettingsImpl extends SettingsStub {
    private static final String ANDROID_INSTALLER = "com.android.packageinstaller";
    private static final String GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String MIUI_ACTION_PACKAGE_FIRST_LAUNCH = "miui.intent.action.PACKAGE_FIRST_LAUNCH";
    private static final String MIUI_INSTALLER = "com.miui.packageinstaller";
    private static final String MIUI_PERMISSION = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final String TAG = SettingsImpl.class.getSimpleName();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SettingsImpl> {

        /* compiled from: SettingsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SettingsImpl INSTANCE = new SettingsImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SettingsImpl m2146provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SettingsImpl m2145provideNewInstance() {
            return new SettingsImpl();
        }
    }

    private static boolean findComponent(List<? extends ParsedMainComponent> components, String targetComponent) {
        if (components == null) {
            return false;
        }
        for (ParsedMainComponent component : components) {
            if (TextUtils.equals(component.getClassName(), targetComponent)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkXSpaceApp(PackageSetting ps, int userHandle) {
        if (XSpaceUserHandle.isXSpaceUserId(userHandle)) {
            if (XSpaceConstant.REQUIRED_APPS.contains(ps.getPkg().getPackageName())) {
                ps.setInstalled(true, userHandle);
            } else {
                ps.setInstalled(false, userHandle);
            }
            if (XSpaceConstant.SPECIAL_APPS.containsKey(ps.getPkg().getPackageName())) {
                ArrayList<String> requiredComponent = (ArrayList) XSpaceConstant.SPECIAL_APPS.get(ps.getPkg().getPackageName());
                ArrayList<ParsedMainComponent> components = new ArrayList<>();
                components.addAll(ps.getPkg().getActivities());
                components.addAll(ps.getPkg().getServices());
                components.addAll(ps.getPkg().getReceivers());
                components.addAll(ps.getPkg().getProviders());
                Iterator<ParsedMainComponent> it = components.iterator();
                while (it.hasNext()) {
                    ParsedMainComponent component = it.next();
                    if (!requiredComponent.contains(component.getClassName())) {
                        ps.addDisabledComponent(component.getClassName(), userHandle);
                    }
                }
            }
            return true;
        }
        if (!Build.IS_INTERNATIONAL_BUILD && !Build.IS_TABLET) {
            if (MIUI_INSTALLER.equals(ps.getPkg().getPackageName())) {
                ps.setInstalled(!AppOpsUtils.isXOptMode(), userHandle);
                return true;
            }
            if (GOOGLE_INSTALLER.equals(ps.getPkg().getPackageName())) {
                ps.setInstalled(AppOpsUtils.isXOptMode(), userHandle);
                return true;
            }
            if (ANDROID_INSTALLER.equals(ps.getName())) {
                ps.setInstalled(AppOpsUtils.isXOptMode(), userHandle);
                return true;
            }
        }
        return false;
    }

    public void noftifyFirstLaunch(PackageManagerService pms, final PackageStateInternal ps, final int userId) {
        if (ps == null || ps.isSystem()) {
            return;
        }
        pms.mHandler.post(new Runnable() { // from class: com.android.server.pm.SettingsImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SettingsImpl.lambda$noftifyFirstLaunch$0(ps, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$noftifyFirstLaunch$0(PackageStateInternal ps, int userId) {
        try {
            Log.i(TAG, "notify first launch");
            Intent intent = new Intent(MIUI_ACTION_PACKAGE_FIRST_LAUNCH);
            intent.putExtra("package", ps.getPackageName());
            if (!TextUtils.isEmpty(ps.getInstallSource().mInstallerPackageName)) {
                intent.putExtra("installer", ps.getInstallSource().mInstallerPackageName);
            }
            intent.putExtra("userId", userId);
            intent.addFlags(BaseMiuiPhoneWindowManager.FLAG_INJECTED_FROM_SHORTCUT);
            PackageEventRecorderInternal peri = (PackageEventRecorderInternal) LocalServices.getService(PackageEventRecorderInternal.class);
            peri.recordPackageFirstLaunch(userId, ps.getPackageName(), ps.getInstallSource().mInstallerPackageName, intent);
            Bundle activeBundle = peri.getSourcePackage(ps.getPackageName(), userId, true);
            if (activeBundle != null) {
                intent.putExtras(activeBundle);
            }
            IActivityManager am = ActivityManager.getService();
            String[] requiredPermissions = {"miui.permission.USE_INTERNAL_GENERAL_API"};
            am.broadcastIntentWithFeature((IApplicationThread) null, "FirstLaunch", intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, 0);
        } catch (Throwable t) {
            Log.e(TAG, "notify first launch exception", t);
        }
    }
}

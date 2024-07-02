package com.miui.server.security;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.miui.server.SecurityManagerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import miui.os.Build;

/* loaded from: classes.dex */
public class DefaultBrowserImpl {
    private static final String DEF_BROWSER_COUNT = "miui.sec.defBrowser";
    private static final long ONE_MINUTE = 60000;
    private static final String PKG_BROWSER;
    private static final String TAG = "DefaultBrowserImpl";
    private final Context mContext;
    private final Handler mHandler;

    static {
        PKG_BROWSER = Build.IS_INTERNATIONAL_BUILD ? "com.mi.globalbrowser" : "com.android.browser";
    }

    public DefaultBrowserImpl(SecurityManagerService service) {
        this.mContext = service.mContext;
        this.mHandler = service.mSecurityWriteHandler;
    }

    public void checkDefaultBrowser(final String packageName) {
        if (Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode()) {
            return;
        }
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.security.DefaultBrowserImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DefaultBrowserImpl.this.lambda$checkDefaultBrowser$0(packageName);
            }
        }, 300L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkDefaultBrowser$0(String packageName) {
        ContentResolver cr = this.mContext.getContentResolver();
        try {
            checkIntentFilterVerifications(packageName);
            int defBrowserCount = Settings.Secure.getInt(cr, DEF_BROWSER_COUNT, -1);
            boolean allow = true;
            if (defBrowserCount >= 10 && defBrowserCount < 100) {
                Settings.Secure.putInt(cr, DEF_BROWSER_COUNT, defBrowserCount + 1);
                allow = false;
            }
            if (allow) {
                setDefaultBrowser(this.mContext);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkDefaultBrowser", e);
        }
    }

    public void resetDefaultBrowser() {
        if (Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode()) {
            return;
        }
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.security.DefaultBrowserImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DefaultBrowserImpl.this.lambda$resetDefaultBrowser$1();
            }
        }, 60000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$resetDefaultBrowser$1() {
        try {
            checkIntentFilterVerifications();
            setDefaultBrowser(this.mContext);
        } catch (Exception e) {
            Log.e(TAG, "resetDefaultBrowser exception", e);
        }
    }

    private void checkIntentFilterVerifications() {
        checkIntentFilterVerifications(null);
    }

    private void checkIntentFilterVerifications(String packageName) {
        List<PackageInfo> applications;
        String str;
        boolean z;
        boolean z2;
        List<IntentFilter> filters;
        String str2 = "android.intent.action.VIEW";
        PackageManager pm = this.mContext.getPackageManager();
        try {
            if (packageName == null) {
                applications = pm.getInstalledPackages(8192);
            } else {
                applications = Collections.singletonList(pm.getPackageInfo(packageName, 0));
            }
            Intent browserIntent = new Intent("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http://"));
            Intent httpIntent = new Intent("android.intent.action.VIEW").setData(Uri.parse("http://www.xiaomi.com"));
            Intent httpsIntent = new Intent("android.intent.action.VIEW").setData(Uri.parse("https://www.xiaomi.com"));
            if (packageName != null) {
                browserIntent.setPackage(packageName);
                httpIntent.setPackage(packageName);
                httpsIntent.setPackage(packageName);
            }
            Set<String> browsers = queryIntentPackages(pm, browserIntent, true, 0);
            Set<String> httpPackages = queryIntentPackages(pm, httpIntent, false, 0);
            Set<String> httpsPackages = queryIntentPackages(pm, httpsIntent, false, 0);
            httpPackages.addAll(httpsPackages);
            ArraySet<String> rejectPks = new ArraySet<>();
            for (PackageInfo info : applications) {
                if (UserHandle.getAppId(info.applicationInfo.uid) < 10000) {
                    str2 = str2;
                } else if (!info.applicationInfo.isSystemApp()) {
                    String pkg = info.applicationInfo.packageName;
                    if (!browsers.contains(pkg) && httpPackages.contains(pkg)) {
                        List<IntentFilter> filters2 = pm.getAllIntentFilters(pkg);
                        boolean add = false;
                        if (filters2 == null || filters2.size() <= 0) {
                            str = str2;
                        } else {
                            for (IntentFilter filter : filters2) {
                                if (filter.hasAction(str2)) {
                                    String str3 = str2;
                                    if (!filter.hasDataScheme("http") && !filter.hasDataScheme("https")) {
                                        filters = filters2;
                                        str2 = str3;
                                        filters2 = filters;
                                    }
                                    ArrayList<String> hostList = filter.getHostsList();
                                    if (hostList.size() != 0) {
                                        filters = filters2;
                                        if (hostList.contains("*")) {
                                        }
                                        str2 = str3;
                                        filters2 = filters;
                                    } else {
                                        filters = filters2;
                                    }
                                    int dataPathsCount = filter.countDataPaths();
                                    if (dataPathsCount > 0) {
                                        int i = 0;
                                        while (true) {
                                            if (i >= dataPathsCount) {
                                                break;
                                            }
                                            int dataPathsCount2 = dataPathsCount;
                                            IntentFilter filter2 = filter;
                                            if (!".*".equals(filter.getDataPath(i).getPath())) {
                                                i++;
                                                dataPathsCount = dataPathsCount2;
                                                filter = filter2;
                                            } else {
                                                add = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        add = true;
                                    }
                                    str2 = str3;
                                    filters2 = filters;
                                }
                            }
                            str = str2;
                        }
                        if (!add) {
                            z = false;
                            z2 = true;
                        } else {
                            z = false;
                            int status = pm.getIntentVerificationStatusAsUser(pkg, 0);
                            if (status != 0) {
                                z2 = true;
                                if (status == 1) {
                                }
                            } else {
                                z2 = true;
                            }
                            rejectPks.add(pkg);
                        }
                        str2 = str;
                    }
                }
            }
            Iterator<String> it = rejectPks.iterator();
            while (it.hasNext()) {
                pm.updateIntentVerificationStatusAsUser(it.next(), 3, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setDefaultBrowser(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            String defaultBrowser = pm.getDefaultBrowserPackageNameAsUser(0);
            if (TextUtils.isEmpty(defaultBrowser)) {
                pm.setDefaultBrowserPackageNameAsUser(PKG_BROWSER, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "setDefaultBrowser", e);
        }
    }

    private Set<String> queryIntentPackages(PackageManager pm, Intent intent, boolean allweb, int userId) {
        List<ResolveInfo> list = pm.queryIntentActivitiesAsUser(intent, 131072, userId);
        int count = list.size();
        Set<String> packages = new ArraySet<>();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && (!allweb || info.handleAllWebDataURI)) {
                String packageName = info.activityInfo.packageName;
                if (!packages.contains(packageName)) {
                    packages.add(packageName);
                }
            }
        }
        return packages;
    }

    public static boolean isDefaultBrowser(Context context, String packageName) {
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                String defaultBrowserPackage = context.getPackageManager().getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
                Slog.d(TAG, "isDefaultBrowser: packageName= " + packageName + " defaultBrowserPackage= " + defaultBrowserPackage);
                return TextUtils.equals(packageName, defaultBrowserPackage);
            } catch (SecurityException exception) {
                Slog.d(TAG, "isDefaultBrowser failed: " + exception);
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}

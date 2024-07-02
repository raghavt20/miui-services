package com.android.server.pm;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.pkg.AndroidPackage;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class MiuiDexopt {
    private static final String BASLINE_TAG = "BaselineProfile";
    private static final long MAX_TIME_INTERVALS = 600000;
    private static volatile Handler sMiuiDexoptHandler;
    private static HandlerThread sMiuiDexoptThread;
    private Context mContext;
    private DexOptHelper mDexOptHelper;
    private boolean mEnableBaseline;
    private PackageManagerService mPms;
    private static Object sMiuiDexoptLock = new Object();
    private static final File disableBaselineFile = new File("/data/system/disable_baseline.xml");
    private Object mDisableBaselineLock = new Object();
    private Map<String, Long> mInvokeDisableBaselineMap = new HashMap();
    private Set<String> mDisableBaselineSet = new HashSet();

    public MiuiDexopt(PackageManagerService pms, DexOptHelper dexOptHelper, Context context) {
        this.mPms = pms;
        this.mDexOptHelper = dexOptHelper;
        this.mContext = context;
        loadDisableBaselineList();
    }

    public void preConfigMiuiDexopt(final Context context) {
        this.mEnableBaseline = SystemProperties.getBoolean("persist.sys.baseline.enable", false);
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.pm.MiuiDexopt.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiDexopt.this.updateDexMetaDataState(context);
            }
        });
    }

    private void loadDisableBaselineList() {
        try {
            SharedPreferences sp = this.mContext.getSharedPreferences(disableBaselineFile, 0);
            Map<String, ?> allPackageNames = sp.getAll();
            for (String packageName : allPackageNames.keySet()) {
                this.mDisableBaselineSet.add(packageName);
            }
        } catch (Exception e) {
            Slog.d(BASLINE_TAG, "load disabled baseline list fail: " + e);
        }
    }

    public boolean setBaselineDisabled(String packageName, boolean disabled) {
        try {
            synchronized (this.mDisableBaselineLock) {
                long currentTime = System.currentTimeMillis();
                long lastInvokeTime = this.mInvokeDisableBaselineMap.getOrDefault(packageName, 0L).longValue();
                if (currentTime - lastInvokeTime > 600000) {
                    SharedPreferences sp = this.mContext.getSharedPreferences(disableBaselineFile, 0);
                    boolean isContain = this.mDisableBaselineSet.contains(packageName);
                    if (disabled && !isContain) {
                        this.mDisableBaselineSet.add(packageName);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean(packageName, true);
                        editor.commit();
                    } else if (!disabled && isContain) {
                        this.mDisableBaselineSet.remove(packageName);
                        SharedPreferences.Editor editor2 = sp.edit();
                        editor2.remove(packageName);
                        editor2.commit();
                    }
                    this.mInvokeDisableBaselineMap.put(packageName, Long.valueOf(currentTime));
                    return true;
                }
                Slog.d(BASLINE_TAG, packageName + " invalid invoke");
                return false;
            }
        } catch (Exception e) {
            Slog.d(BASLINE_TAG, "set baseline state fail: " + e);
            return false;
        }
    }

    public boolean isBaselineDisabled(String packageName) {
        return this.mDisableBaselineSet.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDexMetaDataState(Context context) {
        String enable = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), "DexMetaData", MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, "false");
        Slog.d(BASLINE_TAG, "prepare DexMetadata enable: " + enable);
        SystemProperties.set("persist.sys.baseline.enable", enable);
        this.mEnableBaseline = "true".equals(enable);
    }

    private static Handler getMiuiDexoptHandler() {
        if (sMiuiDexoptHandler == null) {
            synchronized (sMiuiDexoptLock) {
                if (sMiuiDexoptHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("dex_metadata_async_thread");
                    sMiuiDexoptThread = handlerThread;
                    handlerThread.start();
                    sMiuiDexoptHandler = new Handler(sMiuiDexoptThread.getLooper());
                }
            }
        }
        return sMiuiDexoptHandler;
    }

    private void writeFile(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[512];
        while (true) {
            int length = is.read(buf);
            if (length > 0) {
                os.write(buf, 0, length);
            } else {
                return;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:84:0x00b4, code lost:
    
        android.util.Slog.d(com.android.server.pm.MiuiDexopt.BASLINE_TAG, r11 + " primary.prof file exists, no need for DexMetadata dexopt");
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x00ca, code lost:
    
        r3 = null;
     */
    /* JADX WARN: Removed duplicated region for block: B:118:0x020b  */
    /* JADX WARN: Removed duplicated region for block: B:122:? A[ADDED_TO_REGION, RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0091  */
    /* JADX WARN: Removed duplicated region for block: B:156:0x0200  */
    /* JADX WARN: Removed duplicated region for block: B:98:0x015c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean preparepProfile(com.android.server.pm.pkg.AndroidPackage r20) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 537
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.MiuiDexopt.preparepProfile(com.android.server.pm.pkg.AndroidPackage):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean prepareDexMetadata(AndroidPackage pkg, int[] userId) {
        boolean success;
        String packageName = pkg.getPackageName();
        try {
            success = preparepProfile(pkg);
            if (!DexOptHelper.useArtService() && success) {
                this.mPms.mArtManagerService.prepareAppProfiles(pkg, userId, true);
            }
        } catch (Exception e) {
            Slog.d(BASLINE_TAG, packageName + " prepareDexMetadata exception: " + e);
            success = false;
        }
        Slog.d(BASLINE_TAG, packageName + " prepareDexMetadata success: " + success);
        return success;
    }

    public void asyncDexMetadataDexopt(final AndroidPackage pkg, final int[] userId) {
        if (this.mEnableBaseline && !isBaselineDisabled(pkg.getPackageName())) {
            Runnable task = new Runnable() { // from class: com.android.server.pm.MiuiDexopt.2
                @Override // java.lang.Runnable
                public void run() {
                    long asyncDexoptBeginTime = System.currentTimeMillis();
                    boolean success = MiuiDexopt.this.prepareDexMetadata(pkg, userId);
                    long prepareDexMetadataTime = System.currentTimeMillis() - asyncDexoptBeginTime;
                    if (success) {
                        DexoptOptions dexoptOptions = new DexoptOptions(pkg.getPackageName(), 16, 5);
                        boolean success2 = MiuiDexopt.this.mDexOptHelper.performDexOpt(dexoptOptions);
                        long totalTime = System.currentTimeMillis() - asyncDexoptBeginTime;
                        Slog.d(MiuiDexopt.BASLINE_TAG, "packageName: " + pkg.getPackageName() + " prepareDexMetadataTime: " + prepareDexMetadataTime + " totalTime: " + totalTime + " asyncDexMetadataDexopt success: " + success2);
                    }
                }
            };
            getMiuiDexoptHandler().post(task);
        } else {
            Slog.d(BASLINE_TAG, "asyncDexMetadataDexopt not enable");
        }
    }
}

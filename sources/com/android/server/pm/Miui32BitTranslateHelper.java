package com.android.server.pm;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.miui.base.MiuiStubRegistry;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class Miui32BitTranslateHelper extends Miui32BitTranslateHelperStub {
    private static volatile Handler s32BitAsyncPreTransHandler;
    private static HandlerThread s32BitAsyncPreTransThread;
    private static String TAG = Miui32BitTranslateHelper.class.getSimpleName();
    private static Object s32BitAsyncPreTransLock = new Object();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<Miui32BitTranslateHelper> {

        /* compiled from: Miui32BitTranslateHelper$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final Miui32BitTranslateHelper INSTANCE = new Miui32BitTranslateHelper();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public Miui32BitTranslateHelper m2097provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public Miui32BitTranslateHelper m2096provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.Miui32BitTranslateHelper is marked as singleton");
        }
    }

    private static Handler get32BitTransHandler() {
        if (s32BitAsyncPreTransHandler == null) {
            synchronized (s32BitAsyncPreTransLock) {
                if (s32BitAsyncPreTransHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("32bit_pretranslate_thread");
                    s32BitAsyncPreTransThread = handlerThread;
                    handlerThread.start();
                    s32BitAsyncPreTransHandler = new Handler(s32BitAsyncPreTransThread.getLooper());
                }
            }
        }
        return s32BitAsyncPreTransHandler;
    }

    public void do32BitPretranslateOnInstall(final AndroidPackage pkg, final PackageStateInternal pkgSetting) {
        if (SystemProperties.getBoolean("tango.pretrans_on_install", false)) {
            get32BitTransHandler().post(new Runnable() { // from class: com.android.server.pm.Miui32BitTranslateHelper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Miui32BitTranslateHelper.this.lambda$do32BitPretranslateOnInstall$0(pkg, pkgSetting);
                }
            });
        }
    }

    /* renamed from: do32BitPretranslate, reason: merged with bridge method [inline-methods] */
    public void lambda$do32BitPretranslateOnInstall$0(AndroidPackage pkg, PackageStateInternal pkgSetting) {
        int sharedGid = UserHandle.getSharedAppGid(pkg.getUid());
        String libDir = null;
        String primaryCpuAbi = pkgSetting.getPrimaryCpuAbi();
        String secondaryCpuAbi = pkgSetting.getSecondaryCpuAbi();
        boolean is32BitApp = false;
        if (primaryCpuAbi != null && VMRuntime.getInstructionSet(primaryCpuAbi) != null && VMRuntime.getInstructionSet(primaryCpuAbi).equals("arm")) {
            libDir = pkg.getNativeLibraryDir();
            is32BitApp = true;
        } else if (secondaryCpuAbi != null && VMRuntime.getInstructionSet(secondaryCpuAbi) != null && VMRuntime.getInstructionSet(secondaryCpuAbi).equals("arm")) {
            libDir = pkg.getSecondaryNativeLibraryDir();
            is32BitApp = true;
        }
        if (SystemProperties.getBoolean("tango.pretrans.apk", false) && is32BitApp) {
            for (String apk : AndroidPackageUtils.getAllCodePathsExcludingResourceOnly(pkg)) {
                do32BitPretranslateOnArtService(apk, sharedGid, true);
            }
        } else {
            Slog.i(TAG, "32BitPretranslate: skipping APK pre-translation for " + pkg.getPackageName());
        }
        if (SystemProperties.getBoolean("tango.pretrans.lib", false)) {
            if (libDir != null) {
                File f = new File(libDir);
                if (f.exists() && f.isDirectory()) {
                    for (File lib : f.listFiles(new FilenameFilter() { // from class: com.android.server.pm.Miui32BitTranslateHelper.1
                        @Override // java.io.FilenameFilter
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".so");
                        }
                    })) {
                        do32BitPretranslateOnArtService(lib.getAbsolutePath(), sharedGid, false);
                    }
                    return;
                }
                return;
            }
            return;
        }
        Slog.i(TAG, "32BitPretranslate: skipping shared library pre-translation for " + pkg.getPackageName());
    }

    private void do32BitPretranslateOnArtService(String path, int uid, boolean isApk) {
        try {
            Method declaredMethod = DexOptHelper.getArtManagerLocal().getClass().getDeclaredMethod("do32BitPretranslate", String.class, Integer.TYPE, Boolean.TYPE);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(DexOptHelper.getArtManagerLocal(), path, Integer.valueOf(uid), Boolean.valueOf(isApk));
        } catch (Exception e) {
            Slog.i(TAG, "do32BitPretranslate failed. " + e.getStackTrace());
        }
    }
}

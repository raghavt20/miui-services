package com.miui.server.migard;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.util.Singleton;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.GameMemoryReclaimer;
import com.android.server.am.IGameProcessAction;
import com.miui.server.migard.memory.GameMemoryCleaner;
import com.miui.server.migard.memory.GameMemoryCleanerConfig;
import com.miui.server.migard.memory.GameMemoryCleanerDeprecated;
import com.miui.server.migard.surfaceflinger.SurfaceFlingerSetCgroup;
import com.miui.server.migard.trace.GameTrace;
import com.miui.server.migard.utils.LogUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import miui.migard.IMiGard;

/* loaded from: classes.dex */
public class MiGardService extends IMiGard.Stub {
    public static boolean DEBUG_VERSION = Build.IS_DEBUGGABLE;
    private static final Singleton<IMiGard> IMiGardSingleton = new Singleton<IMiGard>() { // from class: com.miui.server.migard.MiGardService.1
        /* JADX INFO: Access modifiers changed from: protected */
        public IMiGard create() {
            IBinder b = ServiceManager.getService(MiGardService.SERVICE_NAME);
            IMiGard service = IMiGard.Stub.asInterface(b);
            return service;
        }
    };
    public static final String JOYOSE_NAME = "xiaomi.joyose";
    public static final String MIGARD_DATA_PATH = "/data/system/migard";
    public static final String SERVICE_NAME = "migard";
    public static final String TAG = "MiGardService";
    private ActivityManagerService mActivityManagerService;
    private final Context mContext;
    private GameMemoryReclaimer mGameMemoryReclaimer;
    GameMemoryCleaner mMemCleaner;
    GameMemoryCleanerDeprecated mMemCleanerDeprecated;
    private SurfaceFlingerSetCgroup mSurfaceFlingerSetCgroup;

    private MiGardService(Context context) {
        this.mContext = context;
        LocalServices.addService(MiGardInternal.class, new LocalService());
        ActivityManagerService activityManagerService = ActivityManagerNative.getDefault();
        this.mActivityManagerService = activityManagerService;
        this.mGameMemoryReclaimer = new GameMemoryReclaimer(context, activityManagerService);
        this.mMemCleaner = new GameMemoryCleaner(context, this.mGameMemoryReclaimer);
        this.mMemCleanerDeprecated = new GameMemoryCleanerDeprecated(context);
        context.registerReceiver(ScreenStatusManager.getInstance(), ScreenStatusManager.getInstance().getFilter());
        if (this.mMemCleanerDeprecated.isLowMemDevice()) {
            PackageStatusManager.getInstance().registerCallback(this.mMemCleanerDeprecated);
        }
        SurfaceFlingerSetCgroup surfaceFlingerSetCgroup = new SurfaceFlingerSetCgroup(context);
        this.mSurfaceFlingerSetCgroup = surfaceFlingerSetCgroup;
        if (surfaceFlingerSetCgroup.SupportProduct()) {
            PackageStatusManager.getInstance().registerCallback(this.mSurfaceFlingerSetCgroup);
        }
    }

    public static MiGardService getService() {
        return (MiGardService) IMiGardSingleton.get();
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiGardService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiGardService(context);
        }

        public void onStart() {
            publishBinderService(MiGardService.SERVICE_NAME, this.mService);
        }
    }

    public void startDefaultTrace(boolean async) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().start(async);
        }
    }

    public void startTrace(String[] categories, boolean async) {
        if (checkPermission(Binder.getCallingUid())) {
            ArrayList<String> categoryList = new ArrayList<>();
            for (String c : categories) {
                categoryList.add(c);
            }
            GameTrace.getInstance().start(categoryList, async);
        }
    }

    public void stopTrace(boolean zip) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().stop(zip);
        }
    }

    public void dumpTrace(boolean zip) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().dump(zip);
        }
    }

    public void configTrace(String json) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().configTrace(json);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setTraceBufferSize(int size) {
        if (checkPermission(Binder.getCallingUid())) {
            GameTrace.getInstance().setBufferSize(size);
        }
    }

    public void addGameCleanUserProtectList(final List<String> list, final boolean append) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addUserProtectList(list, append);
                }
            });
        }
        this.mMemCleanerDeprecated.addGameCleanUserProtectList(list, append);
    }

    public void removeGameCleanUserProtectList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().removeUserProtectList(list);
                }
            });
        }
        this.mMemCleanerDeprecated.removeGameCleanUserProtectList(list);
    }

    public void configGameMemoryCleaner(final String game, final String json) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().configFromCloudControl(game, json);
                }
            });
        }
    }

    public void reclaimBackgroundMemory() {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.reclaimBackgroundMemory();
        }
    }

    public void configGameList(List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.addGameList(list);
        }
    }

    public void configKillerWhiteList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addKillerCommonWhilteList(list);
                }
            });
        }
    }

    public void configCompactorWhiteList(final List<String> list) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().addCompactorCommonWhiteList(list);
                }
            });
        }
    }

    public void configPowerWhiteList(final String pkgList) {
        if (checkPermission(Binder.getCallingUid())) {
            this.mMemCleaner.runOnThread(new Runnable() { // from class: com.miui.server.migard.MiGardService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    GameMemoryCleanerConfig.getInstance().updatePowerWhiteList(pkgList);
                }
            });
        }
    }

    private boolean checkPermission(int uid) {
        return uid == 1000;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        this.mMemCleaner.dump(pw);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new MiGardShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    private final class LocalService extends MiGardInternal {
        private LocalService() {
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onProcessStart(int uid, int pid, String pkg, String caller) {
            MiGardService.this.mMemCleaner.onProcessStart(uid, pid, pkg, caller);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onProcessKilled(int uid, int pid, String pkg, String reason) {
            MiGardService.this.mMemCleaner.onProcessKilled(pkg, reason);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void onVpnConnected(String user, boolean connected) {
            if (user != null && !user.isEmpty()) {
                LogUtils.d(MiGardService.TAG, "on vpn established, user=" + user);
                MiGardService.this.mMemCleaner.onVpnConnected(user, connected);
            }
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void notifyGameForeground(String game) {
            MiGardService.this.mGameMemoryReclaimer.notifyGameForeground(game);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void notifyGameBackground() {
            MiGardService.this.mGameMemoryReclaimer.notifyGameBackground();
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void reclaimBackgroundForGame(long need) {
            MiGardService.this.mGameMemoryReclaimer.reclaimBackground(need);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void addGameProcessKiller(IGameProcessAction.IGameProcessActionConfig killerConfig) {
            MiGardService.this.mGameMemoryReclaimer.addGameProcessKiller(killerConfig);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void addGameProcessCompactor(IGameProcessAction.IGameProcessActionConfig compactorConfig) {
            MiGardService.this.mGameMemoryReclaimer.addGameProcessCompactor(compactorConfig);
        }

        @Override // com.miui.server.migard.MiGardInternal
        public void notifyProcessDied(int pid) {
            MiGardService.this.mGameMemoryReclaimer.notifyProcessDied(pid);
        }
    }
}

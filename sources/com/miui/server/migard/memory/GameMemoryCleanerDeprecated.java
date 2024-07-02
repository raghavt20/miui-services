package com.miui.server.migard.memory;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Slog;
import com.miui.server.migard.MiGardService;
import com.miui.server.migard.PackageStatusManager;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.util.HardwareInfo;

/* loaded from: classes.dex */
public class GameMemoryCleanerDeprecated implements PackageStatusManager.IForegroundChangedCallback {
    private static final String TAG = GameMemoryCleanerDeprecated.class.getSimpleName();
    private Context mContext;
    private String mCurrentGame = null;
    private List<String> mGameList = new ArrayList();
    private List<String> mWhiteList = new ArrayList();

    public GameMemoryCleanerDeprecated(Context context) {
        this.mContext = null;
        this.mContext = context;
        this.mGameList.add("com.tencent.ig");
        this.mGameList.add("com.garena.game.kgtw");
        this.mGameList.add("com.miHoYo.GenshinImpact");
        this.mGameList.add("com.mobile.legends");
        this.mGameList.add("com.tencent.tmgp.pubgmhd");
        this.mGameList.add("com.tencent.tmgp.sgame");
    }

    public boolean isLowMemDevice() {
        long memSize = HardwareInfo.getTotalPhysicalMemory() / FormatBytesUtil.GB;
        if (MiGardService.DEBUG_VERSION) {
            Slog.d(TAG, "check physical mem size, size: " + memSize + " GB");
        }
        if (memSize <= 4) {
            return true;
        }
        return false;
    }

    private void killBackgroundApps() {
        List<String> whiteList = new ArrayList<>();
        synchronized (this.mWhiteList) {
            whiteList.addAll(this.mWhiteList);
            whiteList.add(this.mCurrentGame);
        }
        ProcessConfig config = new ProcessConfig(4);
        if (whiteList.size() > 0) {
            config.setWhiteList(whiteList);
            Slog.i(TAG, "skip white list: " + whiteList);
        }
        ProcessManager.kill(config);
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback
    public void onForegroundChanged(int pid, int uid, String name) {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> appProcessList;
        if (!this.mGameList.contains(name) || (am = (ActivityManager) this.mContext.getSystemService("activity")) == null || (appProcessList = am.getRunningAppProcesses()) == null) {
            return;
        }
        for (ActivityManager.RunningAppProcessInfo ai : appProcessList) {
            this.mWhiteList.add(ai.processName);
        }
        this.mCurrentGame = name;
        String str = TAG;
        Slog.i(str, "start kill background apps...");
        killBackgroundApps();
        Slog.i(str, "finish killing");
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback, com.miui.server.migard.ScreenStatusManager.IScreenChangedCallback, com.miui.server.migard.UidStateManager.IUidStateChangedCallback
    public String getCallbackName() {
        return GameMemoryCleanerDeprecated.class.getSimpleName();
    }

    public void addGameCleanUserProtectList(List<String> list, boolean append) {
        synchronized (this.mWhiteList) {
            if (append) {
                this.mWhiteList.removeAll(list);
            } else {
                this.mWhiteList.clear();
            }
            this.mWhiteList.addAll(list);
        }
    }

    public void removeGameCleanUserProtectList(List<String> list) {
        synchronized (this.mWhiteList) {
            this.mWhiteList.removeAll(list);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("physical mem size (GB): " + (HardwareInfo.getTotalPhysicalMemory() / FormatBytesUtil.GB));
        pw.println("white list packages: ");
        pw.println(this.mWhiteList);
    }
}

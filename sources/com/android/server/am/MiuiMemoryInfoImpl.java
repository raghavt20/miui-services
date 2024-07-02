package com.android.server.am;

import android.app.ActivityManagerNative;
import android.os.Process;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class MiuiMemoryInfoImpl implements MiuiMemoryInfoStub {
    private static final String TAG = "MiuiMemoryInfoImpl";
    private static long sTotalMem = Process.getTotalMemory();
    static ActivityManagerService sAms = null;

    private static native long getNativeCachedLostMemory();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMemoryInfoImpl> {

        /* compiled from: MiuiMemoryInfoImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMemoryInfoImpl INSTANCE = new MiuiMemoryInfoImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiMemoryInfoImpl m537provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiMemoryInfoImpl m536provideNewInstance() {
            return new MiuiMemoryInfoImpl();
        }
    }

    public long getMoreCachedSizeKb(MemInfoReader infos) {
        long[] rawInfo = infos.getRawInfo();
        long kReclaimable = rawInfo[15];
        if (kReclaimable == 0) {
            kReclaimable = rawInfo[6];
        }
        return rawInfo[2] + kReclaimable + rawInfo[3];
    }

    private static long getCachePss() {
        ArrayList<ProcessRecord> procs;
        if (sAms == null) {
            ActivityManagerService activityManagerService = ActivityManagerNative.getDefault();
            if (activityManagerService instanceof ActivityManagerService) {
                sAms = activityManagerService;
            }
        }
        long cachePss = 0;
        ActivityManagerService activityManagerService2 = sAms;
        if (activityManagerService2 != null && (procs = activityManagerService2.collectProcesses((PrintWriter) null, 0, false, (String[]) null)) != null) {
            Iterator<ProcessRecord> it = procs.iterator();
            while (it.hasNext()) {
                ProcessRecord proc = it.next();
                synchronized (sAms.mProcLock) {
                    if (proc.mState.getSetProcState() >= 15) {
                        cachePss += proc.mProfile.getLastPss();
                    }
                }
            }
        }
        return FormatBytesUtil.KB * cachePss;
    }

    public static long getCachedLostRam() {
        return getNativeCachedLostMemory();
    }

    public long getFreeMemory() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long[] rawInfo = minfo.getRawInfo();
        Slog.d(TAG, "MEMINFO_KRECLAIMABLE: " + rawInfo[15] + ", MEMINFO_SLAB_RECLAIMABLE: " + rawInfo[6] + ", MEMINFO_BUFFERS: " + rawInfo[2] + ", MEMINFO_CACHED: " + rawInfo[3] + ", MEMINFO_FREE: " + rawInfo[1]);
        long kReclaimable = rawInfo[15];
        if (kReclaimable == 0) {
            kReclaimable = rawInfo[6];
        }
        long cache = (rawInfo[2] + kReclaimable + rawInfo[3] + rawInfo[1]) * FormatBytesUtil.KB;
        long lostCache = getCachedLostRam();
        long free = cache + lostCache + getCachePss();
        Slog.d(TAG, "cache: " + cache + ", cachePss: " + getCachePss() + ", lostcache: " + lostCache);
        if (free >= sTotalMem) {
            return cache + lostCache;
        }
        return free;
    }
}

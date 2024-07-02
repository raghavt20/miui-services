package com.android.server.power.stats;

import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Slog;
import java.io.FileInputStream;
import java.io.IOException;

/* loaded from: classes.dex */
public class KernelWakelockReaderRewrite {
    private static final String TAG = "KernelWakelockReader";
    private static final String sWakelockFile = "/proc/wakelocks";
    private static final String sWakeupSourceFile = "/d/wakeup_sources";
    private static int sKernelWakelockUpdateVersion = 0;
    private static final int[] PROC_WAKELOCKS_FORMAT = {5129, 8201, 9, 9, 9, 8201};
    private static final int[] WAKEUP_SOURCES_FORMAT = {4105, 8457, 265, 265, 265, 265, 8457};
    private final String[] mProcWakelocksName = new String[3];
    private final long[] mProcWakelocksData = new long[3];
    boolean br_flag = false;

    private int readInfoCheck(byte[] buffer, int len, int cnt, long startTime) {
        if (cnt < 0 || buffer.length - len > 0) {
            this.br_flag = true;
        }
        long readTime = SystemClock.uptimeMillis() - startTime;
        if (readTime > 100) {
            Slog.w(TAG, "Reading wakelock stats took " + readTime + "ms");
        }
        if (len >= buffer.length) {
            Slog.v(TAG, "Kernel wake locks exceeded buffer size " + len + " " + cnt);
        }
        for (int i = 0; i < len; i++) {
            if (buffer[i] == 0) {
                return i;
            }
        }
        return len;
    }

    public final KernelWakelockStats readKernelWakelockStats(KernelWakelockStats staleStats) {
        FileInputStream is;
        boolean wakeup_sources;
        int cnt;
        byte[] buffer = new byte[32768];
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        try {
            FileInputStream is2 = new FileInputStream(sWakelockFile);
            is = is2;
            wakeup_sources = false;
        } catch (Exception e) {
            try {
                FileInputStream is3 = new FileInputStream(sWakeupSourceFile);
                is = is3;
                wakeup_sources = true;
            } catch (Exception e2) {
                Slog.wtf(TAG, "neither /proc/wakelocks nor /d/wakeup_sources exists");
                return null;
            }
        }
        while (true) {
            try {
                try {
                    long startTime = SystemClock.uptimeMillis();
                    this.br_flag = false;
                    int len = 0;
                    while (true) {
                        int len2 = buffer.length;
                        cnt = is.read(buffer, len, len2 - len);
                        if (cnt <= 0) {
                            break;
                        }
                        len += cnt;
                        Slog.v(TAG, "is.read return cnt = " + cnt);
                    }
                    if (len == 0) {
                        is.close();
                        Slog.v(TAG, "is.read return cnt is zero ? " + len);
                        break;
                    }
                    parseProcWakelocks(buffer, readInfoCheck(buffer, len, cnt, startTime), wakeup_sources, staleStats);
                    if (this.br_flag) {
                        is.close();
                        Slog.w(TAG, "read is at the end of file ");
                        break;
                    }
                } catch (IOException e3) {
                    Slog.wtf(TAG, "failed to read kernel wakelocks", e3);
                    StrictMode.setThreadPolicyMask(oldMask);
                    try {
                        is.close();
                    } catch (Exception e4) {
                        Slog.wtf(TAG, "failed to read kernel wakelocks", e4);
                    }
                    return null;
                }
            } finally {
            }
        }
        StrictMode.setThreadPolicyMask(oldMask);
        try {
            is.close();
        } catch (Exception e5) {
            Slog.wtf(TAG, "failed to read kernel wakelocks", e5);
        }
        return staleStats;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:76:0x012e
        	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1166)
        	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:1022)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:55)
        */
    public com.android.server.power.stats.KernelWakelockStats parseProcWakelocks(byte[] r22, int r23, boolean r24, com.android.server.power.stats.KernelWakelockStats r25) {
        /*
            Method dump skipped, instructions count: 305
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.power.stats.KernelWakelockReaderRewrite.parseProcWakelocks(byte[], int, boolean, com.android.server.power.stats.KernelWakelockStats):com.android.server.power.stats.KernelWakelockStats");
    }
}

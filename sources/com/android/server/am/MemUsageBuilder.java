package com.android.server.am;

import android.os.Debug;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.MemInfoReader;
import com.android.server.ScoutHelper;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MemUsageBuilder {
    private static final long DUMP_DMABUF_THRESHOLD = 1048576;
    private static final long DUMP_GPUMemory_THRESHOLD = 1048576;
    static final long[] DUMP_MEM_BUCKETS = {5120, 7168, 10240, 15360, 20480, 30720, 40960, 81920, 122880, 163840, 204800, 256000, 307200, 358400, 409600, 512000, 614400, 819200, FormatBytesUtil.MB, 2097152, 5242880, 10485760, 20971520};
    private final AppProfiler mAppProfiler;
    private final ArrayList<ProcessMemInfo> memInfos;
    private ScoutMeminfo scoutInfo;
    final String title;
    long totalMemtrackGraphics = 0;
    long totalMemtrackGl = 0;
    long totalPss = 0;
    long totalSwapPss = 0;
    long totalMemtrack = 0;
    long cachedPss = 0;
    String subject = "";
    String stack = "";
    String fullNative = "";
    String shortNative = "";
    String fullJava = "";
    String summary = "";
    String topProcs = "";

    /* JADX INFO: Access modifiers changed from: package-private */
    public MemUsageBuilder(AppProfiler appProfiler, ArrayList<ProcessMemInfo> memInfos, String title) {
        this.memInfos = memInfos;
        this.mAppProfiler = appProfiler;
        this.title = title;
        prepare();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setScoutInfo(ScoutMeminfo scoutInfo) {
        this.scoutInfo = scoutInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void buildAll() {
        this.subject = buildSubject();
        this.stack = buildStack();
        this.fullNative = buildFullNative();
        this.shortNative = buildShortNative();
        this.fullJava = buildFullJava();
        this.summary = buildSummary();
        this.topProcs = buildTopProcs();
    }

    private void prepare() {
        List<ProcessCpuTracker.Stats> stats;
        int statsCount;
        SparseArray<ProcessMemInfo> infoMap = new SparseArray<>(this.memInfos.size());
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            infoMap.put(mi.pid, mi);
        }
        this.mAppProfiler.updateCpuStatsNow();
        long[] memtrackTmp = new long[4];
        char c = 2;
        long[] swaptrackTmp = new long[2];
        List<ProcessCpuTracker.Stats> stats2 = this.mAppProfiler.getCpuStats(new Predicate() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MemUsageBuilder.lambda$prepare$0((ProcessCpuTracker.Stats) obj);
            }
        });
        int statsCount2 = stats2.size();
        int i2 = 0;
        while (i2 < statsCount2) {
            ProcessCpuTracker.Stats st = stats2.get(i2);
            long pss = Debug.getPss(st.pid, swaptrackTmp, memtrackTmp);
            if (pss <= 0) {
                stats = stats2;
                statsCount = statsCount2;
            } else if (infoMap.indexOfKey(st.pid) >= 0) {
                stats = stats2;
                statsCount = statsCount2;
            } else {
                ProcessMemInfo mi2 = new ProcessMemInfo(st.name, st.pid, ScoutHelper.OOM_SCORE_ADJ_MIN, -1, "native", (String) null);
                mi2.pss = pss;
                stats = stats2;
                statsCount = statsCount2;
                mi2.swapPss = swaptrackTmp[1];
                mi2.memtrack = memtrackTmp[0];
                this.totalMemtrackGraphics += memtrackTmp[1];
                this.totalMemtrackGl += memtrackTmp[2];
                this.memInfos.add(mi2);
            }
            i2++;
            stats2 = stats;
            statsCount2 = statsCount;
        }
        int i3 = 0;
        int size2 = this.memInfos.size();
        while (i3 < size2) {
            ProcessMemInfo mi3 = this.memInfos.get(i3);
            if (mi3.pss == 0) {
                mi3.pss = Debug.getPss(mi3.pid, swaptrackTmp, memtrackTmp);
                mi3.swapPss = swaptrackTmp[1];
                mi3.memtrack = memtrackTmp[0];
                this.totalMemtrackGraphics += memtrackTmp[1];
                this.totalMemtrackGl += memtrackTmp[c];
            }
            this.totalPss += mi3.pss;
            this.totalSwapPss += mi3.swapPss;
            this.totalMemtrack += mi3.memtrack;
            i3++;
            swaptrackTmp = swaptrackTmp;
            c = 2;
        }
        Collections.sort(this.memInfos, new Comparator<ProcessMemInfo>() { // from class: com.android.server.am.MemUsageBuilder.1
            @Override // java.util.Comparator
            public int compare(ProcessMemInfo lhs, ProcessMemInfo rhs) {
                if (lhs.oomAdj != rhs.oomAdj) {
                    return lhs.oomAdj < rhs.oomAdj ? -1 : 1;
                }
                if (lhs.pss != rhs.pss) {
                    return lhs.pss < rhs.pss ? 1 : -1;
                }
                return 0;
            }
        });
        calculateCachedPss();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$prepare$0(ProcessCpuTracker.Stats st) {
        return st.vsize > 0;
    }

    private void calculateCachedPss() {
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj >= 900) {
                this.cachedPss += mi.pss;
            }
        }
    }

    String buildFullJava() {
        StringBuilder fullJavaBuilder = new StringBuilder(1024);
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj != -1000) {
                appendMemInfo(fullJavaBuilder, mi);
            }
        }
        fullJavaBuilder.append("           ");
        ProcessList.appendRamKb(fullJavaBuilder, this.totalPss);
        fullJavaBuilder.append(": TOTAL");
        if (this.totalMemtrack > 0) {
            fullJavaBuilder.append(" (");
            fullJavaBuilder.append(ActivityManagerService.stringifyKBSize(this.totalMemtrack));
            fullJavaBuilder.append(" memtrack)");
        }
        fullJavaBuilder.append("\n");
        return fullJavaBuilder.toString();
    }

    String buildShortNative() {
        StringBuilder shortNativeBuilder = new StringBuilder(1024);
        int size = this.memInfos.size();
        long extraNativeRam = 0;
        long extraNativeMemtrack = 0;
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj == -1000) {
                if (mi.pss >= 512) {
                    appendMemInfo(shortNativeBuilder, mi);
                } else {
                    extraNativeRam += mi.pss;
                    extraNativeMemtrack += mi.memtrack;
                }
            } else if (extraNativeRam > 0) {
                appendBasicMemEntry(shortNativeBuilder, ScoutHelper.OOM_SCORE_ADJ_MIN, -1, extraNativeRam, extraNativeMemtrack, "(Other native)");
                shortNativeBuilder.append('\n');
                extraNativeRam = 0;
            }
        }
        return shortNativeBuilder.toString();
    }

    String buildFullNative() {
        StringBuilder fullNativeBuilder = new StringBuilder(1024);
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj == -1000) {
                appendMemInfo(fullNativeBuilder, mi);
            }
        }
        return fullNativeBuilder.toString();
    }

    String buildStack() {
        StringBuilder stack = new StringBuilder(128);
        appendMemBucket(stack, this.totalPss, "total", true);
        int lastOomAdj = Integer.MIN_VALUE;
        boolean firstLine = true;
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj != -1000 && (mi.oomAdj < 500 || mi.oomAdj == 600 || mi.oomAdj == 700)) {
                if (lastOomAdj != mi.oomAdj) {
                    lastOomAdj = mi.oomAdj;
                    if (mi.oomAdj >= 0) {
                        if (firstLine) {
                            stack.append(":");
                            firstLine = false;
                        }
                        stack.append("\n\t at ");
                    } else {
                        stack.append("$");
                    }
                } else {
                    stack.append("$");
                }
                appendMemBucket(stack, mi.pss, mi.name, true);
                if (mi.oomAdj >= 0 && (i + 1 >= size || this.memInfos.get(i + 1).oomAdj != lastOomAdj)) {
                    stack.append("(");
                    for (int k = 0; k < ActivityManagerService.DUMP_MEM_OOM_ADJ.length; k++) {
                        if (ActivityManagerService.DUMP_MEM_OOM_ADJ[k] == mi.oomAdj) {
                            stack.append(ActivityManagerService.DUMP_MEM_OOM_LABEL[k]);
                            stack.append(":");
                            stack.append(ActivityManagerService.DUMP_MEM_OOM_ADJ[k]);
                        }
                    }
                    stack.append(")");
                }
            }
        }
        return stack.toString();
    }

    String buildSubject() {
        StringBuilder tag = new StringBuilder(128);
        tag.append(this.title).append(" -- ");
        appendMemBucket(tag, this.totalPss, "total", false);
        int lastOomAdj = Integer.MIN_VALUE;
        int size = this.memInfos.size();
        for (int i = 0; i < size; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            if (mi.oomAdj != -1000 && (mi.oomAdj < 500 || mi.oomAdj == 600 || mi.oomAdj == 700)) {
                if (lastOomAdj != mi.oomAdj) {
                    lastOomAdj = mi.oomAdj;
                    if (mi.oomAdj <= 0) {
                        tag.append(" / ");
                    }
                } else {
                    tag.append(" ");
                }
                if (mi.oomAdj <= 0) {
                    appendMemBucket(tag, mi.pss, mi.name, false);
                }
            }
        }
        return tag.toString();
    }

    String buildSummary() {
        Boolean dumpGpuInfo;
        boolean dumpDmabufInfo;
        Boolean dumpDmabufInfo2;
        MemInfoReader memInfo;
        Boolean dumpDmabufInfo3;
        Boolean dumpGpuInfo2;
        String gpuInfo;
        String dmabufInfo;
        Optional<ScoutMeminfo> scoutInfo = Optional.ofNullable(this.scoutInfo);
        MemInfoReader memInfo2 = new MemInfoReader();
        memInfo2.readMemInfo();
        final long[] infos = memInfo2.getRawInfo();
        StringBuilder memInfoBuilder = new StringBuilder(1024);
        Debug.getMemInfo(infos);
        memInfoBuilder.append("  MemInfo: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[5])).append(" slab, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[4])).append(" shmem, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[12])).append(" vm alloc, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[13])).append(" page tables ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[14])).append(" kernel stack\n");
        memInfoBuilder.append("           ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[2])).append(" buffers, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[3])).append(" cached, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[11])).append(" mapped, ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[1])).append(" free\n");
        if (infos[10] != 0) {
            memInfoBuilder.append("  ZRAM: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[10]));
            memInfoBuilder.append(" RAM, ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[8]));
            memInfoBuilder.append(" swap total, ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(infos[9]));
            memInfoBuilder.append(" swap free\n");
        }
        final long[] ksm = ActivityManagerService.getKsmInfo();
        if (ksm[1] != 0 || ksm[0] != 0 || ksm[2] != 0 || ksm[3] != 0) {
            memInfoBuilder.append("  KSM: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[1]));
            memInfoBuilder.append(" saved from shared ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[0]));
            memInfoBuilder.append("\n       ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[2]));
            memInfoBuilder.append(" unshared; ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ksm[3]));
            memInfoBuilder.append(" volatile\n");
        }
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setInfos(infos);
            }
        });
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setKsm(ksm);
            }
        });
        memInfoBuilder.append("  Free RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(this.cachedPss + memInfo2.getCachedSizeKb() + memInfo2.getFreeSizeKb()));
        memInfoBuilder.append("\n");
        long kernelUsed = memInfo2.getKernelUsedSizeKb();
        final long ionHeap = Debug.getIonHeapsSizeKb();
        final long ionPool = Debug.getIonPoolsSizeKb();
        final long dmabufMapped = Debug.getDmabufMappedSizeKb();
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setIonHeap(ionHeap);
            }
        });
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setIonPool(ionPool);
            }
        });
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setDmabufMapped(dmabufMapped);
            }
        });
        if (ionHeap < 0 || ionPool < 0) {
            dumpGpuInfo = false;
            final long totalExportedDmabuf = Debug.getDmabufTotalExportedKb();
            if (totalExportedDmabuf < 0) {
                dumpDmabufInfo = false;
            } else {
                long dmabufUnmapped = totalExportedDmabuf - dmabufMapped;
                dumpDmabufInfo = false;
                memInfoBuilder.append("DMA-BUF: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalExportedDmabuf));
                memInfoBuilder.append("\n");
                if (dmabufUnmapped > FormatBytesUtil.MB) {
                    dumpDmabufInfo = true;
                }
                long kernelUsed2 = kernelUsed + dmabufUnmapped;
                long j = this.totalPss - this.totalMemtrackGraphics;
                this.totalPss = j;
                this.totalPss = j + dmabufMapped;
                scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda8
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ScoutMeminfo) obj).setTotalExportedDmabuf(totalExportedDmabuf);
                    }
                });
                kernelUsed = kernelUsed2;
            }
            final long totalExportedDmabufHeap = Debug.getDmabufHeapTotalExportedKb();
            if (totalExportedDmabufHeap >= 0) {
                memInfoBuilder.append("DMA-BUF Heap: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalExportedDmabufHeap));
                memInfoBuilder.append("\n");
                scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda9
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ScoutMeminfo) obj).setTotalExportedDmabufHeap(totalExportedDmabufHeap);
                    }
                });
            }
            final long totalDmabufHeapPool = Debug.getDmabufHeapPoolsSizeKb();
            if (totalDmabufHeapPool >= 0) {
                memInfoBuilder.append("DMA-BUF Heaps pool: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(totalDmabufHeapPool));
                memInfoBuilder.append("\n");
                scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda10
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ScoutMeminfo) obj).setTotalDmabufHeapPool(totalDmabufHeapPool);
                    }
                });
            }
            dumpDmabufInfo2 = dumpDmabufInfo;
        } else {
            long ionUnmapped = ionHeap - dmabufMapped;
            memInfoBuilder.append("       ION: ");
            memInfoBuilder.append(ActivityManagerService.stringifyKBSize(ionHeap + ionPool));
            memInfoBuilder.append("\n");
            dumpGpuInfo = false;
            long j2 = this.totalPss - this.totalMemtrackGraphics;
            this.totalPss = j2;
            this.totalPss = j2 + dmabufMapped;
            dumpDmabufInfo2 = false;
            kernelUsed += ionUnmapped;
        }
        final long gpuUsage = Debug.getGpuTotalUsageKb();
        if (gpuUsage < 0) {
            memInfo = memInfo2;
            dumpDmabufInfo3 = dumpDmabufInfo2;
            dumpGpuInfo2 = dumpGpuInfo;
        } else {
            final long gpuPrivateUsage = Debug.getGpuPrivateMemoryKb();
            if (gpuPrivateUsage >= 0) {
                final long dmabufMapped2 = gpuUsage - gpuPrivateUsage;
                memInfoBuilder.append("      GPU: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuUsage));
                memInfoBuilder.append(" (");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(dmabufMapped2));
                memInfoBuilder.append(" dmabuf + ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuPrivateUsage));
                memInfoBuilder.append(" private)\n");
                memInfo = memInfo2;
                dumpDmabufInfo3 = dumpDmabufInfo2;
                this.totalPss -= this.totalMemtrackGl;
                kernelUsed += gpuPrivateUsage;
                scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda11
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ScoutMeminfo) obj).setGpuDmaBufUsage(dmabufMapped2);
                    }
                });
                dumpGpuInfo2 = dumpGpuInfo;
            } else {
                memInfo = memInfo2;
                dumpDmabufInfo3 = dumpDmabufInfo2;
                memInfoBuilder.append("       GPU: ");
                memInfoBuilder.append(ActivityManagerService.stringifyKBSize(gpuUsage));
                memInfoBuilder.append("\n");
                if (gpuUsage <= FormatBytesUtil.MB) {
                    dumpGpuInfo2 = dumpGpuInfo;
                } else {
                    dumpGpuInfo2 = true;
                }
            }
            scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda12
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ScoutMeminfo) obj).setGpuUsage(gpuUsage);
                }
            });
            scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ScoutMeminfo) obj).setGpuPrivateUsage(gpuPrivateUsage);
                }
            });
        }
        memInfoBuilder.append("  Used RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize((this.totalPss - this.cachedPss) + kernelUsed));
        memInfoBuilder.append("\n");
        memInfoBuilder.append("  Lost RAM: ");
        memInfoBuilder.append(ActivityManagerService.stringifyKBSize(((((memInfo.getTotalSizeKb() - (this.totalPss - this.totalSwapPss)) - memInfo.getFreeSizeKb()) - memInfo.getCachedSizeKb()) - kernelUsed) - memInfo.getZramTotalSizeKb()));
        memInfoBuilder.append("\n");
        final long finalKernelUsed = kernelUsed;
        final MemInfoReader memInfo3 = memInfo;
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setMeminfo(memInfo3);
            }
        });
        scoutInfo.ifPresent(new Consumer() { // from class: com.android.server.am.MemUsageBuilder$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScoutMeminfo) obj).setKernelUsed(finalKernelUsed);
            }
        });
        if (dumpDmabufInfo3.booleanValue() && (dmabufInfo = ScoutDisplayMemoryManager.getInstance().getDmabufUsageInfo()) != null) {
            memInfoBuilder.append("\n\nDMABUF usage Info:\n");
            memInfoBuilder.append(dmabufInfo);
            memInfoBuilder.append("\n");
        }
        if (dumpGpuInfo2.booleanValue() && (gpuInfo = ScoutDisplayMemoryManager.getInstance().getGpuMemoryUsageInfo()) != null) {
            memInfoBuilder.append("GPU usage Info:\n");
            memInfoBuilder.append(gpuInfo);
            memInfoBuilder.append("\n");
        }
        return memInfoBuilder.toString();
    }

    String buildTopProcs() {
        Collections.sort(this.memInfos, new Comparator<ProcessMemInfo>() { // from class: com.android.server.am.MemUsageBuilder.2
            @Override // java.util.Comparator
            public int compare(ProcessMemInfo lhs, ProcessMemInfo rhs) {
                if (lhs.pss != rhs.pss) {
                    return lhs.pss < rhs.pss ? 1 : -1;
                }
                return 0;
            }
        });
        StringBuilder sb = new StringBuilder(128);
        sb.append("Number of processes: " + this.memInfos.size()).append("\n");
        int size = this.memInfos.size();
        for (int i = 0; i < size && i < 10; i++) {
            ProcessMemInfo mi = this.memInfos.get(i);
            Debug.MemoryInfo info = new Debug.MemoryInfo();
            Debug.getMemoryInfo(mi.pid, info);
            sb.append("TOP " + (i + 1));
            sb.append("    Pid:" + mi.pid);
            sb.append("    Process:" + mi.name);
            sb.append("    Pss:" + mi.pss);
            sb.append("    Java Heap Pss:" + info.dalvikPss);
            sb.append("    Native Heap Pss:" + info.nativePss);
            sb.append("    Graphics Pss:" + info.getSummaryGraphics());
            sb.append("\n");
        }
        return sb.toString();
    }

    static void appendBasicMemEntry(StringBuilder sb, int oomAdj, int procState, long pss, long memtrack, String name) {
        sb.append("  ");
        sb.append(ProcessList.makeOomAdjString(oomAdj, false));
        sb.append(' ');
        sb.append(ProcessList.makeProcStateString(procState));
        sb.append(' ');
        ProcessList.appendRamKb(sb, pss);
        sb.append(": ");
        sb.append(name);
        if (memtrack > 0) {
            sb.append(" (");
            sb.append(ActivityManagerService.stringifyKBSize(memtrack));
            sb.append(" memtrack)");
        }
    }

    static void appendMemInfo(StringBuilder sb, ProcessMemInfo mi) {
        appendBasicMemEntry(sb, mi.oomAdj, mi.procState, mi.pss, mi.memtrack, mi.name);
        sb.append(" (pid ");
        sb.append(mi.pid);
        sb.append(") ");
        sb.append(mi.adjType);
        sb.append('\n');
        if (mi.adjReason != null) {
            sb.append("                      ");
            sb.append(mi.adjReason);
            sb.append('\n');
        }
    }

    static final void appendMemBucket(StringBuilder out, long memKB, String label, boolean stackLike) {
        int start = label.lastIndexOf(46);
        int start2 = start >= 0 ? start + 1 : 0;
        int end = label.length();
        int i = 0;
        while (true) {
            long[] jArr = DUMP_MEM_BUCKETS;
            if (i < jArr.length) {
                long j = jArr[i];
                if (j < memKB) {
                    i++;
                } else {
                    long bucket = j / FormatBytesUtil.KB;
                    out.append(bucket);
                    out.append(stackLike ? "MB." : "MB ");
                    out.append((CharSequence) label, start2, end);
                    return;
                }
            } else {
                out.append(memKB / FormatBytesUtil.KB);
                out.append(stackLike ? "MB." : "MB ");
                out.append((CharSequence) label, start2, end);
                return;
            }
        }
    }
}

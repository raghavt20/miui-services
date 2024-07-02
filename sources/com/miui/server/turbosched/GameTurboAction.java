package com.miui.server.turbosched;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class GameTurboAction {
    private static final String FLW_CHOOSE_LOAD_POLICY_PATH = "/sys/module/migt/parameters/choose_load_policy";
    private static final String FLW_CHOOSE_WAKE_STATE_PATH = "/sys/module/migt/parameters/choose_wake_state";
    private static final String FLW_DEBUG_PATH = "/sys/module/migt/parameters/flw_debug";
    private static final String FLW_DOWN_THRESH_MARGIN_PATH = "/sys/module/migt/parameters/down_thresh_margin";
    private static final String FLW_ENABLE_PATH = "/sys/module/migt/parameters/flw_enable";
    private static final String FLW_FREQ_ENABLE_PATH = "/sys/module/migt/parameters/flw_freq_enable";
    private static final String FLW_TARGET_DELTA_PATH = "/sys/module/migt/parameters/target_delta";
    private static final String FLW_UP_THRESH_MARGIN_PATH = "/sys/module/migt/parameters/up_thresh_margin";
    private static final String FLW_VERSION_PATH = "/sys/module/migt/parameters/version";
    private static final String SOC_DFLT_BW_ENABLE_PATH = "/sys/module/mist/parameters/dflt_bw_enable";
    private static final String SOC_DFLT_DEBUG_PATH = "/sys/module/mist/parameters/dflt_debug";
    private static final String SOC_DFLT_LAT_ENABLE_PATH = "/sys/module/mist/parameters/dflt_lat_enable";
    private static final String SOC_DFLT_VERSION_PATH = "/sys/module/mist/parameters/mist_version";
    private static final String SOC_GFLT_DEBUG_PATH = "/sys/module/mist/parameters/gflt_debug";
    private static final String SOC_GFLT_ENABLE_PATH = "/sys/module/mist/parameters/gflt_enable";
    private static final String TAG = "TurboSched_GameTurboAction";
    private static final String VERSION = "1.0.0";
    private boolean isFLWAccess = TurboSchedUtil.checkFileAccess(FLW_ENABLE_PATH);
    private boolean isFLWFreqAccess = TurboSchedUtil.checkFileAccess(FLW_FREQ_ENABLE_PATH);
    private boolean isSoCGfltAccess = TurboSchedUtil.checkFileAccess(SOC_GFLT_ENABLE_PATH);
    private boolean isSocDfltBwAccess = TurboSchedUtil.checkFileAccess(SOC_DFLT_BW_ENABLE_PATH);
    private boolean isSocDfltLatAccess = TurboSchedUtil.checkFileAccess(SOC_DFLT_LAT_ENABLE_PATH);
    private boolean isFLWEnable = false;
    private boolean isFLWFreqEnable = false;
    private boolean isSoCGfltEnable = false;
    private boolean isSocDfltBwEnable = false;
    private boolean isSocDfltLatEnable = false;

    /* JADX INFO: Access modifiers changed from: protected */
    public GameTurboAction() {
        readConfigValue();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        boolean handled = parseDumpCommand(fd, pw, args);
        if (handled) {
            return true;
        }
        pw.println("version: 1.0.0");
        pw.println("--------------------current status-----------------------");
        printFLWConfig(pw);
        printSoCGfltConfig(pw);
        printSocDfltConfig(pw);
        pw.println("--------------------kernel config-----------------------");
        pw.println("flw access: " + this.isFLWAccess);
        pw.println("flw freq access: " + this.isFLWFreqAccess);
        pw.println("gflt access: " + this.isSoCGfltAccess);
        pw.println("dflt bw access: " + this.isSocDfltBwAccess);
        pw.println("dflt lat access: " + this.isSocDfltLatAccess);
        return true;
    }

    private void printFLWConfig(PrintWriter pw) {
        pw.println("flw :");
        pw.println(" enable: " + this.isFLWEnable + ", version: " + TurboSchedUtil.readValueFromFile(FLW_VERSION_PATH));
        pw.println(" freq enable: " + this.isFLWFreqEnable);
        pw.println(" target delta: " + TurboSchedUtil.readBooleanValueFromFile(FLW_TARGET_DELTA_PATH));
        pw.println(" up thresh margin: " + TurboSchedUtil.readValueFromFile(FLW_UP_THRESH_MARGIN_PATH));
        pw.println(" down thresh margin: " + TurboSchedUtil.readValueFromFile(FLW_DOWN_THRESH_MARGIN_PATH));
        pw.println(" choose wake state: " + TurboSchedUtil.readValueFromFile(FLW_CHOOSE_WAKE_STATE_PATH));
        pw.println(" choose load policy: " + TurboSchedUtil.readValueFromFile(FLW_CHOOSE_LOAD_POLICY_PATH));
    }

    private void printSoCGfltConfig(PrintWriter pw) {
        pw.println("gflt :");
        pw.println(" enable: " + this.isSoCGfltEnable);
    }

    private void printSocDfltConfig(PrintWriter pw) {
        pw.println("dflt :");
        pw.println(" version: " + TurboSchedUtil.readValueFromFile(SOC_DFLT_VERSION_PATH));
        pw.println(" bw enable: " + this.isSocDfltBwEnable);
        pw.println(" lat enable: " + this.isSocDfltLatEnable);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x0016, code lost:
    
        if (r0.equals("gflt") != false) goto L18;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseDumpCommand(java.io.FileDescriptor r5, java.io.PrintWriter r6, java.lang.String[] r7) {
        /*
            r4 = this;
            int r0 = r7.length
            r1 = 0
            r2 = 1
            if (r0 >= r2) goto L6
            return r1
        L6:
            r0 = r7[r1]
            int r3 = r0.hashCode()
            switch(r3) {
                case 101489: goto L23;
                case 3080586: goto L19;
                case 3169959: goto L10;
                default: goto Lf;
            }
        Lf:
            goto L2d
        L10:
            java.lang.String r3 = "gflt"
            boolean r3 = r0.equals(r3)
            if (r3 == 0) goto Lf
            goto L2e
        L19:
            java.lang.String r2 = "dflt"
            boolean r2 = r0.equals(r2)
            if (r2 == 0) goto Lf
            r2 = 2
            goto L2e
        L23:
            java.lang.String r2 = "flw"
            boolean r2 = r0.equals(r2)
            if (r2 == 0) goto Lf
            r2 = r1
            goto L2e
        L2d:
            r2 = -1
        L2e:
            switch(r2) {
                case 0: goto L3c;
                case 1: goto L37;
                case 2: goto L32;
                default: goto L31;
            }
        L31:
            return r1
        L32:
            boolean r1 = r4.parseDFLTDumpCommand(r5, r6, r7)
            return r1
        L37:
            boolean r1 = r4.parseGFLTDumpCommand(r5, r6, r7)
            return r1
        L3c:
            boolean r1 = r4.parseFLWDumpCommand(r5, r6, r7)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.GameTurboAction.parseDumpCommand(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:12:0x0044. Please report as an issue. */
    /* JADX WARN: Removed duplicated region for block: B:35:0x00b1  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00d0  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseFLWDumpCommand(java.io.FileDescriptor r10, java.io.PrintWriter r11, java.lang.String[] r12) {
        /*
            Method dump skipped, instructions count: 278
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.GameTurboAction.parseFLWDumpCommand(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:12:0x003b. Please report as an issue. */
    /* JADX WARN: Removed duplicated region for block: B:31:0x008e  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x00ad  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseGFLTDumpCommand(java.io.FileDescriptor r11, java.io.PrintWriter r12, java.lang.String[] r13) {
        /*
            r10 = this;
            int r0 = r13.length
            java.lang.String r1 = "invalid gflt command"
            r2 = 1
            r3 = 2
            if (r0 >= r3) goto Lb
            r12.println(r1)
            return r2
        Lb:
            r0 = r13[r2]
            r4 = 0
            int r5 = r0.hashCode()
            switch(r5) {
                case -1298848381: goto L2a;
                case 95458899: goto L20;
                case 1671308008: goto L16;
                default: goto L15;
            }
        L15:
            goto L34
        L16:
            java.lang.String r5 = "disable"
            boolean r5 = r0.equals(r5)
            if (r5 == 0) goto L15
            r5 = r2
            goto L35
        L20:
            java.lang.String r5 = "debug"
            boolean r5 = r0.equals(r5)
            if (r5 == 0) goto L15
            r5 = r3
            goto L35
        L2a:
            java.lang.String r5 = "enable"
            boolean r5 = r0.equals(r5)
            if (r5 == 0) goto L15
            r5 = 0
            goto L35
        L34:
            r5 = -1
        L35:
            java.lang.String r6 = "0"
            java.lang.String r7 = "1"
            java.lang.String r8 = "/sys/module/mist/parameters/gflt_enable"
            switch(r5) {
                case 0: goto L7c;
                case 1: goto L6d;
                case 2: goto L40;
                default: goto L3e;
            }
        L3e:
            goto Ld0
        L40:
            java.lang.String r5 = "/sys/module/mist/parameters/gflt_debug"
            boolean r8 = com.miui.server.turbosched.TurboSchedUtil.checkFileAccess(r5)
            if (r8 == 0) goto Ld0
            int r8 = r13.length
            r9 = 3
            if (r8 >= r9) goto L50
            r12.println(r1)
            return r2
        L50:
            r8 = r13[r3]
            boolean r6 = r6.equals(r8)
            if (r6 != 0) goto L66
            r6 = r13[r3]
            boolean r6 = r7.equals(r6)
            if (r6 != 0) goto L66
            java.lang.String r1 = "invalid gflt debug command"
            r12.println(r1)
            return r2
        L66:
            r3 = r13[r3]
            java.lang.String r4 = com.miui.server.turbosched.TurboSchedUtil.writeValueToFile(r5, r3)
            goto Ld0
        L6d:
            boolean r1 = r10.isSoCGfltAccess
            if (r1 == 0) goto L8a
            java.lang.String r4 = com.miui.server.turbosched.TurboSchedUtil.writeValueToFile(r8, r6)
            boolean r1 = com.miui.server.turbosched.TurboSchedUtil.readBooleanValueFromFile(r8)
            r10.isSoCGfltEnable = r1
            goto L8a
        L7c:
            boolean r1 = r10.isSoCGfltAccess
            if (r1 == 0) goto L8a
            java.lang.String r4 = com.miui.server.turbosched.TurboSchedUtil.writeValueToFile(r8, r7)
            boolean r1 = com.miui.server.turbosched.TurboSchedUtil.readBooleanValueFromFile(r8)
            r10.isSoCGfltEnable = r1
        L8a:
            java.lang.String r1 = "gflt "
            if (r4 == 0) goto Lad
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.StringBuilder r1 = r3.append(r1)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r3 = " failed: "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r4)
            java.lang.String r1 = r1.toString()
            r12.println(r1)
            return r2
        Lad:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.StringBuilder r1 = r3.append(r1)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r3 = " success"
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.String r1 = r1.toString()
            r12.println(r1)
            java.lang.String r1 = "--------------------current status-----------------------"
            r12.println(r1)
            r10.printSoCGfltConfig(r12)
            return r2
        Ld0:
            r12.println(r1)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.GameTurboAction.parseGFLTDumpCommand(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:14:0x0047. Please report as an issue. */
    /* JADX WARN: Removed duplicated region for block: B:33:0x00ba  */
    /* JADX WARN: Removed duplicated region for block: B:35:0x00e3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean parseDFLTDumpCommand(java.io.FileDescriptor r12, java.io.PrintWriter r13, java.lang.String[] r14) {
        /*
            Method dump skipped, instructions count: 300
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.turbosched.GameTurboAction.parseDFLTDumpCommand(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):boolean");
    }

    private void readConfigValue() {
        if (this.isFLWAccess) {
            this.isFLWEnable = TurboSchedUtil.readBooleanValueFromFile(FLW_ENABLE_PATH);
        }
        if (this.isFLWFreqAccess) {
            this.isFLWFreqEnable = TurboSchedUtil.readBooleanValueFromFile(FLW_FREQ_ENABLE_PATH);
        }
        if (this.isSoCGfltAccess) {
            this.isSoCGfltEnable = TurboSchedUtil.readBooleanValueFromFile(SOC_GFLT_ENABLE_PATH);
        }
        if (this.isSocDfltBwAccess) {
            this.isSocDfltBwEnable = TurboSchedUtil.readBooleanValueFromFile(SOC_DFLT_BW_ENABLE_PATH);
        }
        if (this.isSocDfltLatAccess) {
            this.isSocDfltLatEnable = TurboSchedUtil.readBooleanValueFromFile(SOC_DFLT_LAT_ENABLE_PATH);
        }
    }
}

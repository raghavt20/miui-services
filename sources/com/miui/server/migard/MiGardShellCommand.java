package com.miui.server.migard;

import android.os.ShellCommand;
import java.io.PrintWriter;

/* loaded from: classes.dex */
class MiGardShellCommand extends ShellCommand {
    MiGardService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiGardShellCommand(MiGardService service) {
        this.mService = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        char c;
        PrintWriter pw = getOutPrintWriter();
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        try {
            switch (cmd.hashCode()) {
                case -1729603354:
                    if (cmd.equals("trace-buffer-size")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -487393236:
                    if (cmd.equals("dump-trace")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1340897306:
                    if (cmd.equals("start-trace")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1857979322:
                    if (cmd.equals("stop-trace")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    boolean async = Boolean.parseBoolean(getNextArgRequired());
                    this.mService.startDefaultTrace(async);
                    return 0;
                case 1:
                    boolean compressed = Boolean.parseBoolean(getNextArgRequired());
                    this.mService.stopTrace(compressed);
                    return 0;
                case 2:
                    boolean compressed2 = Boolean.parseBoolean(getNextArgRequired());
                    this.mService.dumpTrace(compressed2);
                    return 0;
                case 3:
                    this.mService.setTraceBufferSize(Integer.parseInt(getNextArgRequired()));
                    return 0;
            }
        } catch (Exception e) {
            pw.println(e);
        }
        if (this.mService.mMemCleaner.onShellCommand(cmd, getNextArgRequired(), pw)) {
            return 0;
        }
        return handleDefaultCommands(cmd);
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("MiGardService commands:");
        pw.println();
        pw.println("start-trace [async=true|false]");
        pw.println();
        pw.println("stop-trace [compressed=true|false]");
        pw.println();
        pw.println("dump-trace [compressed=true|false]");
        pw.println();
        pw.println("trace-buffer-size [size KB]");
        pw.println();
        this.mService.mMemCleaner.onShellHelp(pw);
    }
}

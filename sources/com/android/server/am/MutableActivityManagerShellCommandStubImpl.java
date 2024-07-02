package com.android.server.am;

import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import com.android.server.am.ActivityManagerShellCommand;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public class MutableActivityManagerShellCommandStubImpl implements MutableActivityManagerShellCommandStub {
    private static final int DEFAULT_MAX_SHELL_SYNC_BROADCAST = 5;
    private static final String PROPERTIES_MAX_SHELL_SYNC_BROADCAST = "persist.sys.max_shell_sync_broadcast";
    private static AtomicInteger mShellSyncBroadcast = new AtomicInteger(0);
    ActivityManagerShellCommand activityManagerShellCommand;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MutableActivityManagerShellCommandStubImpl> {

        /* compiled from: MutableActivityManagerShellCommandStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MutableActivityManagerShellCommandStubImpl INSTANCE = new MutableActivityManagerShellCommandStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MutableActivityManagerShellCommandStubImpl m546provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MutableActivityManagerShellCommandStubImpl m545provideNewInstance() {
            return new MutableActivityManagerShellCommandStubImpl();
        }
    }

    public void init(ActivityManagerShellCommand activityManagerShellCommand) {
        this.activityManagerShellCommand = activityManagerShellCommand;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int addLog(PrintWriter pw, String cmd) throws RemoteException {
        char c;
        switch (cmd.hashCode()) {
            case -1534970605:
                if (cmd.equals("exit-privatecast")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1416239474:
                if (cmd.equals("exit-cast")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -375316139:
                if (cmd.equals("move-to-cast")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 342281055:
                if (cmd.equals("logging")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1481350252:
                if (cmd.equals("move-to-privatecast")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 2142599091:
                if (cmd.equals("app-logging")) {
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
                runLogging(pw);
                return 0;
            case 1:
                runAppLogging(pw);
                return 0;
            case 2:
                this.activityManagerShellCommand.mTaskInterface.moveTopActivityToCastMode();
                return 0;
            case 3:
                this.activityManagerShellCommand.mTaskInterface.exitCastMode();
                return 0;
            case 4:
                this.activityManagerShellCommand.mWindowInterface.setScreenProjectionList(1, 1);
                return 0;
            case 5:
                this.activityManagerShellCommand.mWindowInterface.setScreenProjectionList(1, 0);
                return 0;
            default:
                return -1;
        }
    }

    public void runLogging(PrintWriter pw) throws RemoteException {
        String cmd = this.activityManagerShellCommand.getNextArgRequired();
        if (!"enable-text".equals(cmd) && !"disable-text".equals(cmd)) {
            pw.println("Error: wrong args , must be enable-text or disable-text");
            return;
        }
        boolean enable = "enable-text".equals(cmd);
        while (true) {
            String config = this.activityManagerShellCommand.getNextArg();
            if (config != null) {
                this.activityManagerShellCommand.mInterface.enableAmsDebugConfig(config, enable);
            } else {
                return;
            }
        }
    }

    public void runAppLogging(PrintWriter pw) throws RemoteException {
        String processName = this.activityManagerShellCommand.getNextArgRequired();
        int uid = Integer.parseInt(this.activityManagerShellCommand.getNextArgRequired());
        String cmd = this.activityManagerShellCommand.getNextArgRequired();
        if (!"enable-text".equals(cmd) && !"disable-text".equals(cmd)) {
            pw.println("Error: wrong args , must be enable-text or disable-text");
            return;
        }
        boolean enable = "enable-text".equals(cmd);
        while (true) {
            String config = this.activityManagerShellCommand.getNextArg();
            if (config != null) {
                this.activityManagerShellCommand.mInterface.enableAppDebugConfig(config, enable, processName, uid);
            } else {
                return;
            }
        }
    }

    public boolean waitForFinishIfNeeded(ActivityManagerShellCommand.IntentReceiver receiver) {
        if (Build.isDebuggable()) {
            if (mShellSyncBroadcast.incrementAndGet() <= SystemProperties.getInt(PROPERTIES_MAX_SHELL_SYNC_BROADCAST, 5)) {
                receiver.waitForFinish();
            }
            mShellSyncBroadcast.decrementAndGet();
            return true;
        }
        receiver.waitForFinish();
        return true;
    }
}

package com.android.server.powerconsumpiton;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.powerconsumption.IMiuiPowerConsumptionManager;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.MiuiBgThread;
import com.android.server.MiuiFgThread;
import com.android.server.PowerConsumptionServiceInternal;
import com.android.server.SystemService;
import com.android.server.wm.IMiuiWindowStateEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.BiConsumer;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class PowerConsumptionService extends SystemService {
    public static final String SERVICE_NAME = "powerconsumption";
    private static final String TAG = "PowerConsumptionService";
    private static boolean sDEBUG = false;
    private final Context mContext;
    private boolean mIsSupportPCS;
    private Handler mPCBgHandler;
    private Handler mPCFgHandler;
    private PowerConsumptionServiceController mPowerConsumptionServiceController;
    private PowerConsumptionServiceCouldData mPowerConsumptionServiceCouldData;
    private SystemStatusListener mSystemStatusListener;

    /* JADX WARN: Multi-variable type inference failed */
    public void onStart() {
        publishLocalService(PowerConsumptionServiceInternal.class, new LocalService());
        publishBinderService(SERVICE_NAME, new BinderService());
        if (sDEBUG) {
            Slog.d(TAG, "PowerConsumptionService start");
        }
    }

    public PowerConsumptionService(Context context) {
        super(context);
        this.mContext = context;
        this.mPCFgHandler = new Handler(MiuiFgThread.get().getLooper());
        Handler handler = new Handler(MiuiBgThread.get().getLooper());
        this.mPCBgHandler = handler;
        this.mPowerConsumptionServiceCouldData = new PowerConsumptionServiceCouldData(context, handler);
        this.mSystemStatusListener = new SystemStatusListener(this.mPCFgHandler, context);
        PowerConsumptionServiceController powerConsumptionServiceController = new PowerConsumptionServiceController(this.mPowerConsumptionServiceCouldData, this.mSystemStatusListener, this.mPCFgHandler, context);
        this.mPowerConsumptionServiceController = powerConsumptionServiceController;
        this.mSystemStatusListener.setPowerConsumptionServiceController(powerConsumptionServiceController);
        boolean z = FeatureParser.getBoolean("support_power_consumption", false);
        this.mIsSupportPCS = z;
        if (!z) {
            Slog.d(TAG, "this device not support PowerConsumption");
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IMiuiPowerConsumptionManager.Stub {
        private BinderService() {
        }

        public void setPowerConsumptionPolicy(int flag) {
            if (PowerConsumptionService.sDEBUG) {
                Slog.d(PowerConsumptionService.TAG, "setPowerConsumptionPolicy flag " + flag);
            }
        }

        public boolean isContentKeyInPowerConsumptionWhiteList() {
            return PowerConsumptionService.this.mPowerConsumptionServiceController.getContentKeyInWhiteList();
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            doDump(fd, pw, args);
        }

        private void doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
            String opt;
            if (DumpUtils.checkDumpPermission(PowerConsumptionService.this.mContext, PowerConsumptionService.TAG, pw)) {
                int opti = 0;
                if (0 < args.length && (opt = args[0]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
                    opti = 0 + 1;
                    if (!"-a".equals(opt)) {
                        if ("-h".equals(opt)) {
                            pw.println("Power consumption dump options:");
                            pw.println(" [-a] [-h] [-debug] [cmd] ...");
                            pw.println("  cmd may be one of:");
                            pw.println("    s[status]: system status");
                            pw.println("    d[data]: cloud data");
                            pw.println("    p[policy]: current policy");
                            pw.println("  -a: include all available server state.");
                            pw.println("  -debug: debug switch in powerconsumption.");
                            if (!PowerConsumptionService.this.mIsSupportPCS) {
                                pw.println(" this device  donot support PowerConsumption");
                                return;
                            }
                            return;
                        }
                        if ("-debug".equals(opt)) {
                            if (opti < args.length) {
                                String opt2 = args[opti];
                                if ("true".equals(opt2)) {
                                    PowerConsumptionService.sDEBUG = true;
                                    pw.println("debug is true");
                                    return;
                                } else if ("false".equals(opt2)) {
                                    PowerConsumptionService.sDEBUG = false;
                                    pw.println("debug is false");
                                    return;
                                } else {
                                    pw.println("wrong argument for debug; use -h for help");
                                    return;
                                }
                            }
                            pw.println("empty argument for debug; use -h for help");
                            return;
                        }
                        pw.println("Unknown argument: " + opt + "; use -h for help");
                        return;
                    }
                }
                if (opti < args.length) {
                    String cmd = args[opti];
                    int i = opti + 1;
                    if ("status".equals(cmd) || "s".equals(cmd)) {
                        dumpSystemStatus(pw);
                        return;
                    }
                    if ("data".equals(cmd) || "d".equals(cmd)) {
                        dumpCouldData(pw);
                        return;
                    } else if ("policy".equals(cmd) || "p".equals(cmd)) {
                        dumpPolicy(pw);
                        return;
                    } else {
                        pw.println("Bad command: " + cmd + "; use -h for help");
                        return;
                    }
                }
                pw.println();
                pw.println("-------------------------------------------------------------------------------");
                dumpSystemStatus(pw);
                pw.println("-------------------------------------------------------------------------------");
                dumpCouldData(pw);
                pw.println("-------------------------------------------------------------------------------");
                dumpPolicy(pw);
            }
        }

        private void dumpCouldData(PrintWriter pw) {
            pw.println("POWER CONSUMPTION Cloud Data (dumpsys powerconsumption data)");
            PowerConsumptionService.this.mPowerConsumptionServiceCouldData.dump(pw);
        }

        private void dumpSystemStatus(PrintWriter pw) {
            pw.println("POWER CONSUMPTION SYSTEM STATE (dumpsys powerconsumption status)");
            PowerConsumptionService.this.mSystemStatusListener.dump(pw);
        }

        private void dumpPolicy(PrintWriter pw) {
            pw.println("POWER CONSUMPTION CURRENT POLICY (dumpsys powerconsumption policy)");
            PowerConsumptionService.this.mPowerConsumptionServiceController.dump(pw);
        }
    }

    /* loaded from: classes.dex */
    private class LocalService extends PowerConsumptionServiceInternal {
        private LocalService() {
        }

        public void noteStartActivityForPowerConsumption(String record) {
            if (!PowerConsumptionService.this.mIsSupportPCS) {
                return;
            }
            if (PowerConsumptionService.sDEBUG) {
                Slog.d(PowerConsumptionService.TAG, "noteStartActivityForPowerConsumption " + record);
            }
            Message message = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.powerconsumpiton.PowerConsumptionService$LocalService$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((PowerConsumptionServiceController) obj).noteStartActivityForPowerConsumption((String) obj2);
                }
            }, PowerConsumptionService.this.mPowerConsumptionServiceController, record);
            PowerConsumptionService.this.mPCFgHandler.sendMessage(message);
        }

        public void noteFoucsChangeForPowerConsumption(String name, IMiuiWindowStateEx miuiWindowStateEx) {
            if (!PowerConsumptionService.this.mIsSupportPCS) {
                return;
            }
            if (PowerConsumptionService.sDEBUG) {
                Slog.d(PowerConsumptionService.TAG, "noteFoucsChangeForPowerConsumption " + name);
            }
            Message message = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.powerconsumpiton.PowerConsumptionService$LocalService$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((PowerConsumptionServiceController) obj).noteFoucsChangeForPowerConsumptionLocked((String) obj2, (IMiuiWindowStateEx) obj3);
                }
            }, PowerConsumptionService.this.mPowerConsumptionServiceController, name, miuiWindowStateEx);
            PowerConsumptionService.this.mPCFgHandler.sendMessage(message);
        }
    }
}

package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.util.Arrays;

/* loaded from: classes.dex */
public class MiuiBoosterUtilsStubImpl implements MiuiBoosterUtilsStub {
    private static final String CLOUD_MIUIBOOSTER_PRIO = "cloud_schedboost_enable";
    public static final int COLD_LAUNCH_ATT_RT_DURATION_MS = 2000;
    public static final int COLD_LAUNCH_REN_RT_DURATION_MS = 1000;
    public static final int HOT_LAUNCH_RT_DURATION_MS = 500;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 1;
    private static final int REQUEST_THREAD_PRIORITY = 2;
    public static final int SCROLL_RT_DURATION_MS = 2000;
    private static final String TAG = "MiuiBoosterUtils";
    public static final int THREAD_PRIORITY_NORMAL_MAX = 100;
    private static final String defaultPkg = "systemCall";
    private Context mContext = null;
    private Handler mH;
    private HandlerThread mHandlerThread;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.miuibooster.debug", false);
    private static boolean enable_miuibooster = SystemProperties.getBoolean("persist.sys.miuibooster.rtmode", false);
    private static boolean enable_miuibooster_launch = SystemProperties.getBoolean("persist.sys.miuibooster.launch.rtmode", true);
    private static IBinder sService = null;
    private static boolean isInit = false;
    private static boolean hasPermission = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBoosterUtilsStubImpl> {

        /* compiled from: MiuiBoosterUtilsStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBoosterUtilsStubImpl INSTANCE = new MiuiBoosterUtilsStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiBoosterUtilsStubImpl m525provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiBoosterUtilsStubImpl m524provideNewInstance() {
            return new MiuiBoosterUtilsStubImpl();
        }
    }

    public void requestLaunchThreadPriority(int uid, int[] req_tids, int timeout, int level) {
        if (!enable_miuibooster_launch) {
            Slog.w(TAG, "enable_miuibooster_launch : " + enable_miuibooster_launch);
            return;
        }
        if (!enable_miuibooster || !isInit) {
            Slog.w(TAG, "enable_miuibooster : " + enable_miuibooster + " ,isInit: " + isInit);
            return;
        }
        MiuiBoosterInfo info = new MiuiBoosterInfo(uid, req_tids, timeout, level);
        Message msg = this.mH.obtainMessage(2);
        msg.obj = info;
        this.mH.sendMessage(msg);
    }

    /* JADX WARN: Code restructure failed: missing block: B:24:0x0071, code lost:
    
        if (r3 != null) goto L32;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x008b, code lost:
    
        if (r0 != 0) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x008f, code lost:
    
        return com.android.server.am.MiuiBoosterUtilsStubImpl.hasPermission;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0090, code lost:
    
        com.android.server.am.MiuiBoosterUtilsStubImpl.hasPermission = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0093, code lost:
    
        return true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0081, code lost:
    
        r3.readException();
        r0 = r3.readInt();
        r3.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x007f, code lost:
    
        if (r3 != null) goto L32;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean checkPermission(java.lang.String r7, int r8) {
        /*
            r6 = this;
            r0 = -1
            boolean r1 = com.android.server.am.MiuiBoosterUtilsStubImpl.DEBUG
            java.lang.String r2 = "MiuiBoosterUtils"
            if (r1 == 0) goto L27
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "check_permission ,uid: "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r8)
            java.lang.String r3 = " ,package_name: "
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r7)
            java.lang.String r1 = r1.toString()
            android.util.Slog.d(r2, r1)
        L27:
            boolean r1 = com.android.server.am.MiuiBoosterUtilsStubImpl.hasPermission
            if (r1 == 0) goto L2c
            return r1
        L2c:
            android.os.Parcel r1 = android.os.Parcel.obtain()
            android.os.Parcel r3 = android.os.Parcel.obtain()
            android.os.IBinder r4 = com.android.server.am.MiuiBoosterUtilsStubImpl.sService     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            if (r4 != 0) goto L3e
            java.lang.String r4 = "miuiboosterservice"
            android.os.IBinder r4 = android.os.ServiceManager.getService(r4)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
        L3e:
            com.android.server.am.MiuiBoosterUtilsStubImpl.sService = r4     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            r5 = 0
            if (r4 != 0) goto L5b
            java.lang.String r4 = "miuibooster service is null"
            android.util.Slog.e(r2, r4)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            if (r1 == 0) goto L4e
            r1.recycle()
        L4e:
            if (r3 == 0) goto L5a
            r3.readException()
            int r0 = r3.readInt()
            r3.recycle()
        L5a:
            return r5
        L5b:
            java.lang.String r2 = "com.miui.performance.IMiuiBoosterManager"
            r1.writeInterfaceToken(r2)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            r1.writeString(r7)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            r1.writeInt(r8)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            android.os.IBinder r2 = com.android.server.am.MiuiBoosterUtilsStubImpl.sService     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            r4 = 2
            r2.transact(r4, r1, r3, r5)     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L76
            if (r1 == 0) goto L71
            r1.recycle()
        L71:
            if (r3 == 0) goto L8b
            goto L81
        L74:
            r2 = move-exception
            goto L94
        L76:
            r2 = move-exception
            r2.printStackTrace()     // Catch: java.lang.Throwable -> L74
            if (r1 == 0) goto L7f
            r1.recycle()
        L7f:
            if (r3 == 0) goto L8b
        L81:
            r3.readException()
            int r0 = r3.readInt()
            r3.recycle()
        L8b:
            if (r0 != 0) goto L90
            boolean r2 = com.android.server.am.MiuiBoosterUtilsStubImpl.hasPermission
            return r2
        L90:
            r2 = 1
            com.android.server.am.MiuiBoosterUtilsStubImpl.hasPermission = r2
            return r2
        L94:
            if (r1 == 0) goto L99
            r1.recycle()
        L99:
            if (r3 == 0) goto La5
            r3.readException()
            int r0 = r3.readInt()
            r3.recycle()
        La5:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.MiuiBoosterUtilsStubImpl.checkPermission(java.lang.String, int):boolean");
    }

    public int requestThreadPriority(int uid, int[] req_tids, int timeout, int level) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "thread_priority is reuqesting, uid:" + uid + " ,req_tids: " + Arrays.toString(req_tids) + " ,timeout:" + timeout);
        }
        if (!enable_miuibooster || !isInit) {
            Slog.w(TAG, "enable_miuibooster : " + enable_miuibooster + " ,isInit: " + isInit);
            return -1;
        }
        if (!checkPermission(defaultPkg, uid)) {
            Slog.w(TAG, "check_permission : false");
            return -1;
        }
        if (req_tids == null) {
            return -1;
        }
        for (int tid : req_tids) {
            if (tid <= 0) {
                return -1;
            }
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return -1;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.readException();
                    reply.readInt();
                    reply.recycle();
                }
                return -1;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            data.writeIntArray(req_tids);
            data.writeInt(timeout);
            data.writeInt(level);
            sService.transact(27, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return -1;
            }
            reply.readException();
            int ret = reply.readInt();
            reply.recycle();
            return ret;
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.readException();
                reply.readInt();
                reply.recycle();
            }
            throw th;
        }
    }

    public void cancelThreadPriority(int uid, int req_id) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "thread_priority is canceled, uid:" + uid + " ,req_id: " + req_id);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            data.writeInt(req_id);
            sService.transact(28, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public int requestBindCore(int uid, int[] req_tids, int timeout, int level) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "thread affinity is reuqesting, uid:" + uid + " ,req_tids: " + Arrays.toString(req_tids) + " ,timeout:" + timeout);
        }
        if (!checkPermission(defaultPkg, uid)) {
            Slog.w(TAG, "check_permission : false");
            return -1;
        }
        if (!enable_miuibooster || !isInit) {
            Slog.w(TAG, "enable_miuibooster : " + enable_miuibooster + " ,isInit: " + isInit);
            return -1;
        }
        if (req_tids == null) {
            return -1;
        }
        for (int tid : req_tids) {
            if (tid <= 0) {
                return -1;
            }
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return -1;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.readException();
                    reply.readInt();
                    reply.recycle();
                }
                return -1;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            data.writeIntArray(req_tids);
            data.writeInt(timeout);
            data.writeInt(level);
            sService.transact(21, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return -1;
            }
            reply.readException();
            int ret = reply.readInt();
            reply.recycle();
            return ret;
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.readException();
                reply.readInt();
                reply.recycle();
            }
            throw th;
        }
    }

    public void cancelBindCore(int uid, int req_id) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "thread affinity is canceled, uid:" + uid + " ,req_id: " + req_id);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            data.writeInt(req_id);
            sService.transact(22, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public void requestCpuHighFreq(int uid, int level, int timeout) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "CpuHighFreq is reuqesting, uid:" + uid + " ,level: " + level + " ,timeout:" + timeout);
        }
        if (!checkPermission(defaultPkg, uid)) {
            Slog.w(TAG, "check_permission : false");
            return;
        }
        if (!enable_miuibooster || !isInit) {
            Slog.w(TAG, "enable_miuibooster : " + enable_miuibooster + " ,isInit: " + isInit);
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            data.writeInt(level);
            data.writeInt(timeout);
            sService.transact(4, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public void cancelCpuHighFreq(int uid) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "CpuHighFreq is canceled, uid:" + uid);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeInt(uid);
            sService.transact(5, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public void init(Context context) {
        if (isInit) {
            return;
        }
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("MiuiBoosterUtilsTh");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.am.MiuiBoosterUtilsStubImpl.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        MiuiBoosterUtilsStubImpl miuiBoosterUtilsStubImpl = MiuiBoosterUtilsStubImpl.this;
                        miuiBoosterUtilsStubImpl.registerCloudObserver(miuiBoosterUtilsStubImpl.mContext);
                        MiuiBoosterUtilsStubImpl.this.updateCloudControlParas();
                        return;
                    case 2:
                        if (msg.obj != null) {
                            MiuiBoosterInfo info = (MiuiBoosterInfo) msg.obj;
                            MiuiBoosterUtilsStubImpl.this.requestThreadPriority(info.uid, info.req_tids, info.timeout, info.level);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mH = handler;
        Message msg = handler.obtainMessage(1);
        this.mH.sendMessage(msg);
        isInit = true;
        Slog.w(TAG, "init MiuiBoosterUtils");
    }

    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mH) { // from class: com.android.server.am.MiuiBoosterUtilsStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MiuiBoosterUtilsStubImpl.CLOUD_MIUIBOOSTER_PRIO))) {
                    MiuiBoosterUtilsStubImpl.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MIUIBOOSTER_PRIO), false, observer, -2);
        Slog.w(TAG, "registerCloudObserver: cloud_schedboost_enable");
    }

    public void updateCloudControlParas() {
        String cloudMiuiboosterPara = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MIUIBOOSTER_PRIO, -2);
        if (cloudMiuiboosterPara != null) {
            enable_miuibooster = Boolean.parseBoolean(cloudMiuiboosterPara);
        }
        Slog.w(TAG, "updateCloudControlParas cloudMiuiboosterPara: " + cloudMiuiboosterPara + " ,enable_miuibooster: " + enable_miuibooster);
    }

    public void notifyForegroundAppChanged(String packageName) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "notifyForegroundAppChanged: packageName=" + packageName);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeString(packageName);
            sService.transact(52, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public void notifyPowerModeChanged(boolean isPowerMode) {
        IBinder iBinder;
        if (DEBUG) {
            Slog.d(TAG, "notifyPowerModeStatus: isPowerMode=" + isPowerMode);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                iBinder = sService;
                if (iBinder == null) {
                    iBinder = ServiceManager.getService("miuiboosterservice");
                }
                sService = iBinder;
            } catch (Exception e) {
                e.printStackTrace();
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            if (iBinder == null) {
                Slog.e(TAG, "miuibooster service is null");
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                    return;
                }
                return;
            }
            data.writeInterfaceToken("com.miui.performance.IMiuiBoosterManager");
            data.writeBoolean(isPowerMode);
            sService.transact(57, data, reply, 0);
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    private class MiuiBoosterInfo {
        private int level;
        private int[] req_tids;
        private int timeout;
        private int uid;

        public MiuiBoosterInfo(int uid, int[] req_tids, int timeout, int level) {
            this.uid = uid;
            this.req_tids = req_tids;
            this.timeout = timeout;
            this.level = level;
        }
    }
}

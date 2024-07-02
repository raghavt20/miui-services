package com.miui.server.stepcounter;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.SystemService;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import miui.stepcounter.StepCell;
import miui.stepcounter.StepCounterManagerInternal;
import miui.stepcounter.StepDetector;
import miui.stepcounter.StepMode;
import miui.stepcounter.StepProvider;
import miui.stepcounter.StepSqlite;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class StepCounterManagerService extends SystemService {
    private static final String ACTION_SLEEP_CHANGED = "com.miui.powerkeeper_sleep_changed";
    private static final int DEFAULT_DELAY = 300000;
    private static final int DEFAULT_RECORD_DELAY = 30000;
    private static final int DUMP_NUMBER = 1000;
    private static final String EXTRA_STATE = "state";
    private static final int FIRST_DELAY = 10000;
    private static final int MSG_ADD_STEP = 1;
    private static final int MSG_TRIM = 2;
    private static final int MSG_TRIM_ALL = 4;
    private static final int MSG_TRIM_ALL_FORCE = 8;
    private static final String PROP_STEPS_SUPPORT = "persist.sys.steps_provider";
    private static final String SENSOR_NAME = "oem_treadmill  Wakeup";
    private static final int STATE_ENTER_SLEEP = 1;
    private static final int STATE_EXIT_SLEEP = 2;
    private static final String TAG = "StepCounterManagerService";
    private static final int TYPE_TREADMILL = 33171041;
    private boolean isRegisterDetectorListener;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks;
    private Context mContext;
    private int mDelay;
    private StepCountHandler mHandler;
    private boolean mHaveStepSensor;
    private IntentReceiver mIntentReceiver;
    private long mLastTrimAllTimeInMills;
    private final Object mLocked;
    private int mRecordDelay;
    private ContentResolver mResolver;
    private Runnable mRunnable;
    private Sensor mSensor;
    private SensorManager mSensorManager;
    private SensorEventListener mStepDetectorListener;
    private List<StepDetector> mStepList;
    private boolean mTreadmillEnabled;
    private SensorEventListener mTreadmillListener;
    private Sensor mTreadmillSensor;
    private int sCurrentPos;
    private static boolean sDEBUG = false;
    private static long SYSTEM_BOOT_TIME = System.currentTimeMillis() - SystemClock.elapsedRealtime();

    /* JADX WARN: Multi-variable type inference failed */
    public StepCounterManagerService(Context context) {
        super(context);
        this.mLocked = new Object();
        this.mHaveStepSensor = false;
        this.isRegisterDetectorListener = false;
        this.mIntentReceiver = null;
        this.mClientDeathCallbacks = new HashMap<>();
        this.mRunnable = new Runnable() { // from class: com.miui.server.stepcounter.StepCounterManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                StepCounterManagerService.this.mHandler.postDelayed(this, StepCounterManagerService.this.mDelay);
                StepCounterManagerService.this.mHandler.sendEmptyMessage(2);
                if (StepCounterManagerService.sDEBUG) {
                    Slog.i(StepCounterManagerService.TAG, "sendEmptyMessage(MSG_TRIM): trigger trim！");
                }
                if (FeatureParser.getBoolean("is_pad", false)) {
                    StepCounterManagerService.this.reportStepToOneTrack();
                }
            }
        };
        this.mContext = context;
        this.mHandler = new StepCountHandler(BackgroundThread.get().getLooper());
        this.mResolver = this.mContext.getContentResolver();
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mSensor = sensorManager.getDefaultSensor(18, true);
        this.mTreadmillSensor = this.mSensorManager.getDefaultSensor(TYPE_TREADMILL, true);
        this.mStepDetectorListener = new StepDetectorListener();
        this.mTreadmillListener = new TreadmillListener();
        this.mIntentReceiver = new IntentReceiver();
        this.mStepList = new ArrayList();
        this.mDelay = DEFAULT_DELAY;
        this.mRecordDelay = DEFAULT_RECORD_DELAY;
        this.sCurrentPos = 0;
        this.mLastTrimAllTimeInMills = 0L;
        Slog.i(TAG, "Create StepCounterManagerService success " + UserHandle.myUserId());
    }

    public void onStart() {
        boolean z = FeatureParser.getBoolean("support_steps_provider", false);
        this.mHaveStepSensor = z;
        if (z) {
            this.mHaveStepSensor = SystemProperties.getBoolean(PROP_STEPS_SUPPORT, true);
        }
        if (!this.mHaveStepSensor) {
            Slog.d(TAG, "StepDetector Sensor not support");
            return;
        }
        publishLocalService(StepCounterManagerInternal.class, new LocalService());
        publishBinderService(BinderService.SERVICE_NAME, new BinderService());
        this.isRegisterDetectorListener = registerDetectorListener();
        registerReceiver();
        this.mHandler.postDelayed(this.mRunnable, 10000L);
        Slog.i(TAG, "step_debug onStart success isRegisterDetectorListener=" + this.isRegisterDetectorListener);
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter("android.intent.action.TIME_SET");
        filter.addAction("com.miui.powerkeeper_sleep_changed");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class StepCountHandler extends Handler {
        public StepCountHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                case 8:
                    StepCounterManagerService.this.trimAll(true);
                    return;
                case 4:
                    StepCounterManagerService.this.trimAll(false);
                    return;
                default:
                    return;
            }
        }
    }

    public void trimAll(boolean force) {
        boolean pass = System.currentTimeMillis() - this.mLastTrimAllTimeInMills >= ((long) this.mRecordDelay);
        if (pass) {
            this.mLastTrimAllTimeInMills = System.currentTimeMillis();
        }
        if ((force || pass) && UserManager.get(this.mContext).isUserUnlocked()) {
            while (!isEmpty()) {
                trim();
            }
            this.mStepList.clear();
            this.sCurrentPos = 0;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:23:0x0076, code lost:
    
        if (com.miui.server.stepcounter.StepCounterManagerService.sDEBUG == false) goto L37;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0078, code lost:
    
        android.util.Slog.i(com.miui.server.stepcounter.StepCounterManagerService.TAG, "step current position start at " + r0 + " end at " + r9.sCurrentPos);
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x009b, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:?, code lost:
    
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void trim() {
        /*
            r9 = this;
            int r0 = r9.sCurrentPos
            java.util.List<miui.stepcounter.StepDetector> r1 = r9.mStepList
            int r2 = r9.sCurrentPos
            java.lang.Object r1 = r1.get(r2)
            miui.stepcounter.StepDetector r1 = (miui.stepcounter.StepDetector) r1
        Lc:
            java.util.List<miui.stepcounter.StepDetector> r2 = r9.mStepList
            boolean r2 = r2.isEmpty()
            java.lang.String r3 = "StepCounterManagerService"
            if (r2 != 0) goto L74
            java.util.List<miui.stepcounter.StepDetector> r2 = r9.mStepList
            int r4 = r9.sCurrentPos
            java.lang.Object r2 = r2.get(r4)
            miui.stepcounter.StepDetector r2 = (miui.stepcounter.StepDetector) r2
            if (r2 == 0) goto L73
            if (r1 == 0) goto L73
            long r4 = r2.getTimestamp()
            long r6 = r1.getTimestamp()
            long r4 = r4 - r6
            int r6 = r9.mRecordDelay
            long r7 = (long) r6
            int r7 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r7 <= 0) goto L3c
            int r7 = r9.sCurrentPos
            if (r7 <= 0) goto L3c
            int r7 = r7 + (-1)
            r9.sCurrentPos = r7
        L3c:
            long r6 = (long) r6
            int r6 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r6 >= 0) goto L50
            boolean r6 = r9.isEmpty()
            if (r6 == 0) goto L48
            goto L50
        L48:
            int r3 = r9.sCurrentPos
            int r3 = r3 + 1
            r9.sCurrentPos = r3
            goto Lc
        L50:
            miui.stepcounter.StepMode r6 = new miui.stepcounter.StepMode     // Catch: java.lang.IllegalArgumentException -> L6c
            java.util.List<miui.stepcounter.StepDetector> r7 = r9.mStepList     // Catch: java.lang.IllegalArgumentException -> L6c
            int r8 = r9.sCurrentPos     // Catch: java.lang.IllegalArgumentException -> L6c
            r6.<init>(r7, r0, r8)     // Catch: java.lang.IllegalArgumentException -> L6c
            android.content.ContentResolver r7 = r9.mResolver     // Catch: java.lang.IllegalArgumentException -> L6c
            r6.run(r7)     // Catch: java.lang.IllegalArgumentException -> L6c
            boolean r6 = r9.isEmpty()
            if (r6 != 0) goto L74
            int r6 = r9.sCurrentPos
            int r6 = r6 + 1
            r9.sCurrentPos = r6
            goto L74
        L6c:
            r6 = move-exception
            java.lang.String r7 = "Illegal argument"
            android.util.Slog.i(r3, r7)
            return
        L73:
            return
        L74:
            boolean r2 = com.miui.server.stepcounter.StepCounterManagerService.sDEBUG
            if (r2 == 0) goto L9b
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r4 = "step current position start at "
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.StringBuilder r2 = r2.append(r0)
            java.lang.String r4 = " end at "
            java.lang.StringBuilder r2 = r2.append(r4)
            int r4 = r9.sCurrentPos
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.String r2 = r2.toString()
            android.util.Slog.i(r3, r2)
        L9b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.stepcounter.StepCounterManagerService.trim():void");
    }

    private boolean isEmpty() {
        return this.mStepList.isEmpty() || this.sCurrentPos == this.mStepList.size() - 1;
    }

    /* loaded from: classes.dex */
    public class LocalService extends StepCounterManagerInternal {
        public LocalService() {
        }

        public boolean haveStepSensor() {
            return StepCounterManagerService.this.haveStepSensorInternal();
        }

        public void getLatestData(boolean isForce) {
            StepCounterManagerService.this.getLatestDataInternal(isForce);
        }
    }

    /* loaded from: classes.dex */
    private class StepDetectorListener implements SensorEventListener {
        private StepDetectorListener() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            int sensorMode;
            int counter = (int) event.values[0];
            if (event.values.length > 1) {
                sensorMode = (int) event.values[1];
            } else {
                sensorMode = 1;
            }
            long timestamp = StepCounterManagerService.SYSTEM_BOOT_TIME + (event.timestamp / 1000000);
            StepCounterManagerService.this.mStepList.add(new StepDetector(counter, timestamp, sensorMode));
            if (StepCounterManagerService.sDEBUG) {
                Slog.i(StepCounterManagerService.TAG, "onSensorChanged: count: " + counter + "\tsensorMode: " + sensorMode + "\ttimestamp: " + timestamp);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /* loaded from: classes.dex */
    private class TreadmillListener implements SensorEventListener {
        private TreadmillListener() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            int counter = (int) event.values[0];
            long timestamp = StepCounterManagerService.SYSTEM_BOOT_TIME + (event.timestamp / 1000000);
            StepCounterManagerService.this.mStepList.add(new StepDetector(counter, timestamp, 3));
            if (StepCounterManagerService.sDEBUG) {
                Slog.i(StepCounterManagerService.TAG, "onTreadmillSensorChanged: count: " + counter + "\tsensorMode: 3\ttimestamp: " + timestamp);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public static void resetSystemBootTime() {
        SYSTEM_BOOT_TIME = System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean registerDetectorListener() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            return sensorManager.registerListener(this.mStepDetectorListener, this.mSensor, 3, this.mDelay * 1000, this.mHandler);
        }
        Slog.e(TAG, "StepDetector Sensor not available!");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterDetectorListener() {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this.mStepDetectorListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class IntentReceiver extends BroadcastReceiver {
        private IntentReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : null;
            if ("android.intent.action.TIME_SET".equals(action)) {
                StepCounterManagerService.resetSystemBootTime();
            } else if ("com.miui.powerkeeper_sleep_changed".equals(action)) {
                int state = intent.getIntExtra("state", -1);
                dealSleepModeChanged(state);
            }
        }

        private void dealSleepModeChanged(int state) {
            if (state == 2 && !StepCounterManagerService.this.isRegisterDetectorListener) {
                StepCounterManagerService stepCounterManagerService = StepCounterManagerService.this;
                stepCounterManagerService.isRegisterDetectorListener = stepCounterManagerService.registerDetectorListener();
            } else if (state == 1 && StepCounterManagerService.this.isRegisterDetectorListener) {
                StepCounterManagerService.this.unregisterDetectorListener();
                StepCounterManagerService.this.isRegisterDetectorListener = false;
            }
        }
    }

    public boolean haveStepSensorInternal() {
        return this.mHaveStepSensor;
    }

    public void getLatestDataInternal(boolean isForce) {
        if (this.mHaveStepSensor) {
            if (sDEBUG) {
                Slog.i(TAG, "getLatestDataInternal: isForce? " + isForce);
            }
            if (isForce) {
                this.mHandler.sendEmptyMessage(8);
            } else {
                this.mHandler.sendEmptyMessage(4);
            }
        }
    }

    /* loaded from: classes.dex */
    private class BinderService extends Binder {
        private static final int GET_LATEST_DATA = 2;
        private static final int HAVE_STEP_SENSOR = 1;
        private static final int RECEIVE_TREADMILL = 0;
        public static final String SERVICE_NAME = "miui_step_counter_service";

        private BinderService() {
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(StepCounterManagerService.this.mContext, StepCounterManagerService.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    StepCounterManagerService.this.dumpInternal(pw);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        @Override // android.os.Binder
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 0:
                    data.enforceInterface(SERVICE_NAME);
                    boolean enabled = data.readBoolean();
                    IBinder mBinder = data.readStrongBinder();
                    return registerTreadmillSensor(enabled, mBinder);
                case 1:
                    data.enforceInterface(SERVICE_NAME);
                    return haveStepSensor(reply);
                case 2:
                    data.enforceInterface(SERVICE_NAME);
                    boolean isForce = data.readBoolean();
                    return getLatestData(isForce);
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        public boolean registerTreadmillSensor(boolean enabled, IBinder token) {
            long ident = Binder.clearCallingIdentity();
            try {
                return registerTreadmillSensorInternal(enabled, token);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean registerTreadmillSensorInternal(boolean enabled, IBinder token) {
            synchronized (StepCounterManagerService.this.mLocked) {
                if (enabled) {
                    registerDeathCallbackLocked(token);
                    if (!StepCounterManagerService.this.mTreadmillEnabled && StepCounterManagerService.this.mClientDeathCallbacks.size() == 1 && StepCounterManagerService.this.mTreadmillSensor != null && StepCounterManagerService.this.mTreadmillSensor.getName().equals(StepCounterManagerService.SENSOR_NAME)) {
                        StepCounterManagerService.this.mTreadmillEnabled = true;
                        StepCounterManagerService.this.mSensorManager.registerListener(StepCounterManagerService.this.mTreadmillListener, StepCounterManagerService.this.mTreadmillSensor, 3, 60000000, StepCounterManagerService.this.mHandler);
                        Slog.d(StepCounterManagerService.TAG, "TreadmillSensor register success!");
                    }
                } else {
                    unregisterDeathCallbackLocked(token);
                    if (StepCounterManagerService.this.mTreadmillEnabled && StepCounterManagerService.this.mClientDeathCallbacks.size() == 0) {
                        StepCounterManagerService.this.mTreadmillEnabled = false;
                        StepCounterManagerService.this.mSensorManager.unregisterListener(StepCounterManagerService.this.mTreadmillListener);
                        Slog.d(StepCounterManagerService.TAG, "TreadmillSensor unregister success!");
                    }
                }
            }
            return true;
        }

        public boolean haveStepSensor(Parcel reply) {
            long ident = Binder.clearCallingIdentity();
            try {
                return haveStepSensorInternal(reply);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean haveStepSensorInternal(Parcel reply) {
            synchronized (StepCounterManagerService.this.mLocked) {
                reply.writeInt(StepCounterManagerService.this.mHaveStepSensor ? 1 : 0);
            }
            return true;
        }

        public boolean getLatestData(boolean isForce) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (StepCounterManagerService.sDEBUG) {
                    Slog.i(StepCounterManagerService.TAG, "getLatestDataInternal: isForce? " + isForce);
                }
                return getLatestDataInternal(isForce);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean getLatestDataInternal(boolean isForce) {
            synchronized (StepCounterManagerService.this.mLocked) {
                if (StepCounterManagerService.this.mHaveStepSensor) {
                    if (isForce) {
                        StepCounterManagerService.this.mHandler.sendEmptyMessage(8);
                    } else {
                        StepCounterManagerService.this.mHandler.sendEmptyMessage(4);
                    }
                }
            }
            return true;
        }

        protected void registerDeathCallbackLocked(IBinder token) {
            if (StepCounterManagerService.this.mClientDeathCallbacks.containsKey(token)) {
                return;
            }
            StepCounterManagerService.this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token));
        }

        protected void unregisterDeathCallbackLocked(IBinder token) {
            if (token != null && StepCounterManagerService.this.mClientDeathCallbacks.containsKey(token)) {
                token.unlinkToDeath((IBinder.DeathRecipient) StepCounterManagerService.this.mClientDeathCallbacks.get(token), 0);
                StepCounterManagerService.this.mClientDeathCallbacks.remove(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private IBinder mToken;

        public ClientDeathCallback(IBinder token) {
            this.mToken = token;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (StepCounterManagerService.this.mLocked) {
                StepCounterManagerService.this.mClientDeathCallbacks.remove(this.mToken);
                if (StepCounterManagerService.this.mClientDeathCallbacks.size() == 0 && StepCounterManagerService.this.mTreadmillSensor != null && StepCounterManagerService.this.mTreadmillSensor.getName().equals(StepCounterManagerService.SENSOR_NAME)) {
                    StepCounterManagerService.this.mTreadmillEnabled = false;
                    StepCounterManagerService.this.mSensorManager.unregisterListener(StepCounterManagerService.this.mTreadmillListener);
                    Slog.d(StepCounterManagerService.TAG, "binderDied:unregisterListener Treadmill Listener");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("Step Counter Manager Service(dumpsys miui_step_counter_service)\n");
        synchronized (this.mLocked) {
            pw.println("Delay: " + (this.mDelay / 1000) + "s, default: " + MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY + "s");
            pw.println("Record Delay: " + (this.mRecordDelay / 1000) + "s, default: 30s");
            pw.println();
            pw.println("current step list cache size: " + this.mStepList.size());
            for (int i = 0; i < this.mStepList.size(); i++) {
                pw.print((i + 1) + " ");
                this.mStepList.get(i).dump(pw);
            }
            pw.println();
            List<StepCell> mStepCellList = new ArrayList<>();
            Cursor cursor = this.mResolver.query(StepSqlite.CONTENT_URI, StepSqlite.DEFAULT_PROJECTION, null, null, "_id DESC");
            long mDumpRunStep = 0;
            long mDumpWalkStep = 0;
            long mDumpTreadmillStep = 0;
            long mTodayBeginTime = getTodayBeginTime();
            int i2 = 1;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (cursor.getLong(2) > mTodayBeginTime) {
                        mStepCellList.add(new StepCell(cursor.getInt(0), cursor.getLong(i2), cursor.getLong(2), cursor.getInt(3), cursor.getInt(4)));
                        if (cursor.getInt(3) == 2) {
                            mDumpWalkStep += cursor.getInt(4);
                            i2 = 1;
                        } else if (cursor.getInt(3) == 3) {
                            mDumpRunStep += cursor.getInt(4);
                            i2 = 1;
                        } else if (cursor.getInt(3) == 4) {
                            mDumpTreadmillStep += cursor.getInt(4);
                            i2 = 1;
                        }
                    }
                    i2 = 1;
                }
                cursor.close();
            }
            pw.println("step counter database info");
            pw.println("Today Total steps: " + (mDumpWalkStep + mDumpRunStep + mDumpTreadmillStep) + ", walk: " + mDumpWalkStep + ", run: " + mDumpRunStep + ", treadmill: " + mDumpTreadmillStep);
            pw.println("Today step history info:");
            for (int i3 = 0; i3 < mStepCellList.size(); i3++) {
                pw.print((i3 + 1) + " ");
                mStepCellList.get((mStepCellList.size() - i3) - 1).dump(pw);
            }
            mStepCellList.clear();
        }
    }

    /* loaded from: classes.dex */
    private class Shell extends ShellCommand {
        private Shell() {
        }

        public int onCommand(String cmd) {
            return onShellCommand(cmd);
        }

        public void onHelp() {
            dumpHelp();
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private int onShellCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            switch (cmd.hashCode()) {
                case -513656744:
                    if (cmd.equals("set-delay")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1499:
                    if (cmd.equals("-h")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 968364178:
                    if (cmd.equals("set-record-delay")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1040450170:
                    if (cmd.equals("logging-disable")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1333069025:
                    if (cmd.equals("--help")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 1506902390:
                    if (cmd.equals("trim-all")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1671308008:
                    if (cmd.equals("disable")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case 1728842673:
                    if (cmd.equals("logging-enable")) {
                        c = 0;
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
                    StepCounterManagerService.this.setDebugEnabled(true);
                    pw.println("Set Step Counter Log Enable");
                    return 0;
                case 1:
                    StepCounterManagerService.this.setDebugEnabled(false);
                    pw.println("Set Step Counter Log Disable");
                    return 0;
                case 2:
                    setDelay();
                    return 0;
                case 3:
                    setRecordDelay();
                    return 0;
                case 4:
                    setTrimAll();
                    return 0;
                case 5:
                    StepCounterManagerService.this.unregisterDetectorListener();
                    StepCounterManagerService.this.isRegisterDetectorListener = false;
                    pw.println("Disable Step Counter");
                    return 0;
                default:
                    onHelp();
                    return 0;
            }
        }

        private void setDelay() {
            PrintWriter pw = getOutPrintWriter();
            int delay = -1;
            try {
                delay = Integer.parseInt(getNextArgRequired());
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
            }
            if (delay <= 0) {
                pw.println("set delay fail! current delay is " + delay + "s");
            } else {
                StepCounterManagerService.this.setDelayValue(delay);
                pw.println("set delay success! current delay is " + delay + "s");
            }
        }

        private void setRecordDelay() {
            PrintWriter pw = getOutPrintWriter();
            int recordDelay = -1;
            try {
                recordDelay = Integer.parseInt(getNextArgRequired());
            } catch (RuntimeException ex) {
                pw.println("Error: " + ex.toString());
            }
            if (recordDelay <= 0) {
                pw.println("set record-delay fail! current record-delay is " + recordDelay + "s");
            } else {
                StepCounterManagerService.this.setRecordDelayValue(recordDelay);
                pw.println("set record-delay success! current record-delay is " + recordDelay + "s");
            }
        }

        private void setTrimAll() {
            PrintWriter pw = getOutPrintWriter();
            if (StepCounterManagerService.this.mHaveStepSensor) {
                StepCounterManagerService.this.mHandler.sendEmptyMessage(8);
                pw.println("trimAll success!");
            } else {
                pw.println("trimAll fail!");
            }
        }

        private void dumpHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Miui Step Counter manager commands:");
            pw.println("  --help|-h");
            pw.println("    Print this help text.");
            pw.println();
            pw.println("  logging-enable");
            pw.println("    Enable logging.");
            pw.println("  logging-disable");
            pw.println("    Disable logging.");
            pw.println("  trim-all");
            pw.println("    store cache steps to database");
            pw.println("  set-delay DELAY");
            pw.println("  disable");
            pw.println("    set system auto delay value(s).");
            pw.println("  set-record-delay DELAY");
            pw.println("    set system record delay value(s).");
            pw.println("    note: this will affect system the minimum interval for");
            pw.println("          recording the number of steps.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDelayValue(int delay) {
        this.mDelay = delay * 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setRecordDelayValue(int recordDelay) {
        this.mRecordDelay = recordDelay * 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDebugEnabled(boolean enabled) {
        synchronized (this.mLocked) {
            long ident = Binder.clearCallingIdentity();
            try {
                sDEBUG = enabled;
                StepProvider.updateDebug(enabled);
                StepMode.updateDebug(enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private long getTodayBeginTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    private long getTodayEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, 23);
        calendar.set(12, 59);
        calendar.set(13, 59);
        calendar.set(14, 999);
        return calendar.getTimeInMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportStepToOneTrack() {
        final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
        if (getTodayEndTime() - System.currentTimeMillis() <= this.mDelay && getTodayEndTime() - System.currentTimeMillis() > 0) {
            long mDumpRunStep = 0;
            long mDumpWalkStep = 0;
            long mDumpTreadmillStep = 0;
            Cursor cursor = this.mResolver.query(StepSqlite.CONTENT_URI, StepSqlite.DEFAULT_PROJECTION, null, null, "_id DESC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (cursor.getLong(2) > getTodayBeginTime()) {
                        if (cursor.getInt(3) == 2) {
                            mDumpWalkStep += cursor.getInt(4);
                        } else if (cursor.getInt(3) == 3) {
                            mDumpRunStep += cursor.getInt(4);
                        } else if (cursor.getInt(3) == 4) {
                            mDumpTreadmillStep += cursor.getInt(4);
                        }
                    }
                }
                cursor.close();
            }
            final long mStep = mDumpWalkStep + mDumpRunStep + mDumpTreadmillStep;
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.miui.server.stepcounter.StepCounterManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    StepCounterManagerService.this.lambda$reportStepToOneTrack$0(mStep, DEVICE_REGION);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$reportStepToOneTrack$0(long mStep, String DEVICE_REGION) {
        try {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, "31000000621");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "StepCount");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, this.mContext.getPackageName());
            Bundle params = new Bundle();
            params.putLong("step_count", mStep);
            if (DEVICE_REGION.equals("CN")) {
                intent.setFlags(2);
            }
            intent.putExtras(params);
            this.mContext.startService(intent);
        } catch (Exception e) {
            Slog.e(TAG, "error reportStepToOneTrack:" + e.toString());
        }
        Slog.d(TAG, "reportStepToOneTrack step=" + mStep);
    }
}

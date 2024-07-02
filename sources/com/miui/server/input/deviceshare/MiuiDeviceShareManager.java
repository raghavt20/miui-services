package com.miui.server.input.deviceshare;

import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;
import com.android.server.input.MiuiInputThread;
import com.miui.server.input.deviceshare.MiuiDeviceShareManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import miui.hardware.input.IDeviceShareStateChangedListener;
import miui.hardware.input.MiuiInputManager;

/* loaded from: classes.dex */
public class MiuiDeviceShareManager {
    private static final String TAG = "MiuiDeviceShareManager";
    private final Handler mHandler;
    private final Object mLock;
    private final MiuiInputManagerInternal mMiuiInputManagerInternal;
    private final SparseArray<DeviceShareStateChangedListenerRecord> mRecords;

    private MiuiDeviceShareManager() {
        this.mRecords = new SparseArray<>();
        this.mHandler = new Handler(MiuiInputThread.getHandler().getLooper());
        this.mLock = new Object();
        this.mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);
    }

    public void setDeviceShareListener(int pid, ParcelFileDescriptor fd, int flags, IDeviceShareStateChangedListener listener) {
        Slog.w(TAG, "pid " + pid + " request set device share listener, flag = " + MiuiInputManager.deviceShareFlagToString(flags));
        if (flags == 0) {
            synchronized (this.mLock) {
                removeRecordLocked(this.mRecords.get(pid));
            }
            return;
        }
        if (fd == null || listener == null) {
            Slog.e(TAG, "pid " + pid + " setDeviceShareListener fail, because fd or listener is null");
            return;
        }
        synchronized (this.mLock) {
            DeviceShareStateChangedListenerRecord record = this.mRecords.get(pid);
            if (record == null) {
                DeviceShareStateChangedListenerRecord newRecord = new DeviceShareStateChangedListenerRecord(pid, listener, fd, flags);
                try {
                    IBinder binder = listener.asBinder();
                    binder.linkToDeath(newRecord, 0);
                    this.mRecords.put(pid, newRecord);
                    this.mMiuiInputManagerInternal.setDeviceShareListener(pid, fd.getFileDescriptor(), flags);
                    return;
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            ParcelFileDescriptor oldFd = record.mParcelFileDescriptor;
            IBinder oldBinder = record.mListener.asBinder();
            record.mParcelFileDescriptor = fd;
            record.mListener = listener;
            record.mFlags = flags;
            try {
                IBinder binder2 = listener.asBinder();
                binder2.linkToDeath(record, 0);
                this.mMiuiInputManagerInternal.setDeviceShareListener(pid, fd.getFileDescriptor(), flags);
                closeSocket(oldFd);
                oldBinder.unlinkToDeath(record, 0);
                return;
            } catch (RemoteException e2) {
                onListenerBinderDied(pid);
                throw new RuntimeException(e2);
            }
        }
    }

    public void onSocketBroken(int pid) {
        Slog.d(TAG, pid + " socket broken");
        synchronized (this.mLock) {
            final DeviceShareStateChangedListenerRecord record = this.mRecords.get(pid);
            if (record == null) {
                return;
            }
            removeRecordLocked(record);
            Handler handler = this.mHandler;
            Objects.requireNonNull(record);
            handler.post(new Runnable() { // from class: com.miui.server.input.deviceshare.MiuiDeviceShareManager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiDeviceShareManager.DeviceShareStateChangedListenerRecord.this.notifySocketBroken();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onListenerBinderDied(int pid) {
        Slog.w(TAG, pid + " binder died");
        synchronized (this.mLock) {
            removeRecordLocked(this.mRecords.get(pid));
        }
    }

    private void removeRecordLocked(DeviceShareStateChangedListenerRecord record) {
        if (record == null) {
            return;
        }
        IBinder binder = record.mListener.asBinder();
        binder.unlinkToDeath(record, 0);
        int pid = record.mPid;
        closeSocket(record.mParcelFileDescriptor);
        this.mRecords.remove(pid);
        this.mMiuiInputManagerInternal.setDeviceShareListener(pid, null, 0);
    }

    private void closeSocket(ParcelFileDescriptor parcelFileDescriptor) {
        if (parcelFileDescriptor == null) {
            return;
        }
        try {
            parcelFileDescriptor.close();
            Slog.w(TAG, "closeSocket " + parcelFileDescriptor);
        } catch (IOException e) {
            Slog.e(TAG, e.getMessage(), e);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("MiuiDeviceShareManager(Java) State:");
        pw.print("  mRecords: ");
        if (this.mRecords.size() == 0) {
            pw.println("<EMPTY>");
        } else {
            pw.println();
            pw.println("    " + this.mRecords);
        }
    }

    public void setTouchpadButtonState(int pid, int deviceId, boolean isDown) {
        Slog.w(TAG, "process " + pid + " request device" + deviceId + "button state, isDown = " + isDown);
        this.mMiuiInputManagerInternal.setTouchpadButtonState(deviceId, isDown);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DeviceShareStateChangedListenerRecord implements IBinder.DeathRecipient {
        private int mFlags;
        private IDeviceShareStateChangedListener mListener;
        private ParcelFileDescriptor mParcelFileDescriptor;
        private final int mPid;

        public DeviceShareStateChangedListenerRecord(int mPid, IDeviceShareStateChangedListener mListener, ParcelFileDescriptor parcelFileDescriptor, int flags) {
            this.mPid = mPid;
            this.mListener = mListener;
            this.mParcelFileDescriptor = parcelFileDescriptor;
            this.mFlags = flags;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiDeviceShareManager.this.onListenerBinderDied(this.mPid);
        }

        public void notifySocketBroken() {
            try {
                this.mListener.onSocketBroken();
            } catch (RemoteException e) {
                binderDied();
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class MiuiDeviceShareManagerHolder {
        private static final MiuiDeviceShareManager sInstance = new MiuiDeviceShareManager();

        private MiuiDeviceShareManagerHolder() {
        }
    }

    public static MiuiDeviceShareManager getInstance() {
        return MiuiDeviceShareManagerHolder.sInstance;
    }
}

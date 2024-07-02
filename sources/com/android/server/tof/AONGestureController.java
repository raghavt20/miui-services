package com.android.server.tof;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.tof.AONGestureController;
import com.xiaomi.aon.IMiAON;
import com.xiaomi.aon.IMiAONListener;
import java.util.concurrent.CountDownLatch;
import miui.aon.IContactlessGestureService;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AONGestureController extends ContactlessGestureController {
    private static final int ALWAYS_CHECK_GESTURE_TIMEOUT = 600000;
    public static final String AON_SERVICE_CLASS = "com.xiaomi.aon.AONService";
    public static final String AON_SERVICE_PACKAGE = "com.xiaomi.aon";
    private static final String CLIENT_ACTION = "miui.intent.action.GESTURE_SERVICE";
    private static final int FPS_CHECK_GESTURE = 6;
    private static final String TAG = "AONGestureController";
    private static final int TYPE_HAND_ROIACTION = 258;
    private ComponentName mAonComponentName;
    private IMiAON mAonService;
    private final ServiceConnection mConnection;
    private final IMiAONListener mGestureListener;
    private int mLastDisplayRotation;
    private int mLastSupportGesture;
    private boolean mListeningState;
    public IContactlessGestureService mService;

    public AONGestureController(Context context, Handler handler, ContactlessGestureService contactlessGestureService) {
        super(context, handler, contactlessGestureService);
        this.mLastSupportGesture = -1;
        this.mLastDisplayRotation = -1;
        this.mGestureListener = new IMiAONListener.Stub() { // from class: com.android.server.tof.AONGestureController.1
            public void onCallbackListener(int type, int[] data) {
                if (data == null || data.length < 2 || data[1] <= 0) {
                    if (ContactlessGestureService.mDebug) {
                        Slog.w(AONGestureController.TAG, "The aon data is valid!");
                        return;
                    }
                    return;
                }
                AONGestureController.this.handleGestureEvent(data[1]);
            }

            public void onImageAvailiable(int frame) {
            }
        };
        this.mConnection = new AnonymousClass2();
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public ComponentName getTofClientComponent() {
        String value = this.mContext.getResources().getString(286195873);
        if (value != null) {
            return ComponentName.unflattenFromString(value);
        }
        return null;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public String getAction() {
        return CLIENT_ACTION;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void showGestureNotification() {
        IContactlessGestureService iContactlessGestureService = this.mService;
        if (iContactlessGestureService != null) {
            try {
                iContactlessGestureService.showContactlessGestureNotification();
                Slog.i(TAG, "show aon gesture notification, if needed");
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void registerGestureListenerIfNeeded(final boolean register) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.AONGestureController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AONGestureController.this.lambda$registerGestureListenerIfNeeded$0(register);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$registerGestureListenerIfNeeded$0(boolean register) {
        if (register) {
            registerListener(TYPE_HAND_ROIACTION, 6, ALWAYS_CHECK_GESTURE_TIMEOUT);
        } else {
            unRegisterListener();
        }
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void notifyRotationChanged(int rotation) {
    }

    private void notifyAonEventChangeIfNeeded() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.AONGestureController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AONGestureController.this.lambda$notifyAonEventChangeIfNeeded$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifyAonEventChangeIfNeeded$1() {
        try {
            int currentSupportGesture = getCurrentSupportGesture();
            if ((this.mLastDisplayRotation != this.mDisplayRotation || this.mLastSupportGesture != currentSupportGesture) && this.mAonService != null && this.mListeningState && currentSupportGesture != 0) {
                Slog.i(TAG, "notify aon event changed,mDisplayRotation:" + this.mDisplayRotation + " currentSupportGesture:" + Integer.toBinaryString(currentSupportGesture));
                this.mAonService.aonEventUpdate(TYPE_HAND_ROIACTION, 1 << (this.mDisplayRotation + 16), currentSupportGesture);
                this.mLastSupportGesture = currentSupportGesture;
                this.mLastDisplayRotation = this.mDisplayRotation;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "notifyRotationChanged: error " + e);
        }
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public boolean updateGestureHint(int label) {
        boolean success = false;
        IContactlessGestureService iContactlessGestureService = this.mService;
        if (iContactlessGestureService != null) {
            try {
                success = iContactlessGestureService.showGestureHint(getFeatureFromLabel(label), getCurrentSupportFeature(), getCurrentAppType(), getCurrentPkg(), this.mDisplayRotation);
                if (ContactlessGestureService.mDebug) {
                    Slog.i(TAG, "AON update view label:" + gestureLabelToString(label));
                }
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
        return success;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public boolean isServiceInit() {
        return this.mService != null;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void restContactlessGestureService() {
        this.mService = null;
        this.mServiceBindingLatch = new CountDownLatch(1);
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void onGestureServiceConnected(ComponentName name, IBinder service) {
        Slog.i(TAG, "onServiceConnected");
        if (this.mService == null) {
            this.mService = IContactlessGestureService.Stub.asInterface(service);
        }
        this.mServiceBindingLatch.countDown();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerListener(int type, int fps, int timeout) {
        bindAonService();
        Slog.i(TAG, "register listener, aonService is " + this.mAonService + " mListeningState:" + this.mListeningState + " mDisplayRotation:" + this.mDisplayRotation);
        try {
            IMiAON iMiAON = this.mAonService;
            if (iMiAON != null && !this.mListeningState) {
                iMiAON.registerListener(type, fps, timeout, this.mGestureListener);
                this.mListeningState = true;
            }
            notifyAonEventChangeIfNeeded();
        } catch (RemoteException e) {
            Slog.e(TAG, "registerListener: error " + e);
        }
    }

    private void unRegisterListener() {
        try {
            if (this.mListeningState && this.mAonService != null) {
                Slog.i(TAG, "unregister listener!");
                this.mAonService.unregisterListener(TYPE_HAND_ROIACTION, this.mGestureListener);
                this.mListeningState = false;
                this.mLastSupportGesture = -1;
                this.mLastDisplayRotation = -1;
                this.mIsTrigger = false;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "unRegisterListener: error " + e);
        }
    }

    private void bindAonService() {
        if (this.mAonService != null) {
            return;
        }
        if (this.mAonComponentName == null) {
            this.mAonComponentName = new ComponentName("com.xiaomi.aon", "com.xiaomi.aon.AONService");
        }
        Intent serviceIntent = new Intent("com.xiaomi.aon.AONService").setComponent(this.mAonComponentName);
        this.mContext.bindServiceAsUser(serviceIntent, this.mConnection, 67108865, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.tof.AONGestureController$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements ServiceConnection {
        AnonymousClass2() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.i(AONGestureController.TAG, "Aon service connected success, service:" + service);
            AONGestureController.this.mAonService = IMiAON.Stub.asInterface(Binder.allowBlocking(service));
            AONGestureController.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.AONGestureController$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AONGestureController.AnonymousClass2.this.lambda$onServiceConnected$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceConnected$0() {
            AONGestureController.this.registerListener(AONGestureController.TYPE_HAND_ROIACTION, 6, AONGestureController.ALWAYS_CHECK_GESTURE_TIMEOUT);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName className) {
            Slog.i(AONGestureController.TAG, "Aon service connected failure");
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            cleanupService();
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            cleanupService();
        }

        public void cleanupService() {
            AONGestureController.this.mHandler.post(new Runnable() { // from class: com.android.server.tof.AONGestureController$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AONGestureController.AnonymousClass2.this.lambda$cleanupService$1();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$cleanupService$1() {
            AONGestureController.this.mAonService = null;
            AONGestureController.this.mListeningState = false;
        }
    }
}

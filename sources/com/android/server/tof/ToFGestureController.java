package com.android.server.tof;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.concurrent.CountDownLatch;
import miui.tof.ITofClientService;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ToFGestureController extends ContactlessGestureController {
    private static final String TAG = "TofGestureController";
    private static final String TOF_CLIENT_ACTION = "miui.intent.action.TOF_SERVICE";
    private ITofClientService mService;

    public ToFGestureController(Context context, Handler handler, ContactlessGestureService contactlessGestureService) {
        super(context, handler, contactlessGestureService);
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public ComponentName getTofClientComponent() {
        String value = this.mContext.getResources().getString(286195882);
        if (value != null) {
            return ComponentName.unflattenFromString(value);
        }
        return null;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public String getAction() {
        return TOF_CLIENT_ACTION;
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void showGestureNotification() {
        ITofClientService iTofClientService = this.mService;
        if (iTofClientService != null) {
            try {
                iTofClientService.showTofGestureNotification();
                Slog.i(TAG, "show Tof gesture notification");
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void registerGestureListenerIfNeeded(boolean register) {
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public void notifyRotationChanged(int rotation) {
    }

    @Override // com.android.server.tof.ContactlessGestureController
    public boolean updateGestureHint(int label) {
        ITofClientService iTofClientService = this.mService;
        if (iTofClientService != null) {
            try {
                iTofClientService.showGestureHint(getFeatureFromLabel(label), getCurrentSupportFeature(), getCurrentPkg());
                if (ContactlessGestureService.mDebug) {
                    Slog.i(TAG, "TMS update view label:" + gestureLabelToString(label));
                    return true;
                }
                return true;
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
                return true;
            }
        }
        return true;
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
    public void onGestureServiceConnected(ComponentName name, final IBinder service) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.tof.ToFGestureController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ToFGestureController.this.lambda$onGestureServiceConnected$0(service);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onGestureServiceConnected$0(IBinder service) {
        Slog.i(TAG, "onServiceConnected");
        if (this.mService == null) {
            this.mService = ITofClientService.Stub.asInterface(service);
        }
        this.mServiceBindingLatch.countDown();
    }
}

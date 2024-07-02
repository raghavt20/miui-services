package com.xiaomi.interconnection;

import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;
import com.xiaomi.interconnection.IInterconnectionManager;
import com.xiaomi.interconnection.ISoftApCallback;
import com.xiaomi.interconnection.IWifiP2pCallback;
import com.xiaomi.interconnection.InterconnectionManager;

/* loaded from: classes.dex */
public class InterconnectionManager {
    public static final int DBS_1 = 1;
    public static final int DBS_2 = 2;
    public static final int DBS_UNSUPPORTED = 0;
    private static final String TAG = "InterconnectionManager";
    private static final SparseArray<ISoftApCallback> sSoftApCallbackMap = new SparseArray<>();
    private static final SparseArray<IWifiP2pCallback> sWifiP2pCallbackMap = new SparseArray<>();
    private Handler mHandler;
    private IInterconnectionManager mInterconnService;

    /* loaded from: classes.dex */
    public interface SoftApCallback {
        void onIfaceInfoChanged(String str);
    }

    /* loaded from: classes.dex */
    public interface WifiP2pCallback {
        void onDevicesInfoChanged(P2pDevicesInfo p2pDevicesInfo);
    }

    private InterconnectionManager() {
        IBinder binder = ServiceManager.getService(InterconnectionService.SERVICE_NAME);
        IInterconnectionManager asInterface = IInterconnectionManager.Stub.asInterface(binder);
        this.mInterconnService = asInterface;
        if (asInterface == null) {
            Log.e(TAG, "InterconnectionService not found");
        }
        this.mHandler = new Handler();
    }

    /* loaded from: classes.dex */
    private static class SingletonHolder {
        private static final InterconnectionManager INSTANCE = new InterconnectionManager();

        private SingletonHolder() {
        }
    }

    public static InterconnectionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SoftApCallbackProxy extends ISoftApCallback.Stub {
        private final SoftApCallback mCallback;

        private SoftApCallbackProxy(SoftApCallback callback) {
            this.mCallback = callback;
        }

        @Override // com.xiaomi.interconnection.ISoftApCallback
        public void onIfaceInfoChanged(final String softApIfaceName) throws RemoteException {
            InterconnectionManager.this.mHandler.post(new Runnable() { // from class: com.xiaomi.interconnection.InterconnectionManager$SoftApCallbackProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InterconnectionManager.SoftApCallbackProxy.this.lambda$onIfaceInfoChanged$0(softApIfaceName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onIfaceInfoChanged$0(String softApIfaceName) {
            this.mCallback.onIfaceInfoChanged(softApIfaceName);
        }
    }

    public void registerSoftApCallback(SoftApCallback softApCallback) {
        if (softApCallback == null) {
            throw new IllegalArgumentException("softApCallback cannot be null");
        }
        if (this.mInterconnService != null) {
            try {
                SparseArray<ISoftApCallback> sparseArray = sSoftApCallbackMap;
                synchronized (sparseArray) {
                    ISoftApCallback.Stub binderCallback = new SoftApCallbackProxy(softApCallback);
                    sparseArray.put(System.identityHashCode(softApCallback), binderCallback);
                    this.mInterconnService.registerSoftApCallback(binderCallback);
                }
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
            }
        }
    }

    public void unregisterSoftApCallback(SoftApCallback softApCallback) {
        if (softApCallback == null) {
            throw new IllegalArgumentException("softApCallback cannot be null");
        }
        if (this.mInterconnService != null) {
            try {
                SparseArray<ISoftApCallback> sparseArray = sSoftApCallbackMap;
                synchronized (sparseArray) {
                    int callbackIdentifier = System.identityHashCode(softApCallback);
                    if (!sparseArray.contains(callbackIdentifier)) {
                        Log.w(TAG, "unknown softApCallback " + callbackIdentifier);
                    } else {
                        this.mInterconnService.unregisterSoftApCallback(sparseArray.get(callbackIdentifier));
                        sparseArray.remove(callbackIdentifier);
                    }
                }
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WifiP2pCallbackProxy extends IWifiP2pCallback.Stub {
        private final WifiP2pCallback mCallback;

        private WifiP2pCallbackProxy(WifiP2pCallback callback) {
            this.mCallback = callback;
        }

        @Override // com.xiaomi.interconnection.IWifiP2pCallback
        public void onDevicesInfoChanged(final P2pDevicesInfo info) throws RemoteException {
            InterconnectionManager.this.mHandler.post(new Runnable() { // from class: com.xiaomi.interconnection.InterconnectionManager$WifiP2pCallbackProxy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InterconnectionManager.WifiP2pCallbackProxy.this.lambda$onDevicesInfoChanged$0(info);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDevicesInfoChanged$0(P2pDevicesInfo info) {
            this.mCallback.onDevicesInfoChanged(info);
        }
    }

    public void registerWifiP2pCallback(WifiP2pCallback wifiP2pCallback) {
        if (wifiP2pCallback == null) {
            throw new IllegalArgumentException("wifiP2pCallback cannot be null");
        }
        if (this.mInterconnService != null) {
            try {
                SparseArray<IWifiP2pCallback> sparseArray = sWifiP2pCallbackMap;
                synchronized (sparseArray) {
                    IWifiP2pCallback.Stub binderCallback = new WifiP2pCallbackProxy(wifiP2pCallback);
                    sparseArray.put(System.identityHashCode(wifiP2pCallback), binderCallback);
                    this.mInterconnService.registerWifiP2pCallback(binderCallback);
                }
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
            }
        }
    }

    public void unregisterWifiP2pCallback(WifiP2pCallback wifiP2pCallback) {
        if (wifiP2pCallback == null) {
            throw new IllegalArgumentException("wifiP2pCallback cannot be null");
        }
        if (this.mInterconnService != null) {
            try {
                SparseArray<IWifiP2pCallback> sparseArray = sWifiP2pCallbackMap;
                synchronized (sparseArray) {
                    int callbackIdentifier = System.identityHashCode(wifiP2pCallback);
                    if (!sparseArray.contains(callbackIdentifier)) {
                        Log.w(TAG, "unknown wifiP2pCallback" + callbackIdentifier);
                    } else {
                        this.mInterconnService.unregisterWifiP2pCallback(sparseArray.get(callbackIdentifier));
                        sparseArray.remove(callbackIdentifier);
                    }
                }
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
            }
        }
    }

    public String getWifiChipModel() {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                return iInterconnectionManager.getWifiChipModel();
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
                return "unknown";
            }
        }
        return "unknown";
    }

    public boolean supportP2pChannel165() {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                return iInterconnectionManager.supportP2pChannel165();
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
                return false;
            }
        }
        return false;
    }

    public boolean supportP2p160Mode() {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                return iInterconnectionManager.supportP2p160Mode();
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
                return false;
            }
        }
        return false;
    }

    public boolean supportHbs() {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                return iInterconnectionManager.supportHbs();
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
                return false;
            }
        }
        return false;
    }

    public int supportDbs() {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                return iInterconnectionManager.supportDbs();
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
                return 0;
            }
        }
        return 0;
    }

    public void notifyConcurrentNetworkState(boolean mcc) {
        IInterconnectionManager iInterconnectionManager = this.mInterconnService;
        if (iInterconnectionManager != null) {
            try {
                iInterconnectionManager.notifyConcurrentNetworkState(mcc);
            } catch (RemoteException e) {
                Log.d(TAG, "exception: " + e);
            }
        }
    }
}

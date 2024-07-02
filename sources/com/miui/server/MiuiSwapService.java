package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.SystemService;
import miui.swap.ISwap;
import miui.swap.ISwapManager;

/* loaded from: classes.dex */
public class MiuiSwapService extends ISwapManager.Stub {
    public static final String ACTION_TEST_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private static final int EXECUTE_SWAP_DISABLE = 4;
    private static final int EXECUTE_SWAP_trigger = 2;
    private static final String HAL_DEFAULT = "default";
    private static final String HAL_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.swap@1.0::ISwap";
    private static final String HAL_SERVICE_NAME = "vendor.xiaomi.hardware.swap@1.0::ISwap";
    private static final int IS_SWAP_SUPPORTED = 1;
    private static final String NATIVE_SERVICE_KEY = "persist.sys.swapservice.ctrl";
    private static final String NATIVE_SERVICE_NAME = "SwapNativeService";
    public static final String SERVICE_NAME = "miui.swap.service";
    private static final int START_SWAP = 1;
    private static final int WHETHER_START = 2;
    private static volatile SwapServiceHandler mSwapServiceHandler;
    private Context mContext;
    private volatile ISwap mSwapNativeService;
    private HandlerThread mSwapServiceThread;
    private boolean mUsbConnect = false;
    private static final String TAG = MiuiSwapService.class.getSimpleName();
    public static final boolean SWAP_DEBUG = SystemProperties.getBoolean("persist.sys.miui_swap_debug", false);
    public static final boolean IS_SWAP_ENABLE = SystemProperties.getBoolean("persist.sys.stability.swapEnable", false);

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiSwapService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiuiSwapService(context);
        }

        public void onStart() {
            publishBinderService(MiuiSwapService.SERVICE_NAME, this.mService);
        }
    }

    /* loaded from: classes.dex */
    private final class SwapServiceHandler extends Handler {
        public SwapServiceHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!MiuiSwapService.this.mUsbConnect) {
                        MiuiSwapService.this.startSwapLocked();
                    }
                    SystemProperties.set(MiuiSwapService.NATIVE_SERVICE_KEY, Boolean.toString(false));
                    return;
                case 2:
                    if (!MiuiSwapService.this.mUsbConnect) {
                        SystemProperties.set(MiuiSwapService.NATIVE_SERVICE_KEY, Boolean.toString(true));
                        return;
                    }
                    return;
                default:
                    Slog.w(MiuiSwapService.TAG, "fail match message");
                    return;
            }
        }
    }

    public MiuiSwapService(Context context) {
        this.mContext = context;
        getUsbStatus();
        HandlerThread handlerThread = new HandlerThread("swapServiceWork");
        this.mSwapServiceThread = handlerThread;
        handlerThread.start();
        mSwapServiceHandler = new SwapServiceHandler(this.mSwapServiceThread.getLooper());
        mSwapServiceHandler.sendEmptyMessageDelayed(2, 5000L);
        mSwapServiceHandler.sendEmptyMessageDelayed(1, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startSwapLocked() {
        String nativeReturnReslut = callNativeSwap();
        String str = TAG;
        Slog.d(str, "native return result :" + nativeReturnReslut);
        if (nativeReturnReslut != "") {
            callHalSwap(nativeReturnReslut);
        } else {
            Slog.w(str, "callNativeSwap return result is null");
        }
    }

    public void swapControl() {
        Boolean swapEnable = Boolean.valueOf(SystemProperties.getBoolean("persist.sys.stability.swapEnable", false));
        if (swapEnable.booleanValue()) {
            if (!this.mUsbConnect) {
                startSwapLocked();
            }
        } else {
            swapHalDisable();
            Slog.w(TAG, "The cloud control version does not support this function");
        }
        SystemProperties.set("persist.sys.stability.preswapEnable", Boolean.toString(swapEnable.booleanValue()));
        SystemProperties.set(NATIVE_SERVICE_KEY, Boolean.toString(false));
    }

    public String callNativeSwap() {
        getNativeService();
        try {
            String getLba = (this.mSwapNativeService == null || !this.mSwapNativeService.SWAP_isSupport()) ? null : this.mSwapNativeService.SWAP_trigger();
            return getLba;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to access native swap methods", e);
            return "";
        }
    }

    private ISwap getNativeService() {
        IBinder Ibinder = null;
        try {
            Boolean isStartService = Boolean.valueOf(SystemProperties.getBoolean(NATIVE_SERVICE_KEY, false));
            if (isStartService.booleanValue() && (Ibinder = ServiceManager.getService(NATIVE_SERVICE_NAME)) != null) {
                Ibinder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.miui.server.MiuiSwapService.1
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        Slog.w(MiuiSwapService.TAG, "SwapNativeService has died, the connection failed");
                        MiuiSwapService.this.mSwapNativeService = null;
                    }
                }, 0);
            }
        } catch (Exception e) {
            Slog.e(TAG, "fail to get SwapNativeService:" + e);
        }
        if (Ibinder != null) {
            this.mSwapNativeService = ISwap.Stub.asInterface(Ibinder);
            Slog.w(TAG, "get ISwap");
        } else {
            this.mSwapNativeService = null;
            Slog.w(TAG, "ISwap get failed, please try again");
        }
        return this.mSwapNativeService;
    }

    private void callHalSwap(String nativeResult) {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.swap@1.0::ISwap", "default");
                if (hwService != null) {
                    if (isSWAPSupport()) {
                        HwParcel hidl_request = new HwParcel();
                        hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.swap@1.0::ISwap");
                        hidl_request.writeString(nativeResult);
                        hwService.transact(2, hidl_request, hidl_reply, 0);
                        hidl_reply.verifySuccess();
                        hidl_request.releaseTemporaryStorage();
                        String HalReturnResult = hidl_reply.readString();
                        Slog.d(TAG, "callHalSwap return result is :" + HalReturnResult);
                    } else {
                        Slog.d(TAG, "This device does not support swap on hal");
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to call SwapHalService" + e);
            }
        } finally {
            hidl_reply.release();
        }
    }

    private boolean swapHalDisable() {
        IHwBinder hwService;
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                hwService = HwBinder.getService("vendor.xiaomi.hardware.swap@1.0::ISwap", "default");
            } catch (Exception e) {
                Slog.e(TAG, "fail to get SwapHalService" + e);
            }
            if (hwService != null) {
                HwParcel hidl_request = new HwParcel();
                hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.swap@1.0::ISwap");
                hwService.transact(4, hidl_request, hidl_reply, 0);
                hidl_reply.verifySuccess();
                hidl_request.releaseTemporaryStorage();
                boolean flag = hidl_reply.readBool();
                return flag;
            }
            Slog.d(TAG, "hwService get failed, please try again");
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    private boolean isSWAPSupport() {
        IHwBinder hwService;
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                hwService = HwBinder.getService("vendor.xiaomi.hardware.swap@1.0::ISwap", "default");
            } catch (Exception e) {
                Slog.e(TAG, "fail to get SwapHalService" + e);
            }
            if (hwService != null) {
                HwParcel hidl_request = new HwParcel();
                hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.swap@1.0::ISwap");
                hwService.transact(1, hidl_request, hidl_reply, 0);
                hidl_reply.verifySuccess();
                hidl_request.releaseTemporaryStorage();
                boolean flag = hidl_reply.readBool();
                return flag;
            }
            Slog.d(TAG, "hwService get failed, please try again");
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    public void getUsbStatus() {
        if (this.mContext != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.hardware.usb.action.USB_STATE");
            this.mContext.registerReceiver(new UsbStatusReceiver(), filter);
        }
    }

    /* loaded from: classes.dex */
    public class UsbStatusReceiver extends BroadcastReceiver {
        public UsbStatusReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null || MiuiSwapService.SWAP_DEBUG) {
                String action = MiuiSwapService.TAG;
                Slog.w(action, "intent is null or debug");
                return;
            }
            String action2 = intent.getAction();
            if ("android.hardware.usb.action.USB_STATE".equals(action2)) {
                boolean connected = intent.getExtras().getBoolean("connected");
                if (connected) {
                    MiuiSwapService.this.mUsbConnect = true;
                    Slog.d(MiuiSwapService.TAG, "Usb connect");
                } else {
                    MiuiSwapService.this.mUsbConnect = false;
                    Slog.d(MiuiSwapService.TAG, "Usb disconnect");
                }
            }
        }
    }
}

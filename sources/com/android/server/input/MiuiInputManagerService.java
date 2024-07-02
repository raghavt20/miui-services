package com.android.server.input;

import android.content.Context;
import android.hardware.display.DisplayViewport;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MiuiSettings;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyboardShortcutInfo;
import android.view.MotionEvent;
import android.view.PointerIcon;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.fling.FlingTracker;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProvider;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.input.shoulderkey.ShoulderKeyManagerInternal;
import com.android.server.input.touchpad.TouchpadOneTrackHelper;
import com.miui.server.input.PadManager;
import com.miui.server.input.deviceshare.MiuiDeviceShareManager;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import com.miui.server.input.magicpointer.MiuiMagicPointerServiceInternal;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import com.miui.server.input.stylus.MiuiStylusUtils;
import com.miui.server.input.stylus.blocker.MiuiEventBlockerManager;
import com.miui.server.input.stylus.laser.LaserPointerController;
import com.miui.server.input.stylus.laser.PointerControllerInterface;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.input.util.MiuiInputShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import miui.hardware.input.IAppScrollerOptimizationConfigChangedListener;
import miui.hardware.input.IDeviceShareStateChangedListener;
import miui.hardware.input.IMiuiInputManager;
import miui.hardware.input.IMiuiMotionEventListener;
import miui.hardware.input.IShortcutSettingsChangedListener;
import miui.hardware.input.MiuiKeyboardHelper;
import miui.hardware.input.MiuiKeyboardStatus;
import miui.util.ITouchFeature;

/* loaded from: classes.dex */
public class MiuiInputManagerService extends IMiuiInputManager.Stub {
    private static final String TAG = "MiuiInputManagerService";
    private final Context mContext;
    private EdgeSuppressionManager mEdgeSuppressionManager;
    private FlingTracker mFlingTracker;
    private boolean mIsScreenOn;
    private volatile LaserPointerController mLaserPointerController;
    private MiuiCustomizeShortCutUtils mMiuiCustomizeShortCutUtils;
    private MiuiGestureMonitor mMiuiGestureMonitor;
    private MiuiMagicPointerServiceInternal mMiuiMagicPointerService;
    private MiuiPadKeyboardManager mMiuiPadKeyboardManager;
    private MiuiStylusShortcutManager mMiuiStylusShortcutManager;
    private PowerManager mPowerManager;
    private final Object mMiuiShortcutSettingsLock = new Object();
    private final SparseArray<ShortcutListenerRecord> mShortcutListeners = new SparseArray<>();
    private final ArrayList<ShortcutListenerRecord> mShortcutListenersToNotify = new ArrayList<>();
    private final Object mMotionEventLock = new Object();
    private final SparseArray<MotionEventListenerRecord> mMotionEventListeners = new SparseArray<>();
    private final ArrayList<MotionEventListenerRecord> mMotionEventListenersToNotify = new ArrayList<>();
    private final MiuiGestureListener mMiuiGestureListener = new MiuiGestureListener() { // from class: com.android.server.input.MiuiInputManagerService$$ExternalSyntheticLambda1
        @Override // com.miui.server.input.gesture.MiuiGestureListener
        public final void onPointerEvent(MotionEvent motionEvent) {
            MiuiInputManagerService.this.sendMotionEventToNotify(motionEvent);
        }
    };
    private int mPointerDisplayId = 0;
    private List<DisplayViewport> mDisplayViewports = Collections.emptyList();
    private ShoulderKeyManagerInternal mShoulderKeyManagerInternal = null;
    private final Runnable mPokeUserActivityRunnable = new Runnable() { // from class: com.android.server.input.MiuiInputManagerService$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            MiuiInputManagerService.this.lambda$new$0();
        }
    };
    private final NativeMiuiInputManagerService mNative = new NativeMiuiInputManagerService(this);
    private final Handler mHandler = new H(MiuiInputThread.getThread().getLooper());

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 2, 0);
    }

    public void setInteractive(boolean interactive) {
        this.mNative.setInteractive(interactive);
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private MiuiInputManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            InputManagerServiceStub inputManagerServiceStub = InputManagerServiceStub.getInstance();
            if (inputManagerServiceStub instanceof InputManagerServiceStubImpl) {
                InputManagerServiceStubImpl impl = (InputManagerServiceStubImpl) inputManagerServiceStub;
                this.mService = impl.getMiuiInputManagerService();
            }
        }

        public void onStart() {
            IMiuiInputManager.Stub stub = this.mService;
            if (stub == null) {
                Slog.e(MiuiInputManagerService.TAG, "MiuiInputManagerService not init...");
            } else {
                publishBinderService("MiuiInputManager", stub);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiuiInputManagerService(Context context) {
        this.mContext = context;
        this.mMiuiGestureMonitor = MiuiGestureMonitor.getInstance(context);
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        LocalServices.addService(MiuiInputManagerInternal.class, new LocalService());
        this.mFlingTracker = new FlingTracker(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start() {
        if (PadManager.getInstance().isPad()) {
            this.mMiuiCustomizeShortCutUtils = MiuiCustomizeShortCutUtils.getInstance(this.mContext);
        }
        if (ITouchFeature.getInstance().hasSupportEdgeMode()) {
            this.mEdgeSuppressionManager = EdgeSuppressionManager.getInstance(this.mContext);
        }
        if (MiuiStylusUtils.isSupportOffScreenQuickNote()) {
            this.mMiuiStylusShortcutManager = MiuiStylusShortcutManager.getInstance();
        }
        this.mFlingTracker.registerPointerEventListener();
    }

    public void updateKeyboardShortcut(KeyboardShortcutInfo info, int type) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application update keyboard shortcut");
            return;
        }
        MiuiCustomizeShortCutUtils miuiCustomizeShortCutUtils = this.mMiuiCustomizeShortCutUtils;
        if (miuiCustomizeShortCutUtils != null) {
            miuiCustomizeShortCutUtils.updateKeyboardShortcut(info, type);
        } else {
            Slog.e(TAG, "Can't update Keyboard ShortcutKey because Not Support");
        }
    }

    public List<KeyboardShortcutInfo> getKeyboardShortcut() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application get keyboard shortcut");
            return null;
        }
        MiuiCustomizeShortCutUtils miuiCustomizeShortCutUtils = this.mMiuiCustomizeShortCutUtils;
        if (miuiCustomizeShortCutUtils != null) {
            return miuiCustomizeShortCutUtils.getKeyboardShortcutInfo();
        }
        Slog.e(TAG, "Can't get Keyboard ShortcutKey because Not Support");
        return null;
    }

    public List<KeyboardShortcutInfo> getDefaultKeyboardShortcutInfos() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application get keyboard Default shortcut");
            return null;
        }
        MiuiCustomizeShortCutUtils miuiCustomizeShortCutUtils = this.mMiuiCustomizeShortCutUtils;
        if (miuiCustomizeShortCutUtils != null) {
            return miuiCustomizeShortCutUtils.getDefaultKeyboardShortcutInfos();
        }
        Slog.e(TAG, "Can't get Keyboard ShortcutKey because Not Support");
        return null;
    }

    public String getMiKeyboardStatus() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application get keyboard status");
            return null;
        }
        if (this.mMiuiPadKeyboardManager == null && PadManager.getInstance().isPad()) {
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            return getKeyboardStatus(miuiPadKeyboardManager.getKeyboardStatus());
        }
        Slog.e(TAG, "Get Keyboard Status fail because Not Support");
        return null;
    }

    public int[] getEdgeSuppressionSize(boolean isAbsolute) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application get edge suppression size");
            return null;
        }
        EdgeSuppressionManager edgeSuppressionManager = this.mEdgeSuppressionManager;
        if (edgeSuppressionManager != null) {
            return isAbsolute ? edgeSuppressionManager.getAbsoluteLevel() : edgeSuppressionManager.getConditionLevel();
        }
        Slog.e(TAG, "Can't get EdgeSuppression Info because not Support");
        return null;
    }

    public int[] getInputMethodSizeScope() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application get inputmethod size");
            return null;
        }
        EdgeSuppressionManager edgeSuppressionManager = this.mEdgeSuppressionManager;
        if (edgeSuppressionManager != null) {
            return edgeSuppressionManager.getInputMethodSizeScope();
        }
        Slog.e(TAG, "Can't get inputmethod size because not Support");
        return null;
    }

    public boolean setCursorPosition(float x, float y) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application set cursor position");
            return false;
        }
        Slog.i(TAG, "setCursorPosition(" + x + ", " + y + ")");
        return this.mNative.setCursorPosition(x, y);
    }

    public boolean hideCursor() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application hide cursor");
            return false;
        }
        Slog.i(TAG, "hideCursor");
        return this.mNative.hideCursor();
    }

    public String[] getAppScrollerOptimizationConfig(String packageName) {
        return ScrollerOptimizationConfigProvider.getInstance().getAppScrollerOptimizationConfigAndSwitchState(packageName);
    }

    public void registerAppScrollerOptimizationConfigListener(String packageName, IAppScrollerOptimizationConfigChangedListener listener) {
        ScrollerOptimizationConfigProvider.getInstance().registerAppScrollerOptimizationConfigListener(packageName, listener);
    }

    public void setKeyboardBackLightBrightness(int brightness) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return;
        }
        if (this.mMiuiPadKeyboardManager == null && PadManager.getInstance().isPad()) {
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.setKeyboardBackLightBrightness(brightness);
        } else {
            Slog.e(TAG, "set padKeyboard backLight fail because Not Support");
        }
    }

    public int getKeyboardBackLightBrightness() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return -1;
        }
        if (this.mMiuiPadKeyboardManager == null && PadManager.getInstance().isPad()) {
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            return miuiPadKeyboardManager.getKeyboardBackLightBrightness();
        }
        Slog.e(TAG, "get padKeyboard backLight fail because Not Support");
        return -1;
    }

    public int getKeyboardType() {
        if (!ActivityManagerServiceImpl.getInstance().isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "the app is not system :" + (!ActivityManagerServiceImpl.getInstance().isSystemApp(Binder.getCallingPid())));
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return -1;
        }
        if (this.mMiuiPadKeyboardManager == null && PadManager.getInstance().isPad()) {
            this.mMiuiPadKeyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            return miuiPadKeyboardManager.getKeyboardType();
        }
        Slog.e(TAG, "get padKeyboard type fail because Not Support");
        return -1;
    }

    private String getKeyboardStatus(MiuiKeyboardStatus status) {
        StringBuilder result = new StringBuilder("Keyboard is Normal");
        if (status.shouldIgnoreKeyboard()) {
            result.setLength(0);
            if (!status.isAngleStatusWork()) {
                result.append("AngleStatus Exception ");
            } else if (!status.isAuthStatus()) {
                result.append("AuthStatus Exception ");
            } else if (!status.isLidStatus()) {
                result.append("LidStatus Exception ");
            } else if (!status.isTabletStatus()) {
                result.append("TabletStatus Exception ");
            } else if (!status.isConnected()) {
                result.append("The Keyboard is disConnect ");
            }
        }
        result.append(", MCU:").append(status.getMCUVersion()).append(", Keyboard:").append(status.getKeyboardVersion());
        return result.toString();
    }

    public boolean putStringForUser(String action, String function) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return false;
        }
        String targetPackage = ActivityManagerServiceImpl.getInstance().getPackageNameForPid(Binder.getCallingPid());
        Slog.i(TAG, "Changed Action:" + action + ",new Function:" + function + ",because for " + targetPackage);
        long origId = Binder.clearCallingIdentity();
        MiuiSettings.System.putStringForUser(this.mContext.getContentResolver(), action, function, -2);
        sendSettingsChangedMessage(1, action, function);
        Binder.restoreCallingIdentity(origId);
        return true;
    }

    public void registerShortcutChangedListener(IShortcutSettingsChangedListener listener) {
        if (listener == null || !isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
            return;
        }
        synchronized (this.mMiuiShortcutSettingsLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mShortcutListeners.get(callingPid) != null) {
                throw new IllegalArgumentException("Can't register repeat listener to MiuiShortcutSettings");
            }
            ShortcutListenerRecord shortcutListenerRecord = new ShortcutListenerRecord(listener, callingPid);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(shortcutListenerRecord, 0);
                this.mShortcutListeners.put(callingPid, shortcutListenerRecord);
            } catch (RemoteException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public void unregisterShortcutChangedListener() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction gesture shortcut");
        } else {
            removeListenerFromSystem(Binder.getCallingPid());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeListenerFromSystem(int pid) {
        synchronized (this.mMiuiShortcutSettingsLock) {
            this.mShortcutListeners.delete(pid);
        }
    }

    private void sendSettingsChangedMessage(int messageWhat, String action, String newFunction) {
        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        bundle.putString("function", newFunction);
        Message msg = this.mHandler.obtainMessage(messageWhat);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public void requestRedirect(int motionEventId) {
        Slog.i(TAG, "app requestRedirect, motionEventId:" + motionEventId);
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support non-system application requestRedirect");
        } else {
            this.mNative.requestRedirect(motionEventId, Binder.getCallingPid());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ShortcutListenerRecord implements IBinder.DeathRecipient {
        private final IShortcutSettingsChangedListener mChangedListener;
        private final int mPid;

        ShortcutListenerRecord(IShortcutSettingsChangedListener settingsChangedListener, int pid) {
            this.mChangedListener = settingsChangedListener;
            this.mPid = pid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiInputManagerService.this.removeListenerFromSystem(this.mPid);
        }

        public void notifyShortcutSettingsChanged(String action, String function) {
            try {
                this.mChangedListener.onSettingsChanged(action, function);
            } catch (RemoteException ex) {
                Slog.w(MiuiInputManagerService.TAG, "Failed to notify process " + this.mPid + " that Settings Changed, assuming it died.", ex);
                binderDied();
            }
        }
    }

    public void setHasEditTextOnScreen(boolean hasEditText) {
        MiuiEventBlockerManager.getInstance().setHasEditTextOnScreen(hasEditText);
    }

    public void setDeviceShareListener(ParcelFileDescriptor fd, int flags, IDeviceShareStateChangedListener listener) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application set device share listener :(");
        } else {
            MiuiDeviceShareManager.getInstance().setDeviceShareListener(Binder.getCallingPid(), fd, flags, listener);
        }
    }

    public void setTouchpadButtonState(int deviceId, boolean isDown) {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application set touchpad button state :(");
        } else {
            MiuiDeviceShareManager.getInstance().setTouchpadButtonState(Binder.getCallingPid(), deviceId, isDown);
        }
    }

    public void registerMiuiMotionEventListener(IMiuiMotionEventListener listener) {
        if (listener == null || !isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction motion event");
            return;
        }
        synchronized (this.mMotionEventLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mMotionEventListeners.get(callingPid) != null) {
                Slog.e(TAG, "Can't register repeat listener to motion event");
                return;
            }
            MotionEventListenerRecord motionEventListenerRecord = new MotionEventListenerRecord(listener, callingPid);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(motionEventListenerRecord, 0);
            } catch (RemoteException e) {
                Slog.e(TAG, "Can't linkToDeath because IllegalStateException");
            }
            if (this.mMotionEventListeners.size() == 0) {
                Slog.i(TAG, "register pointer event listener");
                this.mMiuiGestureMonitor.registerPointerEventListener(this.mMiuiGestureListener);
            }
            this.mMotionEventListeners.put(callingPid, motionEventListenerRecord);
        }
    }

    public void unregisterMiuiMotionEventListener() {
        if (!isSystemApp(Binder.getCallingPid())) {
            Slog.e(TAG, "Not Support normal application interaction motion event");
        } else {
            removeMotionEventListener(Binder.getCallingPid());
        }
    }

    public void reportFlingEvent(String pkgName, int downTimes, int flingTimes) {
        synchronized (this.mMotionEventLock) {
            this.mFlingTracker.trackEvent(pkgName, downTimes, flingTimes);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeMotionEventListener(int pid) {
        synchronized (this.mMotionEventLock) {
            if (this.mMotionEventListeners.contains(pid)) {
                this.mMotionEventListeners.delete(pid);
                if (this.mMotionEventListeners.size() == 0) {
                    Slog.i(TAG, "unregister pointer event listener");
                    this.mMiuiGestureMonitor.unregisterPointerEventListener(this.mMiuiGestureListener);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMotionEventToNotify(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        if (action != 0 && action != 1 && action != 3) {
            return;
        }
        final MotionEvent event = motionEvent.copy();
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.MiuiInputManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiInputManagerService.this.lambda$sendMotionEventToNotify$1(event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendMotionEventToNotify$1(MotionEvent event) {
        this.mMotionEventListenersToNotify.clear();
        synchronized (this.mMotionEventLock) {
            for (int i = 0; i < this.mMotionEventListeners.size(); i++) {
                this.mMotionEventListenersToNotify.add(this.mMotionEventListeners.valueAt(i));
            }
        }
        Iterator<MotionEventListenerRecord> it = this.mMotionEventListenersToNotify.iterator();
        while (it.hasNext()) {
            MotionEventListenerRecord record = it.next();
            record.notifyMotionEvent(event);
        }
        event.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MotionEventListenerRecord implements IBinder.DeathRecipient {
        private final IMiuiMotionEventListener mMotionEventListener;
        private final int mPid;

        MotionEventListenerRecord(IMiuiMotionEventListener motionEventListener, int pid) {
            this.mMotionEventListener = motionEventListener;
            this.mPid = pid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiInputManagerService.this.removeMotionEventListener(this.mPid);
        }

        public void notifyMotionEvent(MotionEvent event) {
            try {
                this.mMotionEventListener.onMiuiMotionEvent(event);
            } catch (RemoteException ex) {
                Slog.w(MiuiInputManagerService.TAG, "Failed to notify process " + this.mPid + " , assuming it died.", ex);
                binderDied();
            }
        }
    }

    public void updatePointerDisplayId(final int displayId) {
        this.mPointerDisplayId = displayId;
        if (this.mLaserPointerController != null) {
            this.mLaserPointerController.setDisplayId(displayId);
        }
        Optional.ofNullable((MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class)).ifPresent(new Consumer() { // from class: com.android.server.input.MiuiInputManagerService$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MiuiMagicPointerServiceInternal) obj).updatePointerDisplayId(displayId);
            }
        });
    }

    public void updateDisplayViewport(final List<DisplayViewport> viewports) {
        this.mDisplayViewports = viewports;
        if (this.mLaserPointerController != null) {
            this.mLaserPointerController.setDisplayViewPort(viewports);
        }
        Optional.ofNullable((MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class)).ifPresent(new Consumer() { // from class: com.android.server.input.MiuiInputManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MiuiMagicPointerServiceInternal) obj).setDisplayViewports(viewports);
            }
        });
    }

    public PointerControllerInterface obtainLaserPointerController() {
        if (this.mLaserPointerController == null) {
            this.mLaserPointerController = new LaserPointerController();
            this.mLaserPointerController.setDisplayId(this.mPointerDisplayId);
            this.mLaserPointerController.setDisplayViewPort(this.mDisplayViewports);
            this.mLaserPointerController.setScreenState(this.mIsScreenOn);
        }
        return this.mLaserPointerController;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenState(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
        if (this.mLaserPointerController != null) {
            this.mLaserPointerController.setScreenState(isScreenOn);
        }
        if (this.mMiuiStylusShortcutManager == null) {
            return;
        }
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        boolean needSyncMotion = !isScreenOn;
        inputCommonConfig.setNeedSyncMotion(needSyncMotion);
        inputCommonConfig.flushToNative();
        Slog.i(TAG, "Flush needSyncMotion to native, value = " + needSyncMotion);
    }

    private void beforeNotifyMotion(MotionEvent motionEvent) {
        MiuiStylusShortcutManager miuiStylusShortcutManager = this.mMiuiStylusShortcutManager;
        if (miuiStylusShortcutManager != null) {
            miuiStylusShortcutManager.processMotionEventForQuickNote(motionEvent);
        }
    }

    private void notifyTouchMotionEvent(MotionEvent event) {
        if (this.mShoulderKeyManagerInternal == null) {
            this.mShoulderKeyManagerInternal = (ShoulderKeyManagerInternal) LocalServices.getService(ShoulderKeyManagerInternal.class);
        }
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.notifyTouchMotionEvent(event);
        }
    }

    private int isXiaomiStylus(int vendorId, int productId) {
        return InputDevice.isXiaomiStylus(vendorId, productId);
    }

    private int isXiaomiKeyboard(int vendorId, int productId) {
        return MiuiKeyboardHelper.getXiaomiKeyboardType(productId, vendorId);
    }

    private int isXiaomiTouchpad(int vendorId, int productId) {
        return MiuiKeyboardHelper.getXiaomiTouchPadType(productId, vendorId);
    }

    public void notifyDeviceShareListenerSocketBroken(int pid) {
        MiuiDeviceShareManager.getInstance().onSocketBroken(pid);
    }

    public void pokeUserActivity() {
        MiuiInputThread.getHandler().post(this.mPokeUserActivityRunnable);
    }

    public void trackTouchpadEvent(int type, int x, int y) {
        TouchpadOneTrackHelper.getInstance(this.mContext).trackTouchpadEvent(type, x, y);
    }

    /* loaded from: classes.dex */
    class H extends Handler {
        private static final String DATA_ACTION = "action";
        private static final String DATA_FUNCTION = "function";
        private static final int MSG_NOTIFY_LISTENER = 1;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String action = bundle.getString(DATA_ACTION, "");
            String function = bundle.getString(DATA_FUNCTION, "");
            if (msg.what == 1) {
                MiuiInputManagerService.this.mShortcutListenersToNotify.clear();
                synchronized (MiuiInputManagerService.this.mMiuiShortcutSettingsLock) {
                    for (int i = 0; i < MiuiInputManagerService.this.mShortcutListeners.size(); i++) {
                        MiuiInputManagerService.this.mShortcutListenersToNotify.add((ShortcutListenerRecord) MiuiInputManagerService.this.mShortcutListeners.valueAt(i));
                    }
                }
                Iterator it = MiuiInputManagerService.this.mShortcutListenersToNotify.iterator();
                while (it.hasNext()) {
                    ShortcutListenerRecord record = (ShortcutListenerRecord) it.next();
                    record.notifyShortcutSettingsChanged(action, function);
                }
            }
        }
    }

    private static boolean isSystemApp(int pid) {
        return ActivityManagerServiceImpl.getInstance().isSystemApp(pid);
    }

    /* loaded from: classes.dex */
    private final class LocalService extends MiuiInputManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public boolean swipe(int downX, int downY, int upX, int upY, int duration) {
            return MiuiInputShellCommand.getInstance().swipeGenerator(downX, downY, upX, upY, duration);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public boolean swipe(int downX, int downY, int upX, int upY, int duration, int everyDelayTime) {
            return MiuiInputShellCommand.getInstance().swipeGenerator(downX, downY, upX, upY, duration, everyDelayTime);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public boolean tap(int tapX, int tapY) {
            return MiuiInputShellCommand.getInstance().tapGenerator(tapX, tapY);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public boolean doubleTap(int tapX, int tapY, int duration) {
            return MiuiInputShellCommand.getInstance().doubleTapGenerator(tapX, tapY, duration);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void injectMotionEvent(MotionEvent event, int mode) {
            MiuiInputManagerService.this.mNative.injectMotionEvent(event, mode);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setInputConfig(int configType, long configNativePtr) {
            MiuiInputManagerService.this.mNative.setInputConfig(configType, configNativePtr);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void hideMouseCursor() {
            MiuiInputManagerService.this.mNative.hideMouseCursor();
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setDeviceShareListener(int pid, FileDescriptor fileDescriptor, int flags) {
            MiuiInputManagerService.this.mNative.setDeviceShareListener(pid, fileDescriptor, flags);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setTouchpadButtonState(int deviceId, boolean isDown) {
            MiuiInputManagerService.this.mNative.setTouchpadButtonState(deviceId, isDown);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setScreenState(boolean isScreenOn) {
            MiuiInputManagerService.this.setScreenState(isScreenOn);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public PointerControllerInterface obtainLaserPointerController() {
            return MiuiInputManagerService.this.obtainLaserPointerController();
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void notifyPhotoHandleConnectionStatus(boolean connection, int deviceId) {
            MiuiInputManagerService.this.mNative.notifyPhotoHandleConnectionStatus(connection, deviceId);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setDimState(boolean isDimming) {
            MiuiInputManagerService.this.mNative.setDimState(isDimming);
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setPointerIconType(int iconType) {
            InputManagerServiceStub inputManagerServiceStub = InputManagerServiceStub.getInstance();
            if (inputManagerServiceStub instanceof InputManagerServiceStubImpl) {
                InputManagerServiceStubImpl stub = (InputManagerServiceStubImpl) inputManagerServiceStub;
                stub.setPointerIconType(iconType);
            }
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public void setCustomPointerIcon(PointerIcon customPointerIcon) {
            InputManagerServiceStub inputManagerServiceStub = InputManagerServiceStub.getInstance();
            if (inputManagerServiceStub instanceof InputManagerServiceStubImpl) {
                InputManagerServiceStubImpl stub = (InputManagerServiceStubImpl) inputManagerServiceStub;
                stub.setCustomPointerIcon(customPointerIcon);
            }
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public int getCurrentPointerDisplayId() {
            return MiuiInputManagerService.this.mPointerDisplayId;
        }

        @Override // com.android.server.input.MiuiInputManagerInternal
        public List<DisplayViewport> getCurrentDisplayViewPorts() {
            return MiuiInputManagerService.this.mDisplayViewports;
        }
    }

    private void setMagicPointerPosition(float pointerX, float pointerY) {
        if (ensureMiuiMagicPointerServiceInit()) {
            return;
        }
        this.mMiuiMagicPointerService.updateMagicPointerPosition(pointerX, pointerY);
    }

    private void setMagicPointerVisibility(boolean visibility) {
        if (ensureMiuiMagicPointerServiceInit()) {
            return;
        }
        this.mMiuiMagicPointerService.setMagicPointerVisibility(visibility);
    }

    private boolean ensureMiuiMagicPointerServiceInit() {
        if (this.mMiuiMagicPointerService != null) {
            return false;
        }
        MiuiMagicPointerServiceInternal miuiMagicPointerServiceInternal = (MiuiMagicPointerServiceInternal) LocalServices.getService(MiuiMagicPointerServiceInternal.class);
        this.mMiuiMagicPointerService = miuiMagicPointerServiceInternal;
        return miuiMagicPointerServiceInternal == null;
    }

    public void dump(PrintWriter pw) {
        pw.println("MI INPUT MANAGER (dumpsys input)\n");
        String dumpStr = this.mNative.dump();
        if (dumpStr != null) {
            pw.println(dumpStr);
        }
        pw.println("Mi InputManagerService (Java) State:\n");
        MiuiDeviceShareManager.getInstance().dump(pw);
        pw.println();
        TouchWakeUpFeatureManager.getInstance().dump(pw, "");
        pw.println();
        pw.println();
    }
}

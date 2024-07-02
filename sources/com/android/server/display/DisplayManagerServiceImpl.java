package com.android.server.display;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayAddress;
import android.view.SurfaceControl;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.display.LocalDisplayAdapter;
import com.android.server.display.VirtualDisplayAdapter;
import com.android.server.display.layout.DisplayIdProducer;
import com.android.server.display.mode.DisplayModeDirector;
import com.android.server.display.mode.DisplayModeDirectorStub;
import com.android.server.display.statistics.OneTrackFoldStateHelper;
import com.android.server.power.PowerManagerServiceStub;
import com.android.server.tof.TofManagerInternal;
import com.android.server.wm.RefreshRatePolicyStub;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.base.annotations.MiuiStubHead;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import miui.security.SecurityManagerInternal;
import miui.util.MiuiMultiDisplayTypeInfo;

@MiuiStubHead(manifestName = "com.android.server.display.DisplayManagerServiceStub$$")
/* loaded from: classes.dex */
public class DisplayManagerServiceImpl extends DisplayManagerServiceStub {
    private static final int FLAG_INCREASE_SCREEN_BRIGHTNESS = 0;
    private static final int FLAG_UPDATE_DOLBY_PREVIEW_STATE = 1;
    private static final boolean IS_FOLDABLE_OR_FLIP_DEVICE;
    private static final int MSG_RESET_SHORT_MODEL = 255;
    private static final String TAG = "DisplayManagerServiceImpl";
    private boolean mBootCompleted;
    private Context mContext;
    private boolean mDebug;
    private DisplayDevice mDefaultDisplayDevice;
    private IBinder mDefaultDisplayToken;
    private LocalDisplayAdapter.LocalDisplayDevice mDefaultLocalDisplayDevice;
    private LogicalDisplay mDefaultLogicalDisplay;
    private DisplayFeatureManagerServiceImpl mDisplayFeatureManagerServiceImpl;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private Handler mHandler;
    private boolean mIsResetRate;
    private Object mLock;
    private LogicalDisplayMapper mLogicalDisplayMapper;
    private MiuiFoldPolicy mMiuiFoldPolicy;
    private int[] mOpenedReverseDeviceStates;
    private int[] mOpenedReversePresentationDeviceStates;
    private SecurityManagerInternal mSecurityManager;
    private TofManagerInternal mTofManagerInternal;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks = new HashMap<>();
    private List<String> mResolutionSwitchProcessProtectList = new ArrayList();
    private List<String> mResolutionSwitchProcessBlackList = new ArrayList();
    private List<DisplayDevice> mDisplayDevices = new ArrayList();
    private int[] mScreenEffectDisplayIndex = {0};

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayManagerServiceImpl> {

        /* compiled from: DisplayManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayManagerServiceImpl INSTANCE = new DisplayManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayManagerServiceImpl m1083provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayManagerServiceImpl m1082provideNewInstance() {
            return new DisplayManagerServiceImpl();
        }
    }

    static {
        IS_FOLDABLE_OR_FLIP_DEVICE = MiuiMultiDisplayTypeInfo.isFoldDeviceInside() || MiuiMultiDisplayTypeInfo.isFlipDevice();
    }

    public void init(Object lock, Context context, Looper looper, LogicalDisplayMapper logicalDisplayMapper) {
        this.mLock = lock;
        this.mContext = context;
        this.mHandler = new DisplayManagerStubHandler(looper);
        this.mLogicalDisplayMapper = logicalDisplayMapper;
        this.mDisplayFeatureManagerServiceImpl = (DisplayFeatureManagerServiceImpl) DisplayFeatureManagerServiceStub.getInstance();
        if (this.mContext.getResources().getBoolean(285540482)) {
            this.mMiuiFoldPolicy = new MiuiFoldPolicy(this.mContext);
        }
    }

    public static DisplayManagerServiceImpl getInstance() {
        return (DisplayManagerServiceImpl) MiuiStubUtil.getImpl(DisplayManagerServiceStub.class);
    }

    public void setUpDisplayPowerControllerImpl(DisplayPowerControllerImpl impl) {
        this.mDisplayPowerControllerImpl = impl;
    }

    public void updateDeviceDisplayChanged(DisplayDevice device, int event) {
        DisplayDeviceInfo info;
        if (device instanceof LocalDisplayAdapter.LocalDisplayDevice) {
            synchronized (this.mLock) {
                switch (event) {
                    case 1:
                        if (!this.mDisplayDevices.contains(device)) {
                            this.mDisplayDevices.add(device);
                            updateScreenEffectDisplayIndexLocked();
                            break;
                        }
                        break;
                    case 3:
                        if (this.mDisplayDevices.contains(device)) {
                            this.mDisplayDevices.remove(device);
                            updateScreenEffectDisplayIndexLocked();
                            break;
                        }
                        break;
                }
            }
            return;
        }
        if ((device instanceof VirtualDisplayAdapter.VirtualDisplayDevice) && (info = device.getDisplayDeviceInfoLocked()) != null) {
            if (this.mSecurityManager == null) {
                this.mSecurityManager = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
            }
            SecurityManagerInternal securityManagerInternal = this.mSecurityManager;
            if (securityManagerInternal != null) {
                securityManagerInternal.onDisplayDeviceEvent(info.ownerPackageName, info.name, device.getDisplayTokenLocked(), event);
            }
            WindowManagerServiceStub.get().updateScreenShareProjectFlag();
        }
    }

    private int[] updateScreenEffectDisplayIndexLocked() {
        int[] iArr;
        synchronized (this.mLock) {
            List<Integer> displayIndex = new ArrayList<>();
            for (int i = 0; i < this.mDisplayDevices.size(); i++) {
                displayIndex.add(Integer.valueOf(i));
            }
            int i2 = displayIndex.size();
            this.mScreenEffectDisplayIndex = new int[i2];
            for (int i3 = 0; i3 < displayIndex.size(); i3++) {
                this.mScreenEffectDisplayIndex[i3] = displayIndex.get(i3).intValue();
            }
            iArr = this.mScreenEffectDisplayIndex;
        }
        return iArr;
    }

    public int[] getScreenEffectAvailableDisplayInternal() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mScreenEffectDisplayIndex;
        }
        return iArr;
    }

    private int getScreenEffectDisplayIndexInternal(long physicalDisplayId) {
        IBinder displayToken = DisplayControl.getPhysicalDisplayToken(physicalDisplayId);
        synchronized (this.mLock) {
            for (int i = 0; i < this.mDisplayDevices.size(); i++) {
                if (displayToken == this.mDisplayDevices.get(i).getDisplayTokenLocked()) {
                    return i;
                }
            }
            return 0;
        }
    }

    boolean getScreenEffectAvailableDisplay(Parcel data, Parcel reply) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            int[] result = getScreenEffectAvailableDisplayInternal();
            reply.writeInt(result.length);
            reply.writeIntArray(result);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    boolean getScreenEffectDisplayIndex(Parcel data, Parcel reply) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            int result = getScreenEffectDisplayIndexInternal(data.readLong());
            reply.writeInt(result);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    boolean getDozeBrightnessThreshold(Parcel data, Parcel reply) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            float[] result = new float[0];
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                result = displayPowerControllerImpl.getDozeBrightnessThreshold();
            }
            reply.writeInt(result.length);
            reply.writeFloatArray(result);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public boolean onTransact(Handler displayControllerHandler, int code, Parcel data, Parcel reply, int flags) {
        if (code == 16777214) {
            return resetAutoBrightnessShortModel(displayControllerHandler, data);
        }
        if (code == 16777213) {
            return setBrightnessRate(data);
        }
        if (code == 16777212) {
            return getScreenEffectAvailableDisplay(data, reply);
        }
        if (code == 16777211) {
            return getScreenEffectDisplayIndex(data, reply);
        }
        if (code == 16777210) {
            return setVideoInformation(data);
        }
        if (code == 16777209) {
            return handleGalleryHdrRequest(data);
        }
        if (code == 16777208) {
            appRequestChangeSceneRefreshRate(data);
            return false;
        }
        if (code == 16777207) {
            return getDozeBrightnessThreshold(data, reply);
        }
        if (code == 16777206) {
            return updateDolbyPreviewState(data);
        }
        return false;
    }

    private boolean updateDolbyPreviewState(Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        IBinder token = data.readStrongBinder();
        boolean isDolbyPreviewEnable = data.readBoolean();
        float dolbyPreviewBoostRatio = data.readFloat();
        long ident = Binder.clearCallingIdentity();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (token != null) {
            try {
                synchronized (this.mLock) {
                    setDeathCallbackLocked(token, 1, isDolbyPreviewEnable);
                    DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
                    if (displayPowerControllerImpl != null) {
                        displayPowerControllerImpl.updateDolbyPreviewStateLocked(isDolbyPreviewEnable, dolbyPreviewBoostRatio);
                    }
                }
                Slog.w(TAG, "updateDolbyPreviewStateLocked: callingUid: " + callingUid + ", callingPid: " + callingPid + ", isDolbyPreviewEnable: " + isDolbyPreviewEnable + ", dolbyPreviewBoostRatio: " + dolbyPreviewBoostRatio);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return true;
    }

    private boolean appRequestChangeSceneRefreshRate(Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        int callingUid = Binder.getCallingUid();
        String targetPkgName = data.readString();
        int maxRefreshRate = data.readInt();
        if (callingUid < 10000) {
            long token = Binder.clearCallingIdentity();
            try {
                if (maxRefreshRate == -1) {
                    RefreshRatePolicyStub.getInstance().removeRefreshRateRangeForPackage(targetPkgName);
                } else {
                    RefreshRatePolicyStub.getInstance().addSceneRefreshRateForPackage(targetPkgName, maxRefreshRate);
                }
                Binder.restoreCallingIdentity(token);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }
        return false;
    }

    private boolean setVideoInformation(Parcel data) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.VIDEO_INFORMATION", "Permission required to set video information");
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        int pid = Binder.getCallingPid();
        boolean bulletChatStatus = data.readBoolean();
        float frameRate = data.readFloat();
        int width = data.readInt();
        int height = data.readInt();
        float compressionRatio = data.readFloat();
        IBinder token = data.readStrongBinder();
        Slog.d(TAG, "setVideoInformation bulletChatStatus:" + bulletChatStatus + ",pid:" + pid + ",token:" + token);
        if (pid > 0 && token != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    this.mDisplayFeatureManagerServiceImpl.setVideoInformation(pid, bulletChatStatus, frameRate, width, height, compressionRatio, token);
                    Binder.restoreCallingIdentity(ident);
                    return true;
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } else {
            return false;
        }
    }

    boolean resetAutoBrightnessShortModel(Handler displayControllerHandler, Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        resetAutoBrightnessShortModelInternal(displayControllerHandler);
        return true;
    }

    private void resetAutoBrightnessShortModelInternal(Handler displayControllerHandler) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Only system uid can reset Short Model!");
        }
        Slog.d(TAG, "reset AutoBrightness ShortModel");
        long token = Binder.clearCallingIdentity();
        if (displayControllerHandler != null) {
            try {
                displayControllerHandler.obtainMessage(MSG_RESET_SHORT_MODEL).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private boolean setBrightnessRate(Parcel data) {
        int uid = Binder.getCallingUid();
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            this.mIsResetRate = data.readBoolean();
            Slog.d(TAG, "setBrightnessRate, uid: " + uid + ", mIsResetRate: " + this.mIsResetRate);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public boolean getIsResetRate() {
        return this.mIsResetRate;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Object getSyncRoot() {
        return this.mLock;
    }

    public void updateRhythmicAppCategoryList(List<String> imageAppList, List<String> readAppList) {
        this.mDisplayFeatureManagerServiceImpl.updateRhythmicAppCategoryList(imageAppList, readAppList);
    }

    public void updateResolutionSwitchList(final List<String> resolutionSwitchProtectList, final List<String> resolutionSwitchBlackList) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayManagerServiceImpl.this.lambda$updateResolutionSwitchList$0(resolutionSwitchBlackList, resolutionSwitchProtectList);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateResolutionSwitchList$0(List resolutionSwitchBlackList, List resolutionSwitchProtectList) {
        this.mResolutionSwitchProcessBlackList.clear();
        this.mResolutionSwitchProcessProtectList.clear();
        this.mResolutionSwitchProcessBlackList.addAll(resolutionSwitchBlackList);
        this.mResolutionSwitchProcessProtectList.addAll(resolutionSwitchProtectList);
    }

    public boolean isInResolutionSwitchProtectList(String processName) {
        return this.mResolutionSwitchProcessProtectList.contains(processName);
    }

    public boolean isInResolutionSwitchBlackList(String processName) {
        return this.mResolutionSwitchProcessBlackList.contains(processName);
    }

    public boolean isDisplayGroupAlwaysOn(int groupId) {
        boolean alwaysOn;
        if (groupId == 0) {
            return false;
        }
        synchronized (this.mLock) {
            DisplayGroup displayGroup = this.mLogicalDisplayMapper.getDisplayGroupLocked(groupId);
            alwaysOn = false;
            if (displayGroup != null) {
                int size = displayGroup.getSizeLocked();
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    int id = displayGroup.getIdLocked(i);
                    LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(id, false);
                    if (display == null || (display.getDisplayInfoLocked().flags & 512) == 0) {
                        i++;
                    } else {
                        alwaysOn = true;
                        break;
                    }
                }
            }
        }
        return alwaysOn;
    }

    public void setSceneMaxRefreshRate(int displayId, float maxFrameRate) {
        synchronized (this.mLock) {
            LogicalDisplay display = this.mLogicalDisplayMapper.getDisplayLocked(displayId);
            if (display == null) {
                return;
            }
            DisplayModeDirectorStub.getInstance().setSceneMaxRefreshRate(displayId, maxFrameRate);
        }
    }

    /* loaded from: classes.dex */
    private class DisplayManagerStubHandler extends Handler {
        public DisplayManagerStubHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    public SurfaceControl.DynamicDisplayInfo updateDefaultDisplaySupportMode() {
        synchronized (this.mLock) {
            updateDefaultDisplayLocked();
            if (this.mDefaultLocalDisplayDevice == null) {
                return null;
            }
            DisplayAddress.Physical physicalAddress = this.mDefaultLogicalDisplay.getDisplayInfoLocked().address;
            long physicalDisplayId = physicalAddress.getPhysicalDisplayId();
            return SurfaceControl.getDynamicDisplayInfo(physicalDisplayId);
        }
    }

    public void shouldUpdateDisplayModeSpecs(SurfaceControl.DesiredDisplayModeSpecs modeSpecs) {
        IBinder iBinder;
        LocalDisplayAdapter.LocalDisplayDevice localDisplayDevice = this.mDefaultLocalDisplayDevice;
        if (localDisplayDevice == null || (iBinder = this.mDefaultDisplayToken) == null) {
            return;
        }
        localDisplayDevice.setDesiredDisplayModeSpecsAsync(iBinder, modeSpecs);
        synchronized (this.mLock) {
            DisplayModeDirector.DesiredDisplayModeSpecs desired = new DisplayModeDirector.DesiredDisplayModeSpecs(modeSpecs.defaultMode, modeSpecs.allowGroupSwitching, modeSpecs.primaryRanges, modeSpecs.appRequestRanges);
            this.mDefaultLocalDisplayDevice.updateDesiredDisplayModeSpecsLocked(desired);
            this.mDefaultLogicalDisplay.setDesiredDisplayModeSpecsLocked(desired);
            DisplayModeDirectorStub.getInstance().onDesiredDisplayModeSpecsChanged(this.mDefaultLogicalDisplay.getDisplayIdLocked(), desired, (SparseArray) null);
        }
    }

    private void updateDefaultDisplayLocked() {
        LogicalDisplayMapper logicalDisplayMapper = this.mLogicalDisplayMapper;
        if (logicalDisplayMapper == null) {
            return;
        }
        LogicalDisplay displayLocked = logicalDisplayMapper.getDisplayLocked(0);
        this.mDefaultLogicalDisplay = displayLocked;
        if (displayLocked == null) {
            Slog.e(TAG, "get default display error");
        }
        LocalDisplayAdapter.LocalDisplayDevice primaryDisplayDeviceLocked = this.mDefaultLogicalDisplay.getPrimaryDisplayDeviceLocked();
        this.mDefaultDisplayDevice = primaryDisplayDeviceLocked;
        if (primaryDisplayDeviceLocked instanceof LocalDisplayAdapter.LocalDisplayDevice) {
            this.mDefaultLocalDisplayDevice = primaryDisplayDeviceLocked;
        }
        this.mDefaultDisplayToken = primaryDisplayDeviceLocked.getDisplayTokenLocked();
    }

    private boolean handleGalleryHdrRequest(Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        IBinder token = data.readStrongBinder();
        boolean increaseScreenBrightness = data.readBoolean();
        requestGalleryHdrBoost(token, increaseScreenBrightness);
        return true;
    }

    private void requestGalleryHdrBoost(IBinder token, boolean enable) {
        long ident = Binder.clearCallingIdentity();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (token != null) {
            try {
                synchronized (this.mLock) {
                    setDeathCallbackLocked(token, 0, enable);
                    DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
                    if (displayPowerControllerImpl != null) {
                        displayPowerControllerImpl.updateGalleryHdrState(enable);
                    }
                }
                Slog.w(TAG, "requestGalleryHdrBoost: callingUid: " + callingUid + ", callingPid: " + callingPid + ", enable: " + enable);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void setDeathCallbackLocked(IBinder token, int flag, boolean enable) {
        if (enable) {
            registerDeathCallbackLocked(token, flag);
        } else {
            unregisterDeathCallbackLocked(token);
        }
    }

    protected void registerDeathCallbackLocked(IBinder token, int flag) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.w(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token, flag));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        ClientDeathCallback deathCallback;
        if (token != null && (deathCallback = this.mClientDeathCallbacks.remove(token)) != null) {
            token.unlinkToDeath(deathCallback, 0);
        }
    }

    public boolean isDolbyPreviewEnable() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.isDolbyPreviewEnable();
        }
        return false;
    }

    public float getDolbyPreviewBoostRatio() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getDolbyPreviewBoostRatio();
        }
        return Float.NaN;
    }

    public boolean isGalleryHdrEnable() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.isGalleryHdrEnable();
        }
        return false;
    }

    public boolean isSupportPeakBrightness() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.isSupportPeakBrightness();
        }
        return false;
    }

    public float getMaxHbmBrightnessForPeak() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getMaxHbmBrightnessForPeak();
        }
        return 1.0f;
    }

    public boolean isSupportManualBrightnessBoost() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.isSupportManualBrightnessBoost();
        }
        return false;
    }

    public float getMaxManualBrightnessBoost() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getMaxManualBrightnessBoost();
        }
        return -1.0f;
    }

    public float getGalleryHdrBoostFactor(float sdrBacklight, float hdrBacklight) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getGalleryHdrBoostFactor(sdrBacklight, hdrBacklight);
        }
        return 1.0f;
    }

    public void temporaryBrightnessStartChange(float brightness) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.temporaryBrightnessStartChange(brightness);
        }
    }

    public void settingBrightnessStartChange(float brightness) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.settingBrightnessStartChange(brightness);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doDieLocked(int flag, IBinder token) {
        if (flag == 0) {
            unregisterDeathCallbackLocked(token);
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                displayPowerControllerImpl.updateGalleryHdrState(false);
                return;
            }
            return;
        }
        if (flag == 1) {
            unregisterDeathCallbackLocked(token);
            DisplayPowerControllerImpl displayPowerControllerImpl2 = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl2 != null) {
                displayPowerControllerImpl2.updateDolbyPreviewStateLocked(false, Float.NaN);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private int mFlag;
        private IBinder mToken;

        public ClientDeathCallback(DisplayManagerServiceImpl this$0, IBinder token) {
            this(token, 0);
        }

        public ClientDeathCallback(IBinder token, int flag) {
            this.mToken = token;
            this.mFlag = flag;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(DisplayManagerServiceImpl.TAG, "binderDied: flag: " + this.mFlag);
            synchronized (DisplayManagerServiceImpl.this.mLock) {
                DisplayManagerServiceImpl.this.doDieLocked(this.mFlag, this.mToken);
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("DisplayManagerServiceImpl Configuration:");
        this.mDebug = DisplayDebugConfig.DEBUG_DMS;
    }

    public void onBootCompleted() {
        this.mTofManagerInternal = (TofManagerInternal) LocalServices.getService(TofManagerInternal.class);
        this.mBootCompleted = true;
        MiuiFoldPolicy miuiFoldPolicy = this.mMiuiFoldPolicy;
        if (miuiFoldPolicy != null) {
            miuiFoldPolicy.initMiuiFoldPolicy();
        }
    }

    public void notifyFinishDisplayTransitionLocked() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.dealDisplayTransition();
    }

    public void screenTurningOn() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.screenTurningOn();
    }

    public void screenTurningOff() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.screenTurningOff();
    }

    public void finishedGoingToSleep() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.notifyFinishedGoingToSleep();
    }

    public void onEarlyInteractivityChange(boolean interactive) {
        if (this.mBootCompleted) {
            TofManagerInternal tofManagerInternal = this.mTofManagerInternal;
            if (tofManagerInternal != null) {
                tofManagerInternal.onEarlyInteractivityChange(interactive);
            }
            if (IS_FOLDABLE_OR_FLIP_DEVICE) {
                OneTrackFoldStateHelper.getInstance().onEarlyInteractivityChange(interactive);
            }
        }
    }

    public void notifyHotplugConnectStateChangedLocked(long physicalDisplayId, boolean isConnected, LocalDisplayAdapter.LocalDisplayDevice device) {
        String productName = null;
        int frameRate = 0;
        String resolution = null;
        if (device != null && isConnected) {
            DisplayDeviceInfo deviceInfo = device.getDisplayDeviceInfoLocked();
            if (deviceInfo.deviceProductInfo != null) {
                productName = deviceInfo.deviceProductInfo.getName();
            }
            frameRate = (int) deviceInfo.renderFrameRate;
            resolution = deviceInfo.width + " x " + deviceInfo.height;
        }
        PowerManagerServiceStub.get().notifyDisplayPortConnectStateChanged(physicalDisplayId, isConnected, productName, frameRate, resolution);
    }

    public void setDeviceStateLocked(int state) {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.setDeviceStateLocked(state);
        OneTrackFoldStateHelper.getInstance().notifyDeviceStateChanged(state);
    }

    public void startCbmStatsJob() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.startCbmStatsJob();
        }
    }

    private boolean setCustomCurveEnabledOnCommand(boolean enable) {
        long token = Binder.clearCallingIdentity();
        try {
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                displayPowerControllerImpl.setCustomCurveEnabledOnCommand(enable);
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean setIndividualModelEnabledOnCommand(boolean enable) {
        long token = Binder.clearCallingIdentity();
        try {
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                displayPowerControllerImpl.setIndividualModelEnabledOnCommand(enable);
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean setForceTrainEnabledOnCommand(boolean enable) {
        long token = Binder.clearCallingIdentity();
        try {
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                displayPowerControllerImpl.setForceTrainEnabledOnCommand(enable);
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean showTouchCoverProtectionRect(boolean enable) {
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mDisplayPowerControllerImpl != null && Build.isDebuggable()) {
                this.mDisplayPowerControllerImpl.showTouchCoverProtectionRect(enable);
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public boolean onCommand(String cmd) {
        char c;
        switch (cmd.hashCode()) {
            case -1524717767:
                if (cmd.equals("set-custom-curve-disable")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1310065936:
                if (cmd.equals("set-individual-model-enable")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -932717866:
                if (cmd.equals("set-force-train-disable")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -709209326:
                if (cmd.equals("set-custom-curve-enable")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -690112555:
                if (cmd.equals("set-force-train-enable")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -473983708:
                if (cmd.equals("touch-cover-protect-hide")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -473656609:
                if (cmd.equals("touch-cover-protect-show")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1323563803:
                if (cmd.equals("set-individual-model-disable")) {
                    c = 3;
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
                return setCustomCurveEnabledOnCommand(true);
            case 1:
                return setCustomCurveEnabledOnCommand(false);
            case 2:
                return setIndividualModelEnabledOnCommand(true);
            case 3:
                return setIndividualModelEnabledOnCommand(false);
            case 4:
                return setForceTrainEnabledOnCommand(true);
            case 5:
                return setForceTrainEnabledOnCommand(false);
            case 6:
                return showTouchCoverProtectionRect(true);
            case 7:
                return showTouchCoverProtectionRect(false);
            default:
                return false;
        }
    }

    public void onHelp(PrintWriter pw) {
        if (!Build.isDebuggable()) {
            return;
        }
        pw.println("  set-custom-curve-[enable|disable]");
        pw.println("    Enable or disable custom curve");
        pw.println("  set-individual-model-[enable|disable]");
        pw.println("    Enable or disable individual model");
        pw.println("  set-force-train-[enable|disable]");
        pw.println("    Enable or disable force train");
        pw.println("  touch-cover-protect-[show|hide]");
        pw.println("    Show or hide the touch cover protection rect");
    }

    public int getMaskedWidth(Rect maskingInsets, DisplayDeviceInfo deviceInfo) {
        int[] screenResolution;
        int deviceInfoWidth = deviceInfo.width;
        if (WindowManagerServiceStub.get().isSupportSetActiveModeSwitchResolution() && deviceInfo.type == 1 && (screenResolution = WindowManagerServiceStub.get().getUserSetResolution()) != null) {
            deviceInfoWidth = screenResolution[0];
        }
        if ("cetus".equals(Build.DEVICE) && deviceInfo.type == 1 && (deviceInfo.flags & 1048576) == 0) {
            deviceInfoWidth = 880;
        }
        return (deviceInfoWidth - maskingInsets.left) - maskingInsets.right;
    }

    public int getMaskedHeight(Rect maskingInsets, DisplayDeviceInfo deviceInfo) {
        int[] screenResolution;
        int deviceInfoHeight = deviceInfo.height;
        if (WindowManagerServiceStub.get().isSupportSetActiveModeSwitchResolution() && deviceInfo.type == 1 && (screenResolution = WindowManagerServiceStub.get().getUserSetResolution()) != null) {
            deviceInfoHeight = screenResolution[1];
        }
        if ("cetus".equals(Build.DEVICE) && deviceInfo.type == 1 && (deviceInfo.flags & 1048576) == 0) {
            deviceInfoHeight = 2640;
        }
        return (deviceInfoHeight - maskingInsets.top) - maskingInsets.bottom;
    }

    public int getMaskedDensity(Rect maskingInsets, DisplayDeviceInfo deviceInfo) {
        int density;
        int maskedDensity = deviceInfo.densityDpi;
        if (WindowManagerServiceStub.get().isSupportSetActiveModeSwitchResolution() && deviceInfo.type == 1 && (density = WindowManagerServiceStub.get().getDefaultDensity()) != -1) {
            return density;
        }
        return maskedDensity;
    }

    public void addDisplayGroupManuallyIfNeededLocked(CopyOnWriteArrayList<DisplayManagerInternal.DisplayGroupListener> mDisplayGroupListeners) {
        if ("star".equals(Build.DEVICE)) {
            Slog.d(TAG, "initPowerManagement: Manually set display group");
            Iterator<DisplayManagerInternal.DisplayGroupListener> it = mDisplayGroupListeners.iterator();
            while (it.hasNext()) {
                DisplayManagerInternal.DisplayGroupListener listener = it.next();
                listener.onDisplayGroupAdded(1);
            }
        }
    }

    public boolean shouldDeviceKeepWake(int deviceState) {
        if (this.mOpenedReverseDeviceStates == null || this.mOpenedReversePresentationDeviceStates == null) {
            this.mOpenedReverseDeviceStates = this.mContext.getResources().getIntArray(285409351);
            this.mOpenedReversePresentationDeviceStates = this.mContext.getResources().getIntArray(285409352);
        }
        return deviceState == -1 || ArrayUtils.contains(this.mOpenedReverseDeviceStates, deviceState) || ArrayUtils.contains(this.mOpenedReversePresentationDeviceStates, deviceState);
    }

    public int getDefaultSleepFlags() {
        return 0;
    }

    public int getLogicalDisplayId(boolean isDefault, DisplayIdProducer idProducer) {
        if (IS_FOLDABLE_OR_FLIP_DEVICE) {
            return idProducer.getId(isDefault);
        }
        return -1;
    }
}

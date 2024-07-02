package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

/* JADX INFO: Access modifiers changed from: package-private */
@MiuiStubHead(manifestName = "com.android.server.MiuiPaperContrastOverlayStub$$")
/* loaded from: classes.dex */
public class MiuiPaperContrastOverlayStubImpl extends MiuiPaperContrastOverlayStub {
    private static final String TAG = "MiuiPaperContrastOverlayStubImpl";
    Context mContext;
    private boolean mFoldDeviceReady;
    MiuiPaperContrastOverlay mMiuiPaperSurface;
    WindowManagerService mWmService;
    private final Uri mTextureEyeCareLevelUri = Settings.System.getUriFor("screen_texture_eyecare_level");
    private final Uri mPaperModeTypeUri = Settings.System.getUriFor("screen_mode_type");
    private final Uri mPaperModeEnableUri = Settings.System.getUriFor("screen_paper_mode_enabled");
    private final Uri mSecurityModeVtbUri = Settings.Secure.getUriFor(MiuiFreeFormGestureController.VTB_BOOSTING);
    private final Uri mSecurityModeGbUri = Settings.Secure.getUriFor(MiuiFreeFormGestureController.GB_BOOSTING);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiPaperContrastOverlayStubImpl> {

        /* compiled from: MiuiPaperContrastOverlayStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiPaperContrastOverlayStubImpl INSTANCE = new MiuiPaperContrastOverlayStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiPaperContrastOverlayStubImpl m2640provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiPaperContrastOverlayStubImpl m2639provideNewInstance() {
            return new MiuiPaperContrastOverlayStubImpl();
        }
    }

    MiuiPaperContrastOverlayStubImpl() {
    }

    public void init(WindowManagerService wms, Context context) {
        this.mContext = context;
        this.mWmService = wms;
        registerObserver(context);
        updateTextureEyeCareLevel(getSecurityCenterStatus());
    }

    public void updateTextureEyeCareWhenAodShow(boolean aodShowing) {
        updateTextureEyeCareLevel(!aodShowing && getSecurityCenterStatus());
    }

    public void updateTextureEyeCareLevel(final boolean eyecareEnable) {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.MiuiPaperContrastOverlayStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiPaperContrastOverlayStubImpl.this.lambda$updateTextureEyeCareLevel$0(eyecareEnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateTextureEyeCareLevel$0(boolean eyecareEnable) {
        synchronized (this.mWmService.mWindowMap) {
            this.mWmService.openSurfaceTransaction();
            try {
                int paperModeType = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_mode_type", 0, -2);
                boolean paperModeEnable = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), "screen_paper_mode_enabled", false, -2);
                Slog.d(TAG, "Update paper-mode, param is " + eyecareEnable);
                if (eyecareEnable && paperModeEnable && (paperModeType == 1 || paperModeType == 3)) {
                    if (this.mMiuiPaperSurface == null) {
                        this.mMiuiPaperSurface = MiuiPaperContrastOverlay.getInstance(this.mWmService, this.mContext);
                    }
                    if (MiuiPaperContrastOverlay.IS_FOLDABLE_DEVICE && this.mFoldDeviceReady) {
                        this.mMiuiPaperSurface.changeDeviceReady();
                    }
                    this.mMiuiPaperSurface.changeShowLayerStatus(true);
                    Slog.d(TAG, "Show paper-mode surface.");
                    this.mMiuiPaperSurface.showPaperModeSurface();
                } else {
                    MiuiPaperContrastOverlay miuiPaperContrastOverlay = this.mMiuiPaperSurface;
                    if (miuiPaperContrastOverlay != null) {
                        miuiPaperContrastOverlay.changeShowLayerStatus(false);
                        this.mMiuiPaperSurface.hidePaperModeSurface();
                    }
                    this.mMiuiPaperSurface = null;
                }
            } finally {
                this.mWmService.closeSurfaceTransaction("updateTextureEyeCareLevel");
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void registerObserver(Context context) {
        ContentObserver contentObserver = new ContentObserver(null) { // from class: com.android.server.wm.MiuiPaperContrastOverlayStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                boolean textureEyecareEnable = MiuiPaperContrastOverlayStubImpl.this.getSecurityCenterStatus();
                if (MiuiPaperContrastOverlayStubImpl.this.mTextureEyeCareLevelUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mPaperModeTypeUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mPaperModeEnableUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mSecurityModeVtbUri.equals(uri) || MiuiPaperContrastOverlayStubImpl.this.mSecurityModeGbUri.equals(uri)) {
                    Slog.d(MiuiPaperContrastOverlayStubImpl.TAG, "registerObserver uri:" + uri + ", enable:" + textureEyecareEnable);
                    MiuiPaperContrastOverlayStubImpl.this.mFoldDeviceReady = true;
                    MiuiPaperContrastOverlayStubImpl.this.updateTextureEyeCareLevel(textureEyecareEnable);
                }
            }
        };
        context.getContentResolver().registerContentObserver(this.mTextureEyeCareLevelUri, false, contentObserver, -1);
        context.getContentResolver().registerContentObserver(this.mPaperModeTypeUri, false, contentObserver, -1);
        context.getContentResolver().registerContentObserver(this.mPaperModeEnableUri, false, contentObserver, -1);
        context.getContentResolver().registerContentObserver(this.mSecurityModeVtbUri, false, contentObserver, -1);
        context.getContentResolver().registerContentObserver(this.mSecurityModeGbUri, false, contentObserver, -1);
        contentObserver.onChange(false);
        this.mContext.registerReceiver(new UserSwitchReceivere(), new IntentFilter("android.intent.action.USER_SWITCHED"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getSecurityCenterStatus() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), MiuiFreeFormGestureController.GB_BOOSTING, 0, -2) == 0 && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), MiuiFreeFormGestureController.VTB_BOOSTING, 0, -2) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UserSwitchReceivere extends BroadcastReceiver {
        private UserSwitchReceivere() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiPaperContrastOverlayStubImpl miuiPaperContrastOverlayStubImpl = MiuiPaperContrastOverlayStubImpl.this;
            miuiPaperContrastOverlayStubImpl.updateTextureEyeCareLevel(miuiPaperContrastOverlayStubImpl.getSecurityCenterStatus());
        }
    }

    public SurfaceControl getMiuiPaperSurfaceControl() {
        MiuiPaperContrastOverlay miuiPaperContrastOverlay = this.mMiuiPaperSurface;
        if (miuiPaperContrastOverlay != null) {
            return miuiPaperContrastOverlay.getSurfaceControl();
        }
        return null;
    }
}

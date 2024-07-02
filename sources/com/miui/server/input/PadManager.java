package com.miui.server.input;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.padkeyboard.MiuiIICKeyboardManager;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import miui.hardware.input.MiuiKeyboardHelper;
import miui.os.Build;

/* loaded from: classes.dex */
public class PadManager {
    private static final String IIC_MI_MEDIA_KEYBOARD_NAME = "Xiaomi Consumer";
    private static final String TAG = "PadManager";
    private static volatile PadManager sIntance;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private final Handler mHandler;
    private volatile boolean mIsCapsLock;
    private boolean mIsUserSetup;
    private MiuiPadSettingsObserver mMiuiPadSettingsObserver;
    private boolean mRunning;
    private volatile boolean mIsLidOpen = true;
    private volatile boolean mIsTabletOpen = true;
    private final Object mLock = new Object();

    private PadManager() {
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mContext = systemContext;
        this.mHandler = new H(MiuiInputThread.getHandler().getLooper());
        this.mAudioManager = (AudioManager) systemContext.getSystemService("audio");
    }

    public static PadManager getInstance() {
        if (sIntance == null) {
            synchronized (PadManager.class) {
                if (sIntance == null) {
                    sIntance = new PadManager();
                }
            }
        }
        return sIntance;
    }

    public boolean isPad() {
        return Build.IS_TABLET;
    }

    public void registerPadSettingsObserver() {
        MiuiPadSettingsObserver miuiPadSettingsObserver = new MiuiPadSettingsObserver(MiuiInputThread.getHandler());
        this.mMiuiPadSettingsObserver = miuiPadSettingsObserver;
        miuiPadSettingsObserver.observer();
    }

    public void setIsLidOpen(boolean isLidOpen) {
        this.mIsLidOpen = isLidOpen;
    }

    public void setIsTableOpen(boolean isTabletOpen) {
        this.mIsTabletOpen = isTabletOpen;
    }

    public boolean padLidInterceptWakeKey(KeyEvent event) {
        return isPad() && !(this.mIsLidOpen && this.mIsTabletOpen) && isKeyFromKeyboard(event);
    }

    private boolean isKeyFromKeyboard(KeyEvent event) {
        InputDevice device = event.getDevice();
        return device != null && MiuiKeyboardHelper.isXiaomiWakeUpDevice(device.getProductId(), device.getVendorId());
    }

    public boolean adjustBrightnessFromKeycode(KeyEvent event) {
        if (event.getKeyCode() == 220 || event.getKeyCode() == 221) {
            if (event.getAction() == 0) {
                this.mRunning = true;
                Message msg = this.mHandler.obtainMessage(99);
                Bundle bundle = new Bundle();
                bundle.putParcelable("brightness_key", event);
                bundle.putInt("delay_adjust", 200);
                msg.setData(bundle);
                this.mHandler.sendMessage(msg);
                if (this.mIsUserSetup) {
                    this.mContext.startActivityAsUser(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG"), null, UserHandle.CURRENT_OR_SELF);
                }
            } else {
                this.mRunning = false;
                this.mHandler.removeCallbacksAndMessages(null);
            }
            Slog.i(TAG, "handle brightness key for miui");
            return true;
        }
        this.mRunning = false;
        this.mHandler.removeCallbacksAndMessages(null);
        return false;
    }

    public void notifySystemBooted() {
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            registerPadSettingsObserver();
        }
    }

    public synchronized boolean getCapsLockStatus() {
        return this.mIsCapsLock;
    }

    public synchronized void setCapsLockStatus(boolean isCapsLock) {
        this.mIsCapsLock = isCapsLock;
    }

    public boolean getMuteStatus() {
        return this.mAudioManager.isMicrophoneMute();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiPadSettingsObserver extends ContentObserver {
        MiuiPadSettingsObserver(Handler handler) {
            super(handler);
        }

        void observer() {
            ContentResolver resolver = PadManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            onChange(false, Settings.Secure.getUriFor("user_setup_complete"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = PadManager.this.mContext.getContentResolver();
            if (Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                PadManager.this.mIsUserSetup = Settings.Secure.getIntForUser(resolver, "user_setup_complete", 0, UserHandle.myUserId()) != 0;
            }
        }
    }

    /* loaded from: classes.dex */
    class H extends Handler {
        private static final float BRIGHTNESS_STEP = 0.059f;
        private static final String DATA_DELAY_TIME = "delay_adjust";
        private static final String DATA_KEYEVENT = "brightness_key";
        private static final int MSG_ADJUST_BRIGHTNESS = 99;

        H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            float gammaValue;
            if (msg.what == 99) {
                KeyEvent event = (KeyEvent) msg.getData().getParcelable(DATA_KEYEVENT);
                int keyCode = event.getKeyCode();
                if (keyCode != 220 && keyCode != 221) {
                    Slog.i(PadManager.TAG, "Exception event for Bright:" + keyCode);
                    return;
                }
                int auto = Settings.System.getIntForUser(PadManager.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                if (auto != 0) {
                    Settings.System.putIntForUser(PadManager.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                }
                int delayTime = msg.getData().getInt(DATA_DELAY_TIME);
                DisplayManager displayManager = (DisplayManager) PadManager.this.mContext.getSystemService(DisplayManager.class);
                float nowBrightness = displayManager.getBrightness(0);
                float gammaValue2 = BrightnessUtils.convertLinearToGamma(nowBrightness);
                if (keyCode != 220) {
                    gammaValue = gammaValue2 + BRIGHTNESS_STEP;
                } else {
                    gammaValue = gammaValue2 - BRIGHTNESS_STEP;
                }
                float linearValue = BrightnessUtils.convertGammaToLinear(gammaValue);
                displayManager.setBrightness(0, linearValue);
                Slog.i(PadManager.TAG, "set Brightness :" + linearValue);
                if (PadManager.this.mRunning) {
                    Message delayMsg = PadManager.this.mHandler.obtainMessage(99);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(DATA_KEYEVENT, event);
                    bundle.putInt(DATA_DELAY_TIME, 50);
                    delayMsg.setData(bundle);
                    PadManager.this.mHandler.sendMessageDelayed(delayMsg, delayTime);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static class BrightnessUtils {
        private static final float A = 0.17883277f;
        private static final float B = 0.28466892f;
        private static final float C = 0.5599107f;
        private static final float R = 0.5f;

        private BrightnessUtils() {
        }

        public static final float convertGammaToLinear(float val) {
            float ret;
            if (val <= 0.5f) {
                ret = MathUtils.sq(val / 0.5f);
            } else {
                ret = MathUtils.exp((val - C) / A) + B;
            }
            float normalizedRet = MathUtils.constrain(ret, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 12.0f);
            return normalizedRet / 12.0f;
        }

        public static final float convertLinearToGamma(float val) {
            float normalizedVal = 12.0f * val;
            if (normalizedVal <= 1.0f) {
                float ret = MathUtils.sqrt(normalizedVal) * 0.5f;
                return ret;
            }
            float ret2 = (MathUtils.log(normalizedVal - B) * A) + C;
            return ret2;
        }
    }

    public void registerPointerEventListener() {
        MiuiGestureMonitor.getInstance(this.mContext).registerPointerEventListener(new TrackMotionListener());
    }

    /* loaded from: classes.dex */
    class TrackMotionListener implements MiuiGestureListener {
        TrackMotionListener() {
        }

        @Override // com.miui.server.input.gesture.MiuiGestureListener
        public void onPointerEvent(MotionEvent motionEvent) {
            if (InputOneTrackUtil.shouldCountDevice(motionEvent)) {
                InputOneTrackUtil.getInstance(PadManager.this.mContext).trackExternalDevice(motionEvent);
            }
        }
    }
}

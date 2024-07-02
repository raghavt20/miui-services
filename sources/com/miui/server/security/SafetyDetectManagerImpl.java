package com.miui.server.security;

import android.os.Binder;
import android.os.SystemClock;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.HashMap;
import java.util.Map;

@MiuiStubHead(manifestName = "com.miui.server.security.SafetyDetectManagerStub$$")
/* loaded from: classes.dex */
public class SafetyDetectManagerImpl extends SafetyDetectManagerStub {
    private static final String TAG = "SafetyDetectManagerImpl";
    private static final String TRUE = "true";
    private boolean mDetectAccessibilityTouch = true;
    private boolean mDetectAdbInput = true;
    private long mLastSimulatedTouchTime;
    private int mLastSimulatedTouchUid;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SafetyDetectManagerImpl> {

        /* compiled from: SafetyDetectManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SafetyDetectManagerImpl INSTANCE = new SafetyDetectManagerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public SafetyDetectManagerImpl m3291provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public SafetyDetectManagerImpl m3290provideNewInstance() {
            throw new RuntimeException("Impl class com.miui.server.security.SafetyDetectManagerImpl is marked as singleton");
        }
    }

    public void stampShellInjectInputEvent(long downTime) {
        if (!this.mDetectAdbInput) {
            return;
        }
        int callingUid = Binder.getCallingUid();
        this.mLastSimulatedTouchTime = downTime;
        this.mLastSimulatedTouchUid = callingUid;
    }

    public void stampAccessibilityDispatchGesture(int uid) {
        if (!this.mDetectAccessibilityTouch) {
            return;
        }
        this.mLastSimulatedTouchTime = SystemClock.uptimeMillis();
        this.mLastSimulatedTouchUid = uid;
    }

    public void stampPerformAccessibilityAction(int uid) {
        if (!this.mDetectAccessibilityTouch) {
            return;
        }
        this.mLastSimulatedTouchTime = SystemClock.uptimeMillis();
        this.mLastSimulatedTouchUid = uid;
    }

    public Map<String, String> getSimulatedTouchInfo() {
        Map<String, String> simulatedTouchInfo = new HashMap<>();
        if (this.mDetectAccessibilityTouch || this.mDetectAdbInput) {
            simulatedTouchInfo.put("lastSimulatedTouchTime", String.valueOf(this.mLastSimulatedTouchTime));
            simulatedTouchInfo.put("lastSimulatedTouchUid", String.valueOf(this.mLastSimulatedTouchUid));
        }
        return simulatedTouchInfo;
    }

    public void switchSimulatedTouchDetect(Map<String, String> config) {
        try {
            this.mDetectAdbInput = Boolean.parseBoolean(config.getOrDefault("detectAdpInput", TRUE));
            this.mDetectAccessibilityTouch = Boolean.parseBoolean(config.getOrDefault("detectAccessibilityTouch", TRUE));
        } catch (Exception e) {
            Slog.e(TAG, "switchSimulatedTouchDetect exception: " + e);
        }
        Slog.i(TAG, "mDetectAdbInput = " + this.mDetectAdbInput + "\tmDetectAccessibilityTouch = " + this.mDetectAccessibilityTouch);
    }
}

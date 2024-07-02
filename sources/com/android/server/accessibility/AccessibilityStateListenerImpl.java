package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.accessibility.util.AccessibilityUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.accessibility.AccessibilityStateListenerStub$$")
/* loaded from: classes.dex */
public class AccessibilityStateListenerImpl extends AccessibilityStateListenerStub {
    private static final String DIANMING_SERVICE = "com.dianming.phoneapp/com.dianming.phoneapp.MyAccessibilityService";
    private static final String DIANMING_SERVICE_OMIT = "com.dianming.phoneapp/.MyAccessibilityService";
    private static final String MIUI_ENHANCE_TALKBACK = "com.miui.accessibility/com.miui.accessibility.enhance.tb.MiuiEnhanceTBService";
    private static final String NIRENR_SERVICE = "com.nirenr.talkman/com.nirenr.talkman.TalkManAccessibilityService";
    private static final String NIRENR_SERVICE_OMIT = "com.nirenr.talkman/.TalkManAccessibilityService";
    private static final String TAG = "AccessibilityStateListenerImpl";
    private static final String TALKBACK_SERVICE = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private static final String TALKBACK_SERVICE_OMIT = "com.google.android.marvin.talkback/.TalkBackService";
    private static final String TBACK_SERVICE = "com.android.tback/net.tatans.soundback.SoundBackService";
    private static final String VOICEBACK_SERVICE = "com.bjbyhd.voiceback/com.bjbyhd.voiceback.BoyhoodVoiceBackService";
    private static final String VOICEBACK_SERVICE_OMIT = "com.bjbyhd.voiceback/.BoyhoodVoiceBackService";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AccessibilityStateListenerImpl> {

        /* compiled from: AccessibilityStateListenerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AccessibilityStateListenerImpl INSTANCE = new AccessibilityStateListenerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AccessibilityStateListenerImpl m282provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AccessibilityStateListenerImpl m281provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.accessibility.AccessibilityStateListenerImpl is marked as singleton");
        }
    }

    public void sendStateToClients(Context context, int stateFlags) {
        if (context == null) {
            return;
        }
        try {
            AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
            if (accessibilityManager == null) {
                return;
            }
            if (!isMiuiEnhanceTBServiceInstalled(accessibilityManager)) {
                Log.e(TAG, "MiuiEnhanceTBService is not installed");
                return;
            }
            String enabledServices = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
            boolean enabled = isNecessaryEnable(enabledServices);
            if (enabled != AccessibilityUtils.isAccessibilityServiceEnabled(context, MIUI_ENHANCE_TALKBACK)) {
                Log.d(TAG, "onAccessibilityStateChanged: " + enabled);
                AccessibilityUtils.setAccessibilityServiceState(context, ComponentName.unflattenFromString(MIUI_ENHANCE_TALKBACK), enabled);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown error found - " + e.getMessage(), e);
        }
    }

    private boolean isNecessaryEnable(String enabledServices) {
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        return enabledServices.contains(TALKBACK_SERVICE) || enabledServices.contains(TALKBACK_SERVICE_OMIT) || enabledServices.contains(VOICEBACK_SERVICE) || enabledServices.contains(VOICEBACK_SERVICE_OMIT) || enabledServices.contains(DIANMING_SERVICE) || enabledServices.contains(DIANMING_SERVICE_OMIT) || enabledServices.contains(NIRENR_SERVICE) || enabledServices.contains(NIRENR_SERVICE_OMIT) || enabledServices.contains(TBACK_SERVICE);
    }

    private static boolean isMiuiEnhanceTBServiceInstalled(AccessibilityManager accessibilityManager) {
        AccessibilityServiceInfo serviceInfo;
        if (TextUtils.isEmpty(MIUI_ENHANCE_TALKBACK) || (serviceInfo = accessibilityManager.getInstalledServiceInfoWithComponentName(ComponentName.unflattenFromString(MIUI_ENHANCE_TALKBACK))) == null) {
            return false;
        }
        return accessibilityManager.getInstalledAccessibilityServiceList().contains(serviceInfo);
    }
}

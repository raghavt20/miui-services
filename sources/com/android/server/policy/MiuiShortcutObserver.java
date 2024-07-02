package com.android.server.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.input.shortcut.MiuiShortcutOperatorCustomManager;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import miui.os.Build;

/* loaded from: classes.dex */
public abstract class MiuiShortcutObserver extends ContentObserver {
    private static final String DEVICE_REGION_RUSSIA = "ru";
    public static final int DOUBLE_VOLUME_DOWN_KEY_TYPE_LAUNCH_CAMERA_AND_TAKE_PHOTO = 2;
    private static final String TAG = "MiuiShortcutObserver";
    private final ContentResolver mContentResolver;
    private final Context mContext;
    protected int mCurrentUserId;
    private final Handler mHandler;
    private final HashSet<MiuiShortcutListener> mMiuiShortcutListeners;
    private final MiuiShortcutOperatorCustomManager mMiuiShortcutOperatorCustomManager;
    public static final String CURRENT_DEVICE_REGION = SystemProperties.get("ro.miui.build.region", "CN");
    public static final Boolean SUPPORT_VARIABLE_APERTURE = Boolean.valueOf(SystemProperties.getBoolean("persist.vendor.camera.IsVariableApertureSupported", false));

    abstract void updateRuleInfo();

    public MiuiShortcutObserver(Handler handler, Context context, int currentUserId) {
        super(handler);
        this.mMiuiShortcutListeners = new HashSet<>();
        this.mContext = context;
        this.mHandler = handler;
        this.mContentResolver = context.getContentResolver();
        this.mCurrentUserId = currentUserId;
        this.mMiuiShortcutOperatorCustomManager = MiuiShortcutOperatorCustomManager.getInstance(context);
    }

    public void registerShortcutListener(MiuiShortcutListener miuiShortcutListener) {
        if (miuiShortcutListener != null) {
            synchronized (this.mMiuiShortcutListeners) {
                this.mMiuiShortcutListeners.add(miuiShortcutListener);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifySingleRuleChanged(MiuiSingleKeyRule rule, Uri uri) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onSingleChanged(rule, uri);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyCombinationRuleChanged(MiuiCombinationRule rule) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onCombinationChanged(rule);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyGestureRuleChanged(MiuiGestureRule rule) {
        synchronized (this.mMiuiShortcutListeners) {
            Iterator<MiuiShortcutListener> it = this.mMiuiShortcutListeners.iterator();
            while (it.hasNext()) {
                MiuiShortcutListener listener = it.next();
                listener.onGestureChanged(rule);
            }
        }
    }

    public boolean currentFunctionValueIsInt(String action) {
        return "volumekey_launch_camera".equals(action);
    }

    protected void unRegisterShortcutListener(MiuiShortcutListener listener, String action) {
        if (listener != null) {
            synchronized (this.mMiuiShortcutListeners) {
                this.mMiuiShortcutListeners.remove(listener);
            }
        }
    }

    public void setDefaultFunction(boolean isNeedReset) {
        updateRuleInfo();
    }

    public String getFunction() {
        return null;
    }

    public String getFunction(String action) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Map<String, String> getActionAndFunctionMap() {
        return null;
    }

    public void onDestroy() {
        ContentResolver contentResolver = this.mContentResolver;
        if (contentResolver != null) {
            contentResolver.unregisterContentObserver(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isFeasibleFunction(String function, Context context) {
        if ("mi_pay".equals(function)) {
            return MiuiSettings.Key.isTSMClientInstalled(context);
        }
        if ("launch_voice_assistant".equals(function)) {
            return MiuiSettings.System.isXiaoAiExist(context);
        }
        if ("partial_screen_shot".equals(function)) {
            return SystemProperties.getBoolean("persist.sys.partial.screenshot.support", true);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean hasCustomizedFunction(String str, String str2, boolean z) {
        if (this.mMiuiShortcutOperatorCustomManager.hasOperatorCustomFunctionForMiuiRule(str2, str)) {
            return true;
        }
        if ("long_press_power_key".equals(str)) {
            if (z) {
                boolean enableLongPressPowerLaunchAssistant = enableLongPressPowerLaunchAssistant(str2);
                Settings.System.putIntForUser(this.mContentResolver, "long_press_power_launch_xiaoai", enableLongPressPowerLaunchAssistant ? 1 : 0, this.mCurrentUserId);
            } else if ("launch_google_search".equals(str2) && !supportRSARegion()) {
                return true;
            }
        }
        if (SUPPORT_VARIABLE_APERTURE.booleanValue() && "volumekey_launch_camera".equals(str) && TextUtils.isEmpty(str2)) {
            Settings.System.putIntForUser(this.mContentResolver, str, 2, this.mCurrentUserId);
            return true;
        }
        return false;
    }

    private boolean enableLongPressPowerLaunchAssistant(String function) {
        if (Build.IS_GLOBAL_BUILD) {
            boolean enableLongPressPowerLaunchAssistant = "launch_google_search".equals(function);
            if (!supportRSARegion()) {
                return false;
            }
            return enableLongPressPowerLaunchAssistant;
        }
        boolean enableLongPressPowerLaunchAssistant2 = "launch_voice_assistant".equals(function);
        return enableLongPressPowerLaunchAssistant2;
    }

    public void onUserSwitch(int currentUserId, boolean isNewUser) {
        this.mCurrentUserId = -2;
        if (isNewUser) {
            setDefaultFunction(true);
        } else {
            updateRuleInfo();
        }
    }

    private boolean supportRSARegion() {
        return Build.IS_INTERNATIONAL_BUILD && !DEVICE_REGION_RUSSIA.equals(CURRENT_DEVICE_REGION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRuleForObserver(MiuiCombinationRule miuiCombinationRule) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRuleForObserver(MiuiGestureRule miuiGestureRule) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRuleForObserver(MiuiSingleKeyRule miuiSingleKeyRule) {
    }

    /* loaded from: classes.dex */
    public interface MiuiShortcutListener {
        default void onCombinationChanged(MiuiCombinationRule rule) {
        }

        default void onSingleChanged(MiuiSingleKeyRule rule, Uri uri) {
        }

        default void onGestureChanged(MiuiGestureRule rule) {
        }
    }
}

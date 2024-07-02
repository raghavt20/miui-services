package com.android.server.wm;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;

/* loaded from: classes.dex */
public class MiuiDesktopModeUtils {
    private static final String TAG = "MiuiDesktopModeUtils";
    private static final boolean IS_SUPPORTED = SystemProperties.getBoolean("ro.config.miui_desktop_mode_enabled", false);
    private static boolean mDesktopOn = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void initDesktop(Context context) {
        if (isEnabled()) {
            mDesktopOn = isActive(context);
            SettingsObserver mSettingsObserver = new SettingsObserver();
            mSettingsObserver.observe(context);
        }
    }

    public static boolean isEnabled() {
        return IS_SUPPORTED;
    }

    public static boolean isActive(Context context) {
        if (!IS_SUPPORTED) {
            mDesktopOn = false;
            return false;
        }
        try {
            int result = Settings.System.getIntForUser(context.getContentResolver(), "miui_dkt_mode", -2);
            boolean z = result != 0;
            mDesktopOn = z;
            return z;
        } catch (Exception e) {
            Slog.d(TAG, "Failed to read MIUI_DESKTOP_MODE settings " + e);
            mDesktopOn = false;
            return false;
        }
    }

    public static boolean isDesktopActive() {
        return isEnabled() && mDesktopOn;
    }

    /* loaded from: classes.dex */
    static class SettingsObserver extends ContentObserver {
        private Context mContext;
        private final Uri mMiuiDesktopModeSetting;

        public SettingsObserver() {
            super(new Handler());
            this.mMiuiDesktopModeSetting = Settings.System.getUriFor("miui_dkt_mode");
        }

        void observe(Context context) {
            Slog.d(MiuiDesktopModeUtils.TAG, "register " + this.mMiuiDesktopModeSetting + ", mDesktopOn=" + MiuiDesktopModeUtils.mDesktopOn);
            this.mContext = context;
            context.getContentResolver().registerContentObserver(this.mMiuiDesktopModeSetting, false, this, -2);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Slog.d(MiuiDesktopModeUtils.TAG, "onChange " + uri + " changed selfChange=" + selfChange);
            if (this.mMiuiDesktopModeSetting.equals(uri)) {
                MiuiDesktopModeUtils.mDesktopOn = MiuiDesktopModeUtils.isActive(this.mContext);
                Slog.d(MiuiDesktopModeUtils.TAG, "onChange " + uri + " changed selfChange=" + selfChange + ", mDesktopOn=" + MiuiDesktopModeUtils.mDesktopOn);
            }
        }
    }
}

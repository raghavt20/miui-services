package com.xiaomi.abtest.d;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;

/* loaded from: classes.dex */
public class e {
    public static final int a = 25;
    public static final int b = 24;
    private static final String c = "FbeUtil";

    private e() {
    }

    public static Context a(Context context) {
        if (e(context)) {
            k.a(c, "getSafeContext return origin ctx");
            return context;
        }
        k.a(c, "getSafeContext , create the safe ctx");
        return context.createDeviceProtectedStorageContext();
    }

    public static boolean a() {
        try {
            return ((Boolean) StorageManager.class.getDeclaredMethod("isFileEncryptedNativeOrEmulated", new Class[0]).invoke(null, new Object[0]).getClass().getDeclaredMethod("isFileEncryptedNativeOrEmulated", Boolean.TYPE).invoke(null, new Object[0])).booleanValue();
        } catch (Exception e) {
            k.b(c, "*** " + e);
            return false;
        }
    }

    public static void a(PreferenceManager preferenceManager) {
        preferenceManager.setStorageDeviceProtected();
    }

    public static boolean b(Context context) {
        try {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(MiuiStylusShortcutManager.SCENE_KEYGUARD);
            if (!a() || keyguardManager == null) {
                return false;
            }
            return keyguardManager.isKeyguardSecure();
        } catch (Exception e) {
            k.a(c, "FBEDeviceAndSetedUpScreenLock Exception: " + e.getMessage());
            return false;
        }
    }

    public static boolean c(Context context) {
        return b(context) && !e(context);
    }

    public static boolean d(Context context) {
        if (e(context)) {
            return false;
        }
        return true;
    }

    private static boolean e(Context context) {
        try {
            UserManager userManager = (UserManager) context.getSystemService("user");
            if (userManager != null) {
                return userManager.isUserUnlocked();
            }
            return false;
        } catch (Exception e) {
            k.a(c, "isUserUnlocked Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean f(Context context) {
        return false;
    }
}

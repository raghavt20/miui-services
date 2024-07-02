package com.android.server.locksettings;

import android.security.MiuiLockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.locksettings.LockSettingsStub$$")
/* loaded from: classes.dex */
public class LockSettingsImpl extends LockSettingsStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LockSettingsImpl> {

        /* compiled from: LockSettingsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LockSettingsImpl INSTANCE = new LockSettingsImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LockSettingsImpl m1867provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LockSettingsImpl m1866provideNewInstance() {
            return new LockSettingsImpl();
        }
    }

    public void savePrivacyPasswordPattern(String pattern, String filename, int userId) {
        MiuiLockPatternUtils.savePrivacyPasswordPattern(pattern, filename, userId);
    }

    public boolean checkPrivacyPasswordPattern(String pattern, String filename, int userId) {
        return MiuiLockPatternUtils.checkPrivacyPasswordPattern(pattern, filename, userId);
    }

    public void miuiSavePinLength(LockSettingsService lockSettingsService, LockscreenCredential newCredential, int userHandle) {
        if (newCredential.isPin()) {
            lockSettingsService.setLong("lockscreen.password_length", newCredential.size(), userHandle);
        }
    }
}

package com.miui.server.security;

import android.util.ArrayMap;
import android.util.ArraySet;
import com.miui.server.security.GameBoosterImpl;
import java.util.HashMap;
import java.util.HashSet;

/* loaded from: classes.dex */
public class SecurityUserState {
    public GameBoosterImpl.GameBoosterServiceDeath gameBoosterServiceDeath;
    public boolean mAccessControlEnabled;
    public boolean mAccessControlLockConvenient;
    public boolean mAccessControlSettingInit;
    public boolean mIsGameMode;
    public String mLastResumePackage;
    public int userHandle;
    public final HashSet<String> mAccessControlPassPackages = new HashSet<>();
    public final HashMap<String, SecurityPackageSettings> mPackages = new HashMap<>();
    public final ArraySet<String> mAccessControlCanceled = new ArraySet<>();
    public final ArrayMap<String, Long> mAccessControlLastCheck = new ArrayMap<>();
    public int mAccessControlLockMode = 0;
}

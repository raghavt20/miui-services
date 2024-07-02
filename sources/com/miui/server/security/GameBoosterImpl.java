package com.miui.server.security;

import android.content.Context;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import com.android.server.wm.MiuiFreeFormGestureController;
import com.miui.server.SecurityManagerService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import miui.security.SecurityManager;

/* loaded from: classes.dex */
public class GameBoosterImpl {
    private static final String TAG = "GameBoosterImpl";
    private final SecurityManagerService mService;

    public GameBoosterImpl(SecurityManagerService service) {
        this.mService = service;
    }

    public void setGameBoosterIBinder(IBinder gameBooster, int userId, boolean isGameMode) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(SecurityManager.getUserHandle(userId));
            try {
                if (userState.gameBoosterServiceDeath == null) {
                    userState.gameBoosterServiceDeath = new GameBoosterServiceDeath(userState, gameBooster);
                    gameBooster.linkToDeath(userState.gameBoosterServiceDeath, 0);
                } else if (gameBooster != userState.gameBoosterServiceDeath.mGameBoosterService) {
                    userState.gameBoosterServiceDeath.mGameBoosterService.unlinkToDeath(userState.gameBoosterServiceDeath, 0);
                    userState.gameBoosterServiceDeath = new GameBoosterServiceDeath(userState, gameBooster);
                    gameBooster.linkToDeath(userState.gameBoosterServiceDeath, 0);
                }
                userState.mIsGameMode = isGameMode;
            } catch (Exception e) {
                Log.e(TAG, "setGameBoosterIBinder", e);
            }
        }
    }

    public boolean getGameMode(int userId) {
        boolean z;
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(SecurityManager.getUserHandle(userId));
            z = userState.mIsGameMode;
        }
        return z;
    }

    public boolean isVtbMode(Context context) {
        boolean z;
        synchronized (this.mService.mUserStateLock) {
            z = Settings.Secure.getIntForUser(context.getContentResolver(), MiuiFreeFormGestureController.VTB_BOOSTING, 0, -2) == 1;
        }
        return z;
    }

    public void setGameStorageApp(String packageName, int userId, boolean isStorage) {
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userStateLocked = this.mService.getUserStateLocked(userId);
            SecurityPackageSettings ps = this.mService.getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isGameStorageApp = isStorage;
            this.mService.scheduleWriteSettings();
        }
    }

    public boolean isGameStorageApp(String packageName, int userId) {
        boolean z;
        synchronized (this.mService.mUserStateLock) {
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            try {
                SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, packageName);
                z = ps.isGameStorageApp;
            } catch (Exception e) {
                Log.e(TAG, "get app is game storage failed", e);
                return false;
            }
        }
        return z;
    }

    public List<String> getAllGameStorageApps(int userId) {
        List<String> storageAppsList;
        synchronized (this.mService.mUserStateLock) {
            storageAppsList = new ArrayList<>();
            SecurityUserState userState = this.mService.getUserStateLocked(userId);
            HashMap<String, SecurityPackageSettings> packages = userState.mPackages;
            Set<String> pkgNames = packages.keySet();
            for (String pkgName : pkgNames) {
                try {
                    SecurityPackageSettings ps = this.mService.getPackageSetting(userState.mPackages, pkgName);
                    if (ps.isGameStorageApp) {
                        storageAppsList.add(pkgName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "get game storage all apps failed", e);
                }
            }
        }
        return storageAppsList;
    }

    /* loaded from: classes.dex */
    public class GameBoosterServiceDeath implements IBinder.DeathRecipient {
        public final IBinder mGameBoosterService;
        private final SecurityUserState mUserState;

        public GameBoosterServiceDeath(SecurityUserState userState, IBinder gameBoosterService) {
            this.mUserState = userState;
            this.mGameBoosterService = gameBoosterService;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (GameBoosterImpl.this.mService.mUserStateLock) {
                try {
                    this.mGameBoosterService.unlinkToDeath(this, 0);
                    this.mUserState.mIsGameMode = false;
                    this.mUserState.gameBoosterServiceDeath = null;
                } catch (Exception e) {
                    Log.e(GameBoosterImpl.TAG, "GameBoosterServiceDeath", e);
                }
            }
        }
    }
}

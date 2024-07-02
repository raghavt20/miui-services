package com.android.server.pm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.pm.Installer;
import com.android.server.pm.UserManagerService;
import com.miui.base.MiuiStubRegistry;
import com.miui.xspace.XSpaceManagerStub;
import java.util.Set;

/* loaded from: classes.dex */
public class UserManagerServiceImpl implements UserManagerServiceStub {
    private static final int STATUS_CHECKING = 0;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_LOCK_CONTENTION = 1;
    private static final String TAG = UserManagerServiceImpl.class.getSimpleName();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<UserManagerServiceImpl> {

        /* compiled from: UserManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final UserManagerServiceImpl INSTANCE = new UserManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public UserManagerServiceImpl m2148provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public UserManagerServiceImpl m2147provideNewInstance() {
            return new UserManagerServiceImpl();
        }
    }

    public int checkAndGetNewUserId(int flags, int defUserId) {
        if ((134217728 & flags) != 0) {
            return 110;
        }
        return defUserId;
    }

    public boolean isInMaintenanceMode(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "maintenance_mode_user_id") == 110;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    public boolean prepareUserDataWithContentionCheck(UserDataPreparer userDataPreparer, Object installLock, final Installer installer, final int userId, int userSerial, final int flags) {
        if (Thread.holdsLock(installLock)) {
            return false;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.wtf(TAG, "prepareUserData isn't supposed to run on the main thread", new RuntimeException());
            return false;
        }
        final int[] lockContention = {0};
        Runnable contentionCheckTask = new Runnable() { // from class: com.android.server.pm.UserManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UserManagerServiceImpl.lambda$prepareUserDataWithContentionCheck$0(lockContention, userId, flags, installer);
            }
        };
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            int timeoutMillis = userId == 0 ? 500 : 10000;
            handler.postDelayed(contentionCheckTask, timeoutMillis);
            synchronized (installLock) {
                handler.removeCallbacks(contentionCheckTask);
                userDataPreparer.prepareUserData(userId, userSerial, flags);
            }
            synchronized (lockContention) {
                if (lockContention[0] == 1) {
                    Log.i(TAG, "Enable dexopt, userId=" + userId + ", flags=" + flags);
                    try {
                        installer.controlDexOptBlocking(false);
                    } catch (Installer.LegacyDexoptDisabledException e) {
                        Slog.d(TAG, "LegacyDexoptDisabledException");
                    }
                }
                lockContention[0] = 2;
            }
            return true;
        } catch (Throwable th) {
            synchronized (lockContention) {
                if (lockContention[0] == 1) {
                    Log.i(TAG, "Enable dexopt, userId=" + userId + ", flags=" + flags);
                    try {
                        installer.controlDexOptBlocking(false);
                    } catch (Installer.LegacyDexoptDisabledException e2) {
                        Slog.d(TAG, "LegacyDexoptDisabledException");
                    }
                }
                lockContention[0] = 2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$prepareUserDataWithContentionCheck$0(int[] lockContention, int userId, int flags, Installer installer) {
        synchronized (lockContention) {
            if (lockContention[0] == 0) {
                lockContention[0] = 1;
                Log.i(TAG, "Lock contention detected while prepare user data: userId=" + userId + ", flags=" + flags + ", disable dexopt");
                try {
                    installer.controlDexOptBlocking(true);
                } catch (Installer.LegacyDexoptDisabledException e) {
                    Slog.d(TAG, "LegacyDexoptDisabledException");
                }
            }
        }
    }

    public boolean getIsXSpaceUpdate(SparseArray<UserManagerService.UserData> mUsers, Set<Integer> userIdsToWrite) {
        UserManagerService.UserData xspaceUserData = mUsers.get(XSpaceManagerStub.getInstance().getXSpaceUserId());
        if (xspaceUserData == null || xspaceUserData.info.userType == "android.os.usertype.profile.CLONE") {
            return false;
        }
        xspaceUserData.info.userType = "android.os.usertype.profile.CLONE";
        xspaceUserData.info.flags = XSpaceManagerStub.getInstance().getXSpaceUserFlag() | 4112;
        userIdsToWrite.add(Integer.valueOf(xspaceUserData.info.id));
        return true;
    }
}

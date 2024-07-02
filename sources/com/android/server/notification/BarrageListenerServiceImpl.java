package com.android.server.notification;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.server.wm.MiuiFreeFormGestureController;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class BarrageListenerServiceImpl implements BarrageListenerServiceStub {
    private ActivityManager mAm;
    private Context mContext;
    private int mCurrentUserId;
    private Handler mHandler;
    private int mInGame;
    private ManagedServices mListener;
    private PackageManager mPm;
    private SettingsObserver mSettingsObserver;
    private int mTurnOn;
    private UserSwitchReceiver mUserSwitchReceiver;
    private final String TAG = "BarrageListenerService";
    private final String GB_BULLET_CHAT = "gb_bullet_chat";
    private final String GB_BOOSTING = MiuiFreeFormGestureController.GB_BOOSTING;
    private final String MI_BARRAGE = "com.xiaomi.barrage";
    private final String COMPONENT_BARRAGE = "com.xiaomi.barrage/com.xiaomi.barrage.service.NotificationMonitorService";
    private final int DELAY_FORCE_STOP = 1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BarrageListenerServiceImpl> {

        /* compiled from: BarrageListenerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BarrageListenerServiceImpl INSTANCE = new BarrageListenerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BarrageListenerServiceImpl m2057provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BarrageListenerServiceImpl m2056provideNewInstance() {
            return new BarrageListenerServiceImpl();
        }
    }

    /* loaded from: classes.dex */
    private class BHandler extends Handler {
        private BHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int userId = msg.arg1;
                    BarrageListenerServiceImpl.this.mAm.forceStopPackageAsUser("com.xiaomi.barrage", userId);
                    Log.d("BarrageListenerService", "stop the barrage, mTurnOn:" + BarrageListenerServiceImpl.this.mTurnOn + " mInGame:" + BarrageListenerServiceImpl.this.mInGame);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (Settings.Secure.getUriFor("gb_bullet_chat").equals(uri)) {
                BarrageListenerServiceImpl barrageListenerServiceImpl = BarrageListenerServiceImpl.this;
                barrageListenerServiceImpl.mTurnOn = Settings.Secure.getIntForUser(barrageListenerServiceImpl.mContext.getContentResolver(), "gb_bullet_chat", 0, BarrageListenerServiceImpl.this.mCurrentUserId);
            }
            if (Settings.Secure.getUriFor(MiuiFreeFormGestureController.GB_BOOSTING).equals(uri)) {
                BarrageListenerServiceImpl barrageListenerServiceImpl2 = BarrageListenerServiceImpl.this;
                barrageListenerServiceImpl2.mInGame = Settings.Secure.getIntForUser(barrageListenerServiceImpl2.mContext.getContentResolver(), MiuiFreeFormGestureController.GB_BOOSTING, 0, BarrageListenerServiceImpl.this.mCurrentUserId);
            }
            update();
        }

        private void update() {
            if (BarrageListenerServiceImpl.this.mTurnOn != 0 && BarrageListenerServiceImpl.this.mInGame != 0) {
                if (BarrageListenerServiceImpl.this.mHandler.hasMessages(1)) {
                    BarrageListenerServiceImpl.this.mHandler.removeMessages(1);
                }
                ComponentName cn = ComponentName.unflattenFromString("com.xiaomi.barrage/com.xiaomi.barrage.service.NotificationMonitorService");
                BarrageListenerServiceImpl.this.mListener.registerService(cn, BarrageListenerServiceImpl.this.mCurrentUserId);
                Log.d("BarrageListenerService", "start the barrage, mTurnOn:" + BarrageListenerServiceImpl.this.mTurnOn + " mInGame:" + BarrageListenerServiceImpl.this.mInGame);
                return;
            }
            if (BarrageListenerServiceImpl.this.mHandler.hasMessages(1)) {
                return;
            }
            Message msg = BarrageListenerServiceImpl.this.mHandler.obtainMessage(1);
            msg.arg1 = BarrageListenerServiceImpl.this.mCurrentUserId;
            BarrageListenerServiceImpl.this.mHandler.sendMessageDelayed(msg, 10000L);
        }

        public void onChangeAll() {
            onChange(false, Settings.Secure.getUriFor("gb_bullet_chat"));
            onChange(false, Settings.Secure.getUriFor(MiuiFreeFormGestureController.GB_BOOSTING));
        }
    }

    /* loaded from: classes.dex */
    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                int oldUserId = BarrageListenerServiceImpl.this.mCurrentUserId;
                BarrageListenerServiceImpl.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                Log.d("BarrageListenerService", "the user switches to u" + BarrageListenerServiceImpl.this.mCurrentUserId);
                if (BarrageListenerServiceImpl.this.mHandler.hasMessages(1)) {
                    BarrageListenerServiceImpl.this.mHandler.removeMessages(1);
                    BarrageListenerServiceImpl.this.mAm.forceStopPackageAsUser("com.xiaomi.barrage", oldUserId);
                    Log.d("BarrageListenerService", "stop the barrage when the user has been changed, mTurnOn:" + BarrageListenerServiceImpl.this.mTurnOn + " mInGame:" + BarrageListenerServiceImpl.this.mInGame + " oldUserId:" + oldUserId);
                }
                BarrageListenerServiceImpl.this.mSettingsObserver.onChangeAll();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void init(Context context, ManagedServices managedServices) {
        this.mContext = context;
        this.mListener = managedServices;
        this.mHandler = new BHandler();
        this.mPm = this.mContext.getPackageManager();
        this.mAm = (ActivityManager) this.mContext.getSystemService("activity");
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("gb_bullet_chat"), false, this.mSettingsObserver, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor(MiuiFreeFormGestureController.GB_BOOSTING), false, this.mSettingsObserver, -1);
        this.mCurrentUserId = 0;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        UserSwitchReceiver userSwitchReceiver = new UserSwitchReceiver();
        this.mUserSwitchReceiver = userSwitchReceiver;
        this.mContext.registerReceiverAsUser(userSwitchReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    public boolean isAllowStartBarrage(String packageName) {
        if (packageName.equals("com.xiaomi.barrage")) {
            return (this.mTurnOn == 0 || this.mInGame == 0) ? false : true;
        }
        return true;
    }
}

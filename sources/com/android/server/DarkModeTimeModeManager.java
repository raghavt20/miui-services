package com.android.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.miui.server.security.AccessControlImpl;
import java.time.LocalTime;
import miui.hardware.display.DisplayFeatureManager;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class DarkModeTimeModeManager {
    public static final String DARK_MODE_ENABLE = "dark_mode_enable";
    public static final int SCREEN_DARKMODE = 38;
    public static final boolean SUPPORT_DARK_MODE_NOTIFY = FeatureParser.getBoolean("support_dark_mode_notify", false);
    private static final String TAG = "DarkModeTimeModeManager";
    private Context mContext;
    private ContentObserver mDarkModeObserver;
    private DarkModeSuggestProvider mDarkModeSuggestProvider;
    private BroadcastReceiver mDarkModeTimeModeReceiver;
    private UiModeManager mUiModeManager;

    public DarkModeTimeModeManager() {
    }

    public DarkModeTimeModeManager(final Context context) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        addAction(intentFilter, context);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.DarkModeTimeModeManager.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                Slog.i(DarkModeTimeModeManager.TAG, " onReceive: action = " + action);
                switch (action.hashCode()) {
                    case -2035794839:
                        if (action.equals("miui.action.intent.DARK_MODE_SUGGEST_ENABLE")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case -415578687:
                        if (action.equals("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 823795052:
                        if (action.equals("android.intent.action.USER_PRESENT")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        if (DarkModeTimeModeManager.this.canShowDarkModeSuggestNotifocation(context2)) {
                            DarkModeTimeModeManager.this.showDarkModeSuggestNotification(context2);
                            return;
                        }
                        return;
                    case 1:
                        DarkModeTimeModeManager.this.showDarkModeSuggestToast(context2);
                        return;
                    case 2:
                        DarkModeTimeModeManager.this.enterSettingsFromNotification(context2);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mDarkModeTimeModeReceiver = broadcastReceiver;
        context.registerReceiver(broadcastReceiver, intentFilter, 2);
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) < 3) {
            DarkModeSuggestProvider darkModeSuggestProvider = DarkModeSuggestProvider.getInstance();
            this.mDarkModeSuggestProvider = darkModeSuggestProvider;
            darkModeSuggestProvider.registerDataObserver(context);
        }
        this.mDarkModeObserver = new ContentObserver(new Handler()) { // from class: com.android.server.DarkModeTimeModeManager.2
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                super.onChange(z);
                if (DarkModeTimeModeManager.SUPPORT_DARK_MODE_NOTIFY) {
                    DisplayFeatureManager.getInstance().setScreenEffect(38, DarkModeTimeModeHelper.isDarkModeEnable(context) ? 1 : 0);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(DARK_MODE_ENABLE), false, this.mDarkModeObserver);
    }

    private void addAction(IntentFilter it, Context context) {
        it.addAction("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE");
        it.addAction("miui.action.intent.DARK_MODE_SUGGEST_ENABLE");
        if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) < 3 || DarkModeStatusTracker.DEBUG) {
            it.addAction("android.intent.action.USER_PRESENT");
        }
    }

    public void onBootPhase(Context context) {
        updateDarkModeTimeModeStatus(context);
    }

    private void updateDarkModeTimeModeStatus(Context context) {
        if (!DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
            Settings.System.getInt(this.mContext.getContentResolver(), "dark_mode_switch_now", 0);
            return;
        }
        Settings.System.putInt(this.mContext.getContentResolver(), "dark_mode_switch_now", 1);
        if (DarkModeTimeModeHelper.isSuntimeType(context)) {
            this.mUiModeManager.setNightMode(0);
            return;
        }
        int time = DarkModeTimeModeHelper.getDarkModeStartTime(this.mContext);
        LocalTime localTime = LocalTime.of(time / 60, time % 60);
        this.mUiModeManager.setCustomNightModeStart(localTime);
        int time2 = DarkModeTimeModeHelper.getDarkModeEndTime(this.mContext);
        LocalTime localTime2 = LocalTime.of(time2 / 60, time2 % 60);
        this.mUiModeManager.setCustomNightModeEnd(localTime2);
        this.mUiModeManager.setNightMode(3);
    }

    public boolean canShowDarkModeSuggestNotifocation(Context context) {
        if (DarkModeStatusTracker.DEBUG) {
            return true;
        }
        if (!DarkModeTimeModeHelper.isDarkModeSuggestEnable(context)) {
            Slog.i(TAG, "not get suggest from cloud");
            return false;
        }
        if (DarkModeTimeModeHelper.getDarkModeSuggestCount(context) >= 3) {
            Slog.i(TAG, "count >= 3");
            return false;
        }
        if (DarkModeTimeModeHelper.isDarkModeOpen(context)) {
            Slog.i(TAG, "darkMode is open");
            return false;
        }
        if (!DarkModeTimeModeHelper.isInNight(context)) {
            Slog.i(TAG, "not in night");
            return false;
        }
        if (DarkModeTimeModeHelper.getNowTimeInMills() - DarkModeTimeModeHelper.getLastSuggestTime(context) < 604800000) {
            Slog.i(TAG, "less than 7 days ago");
            return false;
        }
        if (DarkModeTimeModeHelper.isOnHome(context)) {
            return true;
        }
        Slog.i(TAG, "not on home");
        return false;
    }

    public void showDarkModeSuggestNotification(Context context) {
        Slog.i(TAG, "showDarkModeSuggestNotification");
        initNotification(context);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, DarkModeTimeModeHelper.getDarkModeSuggestCount(context) + 1);
        DarkModeSuggestProvider.getInstance().unRegisterDataObserver(context);
        DarkModeTimeModeHelper.setLastSuggestTime(context, DarkModeTimeModeHelper.getNowTimeInMills());
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggest(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
    }

    private void initNotification(Context context) {
        String title = context.getResources().getString(286196136);
        String message = context.getResources().getString(286196135);
        Intent intentSettings = new Intent("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE");
        PendingIntent pendingIntentSettings = PendingIntent.getBroadcast(context, 0, intentSettings, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        Intent intentEnable = new Intent("miui.action.intent.DARK_MODE_SUGGEST_ENABLE");
        PendingIntent pendingIntentEnable = PendingIntent.getBroadcast(context, 0, intentEnable, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        creatNoticifationChannel("dark_mode_suggest_id", "dark_mode", 4, "des", notificationManager);
        Bundle arg = new Bundle();
        arg.putParcelable("miui.appIcon", Icon.createWithResource(context, 285737359));
        arg.putBoolean("miui.showAction", true);
        Notification notification = new Notification.Builder(context, "dark_mode_suggest_id").addExtras(arg).setSmallIcon(285737359).setContentTitle(title).setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(pendingIntentSettings).addAction(285737294, context.getResources().getString(286196134), pendingIntentEnable).setAutoCancel(true).setTimeoutAfter(5000L).build();
        notificationManager.notifyAsUser(TAG, 1, notification, UserHandle.ALL);
    }

    private void creatNoticifationChannel(String channelId, CharSequence name, int importance, String description, NotificationManager manager) {
        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        manager.createNotificationChannel(channel);
    }

    public void showDarkModeSuggestToast(Context context) {
        Settings.System.putString(context.getContentResolver(), "open_sun_time_channel", DarkModeOneTrackHelper.PARAM_VALUE_CHANNEL_NOTIFY);
        updateButtonStatus(context);
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestEnable(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, 3);
        Toast.makeText(context, context.getResources().getString(286196137), 0).show();
    }

    private void updateButtonStatus(Context context) {
        DarkModeTimeModeHelper.setDarkModeTimeEnable(context, true);
        DarkModeTimeModeHelper.setDarkModeAutoTimeEnable(context, false);
        DarkModeTimeModeHelper.setSunRiseSunSetMode(context, true);
        DarkModeTimeModeHelper.setDarkModeTimeType(context, 2);
    }

    public void enterSettingsFromNotification(final Context context) {
        Settings.System.putInt(context.getContentResolver(), "enter_setting_by_notification", 1);
        Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
        intent.setFlags(268435456);
        context.startActivity(intent);
        DarkModeTimeModeHelper.setDarkModeSuggestCount(context, 3);
        DarkModeStauesEvent event = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestClick(1);
        DarkModeOneTrackHelper.uploadToOneTrack(context, event);
        MiuiBgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.DarkModeTimeModeManager.3
            @Override // java.lang.Runnable
            public void run() {
                if (DarkModeTimeModeHelper.isDarkModeOpen(context)) {
                    DarkModeStauesEvent event2 = new DarkModeStauesEvent().setEventName(DarkModeOneTrackHelper.EVENT_NAME_SUGGEST).setTip("").setSuggestOpenInSetting(1);
                    DarkModeOneTrackHelper.uploadToOneTrack(context, event2);
                }
                Settings.System.putInt(context.getContentResolver(), "enter_setting_by_notification", 0);
            }
        }, AccessControlImpl.LOCK_TIME_OUT);
    }
}

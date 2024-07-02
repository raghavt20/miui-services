package com.android.server.notification;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.MiuiNotification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.NotificationManagerServiceStub;
import com.android.server.notification.NotificationRecord;
import com.android.server.notification.NotificationUsageStats;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.app.MiuiLightsManagerInternal;
import miui.content.pm.ExtraPackageManager;
import miui.content.pm.PreloadedAppPolicy;
import miui.os.Build;
import miui.security.ISecurityManager;
import miui.util.QuietUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.notification.NotificationManagerServiceStub$$")
/* loaded from: classes.dex */
public class NotificationManagerServiceImpl extends NotificationManagerServiceStub {
    private static final String BREATHING_LIGHT = "breathing_light";
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    private static final int INVALID_UID = -1;
    private static final String KEY_MEDIA_NOTIFICATION_CLOUD = "blacklist";
    private static final List<String> MIUI_SYSTEM_APPS_LIST;
    private static final String MODULE_MEDIA_NOTIFICATION_CLOUD_LIST = "config_notification_medianotificationcontrol";
    private static final List<String> OTHER_APPS_LIST;
    private static final String SPLIT_CHAR = "\\|";
    private static final int SYSTEM_APP_MASK = 129;
    public static final String TAG = "NotificationManagerServiceImpl";
    private static final String XMSF_CHANNEL_ID_PREFIX = "mipush|";
    private static final String XMSF_FAKE_CONDITION_PROVIDER_PATH = "xmsf_fake_condition_provider_path";
    protected static final String XMSF_PACKAGE_NAME = "com.xiaomi.xmsf";
    private static List<String> allowMediaNotificationCloudDataList = new ArrayList();
    private static final Set<String> sAllowToastSet;
    private String allowMediaNotificationCloudData;
    private String[] allowNotificationAccessList;
    private String[] interceptChannelId;
    private String[] interceptListener;
    private Context mContext;
    private NotificationManagerService mNMS;
    private final String PRIVACY_INPUT_MODE_PKG_NAME = "miui_privacy_input_pkg_name";
    private String mPrivacyInputModePkgName = null;

    /* loaded from: classes.dex */
    public static final class NMSVersionImpl implements NotificationManagerServiceStub.NMSVersionStub {
        static final String ATT_MIUI_VERSION = "miui_version";
        private int miuiVersion = -1;

        /* loaded from: classes.dex */
        public final class Provider implements MiuiStubRegistry.ImplProvider<NMSVersionImpl> {

            /* compiled from: NotificationManagerServiceImpl$NMSVersionImpl$Provider.java */
            /* loaded from: classes.dex */
            public static final class SINGLETON {
                public static final NMSVersionImpl INSTANCE = new NMSVersionImpl();
            }

            /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
            public NMSVersionImpl m2062provideSingleton() {
                return SINGLETON.INSTANCE;
            }

            /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
            public NMSVersionImpl m2061provideNewInstance() {
                return new NMSVersionImpl();
            }
        }

        public int getMiuiVersion() {
            return this.miuiVersion;
        }

        public void setMiuiVersion(int version) {
            this.miuiVersion = version;
        }

        public void readXml(TypedXmlPullParser parser) {
            this.miuiVersion = XmlUtils.readIntAttribute(parser, ATT_MIUI_VERSION, -1);
        }

        public void writeXml(TypedXmlSerializer out) throws IOException {
            out.attributeInt((String) null, ATT_MIUI_VERSION, this.miuiVersion);
        }
    }

    /* loaded from: classes.dex */
    public static final class NRStatusImpl implements NotificationManagerServiceStub.NRStatusStub {
        private int mStatusBarAllowAlert = -1;

        /* loaded from: classes.dex */
        public final class Provider implements MiuiStubRegistry.ImplProvider<NRStatusImpl> {

            /* compiled from: NotificationManagerServiceImpl$NRStatusImpl$Provider.java */
            /* loaded from: classes.dex */
            public static final class SINGLETON {
                public static final NRStatusImpl INSTANCE = new NRStatusImpl();
            }

            /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
            public NRStatusImpl m2064provideSingleton() {
                return SINGLETON.INSTANCE;
            }

            /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
            public NRStatusImpl m2063provideNewInstance() {
                return new NRStatusImpl();
            }
        }

        public void setStatusBarAllowAlert(int statusBarAllowAlert) {
            this.mStatusBarAllowAlert = statusBarAllowAlert;
        }

        public int getStatusBarAllowAlert() {
            return this.mStatusBarAllowAlert;
        }

        public boolean isStatusBarAllowVibration() {
            return (this.mStatusBarAllowAlert & 1) != 0;
        }

        public boolean isStatusBarAllowSound() {
            return (this.mStatusBarAllowAlert & 2) != 0;
        }

        public boolean isStatusBarAllowLed() {
            return (this.mStatusBarAllowAlert & 4) != 0;
        }
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NotificationManagerServiceImpl> {

        /* compiled from: NotificationManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NotificationManagerServiceImpl INSTANCE = new NotificationManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public NotificationManagerServiceImpl m2066provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public NotificationManagerServiceImpl m2065provideNewInstance() {
            return new NotificationManagerServiceImpl();
        }
    }

    static {
        HashSet hashSet = new HashSet();
        sAllowToastSet = hashSet;
        hashSet.add("com.lbe.security.miui");
        MIUI_SYSTEM_APPS_LIST = Arrays.asList(ExtraPackageManager.MIUI_SYSTEM_APPS);
        ArrayList arrayList = new ArrayList();
        OTHER_APPS_LIST = arrayList;
        arrayList.add("com.xiaomi.wearable");
        arrayList.add("com.xiaomi.hm.health");
        arrayList.add("com.mi.health");
    }

    public void init(Context context, NotificationManagerService nms) {
        this.mContext = context;
        this.interceptListener = context.getResources().getStringArray(285409349);
        this.interceptChannelId = this.mContext.getResources().getStringArray(285409348);
        this.allowNotificationAccessList = this.mContext.getResources().getStringArray(285409353);
        Slog.i(TAG, " allowNotificationListener :" + Arrays.toString(this.allowNotificationAccessList));
        String cloudDataString = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), MODULE_MEDIA_NOTIFICATION_CLOUD_LIST, KEY_MEDIA_NOTIFICATION_CLOUD, "");
        this.allowMediaNotificationCloudData = cloudDataString;
        allowMediaNotificationCloudDataList = getMediaNotificationDataCloudConversion(cloudDataString);
        registerPrivacyInputMode(BackgroundThread.getHandler());
        registerCloudDataResolver(BackgroundThread.getHandler());
        this.mNMS = nms;
    }

    public void allowMiuiDefaultApprovedServices(ManagedServices mListeners, boolean DBG) {
        Pair<Integer, String> listenerWithVersion = getMiuiDefaultListerWithVersion();
        if (DBG) {
            Log.d(TAG, "allowDefaultApprovedServices: " + listenerWithVersion.first + " second " + ((String) listenerWithVersion.second));
        }
        if (((Integer) listenerWithVersion.first).intValue() < 0) {
            return;
        }
        int miuiVersion = mListeners.mVersionStub.getMiuiVersion();
        if (DBG) {
            Log.d(TAG, "allowDefaultApprovedServices: miuiVersion " + miuiVersion);
        }
        if (miuiVersion >= ((Integer) listenerWithVersion.first).intValue()) {
            return;
        }
        String defaultListenerAccess = (String) listenerWithVersion.second;
        ArraySet<ComponentName> defaultListeners = new ArraySet<>();
        if (defaultListenerAccess != null) {
            String[] listeners = defaultListenerAccess.split(ENABLED_SERVICES_SEPARATOR);
            for (int i = 0; i < listeners.length; i++) {
                if (!TextUtils.isEmpty(listeners[i])) {
                    ArraySet<ComponentName> approvedListeners = mListeners.queryPackageForServices(listeners[i], 786432, 0);
                    for (int k = 0; k < approvedListeners.size(); k++) {
                        ComponentName cn = approvedListeners.valueAt(k);
                        mListeners.addDefaultComponentOrPackage(cn.flattenToString());
                        defaultListeners.add(cn);
                    }
                }
            }
            mListeners.mVersionStub.setMiuiVersion(((Integer) listenerWithVersion.first).intValue());
        }
        for (int i2 = 0; i2 < defaultListeners.size(); i2++) {
            NotificationManagerServiceProxy.allowNotificationListener.invoke(this.mNMS, new Object[]{0, defaultListeners.valueAt(i2)});
        }
    }

    public void buzzBeepBlinkForNotification(String key, int buzzBeepBlink, Object mNotificationLock) {
        NotificationManagerServiceProxy.checkCallerIsSystem.invoke(this.mNMS, new Object[0]);
        synchronized (mNotificationLock) {
            NotificationRecord record = (NotificationRecord) this.mNMS.mNotificationsByKey.get(key);
            if (record != null) {
                record.mNRStatusStub.setStatusBarAllowAlert(buzzBeepBlink);
                this.mNMS.buzzBeepBlinkLocked(record);
            }
        }
    }

    public boolean isDeniedPlaySound(AudioManager audioManager, NotificationRecord record) {
        return (isAudioCanPlay(audioManager) && !isSilenceMode(record) && record.mNRStatusStub.isStatusBarAllowSound()) ? false : true;
    }

    public boolean isSupportSilenceMode() {
        return MiuiSettings.SilenceMode.isSupported;
    }

    private boolean isAudioCanPlay(AudioManager audioManager) {
        return (audioManager.getRingerModeInternal() == 1 || audioManager.getRingerModeInternal() == 0) ? false : true;
    }

    private boolean isSilenceMode(NotificationRecord record) {
        MiuiNotification extraNotification = record.getSbn().getNotification().extraNotification;
        return QuietUtils.checkQuiet(5, 0, record.getSbn().getPackageName(), extraNotification == null ? null : extraNotification.getTargetPkg());
    }

    public boolean isDeniedPlayVibration(AudioManager audioManager, NotificationRecord record, NotificationUsageStats.AggregatedStats mStats) {
        boolean exRingerModeSilent = audioManager.getRingerMode() == 0;
        return exRingerModeSilent || !record.mNRStatusStub.isStatusBarAllowVibration() || shouldSkipFrequentlyVib(record, mStats);
    }

    public VibrationEffect adjustVibration(VibrationEffect vibration) {
        if (vibration != null && (vibration instanceof VibrationEffect.Composed)) {
            VibrationEffect.Composed currentVibration = (VibrationEffect.Composed) vibration;
            if (currentVibration.getSegments() != null && currentVibration.getSegments().size() > 4) {
                return new VibrationEffect.Composed(currentVibration.getSegments().subList(0, 4), currentVibration.getRepeatIndex());
            }
            return vibration;
        }
        return vibration;
    }

    public boolean isDeniedLed(NotificationRecord record) {
        return !record.mNRStatusStub.isStatusBarAllowLed();
    }

    public void calculateAudiblyAlerted(AudioManager audioManager, NotificationRecord record, NotificationManagerService mNm) {
        int i = 0;
        boolean beep = false;
        boolean z = true;
        boolean aboveThreshold = record.getImportance() >= 3;
        Uri soundUri = record.getSound();
        VibrationEffect vibration = record.getVibration();
        boolean hasValidVibrate = vibration != null;
        if (aboveThreshold && audioManager != null && shouldOnlyAlertNotification(record)) {
            boolean hasValidSound = (soundUri == null || Uri.EMPTY.equals(soundUri)) ? false : true;
            if (hasValidSound) {
                beep = true;
            }
            boolean ringerModeSilent = audioManager.getRingerModeInternal() == 0;
            if (hasValidVibrate && !ringerModeSilent) {
                i = 1;
            }
        }
        int buzzBeepBlink = (beep ? 2 : 0) | i;
        record.setBuzzBeepBlinkCode(record.getBuzzBeepBlinkCode() | buzzBeepBlink);
        if (i == 0 && !beep) {
            z = false;
        }
        record.setAudiblyAlerted(z);
    }

    boolean shouldOnlyAlertNotification(NotificationRecord record) {
        if (record.isUpdate && (record.getNotification().flags & 8) != 0) {
            return false;
        }
        return true;
    }

    public boolean skipClearAll(NotificationRecord record, int reason) {
        if (reason != 3 && reason != 11) {
            return false;
        }
        return isUpdatableFocusNotification(this.mContext, record.getKey());
    }

    private boolean isUpdatableFocusNotification(Context context, String key) {
        try {
            String settingsValue = Settings.Secure.getString(context.getContentResolver(), "updatable_focus_notifs");
            JSONArray jsonArray = new JSONArray(settingsValue);
            for (int i = 0; i < jsonArray.length(); i++) {
                if (TextUtils.equals(key, jsonArray.optString(i, ""))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.i(TAG, "updatableFocus jsonArray error", e);
            return false;
        }
    }

    public void checkFullScreenIntent(Notification notification, AppOpsManager appOpsManager, int uid, String packageName) {
        if (!Build.IS_INTERNATIONAL_BUILD && notification.fullScreenIntent != null) {
            int mode = appOpsManager.noteOpNoThrow(10021, uid, packageName, (String) null, "NotificationManagementImpl#checkFullScreenIntent");
            if (mode != 0) {
                Slog.i(TAG, "MIUILOG- Permission Denied Activity : " + notification.fullScreenIntent + " pkg : " + packageName + " uid : " + uid);
                notification.fullScreenIntent = null;
            }
        }
    }

    public boolean isAllowAppRenderedToast(String pkg, int callingUid, int userId) {
        if (sAllowToastSet.contains(pkg)) {
            PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0L, Process.myUid(), userId);
                if (info.applicationInfo.isSystemApp()) {
                    return callingUid == info.applicationInfo.uid;
                }
                return false;
            } catch (Exception e) {
                Slog.e(TAG, "can't find package!", e);
            }
        }
        return false;
    }

    public boolean isPackageDistractionRestrictionLocked(NotificationRecord r) {
        String pkg = r.getSbn().getPackageName();
        int callingUid = r.getSbn().getUid();
        return isPackageDistractionRestrictionForUser(pkg, callingUid);
    }

    private boolean isPackageDistractionRestrictionForUser(String pkg, int uid) {
        int userId = UserHandle.getUserId(uid);
        try {
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            return (pmi.getDistractingPackageRestrictions(pkg, userId) & 2) != 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void checkAllowToCreateChannels(String pkg, ParceledListSlice channelsList) {
        if (!isCallerXmsfOrSystem(pkg)) {
            List<NotificationChannel> channels = channelsList.getList();
            for (int i = 0; i < channels.size(); i++) {
                NotificationChannel channel = channels.get(i);
                if (channel != null && isXmsfChannelId(channel.getId())) {
                    throw new SecurityException("Pkg " + pkg + " cannot create channels, because " + channel.getId() + " starts with 'mipush|'");
                }
            }
        }
    }

    private boolean isCallerXmsfOrSystem(String callingPkg) {
        return isXmsf(callingPkg) || isCallerSystem();
    }

    private boolean isXmsf(String pkg) {
        return XMSF_PACKAGE_NAME.equals(pkg);
    }

    private boolean isCallerSystem() {
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        return appId == 1000;
    }

    private boolean isUidSystemOrPhone(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || appid == 1001 || uid == 0;
    }

    private boolean isXmsfChannelId(String channelId) {
        return !TextUtils.isEmpty(channelId) && channelId.startsWith(XMSF_CHANNEL_ID_PREFIX);
    }

    public boolean checkIsXmsfFakeConditionProviderEnabled(String path) {
        return XMSF_FAKE_CONDITION_PROVIDER_PATH.equals(path) && checkCallerIsXmsf();
    }

    private boolean checkCallerIsXmsf(String callingPkg, String targetPkg, int callingUid, int userId) {
        if (!isXmsf(callingPkg) || isXmsf(targetPkg)) {
            return false;
        }
        return isCallerSystem() || checkCallerIsXmsfInternal(callingUid, userId);
    }

    private boolean checkCallerIsXmsfInternal(int callingUid, int userId) {
        try {
            ApplicationInfo ai = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(XMSF_PACKAGE_NAME, 0L, callingUid, userId);
            if (ai == null || !UserHandle.isSameApp(ai.uid, callingUid)) {
                return false;
            }
            if ((ai.flags & 129) != 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDelegateAllowed(String callingPkg, int callingUid, String targetPkg, int userId) {
        return isCallerAndroid(callingPkg, callingUid) || isCallerSecurityCenter(callingPkg, callingUid) || checkCallerIsXmsf(callingPkg, targetPkg, callingUid, userId);
    }

    private boolean isCallerAndroid(String callingPkg, int callingUid) {
        return isUidSystemOrPhone(callingUid) && callingPkg != null && "android".equals(callingPkg);
    }

    private boolean isCallerSecurityCenter(String callingPkg, int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 && ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME.equals(callingPkg);
    }

    public boolean isDeniedLocalNotification(AppOpsManager appops, Notification notification, int callingUid, String callingPkg) {
        if (XMSF_PACKAGE_NAME.equals(callingPkg) || (notification.flags & 64) != 0) {
            return false;
        }
        int mode = appops.noteOpNoThrow(10033, callingUid, callingPkg);
        if (mode == 0) {
            return false;
        }
        Slog.i(TAG, "MIUILOG- Permission Denied to post local notification for " + callingPkg);
        return true;
    }

    public String getPlayVibrationPkg(String pkg, String opPkg, String defaultPkg) {
        if (isXmsf(opPkg) && !isXmsf(pkg)) {
            return pkg;
        }
        return defaultPkg;
    }

    public boolean checkCallerIsXmsf() {
        return checkCallerIsXmsfInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
    }

    public boolean shouldInjectDeleteChannel(String pkg, String channelId, String groupId) {
        if (isXmsf(pkg) && (isXmsfChannelId(channelId) || isXmsfChannelId(groupId))) {
            return true;
        }
        if (!TextUtils.isEmpty(channelId)) {
            checkAllowToDeleteChannel(pkg, channelId);
            return false;
        }
        return false;
    }

    public String getAppPkgByChannel(String channelIdOrGroupId, String defaultPkg) {
        if (!TextUtils.isEmpty(channelIdOrGroupId)) {
            String[] array = channelIdOrGroupId.split(SPLIT_CHAR);
            if (array.length >= 2) {
                return array[1];
            }
        }
        return defaultPkg;
    }

    public int getAppUidByPkg(String pkg, int defaultUid) {
        int appUid = getUidByPkg(pkg, UserHandle.getCallingUserId());
        if (appUid != -1) {
            return appUid;
        }
        return defaultUid;
    }

    private void checkAllowToDeleteChannel(String pkg, String channelId) {
        if (!isCallerXmsfOrSystem(pkg) && isXmsfChannelId(channelId)) {
            throw new SecurityException("Pkg " + pkg + " cannot delete channel " + channelId + " that starts with 'mipush|'");
        }
    }

    private int getUidByPkg(String pkg, int userId) {
        try {
            if (TextUtils.isEmpty(pkg)) {
                return -1;
            }
            int uid = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(pkg, 0L, userId);
            return uid;
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isDeniedDemoPostNotification(int userId, int uid, Notification notification, String pkg) {
        if (notification == null || !Build.IS_DEMO_BUILD || notification.isForegroundService() || hasProgress(notification) || notification.isMediaNotification() || isCallingSystem(userId, uid, pkg)) {
            return false;
        }
        Slog.i(TAG, "Denied demo product third-party app :" + pkg + "to post notification :" + notification.toString());
        return true;
    }

    private boolean hasProgress(Notification n) {
        return n.extras.containsKey("android.progress") && n.extras.containsKey("android.progressMax") && n.extras.getInt("android.progressMax") != 0 && n.extras.getInt("android.progress") != n.extras.getInt("android.progressMax");
    }

    private boolean isCallingSystem(int userId, int uid, String callingPkg) {
        int appid = UserHandle.getAppId(uid);
        if (appid == 1000 || appid == 1001 || uid == 0 || uid == 2000) {
            return true;
        }
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(callingPkg, 0L, userId);
            if (ai == null) {
                Slog.d(TAG, "Unknown package " + callingPkg);
                return false;
            }
            return ai.isSystemApp();
        } catch (Exception e) {
            Slog.e(TAG, "isCallingSystem error: " + e);
            return false;
        }
    }

    public Object getVibRateLimiter() {
        return new VibRateLimiter();
    }

    private boolean shouldSkipFrequentlyVib(NotificationRecord record, NotificationUsageStats.AggregatedStats mStats) {
        if (((VibRateLimiter) mStats.vibRate).shouldRateLimitVib(SystemClock.elapsedRealtime())) {
            mStats.numVibViolations++;
            Slog.e(TAG, "Cancel the recent frequently vibration in 15s " + record.getKey());
            return true;
        }
        return false;
    }

    public boolean checkInterceptListener(StatusBarNotification sbn, ComponentName component) {
        return checkInterceptListenerForXiaomiInternal(sbn, component) || checkInterceptListenerForMIUIInternal(sbn, component.getPackageName());
    }

    private boolean checkInterceptListenerForXiaomiInternal(StatusBarNotification sbn, ComponentName component) {
        if (!ArrayUtils.contains(this.interceptListener, component.flattenToString()) || !ArrayUtils.contains(this.interceptChannelId, sbn.getPackageName() + ENABLED_SERVICES_SEPARATOR + sbn.getNotification().getChannelId())) {
            return false;
        }
        Slog.w(TAG, "checkInterceptListenerForXiaomiInternal pkg:" + sbn.getPackageName() + "listener:" + component.flattenToString());
        return true;
    }

    private boolean checkInterceptListenerForMIUIInternal(StatusBarNotification sbn, String listener) {
        boolean z = false;
        if (!checkAppSystemOrNot(listener) && !MIUI_SYSTEM_APPS_LIST.contains(listener) && !OTHER_APPS_LIST.contains(listener) && !PreloadedAppPolicy.isProtectedDataApp((Context) null, listener, 0) && checkNotificationMasked(sbn.getPackageName(), sbn.getUid())) {
            z = true;
        }
        boolean intercept = z;
        if (intercept) {
            Slog.w(TAG, "checkInterceptListenerForMIUIInternal pkg:" + sbn.getPackageName());
        }
        return intercept;
    }

    private boolean checkNotificationMasked(String pkg, int uid) {
        boolean z;
        IBinder b = ServiceManager.getService("security");
        if (b == null) {
            return false;
        }
        try {
            int userId = UserHandle.getUserId(uid);
            ISecurityManager service = ISecurityManager.Stub.asInterface(b);
            if (service.haveAccessControlPassword(userId) && service.getApplicationAccessControlEnabledAsUser(pkg, userId)) {
                if (service.getApplicationMaskNotificationEnabledAsUser(pkg, userId)) {
                    z = true;
                    boolean result = z;
                    return result;
                }
            }
            z = false;
            boolean result2 = z;
            return result2;
        } catch (Exception e) {
            Slog.e(TAG, "check notification masked error: ", e);
            return false;
        }
    }

    private boolean checkAppSystemOrNot(String monitorPkg) {
        try {
            ApplicationInfo ai = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(monitorPkg, 0L, 1000, 0);
            if (ai == null) {
                return false;
            }
            boolean isSystem = ai.isSystemApp();
            return isSystem;
        } catch (Exception e) {
            Slog.e(TAG, "checkAppSystemOrNot : ", e);
            return false;
        }
    }

    public IBinder getColorLightManager() {
        return ((MiuiLightsManagerInternal) LocalServices.getService(MiuiLightsManagerInternal.class)).getBinderService();
    }

    public void dumpLight(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        try {
            Class<?> clazz = Class.forName("com.android.server.lights.MiuiLightsService");
            Method getInstanceMethod = clazz.getMethod("getInstance", new Class[0]);
            Method dumpLightMethod = clazz.getMethod("dumpLight", PrintWriter.class, NotificationManagerService.DumpFilter.class);
            Object instance = getInstanceMethod.invoke(null, new Object[0]);
            if (instance != null) {
                dumpLightMethod.invoke(instance, pw, filter);
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Slog.e(TAG, "Failed to invoke MiuiLightsService dumpLight method", e);
        }
    }

    public boolean enableBlockedToasts(String pkg) {
        if (this.mPrivacyInputModePkgName == null) {
            String packageName = Settings.Secure.getString(this.mContext.getContentResolver(), "miui_privacy_input_pkg_name");
            this.mPrivacyInputModePkgName = packageName == null ? "" : packageName;
        }
        return !pkg.equals(this.mPrivacyInputModePkgName);
    }

    private void registerPrivacyInputMode(Handler handler) {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_privacy_input_pkg_name"), false, new ContentObserver(handler) { // from class: com.android.server.notification.NotificationManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String pkg = Settings.Secure.getString(NotificationManagerServiceImpl.this.mContext.getContentResolver(), "miui_privacy_input_pkg_name");
                NotificationManagerServiceImpl.this.mPrivacyInputModePkgName = pkg == null ? "" : pkg;
                Slog.d(NotificationManagerServiceImpl.TAG, "onChange:" + pkg);
            }
        });
    }

    private void registerCloudDataResolver(Handler handler) {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, new ContentObserver(handler) { // from class: com.android.server.notification.NotificationManagerServiceImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                NotificationManagerServiceImpl.this.updateCloudData();
            }
        });
    }

    public Pair<Integer, String> getMiuiDefaultListerWithVersion() {
        String defaultListenerAccess = this.mContext.getResources().getString(286195876);
        if (defaultListenerAccess != null) {
            String[] listenersWithVersion = defaultListenerAccess.split(",");
            Slog.d(TAG, "listenersWithVersion " + Arrays.toString(listenersWithVersion));
            if (listenersWithVersion != null) {
                int version = -1;
                String listeners = null;
                if (listenersWithVersion.length == 2) {
                    listeners = listenersWithVersion[0];
                    version = Integer.valueOf(listenersWithVersion[1]).intValue();
                } else if (listenersWithVersion.length == 1) {
                    listeners = listenersWithVersion[0];
                    version = 0;
                }
                return new Pair<>(Integer.valueOf(version), listeners);
            }
        }
        return new Pair<>(-1, null);
    }

    public void readDefaultsAndFixMiuiVersion(ManagedServices services, String defaultComponents, TypedXmlPullParser parser) {
        ComponentName cn;
        boolean isEnableListener = parser.getName().equals("enabled_listeners");
        if (!TextUtils.isEmpty(defaultComponents)) {
            boolean hasBarrage = false;
            String[] components = defaultComponents.split(ENABLED_SERVICES_SEPARATOR);
            for (int i = 0; i < components.length; i++) {
                if (!TextUtils.isEmpty(components[i]) && (cn = ComponentName.unflattenFromString(components[i])) != null && cn.getPackageName().equals("com.xiaomi.barrage") && isEnableListener) {
                    hasBarrage = true;
                }
            }
            if (!hasBarrage && services.mVersionStub.getMiuiVersion() != -1) {
                services.mVersionStub.setMiuiVersion(-1);
                return;
            }
            return;
        }
        if (isEnableListener && services.mVersionStub.getMiuiVersion() != -1) {
            services.mVersionStub.setMiuiVersion(-1);
        }
    }

    public NotificationRecord.Light customizeNotificationLight(Context context, NotificationRecord.Light originLight) {
        String jsonText = Settings.Secure.getStringForUser(context.getContentResolver(), BREATHING_LIGHT, -2);
        if (TextUtils.isEmpty(jsonText)) {
            return originLight;
        }
        int customizeColor = originLight.color;
        int customizeOnMs = originLight.onMs;
        try {
            JSONArray jsonArray = new JSONArray(jsonText);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject logObject = jsonArray.getJSONObject(i);
                if (logObject.getInt("light") == 4) {
                    customizeColor = logObject.getInt("color");
                    customizeOnMs = logObject.getInt("onMS");
                }
            }
        } catch (JSONException e) {
            Slog.i(TAG, "Light jsonArray error", e);
        }
        return new NotificationRecord.Light(customizeColor, customizeOnMs, originLight.offMs);
    }

    public void fixCheckInterceptListenerAutoGroup(Notification notification, String pkg) {
        try {
            if (notification.getChannelId() != null && ArrayUtils.contains(this.interceptChannelId, pkg + ENABLED_SERVICES_SEPARATOR + notification.getChannelId())) {
                Class notificationClazz = notification.getClass();
                Field mGroupKeyField = notificationClazz.getDeclaredField("mGroupKey");
                mGroupKeyField.setAccessible(true);
                mGroupKeyField.set(notification, notification.getChannelId() + "_autogroup");
            }
        } catch (Exception e) {
            Slog.i(TAG, " fixCheckInterceptListenerGroup exception:", e);
        }
    }

    public void setInterceptChannelId(String[] interceptChannelId) {
        this.interceptChannelId = interceptChannelId;
    }

    public void isAllowAppNotificationListener(boolean isPackageAdded, NotificationManagerService.NotificationListeners mListeners, int userId, String pkg) {
        String[] strArr;
        if (isPackageAdded && (strArr = this.allowNotificationAccessList) != null && ArrayUtils.contains(strArr, pkg)) {
            NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
            ArraySet<ComponentName> listeners = mListeners.queryPackageForServices(pkg, 786432, 0);
            for (int k = 0; k < listeners.size(); k++) {
                ComponentName cn = listeners.valueAt(k);
                try {
                    nm.setNotificationListenerAccessGrantedForUser(cn, userId, true);
                } catch (Exception e) {
                    Slog.i(TAG, " isAllowAppNotificationListener exception:", e);
                }
            }
        }
    }

    public boolean checkMediaNotificationControl(int uid, String pkg, PreferencesHelper mPreferencesHelper) {
        if (mPreferencesHelper.getMediaNotificationsEnabled(pkg, uid)) {
            if (!ArrayUtils.contains(allowMediaNotificationCloudDataList, pkg)) {
                return true;
            }
            Slog.w(TAG, "checkMediaNotificationControl pkg:" + pkg + " This media notification is in the blacklist, and media notifications are not allowed to be sent! backlist is " + allowMediaNotificationCloudDataList);
            return false;
        }
        Slog.w(TAG, "checkMediaNotificationControl pkg:" + pkg + " This media notification media notification switch closed, and notifications are not allowed to be sent!");
        return false;
    }

    public void setMediaNotificationEnabled(String pkg, int uid, boolean enabled, PreferencesHelper mPreferencesHelper) {
        mPreferencesHelper.setMediaNotificationEnabled(pkg, uid, enabled);
        this.mNMS.handleSavePolicyFile();
    }

    public boolean getMediaNotificationsEnabled(String pkg, int uid, PreferencesHelper mPreferencesHelper) {
        return checkMediaNotificationControl(uid, pkg, mPreferencesHelper);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudData() {
        String updateNotificationCloudData = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), MODULE_MEDIA_NOTIFICATION_CLOUD_LIST, KEY_MEDIA_NOTIFICATION_CLOUD, "");
        allowMediaNotificationCloudDataList = getMediaNotificationDataCloudConversion(updateNotificationCloudData);
    }

    public static List<String> getMediaNotificationDataCloudConversion(String allowMediaNotificationCloudData) {
        List<String> mediationNotificationDataFromCloud = new ArrayList<>();
        try {
            if (!TextUtils.isEmpty(allowMediaNotificationCloudData)) {
                JSONArray dataConversion = new JSONArray(allowMediaNotificationCloudData);
                for (int i = 0; i < dataConversion.length(); i++) {
                    mediationNotificationDataFromCloud.add(dataConversion.getString(i));
                }
            } else {
                allowMediaNotificationCloudDataList.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when get getMediaNotificationDataCloudConversion :", e);
        }
        return mediationNotificationDataFromCloud;
    }
}

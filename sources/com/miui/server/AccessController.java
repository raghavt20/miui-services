package com.miui.server;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MiuiSettings;
import android.security.AccessControlKeystoreHelper;
import android.security.MiuiLockPatternUtils;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockscreenCredential;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.FoldablePackagePolicy;
import com.miui.server.security.DefaultBrowserImpl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AccessController {
    private static final String ACCESS_CONTROL = "access_control.key";
    private static final String ACCESS_CONTROL_KEYSTORE = "access_control_keystore.key";
    private static final String ACCESS_CONTROL_PASSWORD_TYPE_KEY = "access_control_password_type.key";
    private static final String APPLOCK_WHILTE = "applock_whilte";
    public static final String APP_LOCK_CLASSNAME = "com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl";
    public static final boolean DEBUG = false;
    private static final String GAMEBOOSTER_ANTIMSG = "gamebooster_antimsg";
    private static final String GAMEBOOSTER_PAY = "gamebooster_pay";
    public static final String PACKAGE_CAMERA = "com.android.camera";
    public static final String PACKAGE_GALLERY = "com.miui.gallery";
    public static final String PACKAGE_MEITU_CAMERA = "com.mlab.cam";
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String PASSWORD_TYPE_PATTERN = "pattern";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EDIT = "com.miui.gallery.editor.photo.screen.home.ScreenEditorActivity";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA = "com.miui.gallery.activity.ExternalPhotoPageActivity";
    public static final String SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA_TRANSLUCENT = "com.miui.gallery.activity.TranslucentExternalPhotoPageActivity";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TAG = "AccessController";
    private static final long UPDATE_EVERY_DELAY = 43200000;
    private static final long UPDATE_FIRT_DELAY = 180000;
    private static final int UPDATE_WHITE_LIST = 1;
    private static final String WECHAT_VIDEO_ACTIVITY_CLASSNAME = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    private static Method mPasswordToHash;
    private Context mContext;
    private final Object mFileWriteLock = new Object();
    private final AccessControlKeystoreHelper mKeystoreHelper;
    private LockPatternUtils mLockPatternUtils;
    private WorkHandler mWorkHandler;
    private static ArrayMap<String, ArrayList<Intent>> mSkipList = new ArrayMap<>();
    private static ArrayMap<String, ArrayList<Intent>> mAntimsgInterceptList = new ArrayMap<>();
    private static ArrayMap<String, ArrayList<Intent>> mPayInterceptList = new ArrayMap<>();
    private static List<String> mForceLaunchList = new ArrayList();

    static {
        ArrayList<Pair<String, String>> passList = new ArrayList<>();
        passList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteLock"));
        passList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteFull"));
        passList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.av.ui.VideoInviteActivity"));
        passList.add(new Pair<>("com.tencent.mm", WECHAT_VIDEO_ACTIVITY_CLASSNAME));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.multitalk.ui.MultiTalkMainUI"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.UIEntryStub"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.webview.ui.tools.SDKOAuthUI"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.WXPayEntryActivity"));
        passList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.wallet_index.ui.OrderHandlerUI"));
        passList.add(new Pair<>("com.whatsapp", "com.whatsapp.VoipActivity"));
        passList.add(new Pair<>("com.whatsapp", "com.whatsapp.voipcalling.VoipActivityV2"));
        passList.add(new Pair<>("jp.naver.line.android", "jp.naver.line.android.freecall.FreeCallActivity"));
        passList.add(new Pair<>("com.bbm", "com.bbm.ui.voice.activities.IncomingCallActivity"));
        passList.add(new Pair<>("com.xiaomi.channel", "com.xiaomi.channel.voip.VoipCallActivity"));
        passList.add(new Pair<>("com.facebook.orca", "com.facebook.rtc.activities.WebrtcIncallActivity"));
        passList.add(new Pair<>("com.bsb.hike", "com.bsb.hike.voip.view.VoIPActivity"));
        passList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.android.app.TransProcessPayActivity"));
        passList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.mobile.security.login.ui.AlipayUserLoginActivity"));
        passList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.mobile.bill.detail.ui.EmptyActivity_"));
        passList.add(new Pair<>("com.xiaomi.smarthome", "com.xiaomi.smarthome.miio.activity.ClientAllLockedActivity"));
        passList.add(new Pair<>("com.google.android.dialer", "com.android.dialer.incall.activity.ui.InCallActivity"));
        passList.add(new Pair<>("com.android.settings", "com.android.settings.FallbackHome"));
        passList.add(new Pair<>("com.android.mms", "com.android.mms.ui.DummyActivity"));
        passList.add(new Pair<>("com.android.mms", "com.android.mms.ui.ComposeMessageRouterActivity"));
        passList.add(new Pair<>("com.xiaomi.jr", "com.xiaomi.jr.EntryActivity"));
        passList.add(new Pair<>("com.android.settings", "com.android.settings.MiuiConfirmCommonPassword"));
        Iterator<Pair<String, String>> it = passList.iterator();
        while (it.hasNext()) {
            Pair<String, String> pair = it.next();
            ArrayList<Intent> intents = mSkipList.get(pair.first);
            if (intents == null) {
                intents = new ArrayList<>(1);
                mSkipList.put((String) pair.first, intents);
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName((String) pair.first, (String) pair.second));
            intents.add(intent);
        }
        ArrayList<Pair<String, String>> payList = new ArrayList<>();
        payList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.WXPayEntryActivity"));
        payList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.WXEntryActivity"));
        payList.add(new Pair<>("com.tencent.mm", "com.tencent.mm.plugin.base.stub.WXCustomSchemeEntryActivity"));
        payList.add(new Pair<>("com.tencent.mobileqq", "cooperation.qwallet.open.AppPayActivity"));
        payList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.open.agent.AgentActivity"));
        payList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"));
        payList.add(new Pair<>("com.sina.weibo", "com.sina.weibo.composerinde.ComposerDispatchActivity"));
        payList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.mobile.quinox.SchemeLauncherActivity"));
        payList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.ali.user.mobile.loginupgrade.activity.GuideActivity"));
        payList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.ali.user.mobile.loginupgrade.activity.LoginActivity"));
        payList.add(new Pair<>(ActivityStarterImpl.PACKAGE_NAME_ALIPAY, "com.alipay.mobile.security.login.ui.RecommandAlipayUserLoginActivity"));
        payList.add(new Pair<>("com.tencent.mobileqq", "com.tencent.mobileqq.activity.LoginActivity"));
        Iterator<Pair<String, String>> it2 = payList.iterator();
        while (it2.hasNext()) {
            Pair<String, String> pair2 = it2.next();
            ArrayList<Intent> intents2 = mPayInterceptList.get(pair2.first);
            if (intents2 == null) {
                intents2 = new ArrayList<>(1);
                mPayInterceptList.put((String) pair2.first, intents2);
            }
            Intent intent2 = new Intent();
            intent2.setComponent(new ComponentName((String) pair2.first, (String) pair2.second));
            intents2.add(intent2);
        }
        try {
            Log.i(TAG, "Nowhere to call this method, passwordToHash should not init");
        } catch (Exception e) {
            Log.e(TAG, " passwordToHash static invoke error", e);
        }
        mForceLaunchList.add("com.tencent.mobileqq/.activity.JumpActivity");
        mForceLaunchList.add("com.tencent.mm/.plugin.base.stub.WXCustomSchemeEntryActivity");
        mForceLaunchList.add("com.sina.weibo/.composerinde.ComposerDispatchActivity");
        mForceLaunchList.add("com.tencent.mobileqq/com.tencent.open.agent.AgentActivity");
        mForceLaunchList.add("com.eg.android.AlipayGphone/com.alipay.android.msp.ui.views.MspContainerActivity");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AccessController.this.updateWhiteList();
                    return;
                default:
                    return;
            }
        }
    }

    public AccessController(Context context, Looper looper) {
        this.mContext = context;
        WorkHandler workHandler = new WorkHandler(looper);
        this.mWorkHandler = workHandler;
        workHandler.sendEmptyMessageDelayed(1, UPDATE_FIRT_DELAY);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeystoreHelper = new AccessControlKeystoreHelper();
    }

    public boolean filterIntentLocked(boolean isSkipIntent, String packageName, Intent intent) {
        return filterIntentLocked(isSkipIntent, false, packageName, intent);
    }

    public boolean filterIntentLocked(boolean isSkipIntent, boolean pay, String packageName, Intent intent) {
        String rawPackageName;
        ArrayList<Intent> intents;
        String fullName;
        if (intent == null) {
            return false;
        }
        synchronized (this) {
            if (pay) {
                try {
                    Intent rawIntent = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
                    if (rawIntent != null && rawIntent.getComponent() != null && TextUtils.equals(APP_LOCK_CLASSNAME, intent.getComponent().flattenToShortString())) {
                        rawPackageName = rawIntent.getComponent().getPackageName();
                    } else {
                        rawPackageName = packageName;
                        rawIntent = intent;
                    }
                    if (DefaultBrowserImpl.isDefaultBrowser(this.mContext, rawPackageName)) {
                        Uri uri = rawIntent.getData();
                        return (uri == null || uri.getHost() == null || uri.getScheme() == null || rawIntent.getAction() == null || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) ? false : true;
                    }
                } finally {
                }
            }
            Intent oriIntent = intent;
            if (isSkipIntent) {
                intents = mSkipList.get(packageName);
            } else if (pay) {
                Intent rawIntent2 = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
                String rawPackageName2 = null;
                if (rawIntent2 != null && rawIntent2.getComponent() != null) {
                    rawPackageName2 = rawIntent2.getComponent().getPackageName();
                }
                if (intent.getComponent() != null && TextUtils.equals(APP_LOCK_CLASSNAME, intent.getComponent().flattenToShortString()) && mPayInterceptList.containsKey(rawPackageName2)) {
                    oriIntent = rawIntent2;
                    intents = mPayInterceptList.get(rawPackageName2);
                } else {
                    intents = mPayInterceptList.get(packageName);
                }
            } else {
                intents = mAntimsgInterceptList.get(packageName);
            }
            if (intents == null) {
                return false;
            }
            String action = oriIntent.getAction();
            ComponentName component = oriIntent.getComponent();
            if (action != null) {
                Iterator<Intent> it = intents.iterator();
                while (it.hasNext()) {
                    Intent i = it.next();
                    if (action.equals(i.getAction())) {
                        return true;
                    }
                }
            }
            if (component != null) {
                String cls = component.getClassName();
                if (cls == null) {
                    return false;
                }
                if (cls.charAt(0) == '.') {
                    fullName = component.getPackageName() + cls;
                } else {
                    fullName = cls;
                }
                if (!isSkipIntent && WECHAT_VIDEO_ACTIVITY_CLASSNAME.equals(cls) && (intent.getFlags() & (-268435457)) == 0) {
                    return false;
                }
                Iterator<Intent> it2 = intents.iterator();
                while (it2.hasNext()) {
                    Intent i2 = it2.next();
                    ComponentName c = i2.getComponent();
                    if (c != null && fullName.equals(c.getClassName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean skipActivity(Intent intent, String callingPkg) {
        if (intent != null) {
            try {
                ComponentName componentName = intent.getComponent();
                if (componentName != null) {
                    String packageName = componentName.getPackageName();
                    String activity = componentName.getClassName();
                    if (isOpenedPkg(callingPkg) && !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(activity) && PACKAGE_GALLERY.equals(packageName) && isOpenedActivity(activity)) {
                        return intent.getBooleanExtra("skip_interception", false);
                    }
                    return false;
                }
            } catch (Throwable e) {
                Slog.e(TAG, "can not getStringExtra" + e);
            }
        }
        return false;
    }

    private static boolean isOpenedPkg(String callingPkg) {
        return PACKAGE_GALLERY.equals(callingPkg) || PACKAGE_SYSTEMUI.equals(callingPkg) || PACKAGE_CAMERA.equals(callingPkg) || PACKAGE_MEITU_CAMERA.equals(callingPkg);
    }

    private static boolean isOpenedActivity(String activity) {
        return SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA.equals(activity) || SKIP_INTERCEPT_ACTIVITY_GALLERY_EXTRA_TRANSLUCENT.equals(activity) || SKIP_INTERCEPT_ACTIVITY_GALLERY_EDIT.equals(activity);
    }

    private void saveAccessControlPasswordByKeystore(String password, int userId) {
        byte[] cipherText;
        if (this.mKeystoreHelper.disableEncryptByKeystore(this.mContext)) {
            return;
        }
        Slog.i(TAG, "saveAccessControlPasswordByKeystore " + userId);
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_KEYSTORE);
        String alias = "alias_app_lock_" + userId;
        if (password == null) {
            cipherText = this.mKeystoreHelper.resetPassword(alias);
        } else {
            cipherText = this.mKeystoreHelper.encrypt(alias, password);
        }
        writeFile(filePath, cipherText);
    }

    private boolean checkAccessControlPasswordByKeystore(String password, int userId) {
        if (this.mKeystoreHelper.disableEncryptByKeystore(this.mContext)) {
            return false;
        }
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_KEYSTORE);
        byte[] readFile = readFile(filePath);
        Slog.i(TAG, "checkAccessControlPasswordByKeystore, " + userId);
        if (readFile == null || readFile.length == 0) {
            Slog.e(TAG, "readFile error!");
            return false;
        }
        String alias = "alias_app_lock_" + userId;
        byte[] cipherText = this.mKeystoreHelper.encrypt(alias, password);
        return Arrays.equals(cipherText, readFile);
    }

    private void setAccessControlPattern(String pattern, int userId) {
        byte[] hash = null;
        if (pattern != null) {
            List<LockPatternView.Cell> stringToPattern = MiuiLockPatternUtils.stringToPattern(pattern);
            hash = MiuiLockPatternUtils.patternToHash(stringToPattern);
        }
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        writeFile(filePath, hash);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAccessControlPassword(String passwordType, String password, int userId) {
        saveAccessControlPasswordByKeystore(password, userId);
        if (PASSWORD_TYPE_PATTERN.equals(passwordType)) {
            setAccessControlPattern(password, userId);
        } else {
            byte[] hash = null;
            if (password != null) {
                hash = passwordToHash(password, userId);
            }
            String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
            writeFile(filePath, hash);
        }
        setAccessControlPasswordType(passwordType, userId);
    }

    private boolean checkAccessControlPattern(String pattern, int userId) {
        if (pattern == null) {
            return false;
        }
        List<LockPatternView.Cell> stringToPattern = MiuiLockPatternUtils.stringToPattern(pattern);
        byte[] hash = MiuiLockPatternUtils.patternToHash(stringToPattern);
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        byte[] readFile = readFile(filePath);
        return Arrays.equals(readFile, hash);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkAccessControlPassword(String passwordType, String password, int userId) {
        if (password == null || passwordType == null) {
            return false;
        }
        if (checkAccessControlPasswordByKeystore(password, userId)) {
            return true;
        }
        Slog.i(TAG, "checkAccessControlPassword!");
        if (PASSWORD_TYPE_PATTERN.equals(passwordType)) {
            return checkAccessControlPattern(password, userId);
        }
        byte[] hash = passwordToHash(password, userId);
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        byte[] readFile = readFile(filePath);
        return Arrays.equals(readFile, hash);
    }

    private boolean haveAccessControlPattern(int userId) {
        boolean z;
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL);
        synchronized (this.mFileWriteLock) {
            File file = new File(filePath);
            z = file.exists() && file.length() > 0;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean haveAccessControlPassword(int userId) {
        boolean z;
        String filePathType = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        String filePathPassword = getFilePathForUser(userId, ACCESS_CONTROL);
        synchronized (this.mFileWriteLock) {
            File fileType = new File(filePathType);
            File filePassword = new File(filePathPassword);
            z = fileType.exists() && filePassword.exists() && fileType.length() > 0 && filePassword.length() > 0;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getAccessControlPasswordType(int userId) {
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        if (filePath == null) {
            return PASSWORD_TYPE_PATTERN;
        }
        return readTypeFile(filePath);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePasswordTypeForPattern(int userId) {
        if (haveAccessControlPattern(userId) && !haveAccessControlPasswordType(userId)) {
            setAccessControlPasswordType(PASSWORD_TYPE_PATTERN, userId);
            Log.d(TAG, "update password type succeed");
        }
    }

    private void setAccessControlPasswordType(String passwordType, int userId) {
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        writeTypeFile(filePath, passwordType);
    }

    private boolean haveAccessControlPasswordType(int userId) {
        boolean z;
        String filePath = getFilePathForUser(userId, ACCESS_CONTROL_PASSWORD_TYPE_KEY);
        synchronized (this.mFileWriteLock) {
            File file = new File(filePath);
            z = file.exists() && file.length() > 0;
        }
        return z;
    }

    private byte[] readFile(String name) {
        byte[] stored;
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            stored = null;
            try {
                try {
                    raf = new RandomAccessFile(name, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                    stored = new byte[(int) raf.length()];
                    raf.readFully(stored, 0, stored.length);
                    raf.close();
                    try {
                        raf.close();
                    } catch (IOException e) {
                        str = TAG;
                        str2 = "Error closing file " + e;
                        Slog.e(str, str2);
                        return stored;
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Cannot read file " + e2);
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e3) {
                            str = TAG;
                            str2 = "Error closing file " + e3;
                            Slog.e(str, str2);
                            return stored;
                        }
                    }
                }
            } finally {
            }
        }
        return stored;
    }

    private String readTypeFile(String name) {
        String stored;
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            stored = null;
            try {
                try {
                    raf = new RandomAccessFile(name, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                    stored = raf.readLine();
                    raf.close();
                    try {
                        raf.close();
                    } catch (IOException e) {
                        str = TAG;
                        str2 = "Error closing file " + e;
                        Slog.e(str, str2);
                        return stored;
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Cannot read file " + e2);
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e3) {
                            str = TAG;
                            str2 = "Error closing file " + e3;
                            Slog.e(str, str2);
                            return stored;
                        }
                    }
                }
            } finally {
            }
        }
        return stored;
    }

    private void writeFile(String name, byte[] hash) {
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            try {
                try {
                    raf = new RandomAccessFile(name, "rw");
                    raf.setLength(0L);
                    if (hash != null) {
                        raf.write(hash, 0, hash.length);
                    }
                    raf.close();
                    try {
                        raf.close();
                    } catch (IOException e) {
                        str = TAG;
                        str2 = "Error closing file " + e;
                        Slog.e(str, str2);
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Error writing to file " + e2);
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e3) {
                            str = TAG;
                            str2 = "Error closing file " + e3;
                            Slog.e(str, str2);
                        }
                    }
                }
            } finally {
            }
        }
    }

    private void writeTypeFile(String name, String passwordType) {
        String str;
        String str2;
        synchronized (this.mFileWriteLock) {
            RandomAccessFile raf = null;
            try {
                try {
                    raf = new RandomAccessFile(name, "rw");
                    raf.setLength(0L);
                    if (passwordType != null) {
                        raf.writeBytes(passwordType);
                    }
                    raf.close();
                    try {
                        raf.close();
                    } catch (IOException e) {
                        str = TAG;
                        str2 = "Error closing type file " + e;
                        Slog.e(str, str2);
                    }
                } catch (IOException e2) {
                    Slog.e(TAG, "Error writing type to file " + e2);
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e3) {
                            str = TAG;
                            str2 = "Error closing type file " + e3;
                            Slog.e(str, str2);
                        }
                    }
                }
            } finally {
            }
        }
    }

    private String getFilePathForUser(int userId, String fileName) {
        String dataSystemDirectory = Environment.getDataDirectory().getAbsolutePath() + SYSTEM_DIRECTORY;
        if (userId == 0) {
            return dataSystemDirectory + fileName;
        }
        return new File(Environment.getUserSystemDirectory(userId), fileName).getAbsolutePath();
    }

    public void updateWhiteList() {
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            this.mWorkHandler.removeMessages(1);
            this.mWorkHandler.sendEmptyMessageDelayed(1, UPDATE_EVERY_DELAY);
            List<MiuiSettings.SettingsCloudData.CloudData> appLockList = MiuiSettings.SettingsCloudData.getCloudDataList(resolver, APPLOCK_WHILTE);
            MiuiSettings.SettingsCloudData.getCloudDataList(resolver, GAMEBOOSTER_ANTIMSG);
            List<MiuiSettings.SettingsCloudData.CloudData> gamePayList = MiuiSettings.SettingsCloudData.getCloudDataList(resolver, GAMEBOOSTER_PAY);
            updateWhiteList(appLockList, mSkipList);
            updateWhiteList(gamePayList, mPayInterceptList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWhiteList(List<MiuiSettings.SettingsCloudData.CloudData> dataList, ArrayMap<String, ArrayList<Intent>> list) {
        if (dataList == null) {
            return;
        }
        try {
            if (dataList.size() == 0) {
                return;
            }
            ArrayMap<String, ArrayList<Intent>> cloudList = new ArrayMap<>();
            for (MiuiSettings.SettingsCloudData.CloudData data : dataList) {
                String json = data.toString();
                if (!TextUtils.isEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    String pkg = jsonObject.optString("pkg");
                    String cls = jsonObject.optString("cls");
                    String action = jsonObject.optString("act");
                    Intent intent = new Intent();
                    if (!TextUtils.isEmpty(action)) {
                        intent.setAction(action);
                    } else {
                        intent.setComponent(new ComponentName(pkg, cls));
                    }
                    ArrayList<Intent> intents = cloudList.get(pkg);
                    if (intents == null) {
                        intents = new ArrayList<>(1);
                        cloudList.put(pkg, intents);
                    }
                    intents.add(intent);
                }
            }
            if (cloudList.size() > 0) {
                synchronized (this) {
                    list.clear();
                    list.putAll((ArrayMap<? extends String, ? extends ArrayList<Intent>>) cloudList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] passwordToHash(String password, int userId) {
        if (TextUtils.isEmpty(password)) {
            return null;
        }
        try {
            Method getSalt = LockPatternUtils.class.getDeclaredMethod("getSalt", Integer.TYPE);
            getSalt.setAccessible(true);
            String salt = (String) getSalt.invoke(this.mLockPatternUtils, Integer.valueOf(userId));
            Object hash = LockscreenCredential.legacyPasswordToHash(password.getBytes(), salt.getBytes());
            if (hash != null) {
                return ((String) hash).getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Log.e(TAG, " passwordToHash invoke error", e);
        }
        return null;
    }

    public List<String> getForceLaunchList() {
        return mForceLaunchList;
    }
}

package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.fingerprint.IFingerprintService;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.MiuiSettings;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController;
import com.android.server.wm.MiuiSizeCompatService;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MiuiFingerprintCloudController {
    private static final String ANTI_MISTOUCH_ENABLE = "enable";
    private static final String ANTI_MISTOUCH_VALID_TIMES = "valid_times_per_screenlock";
    private static final String CLOUD_BACKUP_FILE_NAME = "fingerprint_cloud_backup.xml";
    public static final long CLOUD_EVENTS_ANTI_MISTOUCH_CONTROL = 1;
    public static final long CLOUD_EVENTS_LOWBRIGHTNESS_AUTH_CONTROL = 2;
    public static final long CLOUD_EVENTS_OCP_DETECT_CONTROL = 4;
    private static final String COUND_FP_FEATURE_ANTI_MISTOUCH = "anti_mistouch";
    private static final String COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH = "lowbrightness_auth";
    private static final String COUND_FP_FEATURE_OCP_DETECT = "ocp_detect";
    private static final String COUND_FP_SUBMODULE_NAME = "fingerprint_feature_control";
    public static final int FP_CLOUD_ANTI_MISTOUCH_CMD = 14;
    public static final int FP_CLOUD_ANTI_MISTOUCH_DISABLE_PARAM = 0;
    public static final int FP_CLOUD_ANTI_MISTOUCH_ENABLE_PARAM = 16;
    public static final int FP_CLOUD_LOWBRIGHTNESS_AUTH_CMD = 15;
    public static final int FP_CLOUD_LOWBRIGHTNESS_AUTH_DISABLE_PARAM = 0;
    public static final int FP_CLOUD_LOWBRIGHTNESS_AUTH_ENABLE_PARAM = 16;
    public static final int FP_CLOUD_LOWBRIGHTNESS_AUTH_SETTING_DISABLE_DEFAULT_PARAM = 1;
    public static final int FP_CLOUD_LOWBRIGHTNESS_AUTH_SETTING_ENABLE_DEFAULT_PARAM = 17;
    public static final int FP_CLOUD_OCP_DETECT_CMD = 11;
    public static final int FP_CLOUD_OCP_DETECT_DISABLE_PARAM = 0;
    public static final int FP_CLOUD_OCP_DETECT_ENABLE_PARAM = 16;
    public static final int FP_CLOUD_OCP_DETECT_SETTING_DISABLE_DEFAULT_PARAM = 1;
    public static final int FP_CLOUD_OCP_DETECT_SETTING_ENABLE_DEFAULT_PARAM = 17;
    private static final String LOWBRIGHTNESS_AUTH_ENABLE = "enable";
    private static final String LOWBRIGHTNESS_AUTH_SETTING_ENABLE_DEFAULT = "setting_enable_default";
    private static final String OCP_DETECT_ENABLE = "enable";
    private static final String OCP_DETECT_SETTING_ENABLE_DEFAULT = "setting_enable_default";
    private static final String TAG = "MiuiFingerprintCloudController";
    private AtomicFile mCloudBackupXmlFile;
    private Map<String, Object> mCloudEventsData;
    private long mCloudEventsSummary;
    private Context mContext;
    private FingerprintAntiMistouch mFingerprintAntiMistouch;
    private FingerprintLowBrightnessAuth mFingerprintLowBrightnessAuth;
    private FingerprintOcpDetect mFingerprintOcpDetect;
    private Handler mHandler;
    public static int FP_CLOUD_ANTI_MISTOUCH_VALID_TIMES_PARAM = 1;
    private static IFingerprintService mFingerprintService = null;
    private static MiuiFingerprintCloudController mInstance = null;
    private boolean mAntiMistouchEnable = false;
    private int mAntiMistouchValidTimes = 0;
    private boolean mLowBrightnessAuthEnable = false;
    private boolean mLowBrightnessAuthSettingEnableDefault = false;
    private boolean mOcpDetectEnable = false;
    private boolean mOcpDetectSettingEnableDefault = false;
    private List<CloudListener> mCloudListeners = new ArrayList();

    /* loaded from: classes.dex */
    public interface CloudListener {
        void onCloudUpdated(long j, Map<String, Object> map);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public MiuiFingerprintCloudController(Looper looper, Context context) {
        this.mContext = context;
        Slog.i(TAG, "construct.");
        this.mCloudEventsData = new HashMap();
        FingerprintAntiMistouch fingerprintAntiMistouch = new FingerprintAntiMistouch();
        this.mFingerprintAntiMistouch = fingerprintAntiMistouch;
        addCloudListener(fingerprintAntiMistouch);
        FingerprintLowBrightnessAuth fingerprintLowBrightnessAuth = new FingerprintLowBrightnessAuth();
        this.mFingerprintLowBrightnessAuth = fingerprintLowBrightnessAuth;
        addCloudListener(fingerprintLowBrightnessAuth);
        FingerprintOcpDetect fingerprintOcpDetect = new FingerprintOcpDetect();
        this.mFingerprintOcpDetect = fingerprintOcpDetect;
        addCloudListener(fingerprintOcpDetect);
        this.mCloudBackupXmlFile = getFile(CLOUD_BACKUP_FILE_NAME);
        this.mHandler = new Handler(looper);
        registerCloudDataObserver();
    }

    public IFingerprintService getFingerprintService(Context context) {
        return IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
    }

    private void registerCloudDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(this.mHandler));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        AnonymousClass1(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Slog.i(MiuiFingerprintCloudController.TAG, "onChange.");
            boolean changed = MiuiFingerprintCloudController.this.updateDataFromCloudControl();
            if (changed) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiFingerprintCloudController.AnonymousClass1.this.lambda$onChange$0();
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0() {
            MiuiFingerprintCloudController.this.syncLocalBackupFromCloud();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateDataFromCloudControl() {
        Slog.i(TAG, "updateDataFromCloudControl.");
        if (mFingerprintService == null) {
            mFingerprintService = getFingerprintService(this.mContext);
        }
        this.mCloudEventsSummary = 0L;
        this.mCloudEventsData.clear();
        boolean updated = updateAntiMistouchCloudState();
        boolean updated2 = updated | updateAntiLowBrightnessAuthCloudState() | updateOcpDetectCloudState();
        this.mCloudListeners.forEach(new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiFingerprintCloudController.this.lambda$updateDataFromCloudControl$0((MiuiFingerprintCloudController.CloudListener) obj);
            }
        });
        return updated2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateDataFromCloudControl$0(CloudListener l) {
        l.onCloudUpdated(this.mCloudEventsSummary, this.mCloudEventsData);
    }

    private AtomicFile getFile(String fileName) {
        return new AtomicFile(new File(Environment.buildPath(Environment.getDataSystemDirectory(), new String[]{"fingerprintconfig"}), fileName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncLocalBackupFromCloud() {
        if (this.mCloudBackupXmlFile == null) {
            return;
        }
        FileOutputStream outputStream = null;
        try {
            Slog.i(TAG, "Start syncing local backup from cloud.");
            outputStream = this.mCloudBackupXmlFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(outputStream);
            out.startDocument((String) null, true);
            out.setFeature(MiuiSizeCompatService.FAST_XML, true);
            out.startTag((String) null, COUND_FP_SUBMODULE_NAME);
            if ((this.mCloudEventsSummary & 1) != 0) {
                writeFeatureBooleanToXml(this.mCloudBackupXmlFile, outputStream, out, MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, COUND_FP_FEATURE_ANTI_MISTOUCH, this.mAntiMistouchEnable);
                writeFeatureIntToXml(this.mCloudBackupXmlFile, outputStream, out, ANTI_MISTOUCH_VALID_TIMES, COUND_FP_FEATURE_ANTI_MISTOUCH, this.mAntiMistouchValidTimes);
            }
            if ((this.mCloudEventsSummary & 2) != 0) {
                writeFeatureBooleanToXml(this.mCloudBackupXmlFile, outputStream, out, MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH, this.mLowBrightnessAuthEnable);
                writeFeatureBooleanToXml(this.mCloudBackupXmlFile, outputStream, out, "setting_enable_default", COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH, this.mLowBrightnessAuthSettingEnableDefault);
            }
            if ((this.mCloudEventsSummary & 4) != 0) {
                writeFeatureBooleanToXml(this.mCloudBackupXmlFile, outputStream, out, MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, COUND_FP_FEATURE_OCP_DETECT, this.mOcpDetectEnable);
                writeFeatureBooleanToXml(this.mCloudBackupXmlFile, outputStream, out, "setting_enable_default", COUND_FP_FEATURE_OCP_DETECT, this.mOcpDetectSettingEnableDefault);
            }
            out.endTag((String) null, COUND_FP_SUBMODULE_NAME);
            out.endDocument();
            outputStream.flush();
            this.mCloudBackupXmlFile.finishWrite(outputStream);
        } catch (IOException e) {
            this.mCloudBackupXmlFile.failWrite(outputStream);
            Slog.e(TAG, "Failed to write local backup" + e);
        }
    }

    private void writeFeatureBooleanToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, boolean enable) {
        try {
            out.startTag((String) null, tag);
            out.attributeBoolean((String) null, attribute, enable);
            out.endTag((String) null, tag);
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write local backup of value" + e);
        }
    }

    private void writeFeatureIntToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, int value) {
        try {
            out.startTag((String) null, tag);
            out.attributeInt((String) null, attribute, value);
            out.endTag((String) null, tag);
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write local backup of value" + e);
        }
    }

    private void writeTagToXml(XmlSerializer out, String tag, Object value) throws IOException {
        out.startTag(null, tag);
        out.text(String.valueOf(value));
        out.endTag(null, tag);
    }

    private boolean updateAntiMistouchCloudState() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), COUND_FP_SUBMODULE_NAME, COUND_FP_FEATURE_ANTI_MISTOUCH, (String) null, false);
        Slog.d(TAG, "updateAntiMistouchCloudState.");
        if (data == null || data.json() == null) {
            return false;
        }
        Slog.i(TAG, "data.json() anti_mistouch: " + data.json().optString(COUND_FP_FEATURE_ANTI_MISTOUCH, ""));
        JSONObject jsonObject = data.json().optJSONObject(COUND_FP_FEATURE_ANTI_MISTOUCH);
        if (jsonObject != null) {
            this.mAntiMistouchEnable = jsonObject.optBoolean(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, false);
            this.mAntiMistouchValidTimes = jsonObject.optInt(ANTI_MISTOUCH_VALID_TIMES, 0);
            this.mCloudEventsSummary |= 1;
            this.mCloudEventsData.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, Boolean.valueOf(this.mAntiMistouchEnable));
            this.mCloudEventsData.put(ANTI_MISTOUCH_VALID_TIMES, Integer.valueOf(this.mAntiMistouchValidTimes));
            Slog.i(TAG, "Update anti_mistouch, enable: " + this.mAntiMistouchEnable + ", " + ANTI_MISTOUCH_VALID_TIMES + ": " + this.mAntiMistouchValidTimes);
            return true;
        }
        return true;
    }

    private boolean updateAntiLowBrightnessAuthCloudState() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), COUND_FP_SUBMODULE_NAME, COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH, (String) null, false);
        Slog.d(TAG, "updateAntiLowBrightnessAuthCloudState.");
        if (data == null || data.json() == null) {
            return false;
        }
        Slog.i(TAG, "data.json() lowbrightness_auth: " + data.json().optString(COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH, ""));
        JSONObject jsonObject = data.json().optJSONObject(COUND_FP_FEATURE_LOWBRIGHTNESS_AUTH);
        this.mLowBrightnessAuthEnable = jsonObject.optBoolean(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, false);
        if (jsonObject != null) {
            this.mLowBrightnessAuthSettingEnableDefault = jsonObject.optBoolean("setting_enable_default", false);
            this.mCloudEventsSummary |= 2;
            this.mCloudEventsData.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, Boolean.valueOf(this.mLowBrightnessAuthEnable));
            this.mCloudEventsData.put("setting_enable_default", Boolean.valueOf(this.mLowBrightnessAuthSettingEnableDefault));
            Slog.i(TAG, "Update lowbrightness_auth, enable: " + this.mLowBrightnessAuthEnable + ", setting_enable_default: " + this.mLowBrightnessAuthSettingEnableDefault);
            return true;
        }
        return true;
    }

    private boolean updateOcpDetectCloudState() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), COUND_FP_SUBMODULE_NAME, COUND_FP_FEATURE_OCP_DETECT, (String) null, false);
        Slog.d(TAG, "updateOcpDetectCloudState.");
        if (data == null || data.json() == null) {
            return false;
        }
        Slog.i(TAG, "data.json() ocp_detect: " + data.json().optString(COUND_FP_FEATURE_OCP_DETECT, ""));
        JSONObject jsonObject = data.json().optJSONObject(COUND_FP_FEATURE_OCP_DETECT);
        this.mOcpDetectEnable = jsonObject.optBoolean(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, false);
        if (jsonObject != null) {
            this.mOcpDetectSettingEnableDefault = jsonObject.optBoolean("setting_enable_default", false);
            this.mCloudEventsSummary |= 4;
            this.mCloudEventsData.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, Boolean.valueOf(this.mOcpDetectEnable));
            this.mCloudEventsData.put("setting_enable_default", Boolean.valueOf(this.mOcpDetectSettingEnableDefault));
            Slog.i(TAG, "Update ocp_detect, enable: " + this.mOcpDetectEnable + ", setting_enable_default: " + this.mOcpDetectSettingEnableDefault);
            return true;
        }
        return true;
    }

    protected void addCloudListener(CloudListener listener) {
        if (!this.mCloudListeners.contains(listener)) {
            this.mCloudListeners.add(listener);
            listener.onCloudUpdated(this.mCloudEventsSummary, this.mCloudEventsData);
        }
    }

    /* loaded from: classes.dex */
    private final class FingerprintAntiMistouch implements CloudListener {
        private FingerprintAntiMistouch() {
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController.CloudListener
        public void onCloudUpdated(long summary, Map<String, Object> data) {
            if ((summary & 1) != 0) {
                Slog.d(MiuiFingerprintCloudController.TAG, "FingerprintAntiMistouch onCloudUpdated");
                if (MiuiFingerprintCloudController.mFingerprintService != null) {
                    try {
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 14, MiuiFingerprintCloudController.this.mAntiMistouchEnable ? 16 : 0, (String) null);
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 14, MiuiFingerprintCloudController.FP_CLOUD_ANTI_MISTOUCH_VALID_TIMES_PARAM | (MiuiFingerprintCloudController.this.mAntiMistouchValidTimes << 4), (String) null);
                    } catch (RemoteException e) {
                        Slog.e(MiuiFingerprintCloudController.TAG, "RemoteException when FingerprintService extCmd", e);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class FingerprintLowBrightnessAuth implements CloudListener {
        private FingerprintLowBrightnessAuth() {
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController.CloudListener
        public void onCloudUpdated(long summary, Map<String, Object> data) {
            if ((summary & 2) != 0) {
                Slog.d(MiuiFingerprintCloudController.TAG, "FingerprintLowBrightnessAuth onCloudUpdated");
                if (MiuiFingerprintCloudController.mFingerprintService != null) {
                    try {
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 15, MiuiFingerprintCloudController.this.mLowBrightnessAuthEnable ? 16 : 0, (String) null);
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 15, MiuiFingerprintCloudController.this.mLowBrightnessAuthSettingEnableDefault ? 17 : 1, (String) null);
                    } catch (RemoteException e) {
                        Slog.e(MiuiFingerprintCloudController.TAG, "RemoteException when FingerprintService extCmd", e);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class FingerprintOcpDetect implements CloudListener {
        private FingerprintOcpDetect() {
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintCloudController.CloudListener
        public void onCloudUpdated(long summary, Map<String, Object> data) {
            if ((summary & 4) != 0) {
                Slog.d(MiuiFingerprintCloudController.TAG, "FingerprintOcpDetect onCloudUpdated");
                if (MiuiFingerprintCloudController.mFingerprintService != null) {
                    try {
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 11, MiuiFingerprintCloudController.this.mOcpDetectEnable ? 16 : 0, (String) null);
                        MiuiFingerprintCloudController.mFingerprintService.extCmd((IBinder) null, 0, 11, MiuiFingerprintCloudController.this.mOcpDetectSettingEnableDefault ? 17 : 1, (String) null);
                    } catch (RemoteException e) {
                        Slog.e(MiuiFingerprintCloudController.TAG, "RemoteException when FingerprintService extCmd", e);
                    }
                }
            }
        }
    }
}

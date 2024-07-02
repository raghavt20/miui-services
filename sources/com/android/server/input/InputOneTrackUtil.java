package com.android.server.input;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.InputEvent;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.shortcut.ShortcutOneTrackHelper;
import com.miui.analytics.ITrackBinder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.hardware.input.MiuiKeyboardHelper;
import miui.os.Build;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class InputOneTrackUtil {
    private static final String APP_ID_EDGE_SUPPRESSION = "31000000735";
    private static final String APP_ID_EXTERNAL_DEVICE = "31000000618";
    private static final String APP_ID_KEYBOARD_STYLUS = "31000000824";
    private static final String APP_ID_SHORTCUT = "31000401650";
    private static final String APP_ID_SHORTCUT_GLOBAL = "31000401666";
    private static final String EVENT_KEYBOARD_DAU_NAME = "keyboard_dau";
    private static final String EVENT_NAME_EXTERNAL_DEVICE = "external_inputdevice";
    private static final String EVENT_SHORTCUT_KEY_USE_NAME = "hotkeys_use";
    private static final String EXTERNAL_DEVICE_NAME = "device_name";
    private static final String EXTERNAL_DEVICE_TYPE = "device_type";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final String KEYBOARD_6F_FUNCTION_TRIGGER_TIP = "899.4.0.1.27102";
    private static final String KEYBOARD_DAU_TIP = "899.7.0.1.27104";
    private static final String KEYBOARD_SHORTCUT_KEY_TRIGGER_TIP = "899.4.0.1.20553";
    private static final String KEYBOARD_VOICE_TO_WORD_TRIGGER_TIP = "899.4.0.1.27103";
    private static final String PACKAGE_NAME_EDGE_SUPPRESSION = "com.xiaomi.edgesuppression";
    private static final String PACKAGE_NAME_EXTERNAL_DEVICE = "com.xiaomi.padkeyboard";
    private static final String PACKAGE_NAME_KEYBOARD_STYLUS = "com.xiaomi.extendDevice";
    private static final String PACKAGE_NAME_SHORTCUT = "com.xiaomi.input.shortcut";
    private static final String PARAMS_KEYBOARD_DAU_NAME = "connect_type";
    private static final String PARAMS_KEYBOARD_SHORTCUT_NAME = "hotkeys_type";
    private static final String PENCIL_DAU_TRACK_TYPE = "pencil_dau";
    private static final String SERVICE_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    private static final String STATUS_EVENT_NAME = "EVENT_NAME";
    private static final String STATUS_EVENT_TIP = "tip";
    private static final String STYLUS_TRACK_TIP_PENCIL_DAU_TIP = "899.6.0.1.27101";
    private static final String SUPPRESSION_FUNCTION_INITIATIVE = "initiative_settings_by_user";
    private static final String SUPPRESSION_FUNCTION_SIZE = "edge_size";
    private static final String SUPPRESSION_FUNCTION_SIZE_CHANGED = "edge_size_changed";
    private static final String SUPPRESSION_FUNCTION_TYPE = "edge_type";
    private static final String SUPPRESSION_FUNCTION_TYPE_CHANGED = "edge_type_changed";
    private static final String SUPPRESSION_TRACK_TYPE = "edge_suppression";
    private static final String TAG = "InputOneTrackUtil";
    private static volatile InputOneTrackUtil sInstance;
    private final AlarmManager mAlarmManager;
    private Context mContext;
    private final InputDeviceOneTrack mInputDeviceOneTrack;
    private boolean mIsBound;
    private ITrackBinder mService;
    private final AlarmManager.OnAlarmListener mExternalDeviceAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.input.InputOneTrackUtil.1
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            InputOneTrackUtil.this.mInputDeviceOneTrack.mTrackMap.clear();
            InputOneTrackUtil.this.mInputDeviceOneTrack.mXiaomiDeviceMap.clear();
            InputOneTrackUtil.this.updateAlarmForTrack();
        }
    };
    private final ServiceConnection mConnection = new AnonymousClass2();

    public static InputOneTrackUtil getInstance(Context context) {
        if (sInstance == null) {
            synchronized (InputOneTrackUtil.class) {
                if (sInstance == null) {
                    sInstance = new InputOneTrackUtil(context);
                }
            }
        }
        return sInstance;
    }

    private InputOneTrackUtil(Context context) {
        bindTrackService(context);
        this.mInputDeviceOneTrack = new InputDeviceOneTrack();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (Build.IS_TABLET) {
            updateAlarmForTrack();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAlarmForTrack() {
        this.mAlarmManager.setExact(1, System.currentTimeMillis() + 86400000, "clear_external_device", this.mExternalDeviceAlarmListener, MiuiInputThread.getHandler());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.input.InputOneTrackUtil$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements ServiceConnection {
        AnonymousClass2() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, final IBinder service) {
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.input.InputOneTrackUtil$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InputOneTrackUtil.AnonymousClass2.this.lambda$onServiceConnected$0(service);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceConnected$0(IBinder service) {
            InputOneTrackUtil.this.mService = ITrackBinder.Stub.asInterface(service);
            Slog.d(InputOneTrackUtil.TAG, "onServiceConnected: " + InputOneTrackUtil.this.mService);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.input.InputOneTrackUtil$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    InputOneTrackUtil.AnonymousClass2.this.lambda$onServiceDisconnected$1();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceDisconnected$1() {
            InputOneTrackUtil.this.mService = null;
            Slog.d(InputOneTrackUtil.TAG, "onServiceDisconnected");
        }
    }

    private void bindTrackService(Context context) {
        this.mContext = context;
        Intent intent = new Intent();
        intent.setClassName("com.miui.analytics", SERVICE_NAME);
        this.mIsBound = this.mContext.bindServiceAsUser(intent, this.mConnection, 1, UserHandle.SYSTEM);
        Slog.d(TAG, "bindTrackService: " + this.mIsBound);
    }

    public void unbindTrackService(Context context) {
        if (this.mIsBound) {
            context.unbindService(this.mConnection);
        }
    }

    public void trackEvent(final String appId, final String packageName, final String data, final int flag) {
        MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.input.InputOneTrackUtil$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InputOneTrackUtil.this.lambda$trackEvent$0(appId, packageName, data, flag);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackEvent$0(String appId, String packageName, String data, int flag) {
        if (this.mService == null) {
            Slog.d(TAG, "trackEvent: track service not bound");
            bindTrackService(this.mContext);
        }
        try {
            ITrackBinder iTrackBinder = this.mService;
            if (iTrackBinder != null) {
                iTrackBinder.trackEvent(appId, packageName, data, flag);
            } else {
                Slog.e(TAG, "trackEvent: track service is null");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "trackEvent: " + e.getMessage());
        }
    }

    public void trackEvents(final String appId, final String packageName, final List<String> dataList, final int flag) {
        MiuiInputThread.getHandler().post(new Runnable() { // from class: com.android.server.input.InputOneTrackUtil$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InputOneTrackUtil.this.lambda$trackEvents$1(appId, packageName, dataList, flag);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$trackEvents$1(String appId, String packageName, List dataList, int flag) {
        Slog.d(TAG, "trackEvents: appId " + appId + " packageName " + packageName + " size " + dataList.size());
        if (this.mService == null) {
            Slog.d(TAG, "trackEvents: track service not bound");
            bindTrackService(this.mContext);
        }
        try {
            ITrackBinder iTrackBinder = this.mService;
            if (iTrackBinder != null) {
                iTrackBinder.trackEvents(appId, packageName, dataList, flag);
            } else {
                Slog.e(TAG, "trackEvents: track service is null");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "trackEvents: " + e.getMessage());
        }
    }

    public void trackKeyboardShortcut(String data) {
        trackKeyboardEvent(data, KEYBOARD_SHORTCUT_KEY_TRIGGER_TIP);
    }

    public void track6FShortcut(String data) {
        trackKeyboardEvent(data, KEYBOARD_6F_FUNCTION_TRIGGER_TIP);
    }

    public void trackVoice2Word(String data) {
        trackKeyboardEvent(data, KEYBOARD_VOICE_TO_WORD_TRIGGER_TIP);
    }

    public void trackKeyboardEvent(String data, String tip) {
        if (TextUtils.isEmpty(data)) {
            return;
        }
        trackEvent(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, getKeyboardShortcutKeyStatusData(tip, data), 2);
    }

    public void trackKeyboardDAU(boolean data) {
        trackEvent(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, getKeyboardDAUStatusData(data), 2);
    }

    public void trackStylusEvent(String data) {
        trackEvent(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, data, 2);
    }

    public void trackEdgeSuppressionEvent(boolean changed, int changedSize, String changedType, int size, String type) {
        trackEvent(APP_ID_EDGE_SUPPRESSION, PACKAGE_NAME_EDGE_SUPPRESSION, getEdgeSuppressionStatusData(changed, changedSize, changedType, size, type), 2);
    }

    public void trackExtendDevice(String type, String name) {
        if ("STYLUS".equals(type)) {
            trackEvent(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, getStylusDeviceStatusData(), 2);
        } else {
            trackEvent(APP_ID_EXTERNAL_DEVICE, PACKAGE_NAME_EXTERNAL_DEVICE, getExternalDeviceStatusData(type, name), 2);
        }
    }

    public void trackShortcutEvent(String data) {
        trackEvent(getShortcutAppId(), PACKAGE_NAME_SHORTCUT, data, 2);
    }

    public void trackFlingEvent(String data) {
        trackEvent(APP_ID_SHORTCUT, PACKAGE_NAME_SHORTCUT, data, 2);
    }

    public void trackTouchpadEvents(List<String> data) {
        trackEvents(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, data, 2);
    }

    private String getShortcutAppId() {
        return ShortcutOneTrackHelper.isShortcutTrackForOneTrack() ? APP_ID_SHORTCUT : APP_ID_SHORTCUT_GLOBAL;
    }

    private String getKeyboardShortcutKeyStatusData(String tip, String value) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", EVENT_SHORTCUT_KEY_USE_NAME);
            jsonObject.put(PARAMS_KEYBOARD_SHORTCUT_NAME, value);
            jsonObject.put(STATUS_EVENT_TIP, tip);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    private String getKeyboardDAUStatusData(boolean isIICConnected) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", EVENT_KEYBOARD_DAU_NAME);
            jsonObject.put(PARAMS_KEYBOARD_DAU_NAME, isIICConnected ? "pogopin" : "蓝牙");
            jsonObject.put(STATUS_EVENT_TIP, KEYBOARD_DAU_TIP);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    private String getEdgeSuppressionStatusData(boolean changed, int changedSize, String changedType, int size, String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", SUPPRESSION_TRACK_TYPE);
            jsonObject.put(SUPPRESSION_FUNCTION_INITIATIVE, changed);
            jsonObject.put(SUPPRESSION_FUNCTION_SIZE_CHANGED, changedSize);
            jsonObject.put(SUPPRESSION_FUNCTION_TYPE_CHANGED, changedType);
            jsonObject.put(SUPPRESSION_FUNCTION_SIZE, size);
            jsonObject.put(SUPPRESSION_FUNCTION_TYPE, type);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    private String getExternalDeviceStatusData(String type, String name) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", EVENT_NAME_EXTERNAL_DEVICE);
            jsonObject.put(EXTERNAL_DEVICE_TYPE, type);
            jsonObject.put(EXTERNAL_DEVICE_NAME, name);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    private String getStylusDeviceStatusData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", PENCIL_DAU_TRACK_TYPE);
            jsonObject.put(STATUS_EVENT_TIP, STYLUS_TRACK_TIP_PENCIL_DAU_TIP);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    public void trackExternalDevice(InputEvent event) {
        if (event.getDevice() == null || this.mInputDeviceOneTrack.hasDuplicationValue(event.getDevice().getName())) {
            return;
        }
        String name = event.getDevice().getName();
        String type = getEventDeviceType(event);
        int vendorId = event.getDevice().getVendorId();
        int productId = event.getDevice().getProductId();
        if (!"UNKNOW".equals(type) && !this.mInputDeviceOneTrack.hasDuplicationValue(name)) {
            this.mInputDeviceOneTrack.putTrackValue(name, type);
            trackExtendDevice(type, name);
            if (MiuiKeyboardHelper.isXiaomiKeyboard(productId, vendorId) && !this.mInputDeviceOneTrack.hasDuplicationXiaomiDevice(vendorId)) {
                this.mInputDeviceOneTrack.putXiaomiDeviceInfo(vendorId, type);
                trackKeyboardDAU(KeyboardInteraction.INTERACTION.isConnectIICLocked());
            }
        }
    }

    public static String getEventDeviceType(InputEvent event) {
        if (event.isFromSource(16386)) {
            return "STYLUS";
        }
        if (event.getSource() == 257) {
            return "KEYBOARD";
        }
        if (event.getSource() == 8194) {
            return "MOUSE";
        }
        if (event.getSource() == 1048584) {
            return "TOUCHPAD";
        }
        return "UNKNOW";
    }

    public static boolean shouldCountDevice(InputEvent event) {
        InputDevice device = event.getDevice();
        if (device == null) {
            return false;
        }
        return (device.isExternal() && !device.isVirtual()) || event.isFromSource(16386) || MiuiKeyboardHelper.isXiaomiExternalDevice(device.getProductId(), device.getVendorId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class InputDeviceOneTrack {
        static final String TYPE_KEYBOARD = "KEYBOARD";
        static final String TYPE_MOUSE = "MOUSE";
        static final String TYPE_STYLUS = "STYLUS";
        static final String TYPE_TOUCHPAD = "TOUCHPAD";
        static final String TYPE_UNKNOW = "UNKNOW";
        private Map<String, String> mTrackMap = new HashMap();
        private Map<Integer, String> mXiaomiDeviceMap = new HashMap();

        InputDeviceOneTrack() {
        }

        public void putTrackValue(String name, String type) {
            this.mTrackMap.put(name, type);
        }

        public void putXiaomiDeviceInfo(int vendorId, String type) {
            this.mXiaomiDeviceMap.put(Integer.valueOf(vendorId), type);
        }

        public boolean hasDuplicationValue(String name) {
            return this.mTrackMap.containsKey(name);
        }

        public boolean hasDuplicationXiaomiDevice(int vendorId) {
            return this.mXiaomiDeviceMap.containsKey(Integer.valueOf(vendorId));
        }
    }
}

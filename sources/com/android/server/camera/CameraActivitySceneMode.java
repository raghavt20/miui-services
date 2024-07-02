package com.android.server.camera;

import android.content.ComponentName;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import miui.process.IActivityChangeListener;
import miui.process.ProcessManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@MiuiStubHead(manifestName = "com.android.server.camera.CameraActivitySceneStub$$")
/* loaded from: classes.dex */
public class CameraActivitySceneMode extends CameraActivitySceneStub {
    private static final String CAMERA_HIGHRESOLUTIONBLOB_LIST = "highresolutionblob_list";
    private static final String CAMERA_LIVE_LIST = "tplive_list";
    private static final String CAMERA_VIDEOCALL_LIST = "videocall_list";
    private static final String CONFIG_JSON_DEF_PATH = "/system_ext/etc/camerascene.json";
    private static final String CONFIG_JSON_PATH = "/odm/etc/camera/camerascene.json";
    private static final String PERSIST_ACTIVITY = "persist.vendor.vcb.activity";
    private static final String PERSIST_HIGHBLOB = "persist.vendor.camera.3rdhighResolutionBlob.scenes";
    private static final String PERSIST_LIVE = "persist.vendor.camera.3rdlive.scenes";
    private static final String PERSIST_MIFACE = "persist.vendor.miface.registration.scenes";
    private static final String PERSIST_SNAPSHOT = "persist.vendor.vcb.snapshot";
    private static final String PERSIST_VIDEO = "persist.vendor.vcb.video";
    private static final String PERSIST_VIDEOCALL = "persist.vendor.camera.3rdvideocall.scenes";
    private static final String TAG_CONFIG = "config";
    private static final String TAG_CONFIGS = "configs";
    private static final String TAG_DEFAULT = "default";
    private static final String TAG_NAME = "name";
    private static volatile CameraActivitySceneMode sIntance;
    private static final String TAG = CameraActivitySceneMode.class.getSimpleName();
    private static final String ACTIVITY_WECHAT_VIDEO = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    private static final String ACTIVITY_WECHAT_MULTITALK_VIDEO = "com.tencent.mm.plugin.multitalk.ui.MultiTalkMainUI";
    private static final String ACTIVITY_WECHAT_SCAN = "com.tencent.mm.plugin.scanner.ui.BaseScanUI";
    private static final String ACTIVITY_WECHAT_SNAPSHOT = "com.tencent.mm.plugin.recordvideo.activity.MMRecordUI";
    private static final String ACTIVITY_ALIPAY_COMMON_SCAN = "com.alipay.mobile.scan.as.main.MainCaptureActivity";
    private static final String ACTIVITY_ALIPAY_HEALTH_SCAN = "com.alipay.mobile.scan.as.tool.ToolsCaptureActivity";
    private static final String ACTIVITY_MIFACE_REGISTRATION = "com.android.settings.faceunlock.MiuiNormalCameraMultiFaceInput";
    private static final String[] mTargetActivityList = {ACTIVITY_WECHAT_VIDEO, ACTIVITY_WECHAT_MULTITALK_VIDEO, ACTIVITY_WECHAT_SCAN, ACTIVITY_WECHAT_SNAPSHOT, ACTIVITY_ALIPAY_COMMON_SCAN, ACTIVITY_ALIPAY_HEALTH_SCAN, ACTIVITY_MIFACE_REGISTRATION};
    private static final String[] mTargetPackageList = new String[0];
    private Map<String, List<String>> mListConfig = new ConcurrentHashMap();
    private List<String> mCameraVideocallList = new ArrayList();
    private List<String> mCameraHighBlobList = new ArrayList();
    private List<String> mCameraLiveList = new ArrayList();
    private IActivityChangeListener.Stub mActivityChangeListener = new IActivityChangeListener.Stub() { // from class: com.android.server.camera.CameraActivitySceneMode.1
        public void onActivityChanged(ComponentName preName, ComponentName curName) {
            if (curName == null) {
                return;
            }
            String curActivityName = curName.toString();
            CameraActivitySceneMode.this.decideActivitySceneMode(curActivityName);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<CameraActivitySceneMode> {

        /* compiled from: CameraActivitySceneMode$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final CameraActivitySceneMode INSTANCE = new CameraActivitySceneMode();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public CameraActivitySceneMode m903provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public CameraActivitySceneMode m902provideNewInstance() {
            return new CameraActivitySceneMode();
        }
    }

    private void registerActivityChangeListener() {
        if (this.mActivityChangeListener != null) {
            List<String> targetActivities = new ArrayList<>();
            List<String> targetPackages = new ArrayList<>();
            int i = 0;
            while (true) {
                String[] strArr = mTargetActivityList;
                if (i >= strArr.length) {
                    break;
                }
                targetActivities.add(strArr[i]);
                i++;
            }
            for (int i2 = 0; i2 < this.mCameraVideocallList.size(); i2++) {
                targetActivities.add(this.mCameraVideocallList.get(i2));
            }
            for (int i3 = 0; i3 < this.mCameraHighBlobList.size(); i3++) {
                targetActivities.add(this.mCameraHighBlobList.get(i3));
            }
            for (int i4 = 0; i4 < this.mCameraLiveList.size(); i4++) {
                targetActivities.add(this.mCameraLiveList.get(i4));
            }
            int i5 = 0;
            while (true) {
                String[] strArr2 = mTargetPackageList;
                if (i5 < strArr2.length) {
                    targetPackages.add(strArr2[i5]);
                    i5++;
                } else {
                    ProcessManager.registerActivityChangeListener(targetPackages, targetActivities, this.mActivityChangeListener);
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void decideActivitySceneMode(String activityName) {
        SystemProperties.set(PERSIST_VIDEO, "false");
        SystemProperties.set(PERSIST_SNAPSHOT, "false");
        SystemProperties.set(PERSIST_HIGHBLOB, "false");
        SystemProperties.set(PERSIST_VIDEOCALL, "false");
        SystemProperties.set(PERSIST_MIFACE, "false");
        SystemProperties.set(PERSIST_LIVE, "false");
        if (activityName.contains(ACTIVITY_WECHAT_VIDEO) || activityName.contains(ACTIVITY_WECHAT_MULTITALK_VIDEO)) {
            SystemProperties.set(PERSIST_VIDEO, "true");
        }
        if (activityName.contains(ACTIVITY_WECHAT_SNAPSHOT)) {
            SystemProperties.set(PERSIST_SNAPSHOT, "true");
        }
        if (activityName.contains(ACTIVITY_MIFACE_REGISTRATION)) {
            SystemProperties.set(PERSIST_MIFACE, "true");
        }
        for (int i = 0; i < this.mCameraVideocallList.size(); i++) {
            if (activityName.contains(this.mCameraVideocallList.get(i))) {
                SystemProperties.set(PERSIST_VIDEOCALL, "true");
                return;
            }
        }
        for (int i2 = 0; i2 < this.mCameraHighBlobList.size(); i2++) {
            if (activityName.contains(this.mCameraHighBlobList.get(i2))) {
                SystemProperties.set(PERSIST_HIGHBLOB, "true");
                return;
            }
        }
        for (int i3 = 0; i3 < this.mCameraLiveList.size(); i3++) {
            if (activityName.contains(this.mCameraLiveList.get(i3))) {
                SystemProperties.set(PERSIST_LIVE, "true");
                return;
            }
        }
    }

    public void initSystemBooted() {
        SystemProperties.set(PERSIST_VIDEO, "false");
        SystemProperties.set(PERSIST_SNAPSHOT, "false");
        SystemProperties.set(PERSIST_HIGHBLOB, "false");
        SystemProperties.set(PERSIST_VIDEOCALL, "false");
        SystemProperties.set(PERSIST_MIFACE, "false");
        SystemProperties.set(PERSIST_LIVE, "false");
        SystemProperties.set(PERSIST_ACTIVITY, Integer.toString(0));
        parseJson();
        updateActivityList();
        registerActivityChangeListener();
    }

    private void updateActivityList() {
        this.mCameraVideocallList = this.mListConfig.get(CAMERA_VIDEOCALL_LIST);
        this.mCameraHighBlobList = this.mListConfig.get(CAMERA_HIGHRESOLUTIONBLOB_LIST);
        this.mCameraLiveList = this.mListConfig.get(CAMERA_LIVE_LIST);
    }

    public static CameraActivitySceneMode getInstance() {
        if (sIntance == null) {
            synchronized (CameraActivitySceneMode.class) {
                if (sIntance == null) {
                    sIntance = new CameraActivitySceneMode();
                }
            }
        }
        return sIntance;
    }

    private void parseConfigListLocked(String content) {
        try {
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(TAG_CONFIGS)) {
                JSONArray arrays = jsonObject.optJSONArray(TAG_CONFIGS);
                for (int i = 0; i < arrays.length(); i++) {
                    JSONObject obj = arrays.optJSONObject(i);
                    if (obj != null) {
                        String name = obj.optString(TAG_NAME);
                        if (obj.has(TAG_CONFIG)) {
                            JSONArray values = obj.optJSONArray(TAG_CONFIG);
                            List<String> list = new ArrayList<>();
                            for (int j = 0; j < values.length(); j++) {
                                String pkg = values.getString(j);
                                list.add(pkg);
                            }
                            this.mListConfig.put(name, list);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void beginConfig(String content) {
        if ("default".equals(content) || TextUtils.isEmpty(content)) {
            Slog.w(TAG, "json file not found or read fail!");
        } else {
            parseConfigListLocked(content);
        }
    }

    private String readJSONFileToString(String name) {
        final StringBuilder builder = new StringBuilder();
        try {
            Stream<String> stream = Files.lines(Paths.get(name, new String[0]), StandardCharsets.UTF_8);
            try {
                stream.forEach(new Consumer() { // from class: com.android.server.camera.CameraActivitySceneMode$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        builder.append((String) obj).append("\n");
                    }
                });
                if (stream != null) {
                    stream.close();
                }
            } finally {
            }
        } catch (IOException e) {
            Slog.e(TAG, "IOException");
        }
        return builder.toString();
    }

    private boolean checkFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile() && file.canRead();
    }

    private void parseJson() {
        if (checkFile(CONFIG_JSON_DEF_PATH)) {
            Slog.i(TAG, "the default json file path is : /system_ext/etc/camerascene.json");
            String content = readJSONFileToString(CONFIG_JSON_DEF_PATH);
            beginConfig(content);
        }
        if (checkFile(CONFIG_JSON_PATH)) {
            Slog.i(TAG, "the odm json file path is : /odm/etc/camera/camerascene.json");
            String content2 = readJSONFileToString(CONFIG_JSON_PATH);
            beginConfig(content2);
        }
    }
}

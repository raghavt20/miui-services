package com.android.server.display.aiautobrt;

import android.os.Environment;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import miui.io.IOUtils;

/* loaded from: classes.dex */
public class CustomPersistentDataStore {
    private static final String ATTRIBUTE_ENABLED_TAG = "enabled";
    private static final String ATTRIBUTE_NITS_TAG = "nit";
    public static final String CUSTOM_BACKUP_DIR_NAME = "displayconfig";
    public static final String CUSTOM_BACKUP_FILE_NAME = "custom_brightness_backup.xml";
    private static final String CUSTOM_BACKUP_FILE_ROOT_ELEMENT = "custom-config";
    private static final String CUSTOM_CURVE_ENABLED_TAG = "custom_curve_enabled";
    private static final String CUSTOM_CURVE_TAG = "custom_curve";
    private static final String INDIVIDUAL_DEFAULT_POINT_TAG = "default_point";
    private static final String INDIVIDUAL_DEFAULT_SPLINE_TAG = "individual_default_spline";
    private static final String INDIVIDUAL_GAME_POINT_TAG = "game_point";
    private static final String INDIVIDUAL_GAME_SPLINE_TAG = "individual_game_spline";
    private static final String INDIVIDUAL_MODEL_ENABLED_TAG = "individual_model_enabled";
    private static final String INDIVIDUAL_MODEL_TAG = "individual_model";
    private static final String INDIVIDUAL_VIDEO_POINT_TAG = "video_point";
    private static final String INDIVIDUAL_VIDEO_SPLINE_TAG = "individual_video_spline";
    private static final String TAG = "CbmController-CustomPersistentDataStore";
    private final CustomBrightnessModeController mCbmController;
    private boolean mCustomCurveEnabled;
    private final float[] mDefaultLux;
    private float[] mDefaultNits;
    private float[] mGameNits;
    private boolean mIndividualModelEnabled;
    private float[] mVideoNits;
    private final Object mLock = new Object();
    private final Handler mBgHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private final AtomicFile mFile = getFile();

    public CustomPersistentDataStore(CustomBrightnessModeController controller, float[] defaultLux) {
        this.mCbmController = controller;
        this.mDefaultLux = Arrays.copyOf(defaultLux, defaultLux.length);
        this.mDefaultNits = new float[defaultLux.length];
        this.mGameNits = new float[defaultLux.length];
        this.mVideoNits = new float[defaultLux.length];
        loadFromXml();
    }

    public void saveToXml() {
        synchronized (this.mLock) {
            FileOutputStream outputStream = null;
            try {
                Slog.d(TAG, "Save custom brightness config to xml.");
                outputStream = this.mFile.startWrite();
                TypedXmlSerializer out = Xml.resolveSerializer(outputStream);
                out.startDocument((String) null, true);
                out.setFeature(MiuiSizeCompatService.FAST_XML, true);
                out.startTag((String) null, CUSTOM_BACKUP_FILE_ROOT_ELEMENT);
                out.startTag((String) null, CUSTOM_CURVE_TAG);
                writeFeatureEnabledToXml(this.mFile, outputStream, out, ATTRIBUTE_ENABLED_TAG, CUSTOM_CURVE_ENABLED_TAG, this.mCustomCurveEnabled);
                out.endTag((String) null, CUSTOM_CURVE_TAG);
                out.startTag((String) null, INDIVIDUAL_MODEL_TAG);
                writeFeatureEnabledToXml(this.mFile, outputStream, out, ATTRIBUTE_ENABLED_TAG, INDIVIDUAL_MODEL_ENABLED_TAG, this.mIndividualModelEnabled);
                out.endTag((String) null, INDIVIDUAL_MODEL_TAG);
                if (this.mIndividualModelEnabled) {
                    out.startTag((String) null, INDIVIDUAL_DEFAULT_SPLINE_TAG);
                    writeArrayToXml(this.mFile, outputStream, out, INDIVIDUAL_DEFAULT_POINT_TAG, this.mDefaultNits);
                    out.endTag((String) null, INDIVIDUAL_DEFAULT_SPLINE_TAG);
                    out.startTag((String) null, INDIVIDUAL_GAME_SPLINE_TAG);
                    writeArrayToXml(this.mFile, outputStream, out, INDIVIDUAL_GAME_POINT_TAG, this.mGameNits);
                    out.endTag((String) null, INDIVIDUAL_GAME_SPLINE_TAG);
                    out.startTag((String) null, INDIVIDUAL_VIDEO_SPLINE_TAG);
                    writeArrayToXml(this.mFile, outputStream, out, INDIVIDUAL_VIDEO_POINT_TAG, this.mVideoNits);
                    out.endTag((String) null, INDIVIDUAL_VIDEO_SPLINE_TAG);
                }
                out.endTag((String) null, CUSTOM_BACKUP_FILE_ROOT_ELEMENT);
                out.endDocument();
                outputStream.flush();
                this.mFile.finishWrite(outputStream);
            } catch (Exception e) {
                this.mFile.failWrite(outputStream);
                Slog.e(TAG, "Failed to write custom brightness config" + e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$storeCustomCurveEnabled$0(boolean enabled) {
        this.mCustomCurveEnabled = enabled;
    }

    public void storeCustomCurveEnabled(final boolean enabled) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomPersistentDataStore$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                CustomPersistentDataStore.this.lambda$storeCustomCurveEnabled$0(enabled);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$storeIndividualModelEnabled$1(boolean enabled) {
        this.mIndividualModelEnabled = enabled;
    }

    public void storeIndividualModelEnabled(final boolean enabled) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomPersistentDataStore$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CustomPersistentDataStore.this.lambda$storeIndividualModelEnabled$1(enabled);
            }
        });
    }

    public void startWrite() {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomPersistentDataStore$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CustomPersistentDataStore.this.saveToXml();
            }
        });
    }

    public void storeIndividualSpline(final int category, final float[] nits) {
        this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.CustomPersistentDataStore$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                CustomPersistentDataStore.this.lambda$storeIndividualSpline$2(category, nits);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$storeIndividualSpline$2(int category, float[] nits) {
        if (category == 0) {
            this.mDefaultNits = Arrays.copyOf(nits, nits.length);
        } else if (category == 1) {
            this.mGameNits = Arrays.copyOf(nits, nits.length);
        } else if (category == 2) {
            this.mVideoNits = Arrays.copyOf(nits, nits.length);
        }
    }

    private void writeFeatureEnabledToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, boolean enabled) {
        try {
            out.startTag((String) null, tag);
            out.attributeBoolean((String) null, attribute, enabled);
            out.endTag((String) null, tag);
        } catch (Exception e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write backup of feature enabled" + e);
        }
    }

    private void writeArrayToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String tag, float[] nits) {
        for (int i = 0; i < this.mDefaultLux.length; i++) {
            try {
                out.startTag((String) null, tag);
                out.attributeFloat((String) null, ATTRIBUTE_NITS_TAG, nits[i]);
                out.endTag((String) null, tag);
            } catch (Exception e) {
                writeFile.failWrite(outStream);
                Slog.e(TAG, "Failed to write backup of nits" + e);
                return;
            }
        }
    }

    private void loadFromXml() {
        TypedXmlPullParser parser;
        List<Float> defaultNitsList;
        List<Float> gameNitsList;
        List<Float> videoNitsList;
        synchronized (this.mLock) {
            AtomicFile atomicFile = this.mFile;
            if (atomicFile != null && atomicFile.exists()) {
                FileInputStream inputStream = null;
                try {
                    try {
                        inputStream = this.mFile.openRead();
                        Slog.d(TAG, "Start reading custom brightness config from xml.");
                        parser = Xml.resolvePullParser(inputStream);
                        defaultNitsList = new ArrayList<>();
                        gameNitsList = new ArrayList<>();
                        videoNitsList = new ArrayList<>();
                    } catch (Exception e) {
                        this.mFile.delete();
                        Slog.e(TAG, "Failed to read custom brightness backup" + e);
                    }
                    while (true) {
                        int type = parser.next();
                        char c = 1;
                        if (type != 1) {
                            if (type != 3 && type != 4) {
                                String tag = parser.getName();
                                switch (tag.hashCode()) {
                                    case -1558456413:
                                        if (tag.equals(CUSTOM_CURVE_ENABLED_TAG)) {
                                            c = 0;
                                            break;
                                        }
                                        break;
                                    case 348678405:
                                        if (tag.equals(INDIVIDUAL_MODEL_ENABLED_TAG)) {
                                            break;
                                        }
                                        break;
                                    case 967493379:
                                        if (tag.equals(INDIVIDUAL_GAME_POINT_TAG)) {
                                            c = 3;
                                            break;
                                        }
                                        break;
                                    case 1313544722:
                                        if (tag.equals(INDIVIDUAL_DEFAULT_POINT_TAG)) {
                                            c = 2;
                                            break;
                                        }
                                        break;
                                    case 1382696140:
                                        if (tag.equals(INDIVIDUAL_VIDEO_POINT_TAG)) {
                                            c = 4;
                                            break;
                                        }
                                        break;
                                }
                                c = 65535;
                                switch (c) {
                                    case 0:
                                        this.mCustomCurveEnabled = parser.getAttributeBoolean((String) null, ATTRIBUTE_ENABLED_TAG, false);
                                        break;
                                    case 1:
                                        this.mIndividualModelEnabled = parser.getAttributeBoolean((String) null, ATTRIBUTE_ENABLED_TAG, false);
                                        break;
                                    case 2:
                                        defaultNitsList.add(Float.valueOf(parser.getAttributeFloat((String) null, ATTRIBUTE_NITS_TAG, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)));
                                        break;
                                    case 3:
                                        gameNitsList.add(Float.valueOf(parser.getAttributeFloat((String) null, ATTRIBUTE_NITS_TAG, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)));
                                        break;
                                    case 4:
                                        videoNitsList.add(Float.valueOf(parser.getAttributeFloat((String) null, ATTRIBUTE_NITS_TAG, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X)));
                                        break;
                                }
                            }
                        } else {
                            loadCustomBrightnessConfig(defaultNitsList, gameNitsList, videoNitsList);
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
    }

    private void loadCustomBrightnessConfig(List<Float> defaultNitsList, List<Float> gameNitsList, List<Float> videoNitsList) {
        listToArray(this.mDefaultNits, defaultNitsList);
        listToArray(this.mGameNits, gameNitsList);
        listToArray(this.mVideoNits, videoNitsList);
        this.mCbmController.setCustomCurveEnabledFromXml(this.mCustomCurveEnabled);
        this.mCbmController.setIndividualModelEnabledFromXml(this.mIndividualModelEnabled);
        this.mCbmController.buildConfigurationFromXml(0, this.mDefaultNits);
        this.mCbmController.buildConfigurationFromXml(1, this.mGameNits);
        this.mCbmController.buildConfigurationFromXml(2, this.mVideoNits);
    }

    private void listToArray(float[] array, List<Float> list) {
        if (array.length != list.size()) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).floatValue();
        }
    }

    private AtomicFile getFile() {
        return new AtomicFile(new File(Environment.buildPath(Environment.getDataSystemDirectory(), new String[]{"displayconfig"}), CUSTOM_BACKUP_FILE_NAME));
    }

    private boolean isNonZeroArray(float[] array) {
        for (float v : array) {
            if (v != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println(" CustomPersistentDataStore:");
        pw.println("  mCustomCurveEnabled=" + this.mCustomCurveEnabled);
        pw.println("  mIndividualModelEnabled=" + this.mIndividualModelEnabled);
        if (isNonZeroArray(this.mDefaultNits)) {
            pw.println("  mDefaultNits=" + Arrays.toString(this.mDefaultNits));
        }
        if (isNonZeroArray(this.mGameNits)) {
            pw.println("  mGameNits=" + Arrays.toString(this.mGameNits));
        }
        if (isNonZeroArray(this.mVideoNits)) {
            pw.println("  mVideoNits=" + Arrays.toString(this.mVideoNits));
        }
    }
}

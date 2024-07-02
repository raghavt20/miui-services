package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.server.MiuiBatteryIntelligence;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MemoryFreezeCloud {
    private static final String CLOUD_MEMFREEZE_POLICY = "cloud_memory_freeze_policy";
    private static final String CLOUD_MEMFREEZE_WHITE_LIST = "cloud_memory_freeze_whitelist";
    private static final String FILE_PATH = "/data/system/mfz.xml";
    private static final String MEMFREEZE_POLICY_PROP = "persist.sys.mfz.mem_policy";
    private static final String TAG = "MFZCloud";
    private MemoryFreezeStubImpl mMemoryFreezeStubImpl = null;
    private Context mContext = null;

    public void initCloud(Context context) {
        MemoryFreezeStubImpl memoryFreezeStubImpl = MemoryFreezeStubImpl.getInstance();
        this.mMemoryFreezeStubImpl = memoryFreezeStubImpl;
        this.mContext = context;
        if (memoryFreezeStubImpl == null) {
            Slog.e(TAG, "initCloud failure");
        }
        Slog.i(TAG, "initCloud success");
    }

    public void registerCloudWhiteList(Context context) {
        ContentObserver observer = new ContentObserver(this.mMemoryFreezeStubImpl.mHandler) { // from class: com.android.server.am.MemoryFreezeCloud.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryFreezeCloud.CLOUD_MEMFREEZE_WHITE_LIST))) {
                    MemoryFreezeCloud.this.updateCloudWhiteList();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMFREEZE_WHITE_LIST), false, observer, -2);
    }

    public void registerMemfreezeOperation(Context context) {
        ContentObserver observer = new ContentObserver(this.mMemoryFreezeStubImpl.mHandler) { // from class: com.android.server.am.MemoryFreezeCloud.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(MemoryFreezeCloud.CLOUD_MEMFREEZE_POLICY))) {
                    MemoryFreezeCloud.this.updateMemfreezeOperation();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_MEMFREEZE_POLICY), false, observer, -2);
    }

    public void saveToXml(String operations) {
        if (TextUtils.isEmpty(operations)) {
            Slog.e(TAG, "saveToXml operations is empty !");
            return;
        }
        File file = new File(FILE_PATH);
        OutputStream out = null;
        String[] operation = operations.split(",");
        try {
            try {
                try {
                    XmlSerializer serializer = Xml.newSerializer();
                    out = new FileOutputStream(file);
                    serializer.setOutput(out, "UTF-8");
                    serializer.startDocument("UTF-8", true);
                    serializer.text("\n");
                    serializer.startTag(null, "contents");
                    for (String str : operation) {
                        serializer.text("\n\t");
                        serializer.startTag(null, "designated-package");
                        serializer.attribute(null, "name", str);
                        serializer.endTag(null, "designated-package");
                    }
                    serializer.text("\n");
                    serializer.endTag(null, "contents");
                    serializer.text("\n");
                    serializer.endDocument();
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e2) {
                Slog.e(TAG, "Save xml error" + e2.getMessage());
                if (out != null) {
                    out.close();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    out.close();
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    public int getTotalPhysicalRam() {
        try {
            Class clazz = Class.forName("miui.util.HardwareInfo");
            Method method = clazz.getMethod("getTotalPhysicalMemory", null);
            long totalPhysicalMemory = ((Long) method.invoke(null, null)).longValue();
            int memory_G = (int) Math.ceil(((totalPhysicalMemory / 1024.0d) / 1024.0d) / 1024.0d);
            return memory_G;
        } catch (Exception e) {
            Slog.e(TAG, "Mobile Memory not found!");
            return -1;
        }
    }

    public String setParameter(String operations) {
        if (TextUtils.isEmpty(operations)) {
            Slog.e(TAG, "setParameter operations is empty , set as default value!");
            return "2048,3072,4096";
        }
        String[] arr = operations.replaceAll("\"", "").split("\\},");
        Map<String, String> map = new HashMap<>();
        arr[0] = arr[0].substring(1, arr[0].length());
        arr[arr.length - 1] = arr[arr.length - 1].substring(0, arr[arr.length - 1].length() - 2);
        for (String str : arr) {
            String s = str.replace("\\}", "");
            map.put(s.split(":\\{")[0], s.split(":\\{")[1]);
        }
        switch (getTotalPhysicalRam()) {
            case 6:
                return map.get("6").split(":")[1];
            case 8:
                return map.get(MiuiBatteryIntelligence.BatteryInelligenceHandler.CLOSE_LOW_BATTERY_FAST_CHARGE_FUNCTION).split(":")[1];
            case 12:
                return map.get("12").split(":")[1];
            case 16:
                return map.get("16").split(":")[1];
            case 24:
                return map.get("24").split(":")[1];
            default:
                return map.get(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI).split(":")[1];
        }
    }

    public void updateCloudWhiteList() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_WHITE_LIST, -2) != null) {
            String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_WHITE_LIST, -2);
            if (!TextUtils.isEmpty(enableStr)) {
                saveToXml(enableStr);
            }
        }
    }

    public void updateMemfreezeOperation() {
        if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_POLICY, -2) != null) {
            String policy = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_MEMFREEZE_POLICY, -2);
            SystemProperties.set(MEMFREEZE_POLICY_PROP, setParameter(policy));
        }
    }
}

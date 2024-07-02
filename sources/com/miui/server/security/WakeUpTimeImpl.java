package com.miui.server.security;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AtomicFile;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.wm.MiuiSizeCompatService;
import com.miui.server.SecurityManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public class WakeUpTimeImpl {
    private static final String CLASS_NAME = "classname";
    private static final String CLASS_NAMES = "classnames";
    private static final String NAME = "name";
    private static final String TAG = "WakeUpTimeImpl";
    private static final String TIME = "time";
    private final Handler mHandler;
    private final AtomicFile mWakeUpFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "miui-wakeuptime.xml"));
    private final HashMap<String, Long> mWakeUpTime = new HashMap<>();
    private long mWakeTime = 0;

    public WakeUpTimeImpl(SecurityManagerService service) {
        this.mHandler = service.mSecurityWriteHandler;
    }

    public void setWakeUpTime(String componentName, long timeInSeconds) {
        putBootTimeToMap(componentName, timeInSeconds);
        scheduleWriteWakeUpTime();
        setTimeBoot();
    }

    public void writeWakeUpTime() {
        FileOutputStream fos = null;
        try {
            fos = this.mWakeUpFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, CLASS_NAMES);
            synchronized (this.mWakeUpTime) {
                for (String componentName : this.mWakeUpTime.keySet()) {
                    if (getBootTimeFromMap(componentName) != 0) {
                        fastXmlSerializer.startTag(null, CLASS_NAME);
                        fastXmlSerializer.attribute(null, NAME, componentName);
                        fastXmlSerializer.attribute(null, TIME, String.valueOf(getBootTimeFromMap(componentName)));
                        fastXmlSerializer.endTag(null, CLASS_NAME);
                    }
                }
            }
            fastXmlSerializer.endTag(null, CLASS_NAMES);
            fastXmlSerializer.endDocument();
            this.mWakeUpFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mWakeUpFile.failWrite(fos);
            }
        }
    }

    public void readWakeUpTime() {
        synchronized (this.mWakeUpTime) {
            this.mWakeUpTime.clear();
        }
        if (!this.mWakeUpFile.getBaseFile().exists()) {
            return;
        }
        try {
            FileInputStream fis = this.mWakeUpFile.openRead();
            try {
                readWakeUpTime(fis);
                if (fis != null) {
                    fis.close();
                }
            } finally {
            }
        } catch (Exception e) {
            this.mWakeUpFile.getBaseFile().delete();
        }
    }

    public long getBootTimeFromMap(String componentName) {
        long longValue;
        synchronized (this.mWakeUpTime) {
            longValue = this.mWakeUpTime.containsKey(componentName) ? this.mWakeUpTime.get(componentName).longValue() : 0L;
        }
        return longValue;
    }

    private void readWakeUpTime(FileInputStream fis) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fis, null);
        for (int eventType = parser.getEventType(); eventType != 2 && eventType != 1; eventType = parser.next()) {
        }
        String tagName = parser.getName();
        if (CLASS_NAMES.equals(tagName)) {
            int eventType2 = parser.next();
            do {
                if (eventType2 == 2 && parser.getDepth() == 2) {
                    String tagName2 = parser.getName();
                    if (CLASS_NAME.equals(tagName2)) {
                        String componentName = parser.getAttributeValue(null, NAME);
                        long time = Long.parseLong(parser.getAttributeValue(null, TIME));
                        putBootTimeToMap(componentName, time);
                    }
                }
                eventType2 = parser.next();
            } while (eventType2 != 1);
        }
    }

    private void putBootTimeToMap(String componentName, long time) {
        synchronized (this.mWakeUpTime) {
            this.mWakeUpTime.put(componentName, Long.valueOf(time));
        }
    }

    private void scheduleWriteWakeUpTime() {
        if (this.mHandler.hasMessages(2)) {
            return;
        }
        this.mHandler.sendEmptyMessage(2);
    }

    private void scheduleWriteBootTime() {
        if (this.mHandler.hasMessages(3)) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = Long.valueOf(this.mWakeTime);
        this.mHandler.sendMessage(msg);
    }

    private void minWakeUpTime(long nowtime) {
        long min = 0;
        long rightBorder = 300 + nowtime;
        for (String componentName : this.mWakeUpTime.keySet()) {
            long tmp = getBootTimeFromMap(componentName);
            if (tmp >= nowtime && (tmp < min || min == 0)) {
                min = Math.max(tmp, rightBorder);
            }
        }
        this.mWakeTime = min;
    }

    private void setTimeBoot() {
        long now_time = System.currentTimeMillis() / 1000;
        synchronized (this.mWakeUpTime) {
            minWakeUpTime(now_time);
        }
        scheduleWriteBootTime();
    }
}

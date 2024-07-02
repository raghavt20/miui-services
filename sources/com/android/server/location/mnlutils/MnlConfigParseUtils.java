package com.android.server.location.mnlutils;

import com.android.server.location.mnlutils.bean.MnlConfig;
import com.android.server.location.mnlutils.bean.MnlConfigFeature;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
class MnlConfigParseUtils {
    private static final String TAG_CONFIG = "config";
    private static final String TAG_FEATURE = "feature";
    private static final String TAG_FORMAT = "format";
    private static final String TAG_MNL_CONFIG = "mnl_config";
    private static final String TAG_SETTING = "setting";
    private static final String TAG_VERSION = "version";
    private static MnlConfigFeature featureTemp;
    private static String nowFeatureAtt;
    private static String nowFormat;

    private MnlConfigParseUtils() {
    }

    public static synchronized MnlConfig parseXml(String xml) {
        synchronized (MnlConfigParseUtils.class) {
            MnlConfig mnlConfigNow = new MnlConfig();
            if (xml == null) {
                return null;
            }
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    switch (eventType) {
                        case 2:
                            handleStartTag(parser, mnlConfigNow);
                            break;
                        case 3:
                            handleEndTag(parser, mnlConfigNow);
                            break;
                        case 4:
                            handleText(parser);
                            break;
                    }
                }
                inputStream.close();
                return mnlConfigNow;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static void handleStartTag(XmlPullParser parser, MnlConfig mnlConfig) {
        if (mnlConfig == null) {
            mnlConfig = new MnlConfig();
        }
        if (TAG_MNL_CONFIG.equals(parser.getName())) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if (parser.getAttributeName(i).equals("version")) {
                    mnlConfig.setVersion(parser.getAttributeValue(i));
                }
            }
            return;
        }
        if ("feature".equals(parser.getName())) {
            featureTemp = new MnlConfigFeature();
            nowFeatureAtt = "feature";
            return;
        }
        if ("version".equals(parser.getName())) {
            nowFeatureAtt = "version";
            return;
        }
        if (TAG_CONFIG.equals(parser.getName())) {
            nowFeatureAtt = TAG_CONFIG;
        } else if (TAG_FORMAT.equals(parser.getName())) {
            nowFeatureAtt = TAG_FORMAT;
        } else if ("setting".equals(parser.getName())) {
            nowFeatureAtt = "setting";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static void handleText(XmlPullParser parser) {
        char c;
        String str = nowFeatureAtt;
        if (str != null) {
            switch (str.hashCode()) {
                case -1354792126:
                    if (str.equals(TAG_CONFIG)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1268779017:
                    if (str.equals(TAG_FORMAT)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -979207434:
                    if (str.equals("feature")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 351608024:
                    if (str.equals("version")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1985941072:
                    if (str.equals("setting")) {
                        c = 4;
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
                    featureTemp.setName(trimString(parser.getText()));
                    nowFeatureAtt = null;
                    return;
                case 1:
                    featureTemp.setVersion(trimString(parser.getText()));
                    nowFeatureAtt = null;
                    return;
                case 2:
                    featureTemp.setConfig(trimString(parser.getText()));
                    nowFeatureAtt = null;
                    return;
                case 3:
                    nowFormat = trimString(parser.getText());
                    nowFeatureAtt = null;
                    return;
                case 4:
                    featureTemp.getFormatSettings().put(nowFormat, trimString(parser.getText()));
                    nowFeatureAtt = null;
                    return;
                default:
                    return;
            }
        }
    }

    private static void handleEndTag(XmlPullParser parser, MnlConfig mnlConfig) {
        if ("feature".equals(parser.getName())) {
            mnlConfig.getFeatureList().add(featureTemp);
            featureTemp = null;
        }
    }

    private static String trimString(String text) {
        return text.replaceAll("\n", "").trim();
    }
}

package com.android.server.display.thermalbrightnesscondition.config;

import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class TemperatureBrightnessPair {
    private Float maxExclusive;
    private Float minInclusive;
    private Float nit;

    public float getMinInclusive() {
        Float f = this.minInclusive;
        if (f == null) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        return f.floatValue();
    }

    boolean hasMinInclusive() {
        if (this.minInclusive == null) {
            return false;
        }
        return true;
    }

    public void setMinInclusive(float minInclusive) {
        this.minInclusive = Float.valueOf(minInclusive);
    }

    public float getMaxExclusive() {
        Float f = this.maxExclusive;
        if (f == null) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        return f.floatValue();
    }

    boolean hasMaxExclusive() {
        if (this.maxExclusive == null) {
            return false;
        }
        return true;
    }

    public void setMaxExclusive(float maxExclusive) {
        this.maxExclusive = Float.valueOf(maxExclusive);
    }

    public float getNit() {
        Float f = this.nit;
        if (f == null) {
            return MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
        }
        return f.floatValue();
    }

    boolean hasNit() {
        if (this.nit == null) {
            return false;
        }
        return true;
    }

    public void setNit(float nit) {
        this.nit = Float.valueOf(nit);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TemperatureBrightnessPair read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        TemperatureBrightnessPair _instance = new TemperatureBrightnessPair();
        _parser.getDepth();
        while (true) {
            type = _parser.next();
            if (type == 1 || type == 3) {
                break;
            }
            if (_parser.getEventType() == 2) {
                String _tagName = _parser.getName();
                if (_tagName.equals("min-inclusive")) {
                    String _raw = XmlParser.readText(_parser);
                    float _value = Float.parseFloat(_raw);
                    _instance.setMinInclusive(_value);
                } else if (_tagName.equals("max-exclusive")) {
                    String _raw2 = XmlParser.readText(_parser);
                    float _value2 = Float.parseFloat(_raw2);
                    _instance.setMaxExclusive(_value2);
                } else if (_tagName.equals("nit")) {
                    String _raw3 = XmlParser.readText(_parser);
                    float _value3 = Float.parseFloat(_raw3);
                    _instance.setNit(_value3);
                } else {
                    XmlParser.skip(_parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("TemperatureBrightnessPair is not closed");
        }
        return _instance;
    }
}

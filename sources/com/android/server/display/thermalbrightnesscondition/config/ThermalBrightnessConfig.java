package com.android.server.display.thermalbrightnesscondition.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class ThermalBrightnessConfig {
    private List<ThermalConditionItem> thermalConditionItem;

    public List<ThermalConditionItem> getThermalConditionItem() {
        if (this.thermalConditionItem == null) {
            this.thermalConditionItem = new ArrayList();
        }
        return this.thermalConditionItem;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ThermalBrightnessConfig read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        ThermalBrightnessConfig _instance = new ThermalBrightnessConfig();
        _parser.getDepth();
        while (true) {
            type = _parser.next();
            if (type == 1 || type == 3) {
                break;
            }
            if (_parser.getEventType() == 2) {
                String _tagName = _parser.getName();
                if (_tagName.equals("thermal-condition-item")) {
                    ThermalConditionItem _value = ThermalConditionItem.read(_parser);
                    _instance.getThermalConditionItem().add(_value);
                } else {
                    XmlParser.skip(_parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("ThermalBrightnessConfig is not closed");
        }
        return _instance;
    }
}

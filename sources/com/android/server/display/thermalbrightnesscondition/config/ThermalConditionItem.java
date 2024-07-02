package com.android.server.display.thermalbrightnesscondition.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class ThermalConditionItem {
    private String description;
    private Integer identifier;
    private List<TemperatureBrightnessPair> temperatureBrightnessPair;

    public int getIdentifier() {
        Integer num = this.identifier;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    boolean hasIdentifier() {
        if (this.identifier == null) {
            return false;
        }
        return true;
    }

    public void setIdentifier(int identifier) {
        this.identifier = Integer.valueOf(identifier);
    }

    public String getDescription() {
        return this.description;
    }

    boolean hasDescription() {
        if (this.description == null) {
            return false;
        }
        return true;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TemperatureBrightnessPair> getTemperatureBrightnessPair() {
        if (this.temperatureBrightnessPair == null) {
            this.temperatureBrightnessPair = new ArrayList();
        }
        return this.temperatureBrightnessPair;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ThermalConditionItem read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        ThermalConditionItem _instance = new ThermalConditionItem();
        _parser.getDepth();
        while (true) {
            type = _parser.next();
            if (type == 1 || type == 3) {
                break;
            }
            if (_parser.getEventType() == 2) {
                String _tagName = _parser.getName();
                if (_tagName.equals("identifier")) {
                    String _raw = XmlParser.readText(_parser);
                    int _value = Integer.parseInt(_raw);
                    _instance.setIdentifier(_value);
                } else if (_tagName.equals("description")) {
                    String _raw2 = XmlParser.readText(_parser);
                    _instance.setDescription(_raw2);
                } else if (_tagName.equals("temperature-brightness-pair")) {
                    TemperatureBrightnessPair _value2 = TemperatureBrightnessPair.read(_parser);
                    _instance.getTemperatureBrightnessPair().add(_value2);
                } else {
                    XmlParser.skip(_parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("ThermalConditionItem is not closed");
        }
        return _instance;
    }
}

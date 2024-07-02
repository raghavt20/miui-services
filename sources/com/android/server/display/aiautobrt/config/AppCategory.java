package com.android.server.display.aiautobrt.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class AppCategory {
    private Integer id;
    private String name;
    private List<PackageInfo> pkg;

    public List<PackageInfo> getPkg() {
        if (this.pkg == null) {
            this.pkg = new ArrayList();
        }
        return this.pkg;
    }

    public int getId() {
        Integer num = this.id;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    boolean hasId() {
        if (this.id == null) {
            return false;
        }
        return true;
    }

    public void setId(int id) {
        this.id = Integer.valueOf(id);
    }

    public String getName() {
        return this.name;
    }

    boolean hasName() {
        if (this.name == null) {
            return false;
        }
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AppCategory read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        AppCategory _instance = new AppCategory();
        String _raw = _parser.getAttributeValue(null, "id");
        if (_raw != null) {
            int _value = Integer.parseInt(_raw);
            _instance.setId(_value);
        }
        String _raw2 = _parser.getAttributeValue(null, "name");
        if (_raw2 != null) {
            _instance.setName(_raw2);
        }
        _parser.getDepth();
        while (true) {
            type = _parser.next();
            if (type == 1 || type == 3) {
                break;
            }
            if (_parser.getEventType() == 2) {
                String _tagName = _parser.getName();
                if (_tagName.equals("pkg")) {
                    PackageInfo _value2 = PackageInfo.read(_parser);
                    _instance.getPkg().add(_value2);
                } else {
                    XmlParser.skip(_parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("AppCategory is not closed");
        }
        return _instance;
    }
}

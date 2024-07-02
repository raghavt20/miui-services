package com.android.server.display.aiautobrt.config;

import com.android.server.ScoutHelper;
import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class PackageInfo {
    private Integer cateId;
    private String name;
    private Boolean top;

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

    public int getCateId() {
        Integer num = this.cateId;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    boolean hasCateId() {
        if (this.cateId == null) {
            return false;
        }
        return true;
    }

    public void setCateId(int cateId) {
        this.cateId = Integer.valueOf(cateId);
    }

    public boolean isTop() {
        Boolean bool = this.top;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasTop() {
        if (this.top == null) {
            return false;
        }
        return true;
    }

    public void setTop(boolean top) {
        this.top = Boolean.valueOf(top);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PackageInfo read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        PackageInfo _instance = new PackageInfo();
        String _raw = _parser.getAttributeValue(null, "name");
        if (_raw != null) {
            _instance.setName(_raw);
        }
        String _raw2 = _parser.getAttributeValue(null, "cateId");
        if (_raw2 != null) {
            int _value = Integer.parseInt(_raw2);
            _instance.setCateId(_value);
        }
        String _raw3 = _parser.getAttributeValue(null, ScoutHelper.ACTION_TOP);
        if (_raw3 != null) {
            boolean _value2 = Boolean.parseBoolean(_raw3);
            _instance.setTop(_value2);
        }
        XmlParser.skip(_parser);
        return _instance;
    }
}

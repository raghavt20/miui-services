package com.android.server.display.aiautobrt.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class AppCategoryConfig {
    private List<AppCategory> category;

    public List<AppCategory> getCategory() {
        if (this.category == null) {
            this.category = new ArrayList();
        }
        return this.category;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AppCategoryConfig read(XmlPullParser _parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        AppCategoryConfig _instance = new AppCategoryConfig();
        _parser.getDepth();
        while (true) {
            type = _parser.next();
            if (type == 1 || type == 3) {
                break;
            }
            if (_parser.getEventType() == 2) {
                String _tagName = _parser.getName();
                if (_tagName.equals("category")) {
                    AppCategory _value = AppCategory.read(_parser);
                    _instance.getCategory().add(_value);
                } else {
                    XmlParser.skip(_parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("AppCategoryConfig is not closed");
        }
        return _instance;
    }
}

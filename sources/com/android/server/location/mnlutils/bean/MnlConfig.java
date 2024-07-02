package com.android.server.location.mnlutils.bean;

import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class MnlConfig {
    private ArrayList<MnlConfigFeature> featureList;
    private String type = "gps";
    private String version;

    public String toXmlString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<mnl_config version=\"" + this.version + "\" type=\"" + this.type + "\">\n");
        Iterator<MnlConfigFeature> it = this.featureList.iterator();
        while (it.hasNext()) {
            MnlConfigFeature feature = it.next();
            builder.append(feature.toXmlString());
        }
        builder.append("</mnl_config>\n");
        return builder.toString();
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ArrayList<MnlConfigFeature> getFeatureList() {
        if (this.featureList == null) {
            this.featureList = new ArrayList<>();
        }
        return this.featureList;
    }

    public void setFeatureList(ArrayList<MnlConfigFeature> featureList) {
        this.featureList = featureList;
    }

    public MnlConfigFeature getFeature(String name) {
        Iterator<MnlConfigFeature> it = this.featureList.iterator();
        while (it.hasNext()) {
            MnlConfigFeature feature = it.next();
            if (feature.getName().equals(name)) {
                return feature;
            }
        }
        return null;
    }
}

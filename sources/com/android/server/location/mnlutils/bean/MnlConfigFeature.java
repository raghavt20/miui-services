package com.android.server.location.mnlutils.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class MnlConfigFeature {
    private String config;
    private LinkedHashMap<String, String> formatSettings;
    private String name;
    private String version;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public LinkedHashMap<String, String> getFormatSettings() {
        if (this.formatSettings == null) {
            this.formatSettings = new LinkedHashMap<>();
        }
        return this.formatSettings;
    }

    public void setFormatSettings(LinkedHashMap<String, String> formatSettings) {
        this.formatSettings = formatSettings;
    }

    public String toXmlString() {
        StringBuilder buffer = new StringBuilder("\n    <feature>" + this.name + "\n");
        buffer.append("        <version>").append(this.version).append("</version>\n");
        buffer.append("        <config>").append(this.config).append("</config>\n");
        LinkedHashMap<String, String> linkedHashMap = this.formatSettings;
        if (linkedHashMap != null) {
            for (Map.Entry<String, String> entry : linkedHashMap.entrySet()) {
                buffer.append("        <format>").append(entry.getKey()).append("</format>\n");
                buffer.append("        <setting>").append(entry.getValue()).append("</setting>\n");
            }
        }
        buffer.append("    </feature>\n");
        return buffer.toString();
    }
}

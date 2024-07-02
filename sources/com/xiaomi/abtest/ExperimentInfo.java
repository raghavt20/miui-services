package com.xiaomi.abtest;

import java.util.Map;

/* loaded from: classes.dex */
public class ExperimentInfo {
    public int containerId;
    public int expId;
    public int layerId;
    public Map<String, String> params;
    public String xpath;

    public int getContainerId() {
        return this.containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    public int getLayerId() {
        return this.layerId;
    }

    public void setLayerId(int layerId) {
        this.layerId = layerId;
    }

    public String getXpath() {
        return this.xpath;
    }

    public ExperimentInfo setXpath(String xpath) {
        this.xpath = xpath;
        return this;
    }

    public int getExpId() {
        return this.expId;
    }

    public ExperimentInfo setExpId(int expId) {
        this.expId = expId;
        return this;
    }

    public Map<String, String> getParams() {
        return this.params;
    }

    public ExperimentInfo setParams(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public String toString() {
        return "ExperimentInfo{expId=" + this.expId + ", containerId=" + this.containerId + ", layerId=" + this.layerId + ", xpath='" + this.xpath + "', params=" + this.params + '}';
    }
}

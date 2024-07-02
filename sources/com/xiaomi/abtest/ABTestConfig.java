package com.xiaomi.abtest;

/* loaded from: classes.dex */
public class ABTestConfig {
    private String a;
    private String b;
    private String c;
    private String d;
    private int e;
    private boolean f;

    public String getAppName() {
        return this.a;
    }

    public String getUserId() {
        return this.c;
    }

    public String getDeviceId() {
        return this.d;
    }

    public int getLoadConfigInterval() {
        return this.e;
    }

    public boolean isDisableLoadTimer() {
        return this.f;
    }

    public String getLayerName() {
        return this.b;
    }

    public String toString() {
        return "ABTestConfig{mAppName='" + this.a + "', mLayerName='" + this.b + "', mUserId='" + this.c + "', mDeviceId='" + this.d + "', mLoadConfigInterval=" + this.e + ", mDisableLoadTimer=" + this.f + '}';
    }

    private ABTestConfig(Builder builder) {
        this.a = builder.a;
        this.b = builder.b;
        this.c = builder.c;
        this.d = builder.d;
        this.e = builder.e;
        this.f = builder.f;
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private String a;
        private String b;
        private String c;
        private String d;
        private int e;
        private boolean f;

        public Builder setAppName(String appName) {
            this.a = appName;
            return this;
        }

        public Builder setLayerName(String layerName) {
            this.b = layerName;
            return this;
        }

        public Builder setUserId(String userId) {
            this.c = userId;
            return this;
        }

        public Builder setDeviceId(String deviceId) {
            this.d = deviceId;
            return this;
        }

        public Builder setLoadConfigInterval(int interval) {
            this.e = interval;
            return this;
        }

        public Builder setDisableLoadTimer(boolean disableLoadTimer) {
            this.f = disableLoadTimer;
            return this;
        }

        public ABTestConfig build() {
            return new ABTestConfig(this);
        }
    }
}

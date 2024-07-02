package com.xiaomi.NetworkBoost.NetworkSDK;

import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class AppDataUsage {
    public static final String MOBILE = "mobile-";
    public static final String RX_BACK_BYTES = "rxBackgroundBytes";
    public static final String RX_BYTES = "rxBytes";
    public static final String RX_FORE_BYTES = "rxForegroundBytes";
    public static final String TX_BACK_BYTES = "txBackgroundBytes";
    public static final String TX_BYTES = "txBytes";
    public static final String TX_FORE_BYTES = "txForegroundBytes";
    public static final String WIFI = "wifi-";
    private long mRxBytes = 0;
    private long mTxBytes = 0;
    private long mtxForegroundBytes = 0;
    private long mrxForegroundBytes = 0;
    private long mtxBackgroundBytes = 0;
    private long mrxBackgroundBytes = 0;

    public AppDataUsage(long mRxBytes, long mTxBytes, long mtxForegroundBytes, long mrxForegroundBytes) {
        addRxBytes(mRxBytes);
        addTxBytes(mTxBytes);
        addTxForeBytes(mtxForegroundBytes);
        addRxForeBytes(mrxForegroundBytes);
        calculateTxBackBytes();
        calculateRxBackBytes();
    }

    public long getRxBytes() {
        return this.mRxBytes;
    }

    public long getTxBytes() {
        return this.mTxBytes;
    }

    public void addRxBytes(long bytes) {
        this.mRxBytes += bytes;
    }

    public void addTxBytes(long bytes) {
        this.mTxBytes += bytes;
    }

    public void addTxForeBytes(long mtxForegroundBytes) {
        this.mtxForegroundBytes += mtxForegroundBytes;
    }

    public void addRxForeBytes(long mrxForegroundBytes) {
        this.mrxForegroundBytes += mrxForegroundBytes;
    }

    public void calculateTxBackBytes() {
        this.mtxBackgroundBytes += this.mTxBytes - this.mtxForegroundBytes;
    }

    public void calculateRxBackBytes() {
        this.mrxBackgroundBytes += this.mRxBytes - this.mrxForegroundBytes;
    }

    public long getTotal() {
        return this.mTxBytes + this.mRxBytes;
    }

    public void reset() {
        this.mTxBytes = 0L;
        this.mRxBytes = 0L;
    }

    public Map<String, String> toMap(boolean isMobile) {
        String TYPE = isMobile ? MOBILE : WIFI;
        Map<String, String> tomap = new HashMap<>();
        tomap.put(TYPE + RX_BYTES, String.valueOf(this.mRxBytes));
        tomap.put(TYPE + TX_BYTES, String.valueOf(this.mTxBytes));
        tomap.put(TYPE + TX_FORE_BYTES, String.valueOf(this.mtxForegroundBytes));
        tomap.put(TYPE + RX_FORE_BYTES, String.valueOf(this.mrxForegroundBytes));
        tomap.put(TYPE + TX_BACK_BYTES, String.valueOf(this.mtxBackgroundBytes));
        tomap.put(TYPE + RX_BACK_BYTES, String.valueOf(this.mrxBackgroundBytes));
        return tomap;
    }
}

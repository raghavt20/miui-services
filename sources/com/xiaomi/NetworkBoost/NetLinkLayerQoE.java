package com.xiaomi.NetworkBoost;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public final class NetLinkLayerQoE implements Parcelable {
    public static final Parcelable.Creator<NetLinkLayerQoE> CREATOR = new a();
    public static NetLinkLayerQoE singleClass;
    private int bitRateInKbps;
    private int bw;
    private int ccaBusyTimeMs;
    private int frequency;
    private long lostmpdu_be;
    private long lostmpdu_bk;
    private long lostmpdu_vi;
    private long lostmpdu_vo;
    private double mpduLostRatio;
    private int radioOnTimeMs;
    private int rateMcsIdx;
    private double retriesRatio;
    private long retries_be;
    private long retries_bk;
    private long retries_vi;
    private long retries_vo;
    private int rssi_mgmt;
    private long rxmpdu_be;
    private long rxmpdu_bk;
    private long rxmpdu_vi;
    private long rxmpdu_vo;
    private String ssid;
    private long txmpdu_be;
    private long txmpdu_bk;
    private long txmpdu_vi;
    private long txmpdu_vo;
    private String version;

    /* loaded from: classes.dex */
    public class a implements Parcelable.Creator<NetLinkLayerQoE> {
        @Override // android.os.Parcelable.Creator
        public final NetLinkLayerQoE createFromParcel(Parcel parcel) {
            return NetLinkLayerQoE.creatSingleClass(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public final NetLinkLayerQoE[] newArray(int i) {
            return new NetLinkLayerQoE[i];
        }
    }

    public NetLinkLayerQoE() {
    }

    public NetLinkLayerQoE(Parcel parcel) {
        this.version = parcel.readString();
        this.ssid = parcel.readString();
        this.mpduLostRatio = parcel.readDouble();
        this.retriesRatio = parcel.readDouble();
        this.rssi_mgmt = parcel.readInt();
        this.frequency = parcel.readInt();
        this.radioOnTimeMs = parcel.readInt();
        this.ccaBusyTimeMs = parcel.readInt();
        this.bw = parcel.readInt();
        this.rateMcsIdx = parcel.readInt();
        this.bitRateInKbps = parcel.readInt();
        this.rxmpdu_be = parcel.readLong();
        this.txmpdu_be = parcel.readLong();
        this.lostmpdu_be = parcel.readLong();
        this.retries_be = parcel.readLong();
        this.rxmpdu_bk = parcel.readLong();
        this.txmpdu_bk = parcel.readLong();
        this.lostmpdu_bk = parcel.readLong();
        this.retries_bk = parcel.readLong();
        this.rxmpdu_vi = parcel.readLong();
        this.txmpdu_vi = parcel.readLong();
        this.lostmpdu_vi = parcel.readLong();
        this.retries_vi = parcel.readLong();
        this.rxmpdu_vo = parcel.readLong();
        this.txmpdu_vo = parcel.readLong();
        this.lostmpdu_vo = parcel.readLong();
        this.retries_vo = parcel.readLong();
    }

    private static synchronized NetLinkLayerQoE copyFrom(Parcel parcel) {
        NetLinkLayerQoE netLinkLayerQoE;
        synchronized (NetLinkLayerQoE.class) {
            singleClass.setVersion(parcel.readString());
            singleClass.setSsid(parcel.readString());
            singleClass.setMpduLostRatio(parcel.readDouble());
            singleClass.setRetriesRatio(parcel.readDouble());
            singleClass.setRssi_mgmt(parcel.readInt());
            singleClass.setFrequency(parcel.readInt());
            singleClass.setRadioOnTimeMs(parcel.readInt());
            singleClass.setCcaBusyTimeMs(parcel.readInt());
            singleClass.setBw(parcel.readInt());
            singleClass.setRateMcsIdx(parcel.readInt());
            singleClass.setBitRateInKbps(parcel.readInt());
            singleClass.setRxmpdu_be(parcel.readLong());
            singleClass.setTxmpdu_be(parcel.readLong());
            singleClass.setLostmpdu_be(parcel.readLong());
            singleClass.setRetries_be(parcel.readLong());
            singleClass.setRxmpdu_bk(parcel.readLong());
            singleClass.setTxmpdu_bk(parcel.readLong());
            singleClass.setLostmpdu_bk(parcel.readLong());
            singleClass.setRetries_bk(parcel.readLong());
            singleClass.setRxmpdu_vi(parcel.readLong());
            singleClass.setTxmpdu_vi(parcel.readLong());
            singleClass.setLostmpdu_vi(parcel.readLong());
            singleClass.setRetries_vi(parcel.readLong());
            singleClass.setRxmpdu_vo(parcel.readLong());
            singleClass.setTxmpdu_vo(parcel.readLong());
            singleClass.setLostmpdu_vo(parcel.readLong());
            singleClass.setRetries_vo(parcel.readLong());
            netLinkLayerQoE = singleClass;
        }
        return netLinkLayerQoE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized NetLinkLayerQoE creatSingleClass(Parcel parcel) {
        synchronized (NetLinkLayerQoE.class) {
            if (singleClass == null) {
                NetLinkLayerQoE netLinkLayerQoE = new NetLinkLayerQoE(parcel);
                singleClass = netLinkLayerQoE;
                return netLinkLayerQoE;
            }
            return copyFrom(parcel);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int getBitRateInKbps() {
        return this.bitRateInKbps;
    }

    public int getBw() {
        return this.bw;
    }

    public int getCcaBusyTimeMs() {
        return this.ccaBusyTimeMs;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public long getLostmpdu_be() {
        return this.lostmpdu_be;
    }

    public long getLostmpdu_bk() {
        return this.lostmpdu_bk;
    }

    public long getLostmpdu_vi() {
        return this.lostmpdu_vi;
    }

    public long getLostmpdu_vo() {
        return this.lostmpdu_vo;
    }

    public double getMpduLostRatio() {
        return this.mpduLostRatio;
    }

    public int getRadioOnTimeMs() {
        return this.radioOnTimeMs;
    }

    public int getRateMcsIdx() {
        return this.rateMcsIdx;
    }

    public double getRetriesRatio() {
        return this.retriesRatio;
    }

    public long getRetries_be() {
        return this.retries_be;
    }

    public long getRetries_bk() {
        return this.retries_bk;
    }

    public long getRetries_vi() {
        return this.retries_vi;
    }

    public long getRetries_vo() {
        return this.retries_vo;
    }

    public int getRssi_mgmt() {
        return this.rssi_mgmt;
    }

    public long getRxmpdu_be() {
        return this.rxmpdu_be;
    }

    public long getRxmpdu_bk() {
        return this.rxmpdu_bk;
    }

    public long getRxmpdu_vi() {
        return this.rxmpdu_vi;
    }

    public long getRxmpdu_vo() {
        return this.rxmpdu_vo;
    }

    public String getSsid() {
        return this.ssid;
    }

    public long getTxmpdu_be() {
        return this.txmpdu_be;
    }

    public long getTxmpdu_bk() {
        return this.txmpdu_bk;
    }

    public long getTxmpdu_vi() {
        return this.txmpdu_vi;
    }

    public long getTxmpdu_vo() {
        return this.txmpdu_vo;
    }

    public String getVersion() {
        return this.version;
    }

    public void setBitRateInKbps(int i) {
        this.bitRateInKbps = i;
    }

    public void setBw(int i) {
        this.bw = i;
    }

    public void setCcaBusyTimeMs(int i) {
        this.ccaBusyTimeMs = i;
    }

    public void setFrequency(int i) {
        this.frequency = i;
    }

    public void setLostmpdu_be(long j) {
        this.lostmpdu_be = j;
    }

    public void setLostmpdu_bk(long j) {
        this.lostmpdu_bk = j;
    }

    public void setLostmpdu_vi(long j) {
        this.lostmpdu_vi = j;
    }

    public void setLostmpdu_vo(long j) {
        this.lostmpdu_vo = j;
    }

    public void setMpduLostRatio(double d) {
        this.mpduLostRatio = d;
    }

    public void setRadioOnTimeMs(int i) {
        this.radioOnTimeMs = i;
    }

    public void setRateMcsIdx(int i) {
        this.rateMcsIdx = i;
    }

    public void setRetriesRatio(double d) {
        this.retriesRatio = d;
    }

    public void setRetries_be(long j) {
        this.retries_be = j;
    }

    public void setRetries_bk(long j) {
        this.retries_bk = j;
    }

    public void setRetries_vi(long j) {
        this.retries_vi = j;
    }

    public void setRetries_vo(long j) {
        this.retries_vo = j;
    }

    public void setRssi_mgmt(int i) {
        this.rssi_mgmt = i;
    }

    public void setRxmpdu_be(long j) {
        this.rxmpdu_be = j;
    }

    public void setRxmpdu_bk(long j) {
        this.rxmpdu_bk = j;
    }

    public void setRxmpdu_vi(long j) {
        this.rxmpdu_vi = j;
    }

    public void setRxmpdu_vo(long j) {
        this.rxmpdu_vo = j;
    }

    public void setSsid(String str) {
        this.ssid = str;
    }

    public void setTxmpdu_be(long j) {
        this.txmpdu_be = j;
    }

    public void setTxmpdu_bk(long j) {
        this.txmpdu_bk = j;
    }

    public void setTxmpdu_vi(long j) {
        this.txmpdu_vi = j;
    }

    public void setTxmpdu_vo(long j) {
        this.txmpdu_vo = j;
    }

    public void setVersion(String str) {
        this.version = str;
    }

    public String toString() {
        return "NetLinkLayerQoE{version='" + this.version + "', ssid='" + this.ssid + "', rssi_mgmt=" + this.rssi_mgmt + ", frequency=" + this.frequency + ", mpduLostRatio=" + this.mpduLostRatio + ", retriesRatio=" + this.retriesRatio + ", radioOnTimeMs=" + this.radioOnTimeMs + ", ccaBusyTimeMs=" + this.ccaBusyTimeMs + ", bw=" + this.bw + ", rateMcsIdx=" + this.rateMcsIdx + ", bitRateInKbps=" + this.bitRateInKbps + ", rxmpdu_be=" + this.rxmpdu_be + ", txmpdu_be=" + this.txmpdu_be + ", lostmpdu_be=" + this.lostmpdu_be + ", retries_be=" + this.retries_be + ", rxmpdu_bk=" + this.rxmpdu_bk + ", txmpdu_bk=" + this.txmpdu_bk + ", lostmpdu_bk=" + this.lostmpdu_bk + ", retries_bk=" + this.retries_bk + ", rxmpdu_vi=" + this.rxmpdu_vi + ", txmpdu_vi=" + this.txmpdu_vi + ", lostmpdu_vi=" + this.lostmpdu_vi + ", retries_vi=" + this.retries_vi + ", rxmpdu_vo=" + this.rxmpdu_vo + ", txmpdu_vo=" + this.txmpdu_vo + ", lostmpdu_vo=" + this.lostmpdu_vo + ", retries_vo=" + this.retries_vo + '}';
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.version);
        parcel.writeString(this.ssid);
        parcel.writeDouble(this.mpduLostRatio);
        parcel.writeDouble(this.retriesRatio);
        parcel.writeInt(this.rssi_mgmt);
        parcel.writeInt(this.frequency);
        parcel.writeInt(this.radioOnTimeMs);
        parcel.writeInt(this.ccaBusyTimeMs);
        parcel.writeInt(this.bw);
        parcel.writeInt(this.rateMcsIdx);
        parcel.writeInt(this.bitRateInKbps);
        parcel.writeLong(this.rxmpdu_be);
        parcel.writeLong(this.txmpdu_be);
        parcel.writeLong(this.lostmpdu_be);
        parcel.writeLong(this.retries_be);
        parcel.writeLong(this.rxmpdu_bk);
        parcel.writeLong(this.txmpdu_bk);
        parcel.writeLong(this.lostmpdu_bk);
        parcel.writeLong(this.retries_bk);
        parcel.writeLong(this.rxmpdu_vi);
        parcel.writeLong(this.txmpdu_vi);
        parcel.writeLong(this.lostmpdu_vi);
        parcel.writeLong(this.retries_vi);
        parcel.writeLong(this.rxmpdu_vo);
        parcel.writeLong(this.txmpdu_vo);
        parcel.writeLong(this.lostmpdu_vo);
        parcel.writeLong(this.retries_vo);
    }
}

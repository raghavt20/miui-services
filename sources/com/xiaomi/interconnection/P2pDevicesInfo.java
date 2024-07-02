package com.xiaomi.interconnection;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class P2pDevicesInfo implements Parcelable {
    public static final Parcelable.Creator<P2pDevicesInfo> CREATOR = new Parcelable.Creator<P2pDevicesInfo>() { // from class: com.xiaomi.interconnection.P2pDevicesInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public P2pDevicesInfo createFromParcel(Parcel in) {
            return new P2pDevicesInfo(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public P2pDevicesInfo[] newArray(int size) {
            return new P2pDevicesInfo[size];
        }
    };
    private boolean mIsGo;
    private String mPeerDeviceIpAddr;
    private String mPeerDeviceMacAddr;
    private String mThisDeviceIpAddr;
    private String mThisDeviceMacAddr;

    public P2pDevicesInfo(boolean isGo, String thisDeviceMacAddr, String peerDeviceMacAddr, String thisDeviceIpAddr, String peerDeviceIpAddr) {
        this.mIsGo = false;
        this.mThisDeviceMacAddr = "";
        this.mPeerDeviceMacAddr = "";
        this.mThisDeviceIpAddr = "";
        this.mPeerDeviceIpAddr = "";
        this.mIsGo = isGo;
        this.mThisDeviceMacAddr = thisDeviceMacAddr;
        this.mPeerDeviceMacAddr = peerDeviceMacAddr;
        this.mThisDeviceIpAddr = thisDeviceIpAddr;
        this.mPeerDeviceIpAddr = peerDeviceIpAddr;
    }

    protected P2pDevicesInfo(Parcel in) {
        this.mIsGo = false;
        this.mThisDeviceMacAddr = "";
        this.mPeerDeviceMacAddr = "";
        this.mThisDeviceIpAddr = "";
        this.mPeerDeviceIpAddr = "";
        this.mIsGo = in.readBoolean();
        this.mThisDeviceMacAddr = in.readString();
        this.mPeerDeviceMacAddr = in.readString();
        this.mThisDeviceIpAddr = in.readString();
        this.mPeerDeviceIpAddr = in.readString();
    }

    public boolean isGo() {
        return this.mIsGo;
    }

    public String getThisDeviceMacAddr() {
        return this.mThisDeviceMacAddr;
    }

    public String getPeerDeviceMacAddr() {
        return this.mPeerDeviceMacAddr;
    }

    public String getThisDeviceIpAddr() {
        return this.mThisDeviceIpAddr;
    }

    public String getPeerDeviceIpAddr() {
        return this.mPeerDeviceIpAddr;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mIsGo);
        dest.writeString(this.mThisDeviceMacAddr);
        dest.writeString(this.mPeerDeviceMacAddr);
        dest.writeString(this.mThisDeviceIpAddr);
        dest.writeString(this.mPeerDeviceIpAddr);
    }

    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("P2pDevicesInfo{");
        sbuf.append("isGo= ").append(this.mIsGo);
        sbuf.append(", device mac= ").append("*");
        sbuf.append(", peer mac= ").append("*");
        sbuf.append(", device ip= ").append("*");
        sbuf.append(", peer ip= ").append("*");
        sbuf.append("}");
        return sbuf.toString();
    }
}

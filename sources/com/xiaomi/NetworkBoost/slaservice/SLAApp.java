package com.xiaomi.NetworkBoost.slaservice;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Calendar;

/* loaded from: classes.dex */
public class SLAApp implements Parcelable {
    public static final Parcelable.Creator<SLAApp> CREATOR = new Parcelable.Creator<SLAApp>() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAApp.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SLAApp createFromParcel(Parcel in) {
            return new SLAApp(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SLAApp[] newArray(int size) {
            return new SLAApp[size];
        }
    };
    public long mCurTraffic;
    private int mDay;
    private long mDayTraffic;
    private int mMonth;
    private long mMonthTraffic;
    private boolean mState;
    private String mUid;

    public String getUid() {
        return this.mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
    }

    public int getDay() {
        return this.mDay;
    }

    public void setDay(int day) {
        this.mDay = day;
    }

    public int getMonth() {
        return this.mMonth;
    }

    public void setMonth(int month) {
        this.mMonth = month;
    }

    public long getDayTraffic() {
        return this.mDayTraffic;
    }

    public void setDayTraffic(long traffic) {
        this.mDayTraffic = traffic;
    }

    public long getMonthTraffic() {
        return this.mMonthTraffic;
    }

    public void setMonthTraffic(long traffic) {
        this.mMonthTraffic = traffic;
    }

    public boolean getState() {
        return this.mState;
    }

    public void setState(boolean state) {
        this.mState = state;
    }

    public SLAApp() {
    }

    protected SLAApp(Parcel in) {
        this.mUid = in.readString();
        this.mDayTraffic = in.readLong();
        this.mMonthTraffic = in.readLong();
    }

    public SLAApp(String uid) {
        Calendar c = Calendar.getInstance();
        this.mUid = uid;
        this.mDayTraffic = 0L;
        this.mMonthTraffic = 0L;
        this.mState = true;
        this.mDay = c.get(5);
        this.mMonth = c.get(2) + 1;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mUid);
        dest.writeLong(this.mDayTraffic);
        dest.writeLong(this.mMonthTraffic);
    }

    public void readFromParcel(Parcel source) {
        this.mUid = source.readString();
        this.mDayTraffic = source.readLong();
        this.mMonthTraffic = source.readLong();
    }
}

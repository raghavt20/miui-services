package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class ActivityCarWithStarterImpl {
    private static final String MIUI_CARLINK_PERMISSION = "miui.car.permission.MI_CARLINK_STATUS";
    private final String TAG = "ActivityCarWithStarterImpl";
    private double mLatitude = -1.0d;
    private double mLongitude = -1.0d;
    private double mAltitude = 0.0d;
    private String mAddress = "NA";
    private String mCoordinateType = "NA";

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void recordCarWithIntent(final Context context, String callingPackage, String pkg, String dat) {
        char c;
        switch (pkg.hashCode()) {
            case -103524794:
                if (pkg.equals("com.tencent.map")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 744792033:
                if (pkg.equals("com.baidu.BaiduMap")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1254578009:
                if (pkg.equals("com.autonavi.minimap")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                recordGaoDeMap(dat);
                break;
            case 1:
                recordBaiduCoorType(dat);
                recordBaiduMap(dat);
                break;
            case 2:
                recordTencentMap(dat);
                break;
            default:
                recordDefaultMap(dat);
                break;
        }
        double d = this.mLatitude;
        if (d == -1.0d && d == this.mLongitude && TextUtils.equals(dat, "NA")) {
            return;
        }
        try {
            final Intent mCarWithIntent = new Intent();
            Bundle mCarWithBundle = new Bundle();
            mCarWithIntent.setAction("com.miui.carlink.map.CarMapReceiver");
            try {
                String BAIDUMAP = this.mCoordinateType;
                try {
                    mCarWithBundle.putString("poiaddress_poidetailInfo_type", BAIDUMAP);
                    try {
                        mCarWithBundle.putDouble("poiaddress_poidetailInfo_latitude", this.mLatitude);
                        mCarWithBundle.putDouble("poiaddress_poidetailInfo_longitude", this.mLongitude);
                        mCarWithBundle.putDouble("poiaddress_poidetailInfo_altitude", this.mAltitude);
                        mCarWithBundle.putString("poiaddress_address", this.mAddress);
                        try {
                            mCarWithBundle.putString("poiaddress_src_package_name", callingPackage);
                            mCarWithBundle.putString("poiaddress_dest_package_name", pkg);
                            mCarWithIntent.putExtras(mCarWithBundle);
                            mCarWithIntent.setPackage("com.miui.carlink");
                        } catch (Exception e) {
                        }
                    } catch (Exception e2) {
                    }
                } catch (Exception e3) {
                }
            } catch (Exception e4) {
            }
            try {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityCarWithStarterImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActivityCarWithStarterImpl.this.lambda$recordCarWithIntent$0(context, mCarWithIntent);
                    }
                });
            } catch (Exception e5) {
                Slog.e("ActivityCarWithStarterImpl", "Send BroadCast To CarWithFailed");
            }
        } catch (Exception e6) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$recordCarWithIntent$0(Context context, Intent mCarWithIntent) {
        Slog.d("ActivityCarWithStarterImpl", "Send BroadCast To carlink");
        context.sendBroadcast(mCarWithIntent, MIUI_CARLINK_PERMISSION);
    }

    private void recordGaoDeMap(String dat) {
        if (!dat.contains("dlat")) {
            if (dat.contains("denstination")) {
                recordBaiduMap(dat);
                return;
            } else if (dat.contains("tocoord")) {
                recordTencentMap(dat);
                return;
            } else {
                recordDefaultMap(dat);
                return;
            }
        }
        Pattern pattern = Pattern.compile("dlat=(\\d+\\.\\d+)&dlon=(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(dat);
        if (matcher.find()) {
            this.mLatitude = Double.valueOf(matcher.group(1)).doubleValue();
            this.mLongitude = Double.valueOf(matcher.group(2)).doubleValue();
        }
        Pattern pattern2 = Pattern.compile("dname=([^&]+)");
        Matcher matcher2 = pattern2.matcher(dat);
        if (matcher2.find()) {
            this.mAddress = matcher2.group(1);
        }
    }

    private void recordBaiduMap(String dat) {
        if (dat.contains("destination=latlng")) {
            Pattern pattern = Pattern.compile("destination=latlng:([\\d\\.]+),([\\d\\.]+)\\|name:([^&]+)");
            Matcher matcher = pattern.matcher(dat);
            if (matcher.find()) {
                this.mLatitude = Double.valueOf(matcher.group(1)).doubleValue();
                this.mLongitude = Double.valueOf(matcher.group(2)).doubleValue();
                this.mAddress = matcher.group(3);
                return;
            }
            return;
        }
        if (dat.contains("destination=name")) {
            Pattern pattern2 = Pattern.compile("destination=name:([^|]+)\\|latlng:([\\d\\.]+),([\\d\\.]+)");
            Matcher matcher2 = pattern2.matcher(dat);
            if (matcher2.find()) {
                this.mAddress = matcher2.group(1);
                this.mLatitude = Double.valueOf(matcher2.group(2)).doubleValue();
                this.mLongitude = Double.valueOf(matcher2.group(3)).doubleValue();
                return;
            }
            return;
        }
        recordDefaultMap(dat);
    }

    private void recordBaiduCoorType(String dat) {
        if (dat.contains("coord_type")) {
            Pattern pattern = Pattern.compile("coord_type=(\\w+)");
            Matcher matcher = pattern.matcher(dat);
            if (matcher.find()) {
                this.mCoordinateType = matcher.group(1);
                Slog.d("ActivityCarWithStarterImpl", "mCoordinateType: " + this.mCoordinateType);
            }
        }
    }

    private void recordTencentMap(String dat) {
        if (dat.contains("tocoord")) {
            Pattern pattern = Pattern.compile("tocoord=(\\d+\\.\\d+),(\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(dat);
            if (matcher.find()) {
                this.mLatitude = Double.valueOf(matcher.group(1)).doubleValue();
                this.mLongitude = Double.valueOf(matcher.group(2)).doubleValue();
            }
            Pattern pattern2 = Pattern.compile("to=([^&]+)");
            Matcher matcher2 = pattern2.matcher(dat);
            if (matcher2.find()) {
                this.mAddress = matcher2.group(1);
                return;
            }
            return;
        }
        recordDefaultMap(dat);
    }

    private void recordDefaultMap(String dat) {
        int latIndex = 0;
        int lonIndex = 0;
        List<Double> latitudes = new ArrayList<>();
        List<Double> longitudes = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(dat);
        while (matcher.find()) {
            String value = matcher.group();
            if (value.matches("^-?([1-8]\\d?|([1-9]0))(\\.\\d+)?|90(\\.0+)?$")) {
                latitudes.add(Double.valueOf(value));
                latIndex++;
            } else if (value.matches("^-?((0|[1-9]\\d?|1[0-7]\\d)(\\.\\d+)?|180(\\.0+)?)$")) {
                longitudes.add(Double.valueOf(value));
                lonIndex++;
            }
        }
        if (latIndex != 0 && latIndex == lonIndex) {
            this.mLatitude = latitudes.get(latIndex - 1).doubleValue();
            this.mLongitude = longitudes.get(lonIndex - 1).doubleValue();
            this.mAddress = "NA";
        }
    }
}

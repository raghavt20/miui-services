package com.android.server.usb;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miui.util.IMiCharge;

@MiuiStubHead(manifestName = "com.android.server.usb.MiuiUsbServiceStub$$")
/* loaded from: classes.dex */
public class MiuiUsbServiceImpl extends MiuiUsbServiceStub {
    private static final String CARLIFE_PACKAGE_NAME = "com.baidu.carlife";
    private static final String CARWITH_PACKAGE_NAME = "com.miui.carlink";
    private static final int FLAG_NON_ANONYMOUS = 2;
    public static final int SPECIAL_PRODUCT_ID = 16380;
    public static final int SPECIAL_VENDOR_ID = 12806;
    private final String TAG = "MiuiUsbServiceImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiUsbServiceImpl> {

        /* compiled from: MiuiUsbServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiUsbServiceImpl INSTANCE = new MiuiUsbServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiUsbServiceImpl m2383provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiUsbServiceImpl m2382provideNewInstance() {
            return new MiuiUsbServiceImpl();
        }
    }

    public void collectUsbHostConnectedInfo(Context context, UsbDescriptorParser usbDescriptorParser) {
        String str;
        IMiCharge iMiCharge = IMiCharge.getInstance();
        if (!Build.IS_INTERNATIONAL_BUILD) {
            boolean hasAudioInterface = usbDescriptorParser.hasAudioInterface();
            boolean hasHIDInterface = usbDescriptorParser.hasHIDInterface();
            boolean hasStorageInterface = usbDescriptorParser.hasStorageInterface();
            if (iMiCharge.isDPConnected()) {
                str = "DP";
            } else {
                str = hasAudioInterface ? "Audio" : "";
                if (hasHIDInterface) {
                    str = str + "HID";
                }
                if (hasStorageInterface) {
                    str = str + "Storage";
                }
                if (str == null || str.length() == 0) {
                    str = str + "Other";
                }
            }
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, MiuiBatteryStatsService.TrackBatteryUsbInfo.USB_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "usb_host");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "Android");
            intent.putExtra("device_connected", str);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.USB32, iMiCharge.isUSB32() ? 1 : 0);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.e("MiuiUsbServiceImpl", "Start one-Track service failed", e);
            }
            Slog.d("MiuiUsbServiceImpl", "connected device : " + str);
        }
    }

    public boolean isSpecialKeyBoard(UsbDevice device) {
        if (12806 == device.getVendorId() && 16380 == device.getProductId()) {
            return true;
        }
        return false;
    }

    public boolean preSelectUsbHandlerForCarWith(Context context, Intent intent, ArrayList<ResolveInfo> matches, UsbDevice device, UsbAccessory accessory, UsbSettingsManager settingsManager) {
        if (matches.size() != 2 || device != null || accessory == null) {
            return false;
        }
        ResolveInfo carWithResolveInfo = null;
        ResolveInfo carLifeResolveInfo = null;
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        Iterator<ResolveInfo> it = matches.iterator();
        while (it.hasNext()) {
            ResolveInfo resolveInfo = it.next();
            if (resolveInfo.activityInfo != null) {
                if (resolveInfo.activityInfo.packageName.equals(CARWITH_PACKAGE_NAME)) {
                    carWithResolveInfo = resolveInfo;
                } else if (resolveInfo.activityInfo.packageName.equals(CARLIFE_PACKAGE_NAME)) {
                    carLifeResolveInfo = resolveInfo;
                }
            } else {
                Slog.w("MiuiUsbServiceImpl", "resolveInfo has no activityInfo");
            }
        }
        if (carWithResolveInfo == null || carLifeResolveInfo == null) {
            return false;
        }
        ResolveInfo confirmedResolveInfo = carWithResolveInfo;
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> it2 = processes.iterator();
        while (true) {
            if (!it2.hasNext()) {
                break;
            }
            ActivityManager.RunningAppProcessInfo process = it2.next();
            if (process.processName.equals(CARLIFE_PACKAGE_NAME)) {
                if (process.importance == 100) {
                    confirmedResolveInfo = carLifeResolveInfo;
                }
            }
        }
        UsbUserPermissionManager userPermissionsManager = settingsManager.mUsbService.getPermissionsForUser(UserHandle.getUserId(confirmedResolveInfo.activityInfo.applicationInfo.uid));
        userPermissionsManager.grantAccessoryPermission(accessory, confirmedResolveInfo.activityInfo.applicationInfo.uid);
        intent.setComponent(new ComponentName(confirmedResolveInfo.activityInfo.packageName, confirmedResolveInfo.activityInfo.name));
        UserHandle user = UserHandle.getUserHandleForUid(confirmedResolveInfo.activityInfo.applicationInfo.uid);
        context.startActivityAsUser(intent, user);
        return true;
    }
}

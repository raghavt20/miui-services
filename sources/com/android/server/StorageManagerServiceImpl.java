package com.android.server;

import android.content.Context;
import android.os.FileUtils;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.DataUnit;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class StorageManagerServiceImpl extends StorageManagerServiceStub {
    private static final String HAL_DEFAULT = "default";
    private static final String HAL_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String HAL_SERVICE_NAME = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String PROP_ABREUSE_SIZE = "sys.abreuse.size";
    private static final String TAG = "StorageManagerService";
    private static final int TRANSACTION_EUASUPPORTEDSIZE = 9;
    private static final long THRESHOLD_1 = DataUnit.GIBIBYTES.toBytes(0);
    private static final long THRESHOLD_2 = DataUnit.GIBIBYTES.toBytes(5);
    private static final long THRESHOLD_3 = DataUnit.GIBIBYTES.toBytes(10);
    public static boolean sLowStorage = false;
    public static final int EUA_SUPPORTEDSIZE = getEUASupportSizeInternal();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<StorageManagerServiceImpl> {

        /* compiled from: StorageManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final StorageManagerServiceImpl INSTANCE = new StorageManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public StorageManagerServiceImpl m274provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public StorageManagerServiceImpl m273provideNewInstance() {
            return new StorageManagerServiceImpl();
        }
    }

    private static long getThreshold(long totalStorageSize) {
        DataUnit dataUnit = DataUnit.GIGABYTES;
        int i = EUA_SUPPORTEDSIZE;
        if (totalStorageSize < dataUnit.toBytes(i + 64)) {
            return THRESHOLD_1;
        }
        if (totalStorageSize < DataUnit.GIGABYTES.toBytes(i + 512)) {
            return THRESHOLD_2;
        }
        return THRESHOLD_3;
    }

    public static long getSizeInternal(long abreuseSize, long availableUserdataSize, long primaryStorageSize) {
        boolean z = sLowStorage | (availableUserdataSize <= getThreshold(primaryStorageSize));
        sLowStorage = z;
        return (z ? 0L : abreuseSize) + availableUserdataSize;
    }

    public long getAvailableStorageSize(Context mContext, long availableBytes) {
        long abreuseSize;
        StorageManager storage = (StorageManager) mContext.getSystemService(StorageManager.class);
        long primaryStorageSize = storage.getPrimaryStorageSize();
        String abreuseSizeStr = SystemProperties.get(PROP_ABREUSE_SIZE, "0");
        try {
            long abreuseSize2 = Long.parseLong(abreuseSizeStr);
            abreuseSize = abreuseSize2;
        } catch (NumberFormatException e) {
            Slog.w(TAG, "abnormal reuse size " + abreuseSizeStr);
            abreuseSize = 0;
        }
        return getSizeInternal(abreuseSize, availableBytes, primaryStorageSize);
    }

    private static int getEUASupportSizeInternal() {
        IHwBinder hwService;
        HwParcel hidlReply = new HwParcel();
        try {
            try {
                hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", "default");
            } catch (Exception e) {
                Slog.e(TAG, "fail to user MiuiFboService EUA_SupportSize:" + e);
            }
            if (hwService != null) {
                HwParcel hidlRequest = new HwParcel();
                hidlRequest.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                hwService.transact(9, hidlRequest, hidlReply, 0);
                hidlReply.verifySuccess();
                hidlRequest.releaseTemporaryStorage();
                int size = hidlReply.readInt32();
                Slog.d(TAG, "return hidlReply.readInt();" + size);
                return size;
            }
            Slog.e(TAG, "Failed to get MiuiFboService for EUA support size");
            return 0;
        } finally {
            hidlReply.release();
        }
    }

    public long getPrimaryStorageSizeInternal(long totalSize) {
        DataUnit dataUnit = DataUnit.GIBIBYTES;
        int i = EUA_SUPPORTEDSIZE;
        long ufsIntrinsic = FileUtils.squareStorageSize(totalSize - dataUnit.toBytes(i));
        return DataUnit.GIGABYTES.toBytes(i) + ufsIntrinsic;
    }

    public int getEUASupportSize() {
        return EUA_SUPPORTEDSIZE;
    }
}

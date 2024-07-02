package com.miui.server.migard.surfaceflinger;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.miui.server.migard.PackageStatusManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class SurfaceFlingerSetCgroup implements PackageStatusManager.IForegroundChangedCallback {
    private static final int GAME_SURFACEFLINGER_CANCEL_TO_LITTLE_CORE_CODE = 31116;
    private static final int GAME_SURFACEFLINGER_TO_LITTLE_CORE_CODE = 31115;
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final String TAG = SurfaceFlingerSetCgroup.class.getSimpleName();
    private static Set<String> mSupportProductName;
    private Context mContext;
    private List<String> mGameList;

    static {
        HashSet hashSet = new HashSet();
        mSupportProductName = hashSet;
        hashSet.add("corot");
        mSupportProductName.add("miproduct_corot_cn");
    }

    public SurfaceFlingerSetCgroup(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mGameList = arrayList;
        this.mContext = null;
        this.mContext = context;
        arrayList.add("com.tencent.tmgp.pubgmhd");
        this.mGameList.add("com.tencent.tmgp.sgame");
    }

    public boolean SupportProduct() {
        String product = SystemProperties.get("ro.product.product.name");
        if (mSupportProductName.contains(product)) {
            return true;
        }
        return false;
    }

    private void setSurfaceFlingerToLittleCore() {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(1);
            try {
                try {
                    flinger.transact(GAME_SURFACEFLINGER_TO_LITTLE_CORE_CODE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to set sdr2hdr to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    private void cancelSurfaceFlingerToLittleCore() {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(1);
            try {
                try {
                    flinger.transact(GAME_SURFACEFLINGER_CANCEL_TO_LITTLE_CORE_CODE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to set SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback
    public void onForegroundChanged(int pid, int uid, String name) {
        if (this.mGameList.contains(name)) {
            Slog.i(TAG, "start set SurfaceFlinger To Little Core...");
            setSurfaceFlingerToLittleCore();
        } else {
            Slog.i(TAG, "start set SurfaceFlinger cancel To Little Core...");
            cancelSurfaceFlingerToLittleCore();
        }
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback, com.miui.server.migard.ScreenStatusManager.IScreenChangedCallback, com.miui.server.migard.UidStateManager.IUidStateChangedCallback
    public String getCallbackName() {
        return SurfaceFlingerSetCgroup.class.getSimpleName();
    }
}

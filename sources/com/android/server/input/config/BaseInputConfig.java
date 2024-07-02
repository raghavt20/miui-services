package com.android.server.input.config;

import android.os.Parcel;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.input.MiuiInputManagerInternal;
import java.lang.reflect.Field;

/* loaded from: classes.dex */
abstract class BaseInputConfig {
    public static final long PARCEL_NATIVE_PTR_INVALID = -1;
    private static final String TAG = "InputConfig";
    private final MiuiInputManagerInternal mMiuiInputManagerInternal = (MiuiInputManagerInternal) LocalServices.getService(MiuiInputManagerInternal.class);

    public abstract int getConfigType();

    protected abstract void writeToParcel(Parcel parcel);

    public long getConfigNativePtr() {
        Parcel dest = Parcel.obtain();
        writeToParcel(dest);
        dest.marshall();
        dest.setDataPosition(0);
        try {
            Field privateStringFiled = Parcel.class.getDeclaredField("mNativePtr");
            privateStringFiled.setAccessible(true);
            return ((Long) privateStringFiled.get(dest)).longValue();
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage(), e);
            return -1L;
        }
    }

    public void flushToNative() {
        this.mMiuiInputManagerInternal.setInputConfig(getConfigType(), getConfigNativePtr());
    }
}

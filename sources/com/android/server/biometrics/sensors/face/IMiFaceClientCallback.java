package com.android.server.biometrics.sensors.face;

import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import miui.android.services.internal.hidl.base.V1_0.DebugInfo;
import miui.android.services.internal.hidl.base.V1_0.IBase;

/* loaded from: classes.dex */
public interface IMiFaceClientCallback extends IBase {
    public static final String kInterfaceName = "vendor.xiaomi.hardware.miface@1.0::IMiFaceClientCallback";

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    void onAcquired(long j, int i, int i2, int i3) throws RemoteException;

    void onAuthenticated(long j, int i, int i2, ArrayList<Byte> arrayList) throws RemoteException;

    void onEnrollResult(long j, int i, int i2, int i3) throws RemoteException;

    void onEnumerate(long j, ArrayList<Integer> arrayList, int i) throws RemoteException;

    void onError(long j, int i, int i2, int i3) throws RemoteException;

    void onLockoutChanged(long j) throws RemoteException;

    void onRemoved(long j, ArrayList<Integer> arrayList, int i) throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IMiFaceClientCallback asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IMiFaceClientCallback)) {
            return (IMiFaceClientCallback) iface;
        }
        IMiFaceClientCallback proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                String descriptor = it.next();
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IMiFaceClientCallback castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IMiFaceClientCallback getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IMiFaceClientCallback getService(boolean retry) throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, retry);
    }

    static IMiFaceClientCallback getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IMiFaceClientCallback getService() throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IMiFaceClientCallback {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.xiaomi.hardware.miface@1.0::IMiFaceClientCallback]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onEnrollResult(long deviceId, int faceId, int userId, int remaining) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(faceId);
            _hidl_request.writeInt32(userId);
            _hidl_request.writeInt32(remaining);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onAuthenticated(long deviceId, int faceId, int userId, ArrayList<Byte> token) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(faceId);
            _hidl_request.writeInt32(userId);
            _hidl_request.writeInt8Vector(token);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onAcquired(long deviceId, int userId, int acquiredInfo, int vendorCode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(userId);
            _hidl_request.writeInt32(acquiredInfo);
            _hidl_request.writeInt32(vendorCode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onError(long deviceId, int userId, int error, int vendorCode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(userId);
            _hidl_request.writeInt32(error);
            _hidl_request.writeInt32(vendorCode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onRemoved(long deviceId, ArrayList<Integer> removed, int userId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32Vector(removed);
            _hidl_request.writeInt32(userId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onEnumerate(long deviceId, ArrayList<Integer> faceIds, int userId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32Vector(faceIds);
            _hidl_request.writeInt32(userId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback
        public void onLockoutChanged(long duration) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceClientCallback.kInterfaceName);
            _hidl_request.writeInt64(duration);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16L);
                int _hidl_vec_size = _hidl_blob.getInt32(8L);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                    childBlob.copyToInt8Array(_hidl_array_offset_1, _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IMiFaceClientCallback {
        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IMiFaceClientCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IMiFaceClientCallback.kInterfaceName;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-62, 58, 94, 112, CommunicationUtil.KEYBOARD_COLOR_WHITE, -63, -72, -114, -101, -61, 23, -92, -2, -21, 10, 115, -30, 125, -81, -97, 5, -59, CommunicationUtil.KEYBOARD_COLOR_BLACK, CommunicationUtil.TOUCHPAD_ADDRESS, -65, CommunicationUtil.COMMAND_G_SENSOR, CommunicationUtil.COMMAND_KB_FEATURE_SLEEP, 82, 50, 25, 59, -25}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, CommunicationUtil.RESPONSE_TYPE, 19, -109, 36, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceClientCallback, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IMiFaceClientCallback.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId = _hidl_request.readInt64();
                    int faceId = _hidl_request.readInt32();
                    int userId = _hidl_request.readInt32();
                    int remaining = _hidl_request.readInt32();
                    onEnrollResult(deviceId, faceId, userId, remaining);
                    return;
                case 2:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId2 = _hidl_request.readInt64();
                    int faceId2 = _hidl_request.readInt32();
                    int userId2 = _hidl_request.readInt32();
                    ArrayList<Byte> token = _hidl_request.readInt8Vector();
                    onAuthenticated(deviceId2, faceId2, userId2, token);
                    return;
                case 3:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId3 = _hidl_request.readInt64();
                    int userId3 = _hidl_request.readInt32();
                    int acquiredInfo = _hidl_request.readInt32();
                    int vendorCode = _hidl_request.readInt32();
                    onAcquired(deviceId3, userId3, acquiredInfo, vendorCode);
                    return;
                case 4:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId4 = _hidl_request.readInt64();
                    int userId4 = _hidl_request.readInt32();
                    int error = _hidl_request.readInt32();
                    int vendorCode2 = _hidl_request.readInt32();
                    onError(deviceId4, userId4, error, vendorCode2);
                    return;
                case 5:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId5 = _hidl_request.readInt64();
                    ArrayList<Integer> removed = _hidl_request.readInt32Vector();
                    int userId5 = _hidl_request.readInt32();
                    onRemoved(deviceId5, removed, userId5);
                    return;
                case 6:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long deviceId6 = _hidl_request.readInt64();
                    ArrayList<Integer> faceIds = _hidl_request.readInt32Vector();
                    int userId6 = _hidl_request.readInt32();
                    onEnumerate(deviceId6, faceIds, userId6);
                    return;
                case 7:
                    _hidl_request.enforceInterface(IMiFaceClientCallback.kInterfaceName);
                    long duration = _hidl_request.readInt64();
                    onLockoutChanged(duration);
                    return;
                case 256067662:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    return;
                case 256131655:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    NativeHandle fd = _hidl_request.readNativeHandle();
                    ArrayList<String> options = _hidl_request.readStringVector();
                    debug(fd, options);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 256136003:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    return;
                case 256398152:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(0);
                    HwBlob _hidl_blob = new HwBlob(16);
                    int _hidl_vec_size = _hidl_out_hashchain.size();
                    _hidl_blob.putInt32(8L, _hidl_vec_size);
                    _hidl_blob.putBool(12L, false);
                    HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                    for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                        long _hidl_array_offset_1 = _hidl_index_0 * 32;
                        byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                        if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                            throw new IllegalArgumentException("Array element is not of the expected length");
                        }
                        childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                    }
                    _hidl_blob.putBlob(0L, childBlob);
                    _hidl_reply.writeBuffer(_hidl_blob);
                    _hidl_reply.send();
                    return;
                case 256462420:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    setHALInstrumentation();
                    return;
                case 256660548:
                default:
                    return;
                case 256921159:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ping();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 257049926:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_info.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 257120595:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    notifySyspropsChanged();
                    return;
            }
        }
    }
}

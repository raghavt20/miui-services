package vendor.xiaomi.hardware.misys.V2_0;

import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import miui.android.services.internal.hidl.base.V1_0.DebugInfo;
import miui.android.services.internal.hidl.base.V1_0.IBase;

/* loaded from: classes.dex */
public interface IMiSys extends IBase {
    public static final String kInterfaceName = "vendor.xiaomi.hardware.misys@2.0::IMiSys";

    boolean ConnectVirtualCamera() throws RemoteException;

    boolean DisconnectVirtualCamera() throws RemoteException;

    boolean IsExists(String str, String str2) throws RemoteException;

    int MiSysCreateFolder(String str, String str2) throws RemoteException;

    IBufferReadResult MiSysReadBuffer(String str, String str2) throws RemoteException;

    int MiSysWriteBuffer(String str, String str2, ArrayList<Byte> arrayList, long j) throws RemoteException;

    boolean OnFrameData(ArrayList<Byte> arrayList, int i) throws RemoteException;

    void RegisterVCameraCallback(IVCameraCallback iVCameraCallback) throws RemoteException;

    boolean SetVirtualCameraConfig(int i, int i2, double d) throws RemoteException;

    void UnregisterVCameraCallback() throws RemoteException;

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

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IMiSys asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IMiSys)) {
            return (IMiSys) iface;
        }
        IMiSys proxy = new Proxy(binder);
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

    static IMiSys castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IMiSys getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IMiSys getService(boolean retry) throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI, retry);
    }

    @Deprecated
    static IMiSys getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IMiSys getService() throws RemoteException {
        return getService(NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IMiSys {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.xiaomi.hardware.misys@2.0::IMiSys]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public int MiSysCreateFolder(String path, String folder_name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(folder_name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public int MiSysWriteBuffer(String path, String file_name, ArrayList<Byte> writebuf, long buf_len) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(file_name);
            _hidl_request.writeInt8Vector(writebuf);
            _hidl_request.writeInt64(buf_len);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public IBufferReadResult MiSysReadBuffer(String path, String file_name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(file_name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IBufferReadResult _hidl_out_result = new IBufferReadResult();
                _hidl_out_result.readFromParcel(_hidl_reply);
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public boolean IsExists(String path, String file_name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeString(path);
            _hidl_request.writeString(file_name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_result = _hidl_reply.readBool();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public boolean SetVirtualCameraConfig(int width, int height, double frameRate) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeInt32(width);
            _hidl_request.writeInt32(height);
            _hidl_request.writeDouble(frameRate);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_result = _hidl_reply.readBool();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public boolean ConnectVirtualCamera() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_result = _hidl_reply.readBool();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public boolean DisconnectVirtualCamera() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_result = _hidl_reply.readBool();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public boolean OnFrameData(ArrayList<Byte> data, int len) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeInt8Vector(data);
            _hidl_request.writeInt32(len);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_result = _hidl_reply.readBool();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public void RegisterVCameraCallback(IVCameraCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys
        public void UnregisterVCameraCallback() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiSys.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IMiSys {
        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IMiSys.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IMiSys.kInterfaceName;
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-37, 90, -49, 68, 99, CommunicationUtil.COMMAND_DATA_PKG, 49, 42, 47, 31, 83, 125, 1, 25, MiuiKeyboardUtil.KEYBOARD_TYPE_LQ_BLE, 111, 9, 60, 46, -25, 125, -79, 71, 3, -112, 12, 47, -42, CommunicationUtil.COMMAND_KEYBOARD_UPGRADE_STATUS, 42, -49, -47}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, CommunicationUtil.RESPONSE_TYPE, 19, -109, 36, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.xiaomi.hardware.misys.V2_0.IMiSys, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IMiSys.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    String path = _hidl_request.readString();
                    String folder_name = _hidl_request.readString();
                    int _hidl_out_result = MiSysCreateFolder(path, folder_name);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    String path2 = _hidl_request.readString();
                    String file_name = _hidl_request.readString();
                    ArrayList<Byte> writebuf = _hidl_request.readInt8Vector();
                    long buf_len = _hidl_request.readInt64();
                    int _hidl_out_result2 = MiSysWriteBuffer(path2, file_name, writebuf, buf_len);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result2);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    String path3 = _hidl_request.readString();
                    String file_name2 = _hidl_request.readString();
                    IBufferReadResult _hidl_out_result3 = MiSysReadBuffer(path3, file_name2);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_result3.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    String path4 = _hidl_request.readString();
                    String file_name3 = _hidl_request.readString();
                    boolean _hidl_out_result4 = IsExists(path4, file_name3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_result4);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    int width = _hidl_request.readInt32();
                    int height = _hidl_request.readInt32();
                    double frameRate = _hidl_request.readDouble();
                    boolean _hidl_out_result5 = SetVirtualCameraConfig(width, height, frameRate);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_result5);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    boolean _hidl_out_result6 = ConnectVirtualCamera();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_result6);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    boolean _hidl_out_result7 = DisconnectVirtualCamera();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_result7);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    ArrayList<Byte> data = _hidl_request.readInt8Vector();
                    int len = _hidl_request.readInt32();
                    boolean _hidl_out_result8 = OnFrameData(data, len);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_result8);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    IVCameraCallback callback = IVCameraCallback.asInterface(_hidl_request.readStrongBinder());
                    RegisterVCameraCallback(callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(IMiSys.kInterfaceName);
                    UnregisterVCameraCallback();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
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

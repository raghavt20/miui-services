package com.android.server.input.padkeyboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.input.padkeyboard.MiuiPadKeyboardManager;
import com.xiaomi.devauth.IMiDevAuthInterface;

/* loaded from: classes.dex */
public class KeyboardAuthHelper {
    private static final String CLASS_NAME = "com.xiaomi.devauth.MiDevAuthService";
    public static final int INTERNAL_ERROR_LIMIT = 3;
    public static final int KEYBOARD_AUTH_OK = 0;
    public static final int KEYBOARD_IDENTITY_RETRY_TIME = 5000;
    public static final int KEYBOARD_INTERNAL_ERROR = 3;
    public static final int KEYBOARD_NEED_CHECK_AGAIN = 2;
    public static final int KEYBOARD_REJECT = 1;
    public static final int KEYBOARD_TRANSFER_ERROR = 4;
    private static final String MIDEVAUTH_TAG = "MiuiKeyboardManager_MiDevAuthService";
    private static final String PACKAGE_NAME = "com.xiaomi.devauth";
    public static final int TRANSFER_ERROR_LIMIT = 2;
    private MiuiPadKeyboardManager.CommandCallback mChallengeCallbackLaunchAfterU;
    private MiuiPadKeyboardManager.CommandCallback mChallengeCallbackLaunchBeforeU;
    private ServiceConnection mConn;
    private Context mContext;
    private IBinder.DeathRecipient mDeathRecipient;
    private MiuiPadKeyboardManager.CommandCallback mInitCallbackLaunchAfterU;
    private MiuiPadKeyboardManager.CommandCallback mInitCallbackLaunchBeforeU;
    private IMiDevAuthInterface mService;
    private static int sTransferErrorCount = 0;
    private static int sInternalErrorCount = 0;

    private static void increaseTransferErrorCounter() {
        sTransferErrorCount++;
    }

    private static void increaseInternalErrorCounter() {
        sInternalErrorCount++;
    }

    private static void resetTransferErrorCounter() {
        sTransferErrorCount = 0;
    }

    private static void resetInternalErrorCounter() {
        sInternalErrorCount = 0;
    }

    private static void BreakInternalErrorCounter() {
        sInternalErrorCount = 3;
    }

    private static void BreakTransferErrorCounter() {
        sTransferErrorCount = 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class KeyboardAuthInstance {
        private static final KeyboardAuthHelper INSTANCE = new KeyboardAuthHelper();

        private KeyboardAuthInstance() {
        }
    }

    private KeyboardAuthHelper() {
        this.mContext = null;
        this.mService = null;
        this.mInitCallbackLaunchBeforeU = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda0
            @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$0(bArr);
            }
        };
        this.mChallengeCallbackLaunchBeforeU = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda1
            @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$1(bArr);
            }
        };
        this.mInitCallbackLaunchAfterU = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda2
            @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$2(bArr);
            }
        };
        this.mChallengeCallbackLaunchAfterU = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda3
            @Override // com.android.server.input.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$3(bArr);
            }
        };
        this.mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper.1
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                if (KeyboardAuthHelper.this.mService == null) {
                    return;
                }
                Slog.d(KeyboardAuthHelper.MIDEVAUTH_TAG, "binderDied, unlink service");
                KeyboardAuthHelper.this.mService.asBinder().unlinkToDeath(KeyboardAuthHelper.this.mDeathRecipient, 0);
            }
        };
        this.mConn = new ServiceConnection() { // from class: com.android.server.input.padkeyboard.KeyboardAuthHelper.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    service.linkToDeath(KeyboardAuthHelper.this.mDeathRecipient, 0);
                } catch (RemoteException e) {
                    Slog.e(KeyboardAuthHelper.MIDEVAUTH_TAG, "linkToDeath fail: " + e);
                }
                KeyboardAuthHelper.this.mService = IMiDevAuthInterface.Stub.asInterface(service);
                if (KeyboardAuthHelper.this.mService == null) {
                    Slog.e(KeyboardAuthHelper.MIDEVAUTH_TAG, "Try connect midevauth service fail");
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                if (KeyboardAuthHelper.this.mContext != null) {
                    Slog.i(KeyboardAuthHelper.MIDEVAUTH_TAG, "re-bind to MiDevAuth service");
                    KeyboardAuthHelper.this.mContext.unbindService(KeyboardAuthHelper.this.mConn);
                    KeyboardAuthHelper.this.initService();
                }
            }
        };
    }

    private void setContext(Context context) {
        this.mContext = context;
    }

    public static KeyboardAuthHelper getInstance(Context context) {
        Slog.i(MIDEVAUTH_TAG, "Init bind to MiDevAuth service");
        KeyboardAuthInstance.INSTANCE.setContext(context);
        KeyboardAuthInstance.INSTANCE.initService();
        return KeyboardAuthInstance.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$0(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchBeforeU]Init, get rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 26) {
            Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchBeforeU]Init, Wrong length:" + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 32, recBuf[32])) {
            Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchBeforeU]MiDevAuth Init, Receive wrong checksum");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$1(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchBeforeU]Get token from keyboard, rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 16) {
            Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchBeforeU]Get token from keyboard, Wrong length:" + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 22, recBuf[22])) {
            Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchBeforeU]Get token from keyboard, Receive wrong checksum");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$2(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchAfterU]Init, get rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 26) {
            Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchAfterU]Init, Wrong length:" + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        }
        if (!MiuiKeyboardUtil.checkSum(recBuf, 0, 32, recBuf[32])) {
            Slog.i(MIDEVAUTH_TAG, "[mInitCallbackLaunchAfterU]MiDevAuth Init, Receive wrong checksum");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$new$3(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchAfterU]Get token from keyboard, rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 32 && recBuf[5] != 16) {
            Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchAfterU]Get token from keyboard, Wrong length: " + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        }
        if (recBuf[5] == 16) {
            Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchAfterU]Get token from keyboard should be 32 bytes but hack for 120 version");
            return true;
        }
        if (MiuiKeyboardUtil.checkSum(recBuf, 0, 38, recBuf[38])) {
            return true;
        }
        Slog.i(MIDEVAUTH_TAG, "[mChallengeCallbackLaunchAfterU]Get token from keyboard, Receive wrong checksum");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PACKAGE_NAME, CLASS_NAME));
        if (!this.mContext.bindServiceAsUser(intent, this.mConn, 1, UserHandle.CURRENT)) {
            Slog.e(MIDEVAUTH_TAG, "cannot bind service: com.xiaomi.devauth.MiDevAuthService");
        }
    }

    public int doCheckKeyboardIdentityLaunchBeforeU(MiuiPadKeyboardManager miuiPadKeyboardManager, boolean isFirst) {
        Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU ]Begin check keyboardIdentity ");
        if (isFirst) {
            resetTransferErrorCounter();
            resetInternalErrorCounter();
        }
        if (sTransferErrorCount > 2) {
            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Meet transfer error counter:" + sTransferErrorCount);
            return 4;
        }
        if (sInternalErrorCount > 3) {
            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Meet internal error counter:" + sInternalErrorCount);
            return 3;
        }
        byte[] uid = new byte[16];
        byte[] keyMetaData1 = new byte[4];
        byte[] keyMetaData2 = new byte[4];
        byte[] initCommand = miuiPadKeyboardManager.commandMiDevAuthInit();
        byte[] r1 = miuiPadKeyboardManager.sendCommandForRespond(initCommand, this.mInitCallbackLaunchBeforeU);
        if (r1.length != 0) {
            System.arraycopy(r1, 8, uid, 0, 16);
            System.arraycopy(r1, 24, keyMetaData1, 0, 4);
            System.arraycopy(r1, 28, keyMetaData2, 0, 4);
            if (this.mService == null) {
                initService();
            }
            IMiDevAuthInterface iMiDevAuthInterface = this.mService;
            if (iMiDevAuthInterface == null) {
                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] MiDevAuth Service is unavaiable!");
                return 2;
            }
            try {
                byte[] chooseKeyMeta = iMiDevAuthInterface.chooseKey(uid, keyMetaData1, keyMetaData2);
                if (chooseKeyMeta.length == 4) {
                    try {
                        byte[] challenge = this.mService.getChallenge(16);
                        if (challenge.length != 16) {
                            increaseInternalErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Get challenge from midevauth service fail!");
                            return 2;
                        }
                        byte[] challengeCommand = miuiPadKeyboardManager.commandMiAuthStep3Type1(chooseKeyMeta, challenge);
                        byte[] r2 = miuiPadKeyboardManager.sendCommandForRespond(challengeCommand, this.mChallengeCallbackLaunchBeforeU);
                        if (r2.length == 0) {
                            increaseTransferErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] MiDevAuth Get token from keyboard fail! Error counter:" + sTransferErrorCount);
                            return 2;
                        }
                        byte[] token = new byte[16];
                        System.arraycopy(r2, 6, token, 0, 16);
                        try {
                        } catch (RemoteException e) {
                            e = e;
                        }
                        try {
                            int result = this.mService.tokenVerify(1, uid, chooseKeyMeta, challenge, token);
                            if (result == 1) {
                                Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Check keyboard PASS with online key");
                                resetTransferErrorCounter();
                                resetInternalErrorCounter();
                                return 0;
                            }
                            if (result == 2) {
                                Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Check keyboard PASS with offline key, need check again later");
                                resetTransferErrorCounter();
                                resetInternalErrorCounter();
                                return 2;
                            }
                            if (result == 3) {
                                resetTransferErrorCounter();
                                increaseInternalErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Meet internal error when try check keyboard!");
                                return 2;
                            }
                            if (result == -1) {
                                BreakInternalErrorCounter();
                                BreakTransferErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] fail to verify keyboard token!");
                                return 1;
                            }
                            Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Check keyboard Fail!");
                            return 1;
                        } catch (RemoteException e2) {
                            e = e2;
                            increaseInternalErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] call token_verify fail: " + e);
                            return 2;
                        }
                    } catch (RemoteException e3) {
                        increaseInternalErrorCounter();
                        Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] call getChallenge fail: " + e3);
                        return 2;
                    }
                }
                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] Choose KeyMeta from midevauth service fail!");
                if (chooseKeyMeta.length == 0) {
                    return 1;
                }
                increaseInternalErrorCounter();
                return 2;
            } catch (RemoteException e4) {
                increaseInternalErrorCounter();
                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] call chooseKeyMeta fail: " + e4);
                return 2;
            }
        }
        increaseTransferErrorCounter();
        Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchBeforeU] MiDevAuth Init fail! Error counter:" + sTransferErrorCount);
        return 2;
    }

    public int doCheckKeyboardIdentityLaunchAfterU(MiuiPadKeyboardManager miuiPadKeyboardManager, boolean isFirst) {
        Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU ]Begin check keyboardIdentity ");
        if (isFirst) {
            resetTransferErrorCounter();
            resetInternalErrorCounter();
        }
        if (sTransferErrorCount > 2) {
            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Meet transfer error counter:" + sTransferErrorCount);
            return 4;
        }
        if (sInternalErrorCount > 3) {
            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Meet internal error counter:" + sInternalErrorCount);
            return 3;
        }
        byte[] uid = new byte[16];
        byte[] keyMetaData1 = new byte[4];
        byte[] keyMetaData2 = new byte[4];
        byte[] initCommand = miuiPadKeyboardManager.commandMiDevAuthInit();
        byte[] r1 = miuiPadKeyboardManager.sendCommandForRespond(initCommand, this.mInitCallbackLaunchAfterU);
        if (r1.length != 0) {
            System.arraycopy(r1, 8, uid, 0, 16);
            System.arraycopy(r1, 24, keyMetaData1, 0, 4);
            System.arraycopy(r1, 28, keyMetaData2, 0, 4);
            if (this.mService == null) {
                initService();
            }
            IMiDevAuthInterface iMiDevAuthInterface = this.mService;
            if (iMiDevAuthInterface == null) {
                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] MiDevAuth Service is unavaiable!");
                return 2;
            }
            try {
                byte[] chooseKeyMeta = iMiDevAuthInterface.chooseKey(uid, keyMetaData1, keyMetaData2);
                if (chooseKeyMeta.length == 4) {
                    try {
                        byte[] challenge = this.mService.getChallenge(16);
                        if (challenge.length != 16) {
                            increaseInternalErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Get challenge from midevauth service fail!");
                            return 2;
                        }
                        byte[] challengeCommand = miuiPadKeyboardManager.commandMiAuthStep3Type1(chooseKeyMeta, challenge);
                        byte[] r2 = miuiPadKeyboardManager.sendCommandForRespond(challengeCommand, this.mChallengeCallbackLaunchAfterU);
                        if (r2.length != 0) {
                            if (r2[5] == 16) {
                                Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] hack for 120 version, return auth ok");
                                return 0;
                            }
                            byte[] keyboardToken = new byte[16];
                            System.arraycopy(r2, 6, keyboardToken, 0, 16);
                            try {
                            } catch (RemoteException e) {
                                e = e;
                            }
                            try {
                                int result = this.mService.tokenVerify(1, uid, chooseKeyMeta, challenge, keyboardToken);
                                if (result == 1) {
                                    Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] verify token success with online key");
                                }
                                if (result == 2) {
                                    Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] verify token success with offline key");
                                }
                                if (result == 3) {
                                    resetTransferErrorCounter();
                                    increaseInternalErrorCounter();
                                    Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Meet internal error when try verify token!");
                                    return 2;
                                }
                                if (result != -1) {
                                    System.arraycopy(r2, 22, challenge, 0, 16);
                                    try {
                                        try {
                                            byte[] padToken = this.mService.tokenGet(1, uid, chooseKeyMeta, challenge);
                                            if (padToken.length != 16) {
                                                increaseInternalErrorCounter();
                                                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Get token from midevauth service fail!");
                                                return 2;
                                            }
                                            Slog.i(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] tokenGet from midevauthservice  =  " + MiuiKeyboardUtil.Bytes2Hex(padToken, padToken.length));
                                            byte[] verifyDeviceCommand = miuiPadKeyboardManager.commandMiAuthStep5Type1(padToken);
                                            miuiPadKeyboardManager.sendCommandForRespond(verifyDeviceCommand, null);
                                            resetTransferErrorCounter();
                                            resetInternalErrorCounter();
                                            return 0;
                                        } catch (RemoteException e2) {
                                            e = e2;
                                            increaseInternalErrorCounter();
                                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] call tokenGet fail: " + e);
                                            return 2;
                                        }
                                    } catch (RemoteException e3) {
                                        e = e3;
                                    }
                                } else {
                                    BreakInternalErrorCounter();
                                    BreakTransferErrorCounter();
                                    Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] fail to verify keyboard token!");
                                    return 1;
                                }
                            } catch (RemoteException e4) {
                                e = e4;
                                increaseInternalErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] call token_verify fail: " + e);
                                return 2;
                            }
                        } else {
                            increaseTransferErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Get wrong token from keyboard! Error counter:" + sTransferErrorCount);
                            return 2;
                        }
                    } catch (RemoteException e5) {
                        increaseInternalErrorCounter();
                        Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] call getChallenge fail: " + e5);
                        return 2;
                    }
                } else {
                    Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] Choose KeyMeta from midevauth service fail!");
                    if (chooseKeyMeta.length == 0) {
                        return 1;
                    }
                    increaseInternalErrorCounter();
                    return 2;
                }
            } catch (RemoteException e6) {
                increaseInternalErrorCounter();
                Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU] call chooseKeyMeta fail: " + e6);
                return 2;
            }
        } else {
            increaseTransferErrorCounter();
            Slog.e(MIDEVAUTH_TAG, "[doCheckKeyboardIdentityLaunchAfterU]Get wrong init response, Error counter:" + sTransferErrorCount);
            return 2;
        }
    }
}

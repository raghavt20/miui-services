package com.android.server.input.padkeyboard.iic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.padkeyboard.KeyboardInteraction;
import com.android.server.input.padkeyboard.MiuiIICKeyboardManager;
import com.android.server.input.padkeyboard.MiuiKeyboardUtil;
import com.android.server.input.padkeyboard.OnehopInfo;
import com.android.server.input.padkeyboard.bluetooth.BluetoothKeyboardManager;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import com.android.server.input.padkeyboard.iic.IICNodeHelper;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.server.greeze.AurogonImmobulusMode;
import com.xiaomi.abtest.d.d;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class IICNodeHelper {
    public static final int BUFFER_SIZE = 100;
    private static final String KEYBOARD_COLOR_BLACK = "black";
    private static final String KEYBOARD_COLOR_WHITE = "white";
    private static final String KEYBOARD_INFO_CHANGED = "notify_keyboard_info_changed";
    private static final String TAG = "MiuiPadKeyboard_IICNodeHelper";
    private static final String UPGRADE_FOOT_LOCATION = ".bin";
    private static final String UPGRADE_HEAD_LOCATION_ODM = "odm/etc/";
    private static final String UPGRADE_HEAD_LOCATION_VENDOR = "vendor/etc/";
    private static volatile IICNodeHelper sInstance;
    private byte[] mAuthCommandResponse;
    private final BluetoothKeyboardManager mBlueToothKeyboardManager;
    private final CommunicationUtil mCommunicationUtil;
    private final Context mContext;
    private CountDownLatch mCountDownLatch;
    private CommandWorker mEmptyCommandWorker;
    private volatile boolean mIsBinDataOK;
    private boolean mIsBleKeyboard;
    private KeyboardUpgradeUtil mKeyBoardUpgradeUtil;
    private CommandWorker mKeyUpgradeCommandWorker;
    private volatile String mKeyboardFlashVersion;
    private volatile String mKeyboardVersion;
    private CommandWorker mKeyboardVersionWorker;
    private CommandWorker mMCUUpgradeCommandWorker;
    private volatile String mMCUVersion;
    private CommandWorker mMCUVersionWorker;
    private McuUpgradeUtil mMcuUpgradeUtil;
    public NanoSocketCallback mNanoSocketCallback;
    private CommandWorker mTouchPadCommandWorker;
    private TouchPadUpgradeUtil mTouchPadUpgradeUtil;
    private volatile String mTouchPadVersion;
    private final UpgradeCommandHandler mUpgradeCommandHandler;
    private boolean mUpgradeNoCheck;
    private final StringBuilder mMCUUpgradeFilePath = new StringBuilder();
    private final StringBuilder mKeyBoardUpgradeFilePath = new StringBuilder();
    private final StringBuilder mTouchPadUpgradeFilePath = new StringBuilder("TouchPad_Upgrade.bin");
    private volatile int mReceiverBinIndex = -1;
    private byte mKeyboardType = 1;
    private byte mTouchPadType = 0;
    private final List<CommandWorker> mAllWorkerList = new ArrayList();
    private final Queue<Runnable> mReadyRunningTask = new LinkedList();
    private String mKeyboardBleAdd = "66:55:44:33:22:11";
    private final Runnable mUpgradeMCURunnable = new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper.1
        @Override // java.lang.Runnable
        public void run() {
            IICNodeHelper iICNodeHelper = IICNodeHelper.this;
            iICNodeHelper.mMcuUpgradeUtil = new McuUpgradeUtil(iICNodeHelper.mContext, "vendor/etc/MCU_Upgrade.bin");
            if (!IICNodeHelper.this.mMcuUpgradeUtil.isValidFile()) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "Invalid upgrade file, with vendor/etc/MCU_Upgrade.bin");
                IICNodeHelper.this.mMCUUpgradeCommandWorker.emptyCommandResponse();
            } else {
                if (!IICNodeHelper.this.supportUpgradeMCU()) {
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.emptyCommandResponse();
                    return;
                }
                IICNodeHelper.this.mMCUUpgradeCommandWorker.insertCommandToQueue(IICNodeHelper.this.mMcuUpgradeUtil.getUpgradeCommand());
                IICNodeHelper.this.mMCUUpgradeCommandWorker.insertCommandToQueue(IICNodeHelper.this.mMcuUpgradeUtil.getBinPackInfo(0));
                IICNodeHelper.this.mMCUUpgradeCommandWorker.sendCommand(4);
            }
        }
    };
    private final Runnable mUpgradeKeyboardRunnable = new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper.2
        @Override // java.lang.Runnable
        public void run() {
            IICNodeHelper iICNodeHelper = IICNodeHelper.this;
            iICNodeHelper.mKeyBoardUpgradeUtil = new KeyboardUpgradeUtil(iICNodeHelper.mContext, IICNodeHelper.this.getUpgradeHead() + IICNodeHelper.this.mKeyBoardUpgradeFilePath.toString(), IICNodeHelper.this.mKeyboardType);
            if (!IICNodeHelper.this.mKeyBoardUpgradeUtil.isValidFile()) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.KEYBOARD_ADDRESS, "Invalid upgrade file, with " + IICNodeHelper.this.mKeyBoardUpgradeFilePath.toString());
                IICNodeHelper.this.mKeyUpgradeCommandWorker.emptyCommandResponse();
            } else {
                if (!IICNodeHelper.this.supportUpgradeKeyboard()) {
                    IICNodeHelper.this.mKeyUpgradeCommandWorker.emptyCommandResponse();
                    return;
                }
                IICNodeHelper.this.mKeyUpgradeCommandWorker.insertCommandToQueue(IICNodeHelper.this.mKeyBoardUpgradeUtil.getUpgradeInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                IICNodeHelper.this.mKeyUpgradeCommandWorker.insertCommandToQueue(IICNodeHelper.this.mKeyBoardUpgradeUtil.getBinPackInfo(CommunicationUtil.KEYBOARD_ADDRESS, 0));
                IICNodeHelper.this.mKeyUpgradeCommandWorker.sendCommand(4);
            }
        }
    };
    private final Runnable mUpgradeTouchPadRunnable = new AnonymousClass3();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ResponseListener {
        void onCommandResponse();
    }

    public static IICNodeHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (IICNodeHelper.class) {
                if (sInstance == null) {
                    sInstance = new IICNodeHelper(context);
                }
            }
        }
        return sInstance;
    }

    private IICNodeHelper(Context context) {
        this.mContext = context;
        CommunicationUtil communicationUtil = CommunicationUtil.getInstance();
        this.mCommunicationUtil = communicationUtil;
        communicationUtil.registerSocketCallback(new SocketCallbackListener());
        this.mMcuUpgradeUtil = new McuUpgradeUtil(context, null);
        this.mKeyBoardUpgradeUtil = new KeyboardUpgradeUtil(context, null, this.mKeyboardType);
        this.mTouchPadUpgradeUtil = new TouchPadUpgradeUtil(context, null, this.mTouchPadType);
        HandlerThread thread = new HandlerThread("keyboard_upgrade");
        thread.start();
        this.mUpgradeCommandHandler = new UpgradeCommandHandler(thread.getLooper());
        initCommandWorker();
        this.mBlueToothKeyboardManager = BluetoothKeyboardManager.getInstance(context);
    }

    public void setOtaCallBack(NanoSocketCallback callBack) {
        this.mNanoSocketCallback = callBack;
        this.mCommunicationUtil.setOtaCallBack(callBack);
    }

    public void sendGetKeyboardVersionCommand() {
        Slog.i(TAG, "offer getKeyboardVersion worker");
        this.mKeyboardVersionWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.KEYBOARD_ADDRESS));
        this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$sendGetKeyboardVersionCommand$1();
            }
        });
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendGetKeyboardVersionCommand$0() {
        this.mKeyboardVersionWorker.sendCommand(4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendGetKeyboardVersionCommand$1() {
        this.mReadyRunningTask.offer(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$sendGetKeyboardVersionCommand$0();
            }
        });
    }

    public void sendRestoreMcuCommand() {
        this.mCommunicationUtil.sendRestoreMcuCommand();
    }

    public void sendGetMCUVersionCommand() {
        Slog.i(TAG, "offer getMCUVersion worker");
        this.mMCUVersionWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.MCU_ADDRESS));
        this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$sendGetMCUVersionCommand$3();
            }
        });
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendGetMCUVersionCommand$3() {
        this.mReadyRunningTask.offer(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$sendGetMCUVersionCommand$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendGetMCUVersionCommand$2() {
        this.mMCUVersionWorker.sendCommand(4);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setKeyboardFeature(boolean z, int i) {
        byte command;
        byte[] keyboardFeatureCommand;
        if (i == CommunicationUtil.KB_FEATURE.KB_ENABLE.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_ENABLE.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_POWER.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_POWER.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_CAPS_KEY_NEW.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_CAPS_KEY_NEW.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_MIC_MUTE.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_MIC_MUTE.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_MIC_MUTE_NEW.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_MIC_MUTE_NEW.getCommand();
        } else if (i == CommunicationUtil.KB_FEATURE.KB_G_SENSOR.getIndex()) {
            command = CommunicationUtil.KB_FEATURE.KB_G_SENSOR.getCommand();
        } else {
            return;
        }
        byte ledStatusValue = this.mCommunicationUtil.getLedStatusValue(i, z, this.mKeyboardType);
        byte b = ledStatusValue;
        if (ledStatusValue == 0) {
            b = z;
        }
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
            keyboardFeatureCommand = this.mCommunicationUtil.getLongRawCommand();
            this.mCommunicationUtil.setCommandHead(keyboardFeatureCommand);
            this.mCommunicationUtil.setSetKeyboardStatusCommand(keyboardFeatureCommand, command, b);
        } else if (KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
            keyboardFeatureCommand = CommunicationUtil.BleDeviceUtil.getKeyboardFeatureCommand(command, b);
        } else {
            return;
        }
        KeyboardInteraction.INTERACTION.communicateWithKeyboardLocked(keyboardFeatureCommand);
    }

    public void setKeyboardBacklight(byte level) {
        byte[] command;
        byte feature = CommunicationUtil.KB_FEATURE.KB_BACKLIGHT.getCommand();
        if (KeyboardInteraction.INTERACTION.isConnectIICLocked()) {
            command = this.mCommunicationUtil.getLongRawCommand();
            this.mCommunicationUtil.setCommandHead(command);
            this.mCommunicationUtil.setSetKeyboardStatusCommand(command, feature, level);
            Settings.System.putIntForUser(this.mContext.getContentResolver(), MiuiIICKeyboardManager.CURRENT_BACK_BRIGHTNESS, level, -2);
        } else if (KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
            command = CommunicationUtil.BleDeviceUtil.getKeyboardFeatureCommand(feature, level);
        } else {
            return;
        }
        KeyboardInteraction.INTERACTION.communicateWithKeyboardLocked(command);
    }

    public void readKeyboardStatus() {
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setCommandHead(command);
        this.mCommunicationUtil.setLocalAddress2KeyboardCommand(command, CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, (byte) 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, (byte) 82);
        KeyboardInteraction.INTERACTION.communicateWithKeyboardLocked(command);
    }

    public byte[] checkAuth(byte[] command) {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.mCountDownLatch = countDownLatch;
        countDownLatch.countDown();
        KeyboardInteraction.INTERACTION.communicateWithKeyboardLocked(command);
        try {
            this.mCountDownLatch.await(400L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Slog.i(TAG, exception.toString());
            Thread.currentThread().interrupt();
        }
        return this.mAuthCommandResponse;
    }

    public void checkHallStatus() {
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setGetHallStatusCommand(command);
        KeyboardInteraction.INTERACTION.communicateWithKeyboardLocked(command);
    }

    public void startUpgradeMCUIfNeed() {
        Slog.i(TAG, "offer upgrade mcu worker");
        this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$startUpgradeMCUIfNeed$4();
            }
        });
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startUpgradeMCUIfNeed$4() {
        this.mReadyRunningTask.offer(this.mUpgradeMCURunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean supportUpgradeMCU() {
        if (this.mMcuUpgradeUtil.checkVersion(this.mMCUVersion) || this.mUpgradeNoCheck) {
            return true;
        }
        this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, NanoSocketCallback.OTA_ERROR_REASON_VERSION);
        return false;
    }

    public void upgradeKeyboardIfNeed() {
        Slog.i(TAG, "offer upgrade keyboard worker");
        this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$upgradeKeyboardIfNeed$5();
            }
        });
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$upgradeKeyboardIfNeed$5() {
        this.mReadyRunningTask.offer(this.mUpgradeKeyboardRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getUpgradeHead() {
        if (MiuiKeyboardUtil.isXM2022MCU()) {
            return UPGRADE_HEAD_LOCATION_VENDOR;
        }
        return UPGRADE_HEAD_LOCATION_ODM;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean supportUpgradeKeyboard() {
        if (this.mKeyBoardUpgradeUtil.checkVersion(this.mKeyboardVersion) || this.mUpgradeNoCheck) {
            return true;
        }
        this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.KEYBOARD_ADDRESS, NanoSocketCallback.OTA_ERROR_REASON_VERSION);
        return false;
    }

    public void upgradeTouchPadIfNeed() {
        Slog.i(TAG, "offer upgrade touchPad worker");
        this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                IICNodeHelper.this.lambda$upgradeTouchPadIfNeed$6();
            }
        });
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$upgradeTouchPadIfNeed$6() {
        this.mReadyRunningTask.offer(this.mUpgradeTouchPadRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.input.padkeyboard.iic.IICNodeHelper$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements Runnable {
        AnonymousClass3() {
        }

        @Override // java.lang.Runnable
        public void run() {
            IICNodeHelper iICNodeHelper = IICNodeHelper.this;
            iICNodeHelper.mTouchPadUpgradeUtil = new TouchPadUpgradeUtil(iICNodeHelper.mContext, IICNodeHelper.this.getUpgradeHead() + ((Object) IICNodeHelper.this.mTouchPadUpgradeFilePath), IICNodeHelper.this.mTouchPadType);
            if (!IICNodeHelper.this.mTouchPadUpgradeUtil.isValidFile()) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.TOUCHPAD_ADDRESS, "Invalid upgrade file, with " + ((Object) IICNodeHelper.this.mTouchPadUpgradeFilePath));
                IICNodeHelper.this.mTouchPadCommandWorker.emptyCommandResponse();
            } else {
                if (!IICNodeHelper.this.supportUpgradeTouchPad()) {
                    IICNodeHelper.this.mTouchPadCommandWorker.emptyCommandResponse();
                    return;
                }
                Slog.i(IICNodeHelper.TAG, "Start upgrade TouchPad");
                IICNodeHelper.this.mTouchPadCommandWorker.insertCommandToQueue(IICNodeHelper.this.mTouchPadUpgradeUtil.getUpgradeInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                IICNodeHelper.this.mTouchPadCommandWorker.insertCommandToQueue(IICNodeHelper.this.mTouchPadUpgradeUtil.getBinPackInfo(CommunicationUtil.KEYBOARD_ADDRESS, 0));
                IICNodeHelper.this.mUpgradeCommandHandler.postDelayed(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        IICNodeHelper.AnonymousClass3.this.lambda$run$0();
                    }
                }, 5000L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$run$0() {
            IICNodeHelper.this.mTouchPadCommandWorker.sendCommand(4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean supportUpgradeTouchPad() {
        if (this.mTouchPadUpgradeUtil.checkVersion(this.mTouchPadVersion) || this.mUpgradeNoCheck) {
            return true;
        }
        this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.TOUCHPAD_ADDRESS, NanoSocketCallback.OTA_ERROR_REASON_VERSION);
        return false;
    }

    public void setNoCheckUpgrade(boolean enable) {
        this.mUpgradeNoCheck = enable;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkDataPkgSum(byte[] data, String reason) {
        if (data.length < data[5] + 6) {
            Slog.i(TAG, "Exception Data:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
        } else if (!this.mCommunicationUtil.checkSum(data, 0, data[5] + 6, data[data[5] + 6]) && !KeyboardInteraction.INTERACTION.isConnectBleLocked()) {
            this.mNanoSocketCallback.onReadSocketNumError(NanoSocketCallback.OTA_ERROR_REASON_PACKAGE_NUM + reason);
        }
    }

    private void initCommandWorker() {
        CommandWorker commandWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda1
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.lambda$initCommandWorker$7();
            }
        });
        this.mKeyUpgradeCommandWorker = commandWorker;
        commandWorker.setTargetAddress(CommunicationUtil.KEYBOARD_ADDRESS);
        this.mAllWorkerList.add(this.mKeyUpgradeCommandWorker);
        CommandWorker commandWorker2 = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda2
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.lambda$initCommandWorker$8();
            }
        });
        this.mTouchPadCommandWorker = commandWorker2;
        commandWorker2.setTargetAddress(CommunicationUtil.TOUCHPAD_ADDRESS);
        this.mAllWorkerList.add(this.mTouchPadCommandWorker);
        CommandWorker commandWorker3 = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda3
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.lambda$initCommandWorker$9();
            }
        });
        this.mMCUUpgradeCommandWorker = commandWorker3;
        commandWorker3.setTargetAddress(CommunicationUtil.MCU_ADDRESS);
        this.mAllWorkerList.add(this.mMCUUpgradeCommandWorker);
        CommandWorker commandWorker4 = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda4
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.lambda$initCommandWorker$10();
            }
        });
        this.mMCUVersionWorker = commandWorker4;
        commandWorker4.setTargetAddress(CommunicationUtil.MCU_ADDRESS);
        this.mAllWorkerList.add(this.mMCUVersionWorker);
        CommandWorker commandWorker5 = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda5
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.lambda$initCommandWorker$11();
            }
        });
        this.mKeyboardVersionWorker = commandWorker5;
        commandWorker5.setTargetAddress(CommunicationUtil.KEYBOARD_ADDRESS);
        this.mAllWorkerList.add(this.mKeyboardVersionWorker);
        this.mEmptyCommandWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda6
            @Override // com.android.server.input.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                Slog.e(IICNodeHelper.TAG, "No Comannd trigger, The keyboard is Exception response!");
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initCommandWorker$7() {
        byte[] responseData = this.mKeyUpgradeCommandWorker.getCommandResponse();
        if (responseData[4] == -111) {
            if (!this.mIsBinDataOK) {
                int index = this.mKeyUpgradeCommandWorker.getReceiverPackageIndex() + 1;
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getBinPackInfo(CommunicationUtil.KEYBOARD_ADDRESS, index * 52));
            } else {
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getUpEndInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getUpFlashInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mIsBinDataOK = false;
            }
            this.mKeyUpgradeCommandWorker.sendCommand(4);
            return;
        }
        if (this.mKeyUpgradeCommandWorker.getCommandSize() != 0) {
            this.mKeyUpgradeCommandWorker.sendCommand(4);
            return;
        }
        this.mKeyUpgradeCommandWorker.resetWorker();
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initCommandWorker$8() {
        byte[] responseData = this.mTouchPadCommandWorker.getCommandResponse();
        if (responseData[4] == -111) {
            if (!this.mIsBinDataOK) {
                int index = this.mTouchPadCommandWorker.getReceiverPackageIndex() + 1;
                this.mTouchPadCommandWorker.insertCommandToQueue(this.mTouchPadUpgradeUtil.getBinPackInfo(CommunicationUtil.KEYBOARD_ADDRESS, index * 52));
            } else {
                this.mTouchPadCommandWorker.insertCommandToQueue(this.mTouchPadUpgradeUtil.getUpEndInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mTouchPadCommandWorker.insertCommandToQueue(this.mTouchPadUpgradeUtil.getUpFlashInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mIsBinDataOK = false;
            }
            this.mTouchPadCommandWorker.sendCommand(4);
            return;
        }
        if (this.mTouchPadCommandWorker.getCommandSize() != 0) {
            this.mTouchPadCommandWorker.sendCommand(4);
            return;
        }
        this.mTouchPadCommandWorker.resetWorker();
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initCommandWorker$9() {
        byte[] responseData = this.mMCUUpgradeCommandWorker.getCommandResponse();
        if (responseData[4] == -111) {
            if (!this.mIsBinDataOK) {
                int index = this.mMCUUpgradeCommandWorker.getReceiverPackageIndex() + 1;
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getBinPackInfo(index * 52));
            } else {
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getUpEndInfo());
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getUpFlashInfo());
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getResetInfo());
                this.mIsBinDataOK = false;
            }
            this.mMCUUpgradeCommandWorker.sendCommand(4);
            return;
        }
        if (this.mMCUUpgradeCommandWorker.getCommandSize() != 0) {
            this.mMCUUpgradeCommandWorker.sendCommand(4);
            return;
        }
        this.mMCUUpgradeCommandWorker.resetWorker();
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initCommandWorker$10() {
        if (this.mMCUVersionWorker.getCommandSize() != 0) {
            this.mMCUVersionWorker.sendCommand(4);
            return;
        }
        this.mMCUVersionWorker.resetWorker();
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initCommandWorker$11() {
        if (this.mKeyboardVersionWorker.getCommandSize() != 0) {
            this.mKeyboardVersionWorker.sendCommand(4);
            return;
        }
        this.mKeyboardVersionWorker.resetWorker();
        UpgradeCommandHandler upgradeCommandHandler = this.mUpgradeCommandHandler;
        upgradeCommandHandler.sendMessage(upgradeCommandHandler.obtainMessage(96));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public CommandWorker getTriggerWorker() {
        for (CommandWorker worker : this.mAllWorkerList) {
            if (worker.isWorkRunning()) {
                return worker;
            }
        }
        Slog.e(TAG, "can't find the running worker");
        return this.mEmptyCommandWorker;
    }

    /* loaded from: classes.dex */
    class SocketCallbackListener implements CommunicationUtil.SocketCallBack {
        SocketCallbackListener() {
        }

        @Override // com.android.server.input.padkeyboard.iic.CommunicationUtil.SocketCallBack
        public void responseFromKeyboard(byte[] data) {
            if (data.length < 6) {
                Slog.i(IICNodeHelper.TAG, "Exception Data:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
                return;
            }
            switch (data[4]) {
                case -111:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Bin Package");
                    parseKeyboardUpdatePackageInfo(data);
                    return;
                case -94:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Search Status");
                    if (data[7] == 0) {
                        parseKeyboardStatus(data);
                        return;
                    } else {
                        Slog.e(IICNodeHelper.TAG, "read Mcu Status error : " + String.format("0x%02x", Byte.valueOf(data[7])));
                        return;
                    }
                case -16:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver 803 report read Keyboard status");
                    if (data[7] == 0) {
                        if (data[5] == CommunicationUtil.KB_FEATURE.KB_BACKLIGHT.getCommand()) {
                            Slog.i(IICNodeHelper.TAG, "set backLight success");
                            return;
                        }
                        return;
                    } else {
                        if (data[7] == 1) {
                            Slog.e(IICNodeHelper.TAG, "Command word not supported/malformed");
                            return;
                        }
                        if (data[7] == 2) {
                            Slog.e(IICNodeHelper.TAG, "Keyboard is disconnection, Command write fail");
                            return;
                        } else if (data[7] == 3) {
                            Slog.e(IICNodeHelper.TAG, "MCU is busy,The Command lose possibility");
                            return;
                        } else {
                            Slog.e(IICNodeHelper.TAG, "Other Error");
                            return;
                        }
                    }
                case 1:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver keyBoard/Flash Version");
                    parseKeyboardVersionInfo(data);
                    return;
                case 2:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Upgrade");
                    parseKeyboardUpdateInfo(data);
                    return;
                case 4:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Upgrade End");
                    parseKeyboardUpdateFinishInfo(data);
                    return;
                case 6:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Flash");
                    parseKeyboardUpgradeFlash(data);
                    return;
                case 32:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard recover firmware status");
                    if (data[5] != 1) {
                        Slog.e(IICNodeHelper.TAG, "parse keyboard recover firmware status fail!");
                        return;
                    }
                    if (data[6] == 54) {
                        Toast.makeText(IICNodeHelper.this.mContext, IICNodeHelper.this.mContext.getResources().getString(286196289), 0).show();
                        if (IICNodeHelper.this.mIsBleKeyboard) {
                            IICNodeHelper.this.sendBroadCast2Ble(false);
                            IICNodeHelper.this.sendBroadCast2Ble();
                            return;
                        }
                        return;
                    }
                    return;
                case 34:
                    boolean isEnable = false;
                    if (data[5] == 1) {
                        isEnable = true;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onKeyboardEnableStateChanged(isEnable);
                    return;
                case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                    if (data[6] == 0) {
                        Slog.i(IICNodeHelper.TAG, "keyboard has restart! ");
                        return;
                    } else if (data[6] != 100) {
                        Slog.e(IICNodeHelper.TAG, "keyboard reauth data error! " + ((int) data[6]));
                        return;
                    } else {
                        IICNodeHelper.this.mNanoSocketCallback.requestReAuth();
                        return;
                    }
                case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                    if (data[5] != 1) {
                        Slog.e(IICNodeHelper.TAG, "parse keyboard power fail!");
                        return;
                    } else if (data[6] == 1) {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard power on");
                        return;
                    } else {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard power low");
                        return;
                    }
                case NanoSocketCallback.CALLBACK_TYPE_TOUCHPAD /* 40 */:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard sleep status");
                    if (data[5] != 1) {
                        Slog.e(IICNodeHelper.TAG, "parse KeyboardSleepStatus fail!");
                        return;
                    } else if (data[6] == 0) {
                        IICNodeHelper.this.mNanoSocketCallback.onKeyboardSleepStatusChanged(true);
                        return;
                    } else {
                        if (data[6] == 1) {
                            IICNodeHelper.this.mNanoSocketCallback.onKeyboardSleepStatusChanged(false);
                            return;
                        }
                        return;
                    }
                case 49:
                case UsbKeyboardUtil.COMMAND_MIAUTH_STEP3_TYPE1 /* 50 */:
                case 51:
                case 52:
                case 53:
                    IICNodeHelper.this.mAuthCommandResponse = data;
                    IICNodeHelper.this.mCountDownLatch.countDown();
                    return;
                case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                    if (IICNodeHelper.this.mIsBleKeyboard) {
                        IICNodeHelper.this.mKeyboardBleAdd = String.format("%02x", Byte.valueOf(data[19])) + ":" + String.format("%02x", Byte.valueOf(data[20])) + ":" + String.format("%02x", Byte.valueOf(data[21])) + ":" + String.format("%02x", Byte.valueOf(data[22])) + ":" + String.format("%02x", Byte.valueOf(data[23])) + ":" + String.format("%02x", Byte.valueOf(data[24]));
                        IICNodeHelper.this.sendBroadCast2Ble();
                        Settings.System.putStringForUser(IICNodeHelper.this.mContext.getContentResolver(), MiuiIICKeyboardManager.LAST_CONNECT_BLE_ADDRESS, IICNodeHelper.this.mKeyboardBleAdd, -2);
                        return;
                    }
                    return;
                case 100:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver GSensor status");
                    parseGsensorDataInfo(data);
                    return;
                case AurogonImmobulusMode.MSG_REMOVE_ALL_MESSAGE /* 104 */:
                    if (data[6] == 1) {
                        Slog.i(IICNodeHelper.TAG, "Receiver Nfc Device");
                        IICNodeHelper.this.mNanoSocketCallback.onNFCTouched();
                        return;
                    }
                    return;
                default:
                    Slog.e(IICNodeHelper.TAG, "Other Command:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
                    return;
            }
        }

        private void parseKeyboardVersionInfo(byte[] data) {
            if (data.length < 19) {
                return;
            }
            int keyboardType = 0;
            if (data[2] == 56) {
                IICNodeHelper.this.mKeyBoardUpgradeFilePath.setLength(0);
                IICNodeHelper.this.mKeyBoardUpgradeFilePath.append("Keyboard_Upgrade");
                if (data[5] != 5) {
                    IICNodeHelper.this.mTouchPadVersion = String.format("%02x", Byte.valueOf(data[9])) + String.format("%02x", Byte.valueOf(data[8]));
                    NanoSocketCallback nanoSocketCallback = IICNodeHelper.this.mNanoSocketCallback;
                    NanoSocketCallback nanoSocketCallback2 = IICNodeHelper.this.mNanoSocketCallback;
                    nanoSocketCallback.onUpdateVersion(40, IICNodeHelper.this.mTouchPadVersion);
                    boolean hasTouchPad = MiuiKeyboardUtil.hasTouchpad(data[13]);
                    IICNodeHelper.this.mNanoSocketCallback.onUpdateKeyboardType(data[13], hasTouchPad);
                    keyboardType = 0 | (hasTouchPad ? 512 : 256);
                    getSuitableBinName(data[13], data[14]);
                    IICNodeHelper.this.mIsBleKeyboard = data[17] == 1;
                    InputCommonConfig commonConfig = InputCommonConfig.getInstance();
                    if (IICNodeHelper.this.mKeyboardType == 32 || IICNodeHelper.this.mKeyboardType == 33 || IICNodeHelper.this.mKeyboardType == 16) {
                        commonConfig.setTopGestureHotZoneHeightRate(0.07f);
                        commonConfig.setSlidGestureHotZoneWidthRate(0.06f);
                    }
                    commonConfig.flushToNative();
                }
                IICNodeHelper.this.mKeyBoardUpgradeFilePath.append(IICNodeHelper.UPGRADE_FOOT_LOCATION);
                int keyboardType2 = keyboardType | (data[10] & 255);
                MiuiIICKeyboardManager.getInstance(IICNodeHelper.this.mContext).setKeyboardType(keyboardType2);
                if (MiuiKeyboardUtil.isKeyboardSupportMuteLight(IICNodeHelper.this.mKeyboardType)) {
                    MiuiIICKeyboardManager.getInstance(IICNodeHelper.this.mContext).registerMuteLightReceiver();
                }
                Settings.System.putInt(IICNodeHelper.this.mContext.getContentResolver(), IICNodeHelper.KEYBOARD_INFO_CHANGED, keyboardType2);
                if (IICNodeHelper.this.mKeyboardVersionWorker.isWorkRunning()) {
                    IICNodeHelper.this.mKeyboardVersionWorker.triggerResponse(data);
                }
                IICNodeHelper.this.mKeyboardVersion = String.format("%02x", Byte.valueOf(data[7])) + String.format("%02x", Byte.valueOf(data[6]));
                NanoSocketCallback nanoSocketCallback3 = IICNodeHelper.this.mNanoSocketCallback;
                NanoSocketCallback nanoSocketCallback4 = IICNodeHelper.this.mNanoSocketCallback;
                nanoSocketCallback3.onUpdateVersion(20, IICNodeHelper.this.mKeyboardVersion);
                return;
            }
            if (data[2] == 57) {
                IICNodeHelper.this.mKeyboardFlashVersion = String.format("%02x", Byte.valueOf(data[9])) + String.format("%02x", Byte.valueOf(data[8]));
                NanoSocketCallback nanoSocketCallback5 = IICNodeHelper.this.mNanoSocketCallback;
                NanoSocketCallback nanoSocketCallback6 = IICNodeHelper.this.mNanoSocketCallback;
                nanoSocketCallback5.onUpdateVersion(3, IICNodeHelper.this.mKeyboardFlashVersion);
            }
        }

        private void getSuitableBinName(byte keyboardValue, byte touchpadValue) {
            IICNodeHelper.this.mKeyboardType = keyboardValue;
            IICNodeHelper.this.mKeyBoardUpgradeFilePath.append(d.h).append(String.format("0x%02x", Byte.valueOf(keyboardValue)));
            IICNodeHelper.this.mTouchPadType = touchpadValue;
            IICNodeHelper.this.mTouchPadUpgradeFilePath.setLength(0);
            IICNodeHelper.this.mTouchPadUpgradeFilePath.append("TouchPad_Upgrade").append(d.h).append(String.format("0x%02x", Byte.valueOf(touchpadValue))).append(IICNodeHelper.UPGRADE_FOOT_LOCATION);
            Slog.i(IICNodeHelper.TAG, "set TouchPad Upgrade File:" + IICNodeHelper.this.mTouchPadUpgradeFilePath.toString());
        }

        private void parseKeyboardUpdateInfo(byte[] data) {
            CommandWorker runningWorker = IICNodeHelper.this.getTriggerWorker();
            if (data[6] == 0) {
                Slog.i(IICNodeHelper.TAG, "Receiver Keyboard/TouchPad Upgrade Mode");
                if (IICNodeHelper.this.mKeyUpgradeCommandWorker.isWorkRunning()) {
                    IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(runningWorker.getTargetAddress(), 0);
                } else if (IICNodeHelper.this.mTouchPadCommandWorker.isWorkRunning()) {
                    IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(runningWorker.getTargetAddress(), 0);
                }
                runningWorker.triggerResponse(data);
                return;
            }
            if (data[6] == 7) {
                Slog.e(IICNodeHelper.TAG, "The Keyboard Upgrade not match devices");
                runningWorker.onWorkException(data);
            } else {
                runningWorker.onWorkException(data);
                Slog.i(IICNodeHelper.TAG, "stop upgrade keyboard and touch pad");
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(runningWorker.getTargetAddress(), "Upgrade Info Response error" + ((int) data[6]));
                IICNodeHelper.this.shutdownAllCommandQueue();
            }
        }

        private void parseKeyboardUpdatePackageInfo(byte[] data) {
            CommandWorker runningWorker = IICNodeHelper.this.getTriggerWorker();
            if (data[6] != 0) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(runningWorker.getTargetAddress(), "Keyboard Bin Package Info Response error " + ((int) data[6]), true, 286196301);
                runningWorker.onWorkException(data);
                return;
            }
            IICNodeHelper.this.mReceiverBinIndex = ((((data[10] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((data[9] << 8) & 65280)) + (data[8] & 255)) / 52;
            float progress = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            if (runningWorker.getTargetAddress() == 56) {
                if (IICNodeHelper.this.mReceiverBinIndex == IICNodeHelper.this.mKeyBoardUpgradeUtil.getBinPacketTotal(64) - 1) {
                    IICNodeHelper.this.mIsBinDataOK = true;
                }
                progress = Math.round(((IICNodeHelper.this.mReceiverBinIndex / IICNodeHelper.this.mKeyBoardUpgradeUtil.getBinPacketTotal(64)) * 10000.0f) / 100.0f);
            } else if (runningWorker.getTargetAddress() == 64) {
                if (IICNodeHelper.this.mReceiverBinIndex == IICNodeHelper.this.mTouchPadUpgradeUtil.getBinPacketTotal(64) - 1) {
                    IICNodeHelper.this.mIsBinDataOK = true;
                }
                progress = Math.round(((IICNodeHelper.this.mReceiverBinIndex / IICNodeHelper.this.mTouchPadUpgradeUtil.getBinPacketTotal(64)) * 10000.0f) / 100.0f);
            } else {
                Slog.e(IICNodeHelper.TAG, "not match command worker, stop send pkg info");
                runningWorker.onWorkException(data);
            }
            runningWorker.triggerResponseForPackage(data, IICNodeHelper.this.mReceiverBinIndex);
            if (IICNodeHelper.this.mNanoSocketCallback != null) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaProgress(runningWorker.getTargetAddress(), progress);
            }
        }

        private void parseKeyboardUpdateFinishInfo(byte[] data) {
            CommandWorker runningWorker = IICNodeHelper.this.getTriggerWorker();
            if (data[6] == 0) {
                runningWorker.triggerResponse(data);
            } else if (IICNodeHelper.this.mNanoSocketCallback != null) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(runningWorker.getTargetAddress(), "Upgrade finished Info Response error " + ((int) data[6]), true, 286196301);
                runningWorker.onWorkException(data);
            }
        }

        private void parseKeyboardUpgradeFlash(byte[] data) {
            CommandWorker runningWorker = IICNodeHelper.this.getTriggerWorker();
            if (data[6] == 0) {
                runningWorker.triggerResponse(data);
                IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(runningWorker.getTargetAddress(), 2);
            } else if (data[6] != 7) {
                IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(runningWorker.getTargetAddress(), "Keyboard Flash Info Response error " + ((int) data[6]), true, 286196301);
                runningWorker.onWorkException(data);
            } else {
                Slog.i(IICNodeHelper.TAG, "Keyboard is busy upgrading");
            }
        }

        private void parseGsensorDataInfo(byte[] data) {
            int x = ((data[7] << 4) & 4080) | ((data[6] >> 4) & 15);
            int y = ((data[9] << 4) & 4080) | ((data[8] >> 4) & 15);
            int z = ((data[11] << 4) & 4080) | ((data[10] >> 4) & 15);
            if ((x & 2048) == 2048) {
                x = -(4096 - x);
            }
            if ((y & 2048) == 2048) {
                y = -(4096 - y);
            }
            if ((z & 2048) == 2048) {
                z = -(4096 - z);
            }
            float x_normal = (x * 9.8f) / 256.0f;
            float y_normal = ((-y) * 9.8f) / 256.0f;
            float z_normal = ((-z) * 9.8f) / 256.0f;
            IICNodeHelper.this.mNanoSocketCallback.onKeyboardGSensorChanged(x_normal, y_normal, z_normal);
        }

        private void parseKeyboardStatus(byte[] data) {
            if (data[0] == 38 || data[0] == 36 || data[0] == 35) {
                if (data[18] == 0) {
                    if ((data[9] & 99) != 35) {
                        KeyboardInteraction.INTERACTION.setConnectIICLocked(false);
                        if ((data[9] & 99) == 67) {
                            Slog.i(IICNodeHelper.TAG, "Keyboard is connect,but Pogo pin is exception");
                            Toast.makeText(IICNodeHelper.this.mContext, IICNodeHelper.this.mContext.getResources().getString(286196297), 0).show();
                        } else if ((data[9] & 3) == 1) {
                            Slog.i(IICNodeHelper.TAG, "Keyboard is connect,but TRX is exception or leather case coming");
                        } else {
                            Slog.e(IICNodeHelper.TAG, "Keyboard is disConnection");
                        }
                    } else {
                        KeyboardInteraction.INTERACTION.setConnectIICLocked(true);
                    }
                } else if (data[18] == 1) {
                    KeyboardInteraction.INTERACTION.setConnectIICLocked(false);
                    Slog.i(IICNodeHelper.TAG, "The keyboard power supply current exceeds the limit！");
                }
                if (!KeyboardInteraction.INTERACTION.isConnectIICLocked() && IICNodeHelper.this.mIsBleKeyboard) {
                    IICNodeHelper.this.sendBroadCast2Ble();
                }
            }
            int battry = ((data[11] << 8) & 65280) + (data[10] & 255);
            int serial = data[17] & 7;
            int times = (data[17] >> 4) & 15;
            Slog.i(IICNodeHelper.TAG, "Receiver mcu KEY_S:" + String.format("0x%02x", Byte.valueOf(data[9])) + " KEY_R:" + battry + "mv E_UART:" + String.format("0x%02x%02x", Byte.valueOf(data[13]), Byte.valueOf(data[12])) + " E_PPM:" + String.format("0x%02x%02x%02x%02x", Byte.valueOf(data[17]), Byte.valueOf(data[16]), Byte.valueOf(data[15]), Byte.valueOf(data[14])) + " Serial:" + serial + " PowerUp:" + times + " OCP_F:" + ((int) data[18]));
        }

        @Override // com.android.server.input.padkeyboard.iic.CommunicationUtil.SocketCallBack
        public void responseFromMCU(byte[] data) {
            if (data.length < 6) {
                Slog.i(IICNodeHelper.TAG, "Exception Data:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
                return;
            }
            switch (data[4]) {
                case -111:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Bin Package");
                    if (data[6] == 0) {
                        int nextIndex = ((((data[10] << MiuiKeyboardUtil.KEYBOARD_TYPE_UCJ_HIGH) & 16711680) + ((data[9] << 8) & 65280)) + (data[8] & 255)) / 52;
                        IICNodeHelper.this.mReceiverBinIndex = nextIndex;
                        if (IICNodeHelper.this.mReceiverBinIndex == IICNodeHelper.this.mMcuUpgradeUtil.getBinPacketTotal(64) - 1) {
                            IICNodeHelper.this.mIsBinDataOK = true;
                        }
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponseForPackage(data, IICNodeHelper.this.mReceiverBinIndex);
                        float progress = Math.round(((IICNodeHelper.this.mReceiverBinIndex / IICNodeHelper.this.mMcuUpgradeUtil.getBinPacketTotal(64)) * 10000.0f) / 100.0f);
                        if (IICNodeHelper.this.mNanoSocketCallback != null) {
                            IICNodeHelper.this.mNanoSocketCallback.onOtaProgress(CommunicationUtil.MCU_ADDRESS, progress);
                            return;
                        }
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "MCU Package info error:" + ((int) data[6]));
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                    return;
                case 1:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver get MCU version");
                    if (data[6] == 2) {
                        onMcuVersionResponse(data);
                        return;
                    }
                    return;
                case 2:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Upgrade");
                    if (data[6] == 0) {
                        Slog.i(IICNodeHelper.TAG, "receiver MCU Upgrade start");
                        IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(CommunicationUtil.MCU_ADDRESS, 0);
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    } else {
                        IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "MCU Upgrade info error" + ((int) data[6]));
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                        return;
                    }
                case 3:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Reset");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mMCUVersion = null;
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(CommunicationUtil.MCU_ADDRESS, 2);
                        return;
                    } else {
                        IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "Reset error:" + ((int) data[6]));
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                        return;
                    }
                case 4:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU UpEnd");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    } else {
                        IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "UpEnd error:" + ((int) data[6]));
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                        return;
                    }
                case 6:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Flash");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    } else {
                        if (data[6] != 5) {
                            IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                            IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(CommunicationUtil.MCU_ADDRESS, "MCU Flash error:" + ((int) data[6]));
                            return;
                        }
                        return;
                    }
                default:
                    return;
            }
        }

        private void onMcuVersionResponse(byte[] data) {
            byte[] deviceVersion = new byte[16];
            System.arraycopy(data, 7, deviceVersion, 0, 16);
            IICNodeHelper.this.mMCUVersion = MiuiKeyboardUtil.Bytes2String(deviceVersion);
            IICNodeHelper.this.mMCUUpgradeFilePath.setLength(0);
            IICNodeHelper.this.mMCUUpgradeFilePath.append("MCU_Upgrade").append(IICNodeHelper.UPGRADE_FOOT_LOCATION);
            Slog.i(IICNodeHelper.TAG, "set MCU Upgrade File:" + IICNodeHelper.this.mMCUUpgradeFilePath.toString());
            if (IICNodeHelper.this.mMCUVersionWorker.isWorkRunning()) {
                IICNodeHelper.this.mMCUVersionWorker.triggerResponse(data);
            }
            if (IICNodeHelper.this.mNanoSocketCallback != null) {
                NanoSocketCallback nanoSocketCallback = IICNodeHelper.this.mNanoSocketCallback;
                NanoSocketCallback nanoSocketCallback2 = IICNodeHelper.this.mNanoSocketCallback;
                nanoSocketCallback.onUpdateVersion(10, IICNodeHelper.this.mMCUVersion);
            }
        }
    }

    public void sendNFCData() {
        OnehopInfo info = new OnehopInfo.Builder().setActionSuffix(OnehopInfo.ACTION_SUFFIX_MIRROR).setBtMac(OnehopInfo.getBtMacAddress(this.mContext)).setExtAbility(OnehopInfo.getExtAbility()).build();
        this.mCommunicationUtil.sendNFC(info.getPayload(info));
    }

    public void getBleKeyboardStatus() {
        this.mBlueToothKeyboardManager.writeDataToBleDevice(CommunicationUtil.BleDeviceUtil.CMD_KB_STATUS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UpgradeCommandHandler extends Handler {
        public static final String DATA_SEND_COMMAND = "command";
        public static final String DATA_WORKER_OBJECT = "worker";
        public static final int MSG_NOTIFY_READY_DO_COMMAND = 96;
        public static final int MSG_SEND_COMMAND_QUEUE = 97;

        UpgradeCommandHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 97) {
                int repeatTimes = msg.arg1;
                CommandWorker worker = (CommandWorker) msg.getData().getParcelable(DATA_WORKER_OBJECT);
                if (repeatTimes != 0) {
                    byte[] command = msg.getData().getByteArray(DATA_SEND_COMMAND);
                    KeyboardInteraction.INTERACTION.communicateWithIIC(command);
                    Bundle data = new Bundle();
                    data.putByteArray(DATA_SEND_COMMAND, command);
                    data.putParcelable(DATA_WORKER_OBJECT, worker);
                    Message msg2 = Message.obtain(IICNodeHelper.this.mUpgradeCommandHandler, 97);
                    msg2.arg1 = repeatTimes - 1;
                    msg2.setData(data);
                    IICNodeHelper.this.mUpgradeCommandHandler.sendMessageDelayed(msg2, 1500L);
                    return;
                }
                worker.notResponseException();
                return;
            }
            if (msg.what == 96) {
                if (!IICNodeHelper.this.hasAnyWorkerRunning()) {
                    Runnable worker2 = (Runnable) IICNodeHelper.this.mReadyRunningTask.poll();
                    if (worker2 != null) {
                        Slog.i(IICNodeHelper.TAG, "The WorkQueue has:" + IICNodeHelper.this.mReadyRunningTask.size());
                        IICNodeHelper.this.mNanoSocketCallback.requestStayAwake();
                        worker2.run();
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.requestReleaseAwake();
                    return;
                }
                Slog.i(IICNodeHelper.TAG, "Still has running commandWorker");
            }
        }
    }

    public void shutdownAllCommandQueue() {
        Slog.e(TAG, "clear all keyboard worker");
        this.mKeyboardVersionWorker.resetWorker();
        this.mKeyUpgradeCommandWorker.resetWorker();
        this.mTouchPadCommandWorker.resetWorker();
        this.mMCUVersionWorker.resetWorker();
        this.mMCUUpgradeCommandWorker.resetWorker();
        this.mReadyRunningTask.clear();
    }

    public boolean hasAnyWorkerRunning() {
        for (CommandWorker worker : this.mAllWorkerList) {
            if (worker.isWorkRunning()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBroadCast2Ble() {
        sendBroadCast2Ble(KeyboardInteraction.INTERACTION.isConnectIICLocked());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBroadCast2Ble(boolean isIICConnected) {
        Slog.i(TAG, "Notify Ble iic is " + isIICConnected);
        Intent intent = new Intent("com.xiaomi.bluetooth.action.KEYBOARD_ATTACH");
        intent.setFlags(1073741824);
        intent.putExtra("com.xiaomi.bluetooth.keyboard.extra.ADDRESS", this.mKeyboardBleAdd);
        intent.putExtra("com.xiaomi.bluetooth.keyboard.extra.ATTACH_STATE", isIICConnected);
        this.mContext.sendBroadcast(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CommandWorker implements Parcelable {
        private byte[] mDoingCommand;
        private boolean mIsRunning;
        private int mReceiverPackageIndex;
        private ResponseListener mResponseListener;
        private byte mTargetAddress = 0;
        private final Queue<byte[]> mCommandQueue = new LinkedBlockingQueue();
        private byte[] mResponse = new byte[64];

        public void insertCommandToQueue(byte[] command) {
            this.mCommandQueue.offer(command);
        }

        public CommandWorker(ResponseListener listener) {
            this.mResponseListener = listener;
        }

        public void setTargetAddress(byte address) {
            this.mTargetAddress = address;
        }

        public byte getTargetAddress() {
            return this.mTargetAddress;
        }

        public boolean isWorkRunning() {
            return this.mIsRunning;
        }

        public int getCommandSize() {
            return this.mCommandQueue.size();
        }

        public void sendCommand(int repeatTimes) {
            this.mIsRunning = true;
            this.mDoingCommand = this.mCommandQueue.poll();
            IICNodeHelper.this.mUpgradeCommandHandler.removeMessages(97);
            IICNodeHelper.this.mUpgradeCommandHandler.post(new Runnable() { // from class: com.android.server.input.padkeyboard.iic.IICNodeHelper$CommandWorker$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    IICNodeHelper.CommandWorker.this.lambda$sendCommand$0();
                }
            });
            Bundle data = new Bundle();
            data.putByteArray(UpgradeCommandHandler.DATA_SEND_COMMAND, this.mDoingCommand);
            data.putParcelable(UpgradeCommandHandler.DATA_WORKER_OBJECT, this);
            Message msg2 = Message.obtain(IICNodeHelper.this.mUpgradeCommandHandler, 97);
            msg2.arg1 = repeatTimes;
            msg2.setData(data);
            IICNodeHelper.this.mUpgradeCommandHandler.sendMessageDelayed(msg2, 1500L);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$sendCommand$0() {
            KeyboardInteraction.INTERACTION.communicateWithIIC(this.mDoingCommand);
        }

        public void notResponseException() {
            if (!this.mIsRunning) {
                return;
            }
            IICNodeHelper.this.mNanoSocketCallback.onWriteSocketErrorInfo("Time Out!");
            resetWorker();
            this.mResponseListener.onCommandResponse();
        }

        public void triggerResponse(byte[] response) {
            if (this.mIsRunning && response[4] == this.mDoingCommand[8]) {
                this.mResponse = response;
                this.mResponseListener.onCommandResponse();
            }
        }

        public void triggerResponseForPackage(byte[] response, int index) {
            if (!this.mIsRunning) {
                return;
            }
            this.mResponse = response;
            this.mReceiverPackageIndex = index;
            this.mResponseListener.onCommandResponse();
        }

        public int getReceiverPackageIndex() {
            return this.mReceiverPackageIndex;
        }

        public byte[] getCommandResponse() {
            return this.mResponse;
        }

        public void onWorkException(byte[] response) {
            if (!this.mIsRunning) {
                return;
            }
            byte b = response[4];
            byte b2 = this.mDoingCommand[8];
            if (b == b2 || (response[4] == -111 && b2 == 17)) {
                resetWorker();
                this.mResponseListener.onCommandResponse();
            }
        }

        public void emptyCommandResponse() {
            this.mResponseListener.onCommandResponse();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void resetWorker() {
            this.mReceiverPackageIndex = 0;
            this.mDoingCommand = new byte[66];
            this.mIsRunning = false;
            this.mCommandQueue.clear();
            this.mResponse = new byte[66];
            IICNodeHelper.this.mUpgradeCommandHandler.removeMessages(97);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mReceiverPackageIndex);
        }
    }
}

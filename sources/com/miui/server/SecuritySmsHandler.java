package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;
import com.android.server.wm.ActivityStarterInjector;
import miui.provider.ExtraTelephony;
import miui.telephony.SubscriptionManager;
import miui.telephony.TelephonyManager;

/* loaded from: classes.dex */
class SecuritySmsHandler {
    private static final String TAG = "SecuritySmsHandler";
    private final Context mContext;
    private final Handler mHandler;
    private int mInterceptSmsCallerUid = 0;
    private String mInterceptSmsCallerPkgName = null;
    private int mInterceptSmsCount = 0;
    private String mInterceptSmsSenderNum = null;
    private final Object mInterceptSmsLock = new Object();
    private BroadcastReceiver mNormalMsgResultReceiver = new BroadcastReceiver() { // from class: com.miui.server.SecuritySmsHandler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(SecuritySmsHandler.TAG, "mNormalMsgResultReceiver sms dispatched, action:" + action);
            if ("android.provider.Telephony.SMS_DELIVER".equals(action)) {
                intent.setComponent(null);
                intent.setFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                intent.setAction("android.provider.Telephony.SMS_RECEIVED");
                SecuritySmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
                Log.i(SecuritySmsHandler.TAG, "mNormalMsgResultReceiver dispatch SMS_RECEIVED_ACTION");
                return;
            }
            if ("android.provider.Telephony.WAP_PUSH_DELIVER".equals(action)) {
                intent.setComponent(null);
                intent.setFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
                intent.setAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
                SecuritySmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
                Log.i(SecuritySmsHandler.TAG, "mNormalMsgResultReceiver dispatch WAP_PUSH_RECEIVED_ACTION");
            }
        }
    };
    private BroadcastReceiver mInterceptedSmsResultReceiver = new BroadcastReceiver() { // from class: com.miui.server.SecuritySmsHandler.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int slotId = SecuritySmsHandler.getSlotIdFromIntent(intent);
            Log.i(SecuritySmsHandler.TAG, "mInterceptedSmsResultReceiver sms dispatched, action:" + action);
            if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
                int resultCode = getResultCode();
                if (resultCode == -1) {
                    Log.i(SecuritySmsHandler.TAG, "mInterceptedSmsResultReceiver SMS_RECEIVED_ACTION not aborted");
                    SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    StringBuilder sb = new StringBuilder();
                    for (SmsMessage smsMessage : msgs) {
                        sb.append(smsMessage.getDisplayMessageBody());
                    }
                    String address = msgs[0].getOriginatingAddress();
                    String body = sb.toString();
                    int blockType = SecuritySmsHandler.this.checkByAntiSpam(address, body, slotId);
                    if (blockType != 0) {
                        intent.putExtra("blockType", blockType);
                        if (ExtraTelephony.getRealBlockType(blockType) >= 3) {
                            Log.i(SecuritySmsHandler.TAG, "mInterceptedSmsResultReceiver: This sms is intercepted by AntiSpam");
                            SecuritySmsHandler.this.dispatchSmsToAntiSpam(intent);
                        } else {
                            SecuritySmsHandler.this.dispatchNormalSms(intent);
                        }
                    }
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public SecuritySmsHandler(Context context, Handler handler) {
        this.mHandler = handler;
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkSmsBlocked(Intent intent) {
        boolean blocked;
        Class<?> pduClass;
        Class<?> valueClass;
        Class<?> persisterClass;
        Object pdu;
        int blockType;
        Log.i(TAG, "enter checkSmsBlocked");
        boolean blocked2 = false;
        String action = intent.getAction();
        int slotId = getSlotIdFromIntent(intent);
        if (action.equals("android.provider.Telephony.SMS_DELIVER")) {
            SmsMessage[] msg = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            StringBuilder sb = new StringBuilder();
            if (msg == null) {
                return false;
            }
            for (SmsMessage smsMessage : msg) {
                sb.append(smsMessage.getDisplayMessageBody());
            }
            String address = msg[0].getOriginatingAddress();
            String body = sb.toString();
            if (checkWithInterceptedSender(address)) {
                Log.i(TAG, "Intercepted by sender address");
                dispatchToInterceptApp(intent);
                blocked2 = true;
            }
            if (!blocked2 && (blockType = checkByAntiSpam(address, body, slotId)) != 0) {
                intent.putExtra("blockType", blockType);
                if (ExtraTelephony.getRealBlockType(blockType) >= 3) {
                    Log.i(TAG, "This sms is intercepted by AntiSpam");
                    dispatchSmsToAntiSpam(intent);
                    blocked2 = true;
                }
            }
        } else {
            if (!action.equals("android.provider.Telephony.WAP_PUSH_DELIVER")) {
                blocked = false;
            } else {
                byte[] pushData = intent.getByteArrayExtra("data");
                boolean contentDisposition = Resources.getSystem().getBoolean(17891754);
                try {
                    Class<?> pduParserClass = Class.forName("com.google.android.mms.pdu.PduParser");
                    pduClass = Class.forName("com.google.android.mms.pdu.GenericPdu");
                    valueClass = Class.forName("com.google.android.mms.pdu.EncodedStringValue");
                    persisterClass = Class.forName("com.google.android.mms.pdu.PduPersister");
                    Object parser = pduParserClass.getConstructor(byte[].class, Boolean.TYPE).newInstance(pushData, Boolean.valueOf(contentDisposition));
                    blocked = false;
                    try {
                        pdu = pduParserClass.getMethod("parse", new Class[0]).invoke(parser, new Object[0]);
                    } catch (Exception e) {
                        e = e;
                    }
                } catch (Exception e2) {
                    e = e2;
                }
                try {
                    Object encodedStringValue = pduClass.getMethod("getFrom", new Class[0]).invoke(pdu, new Object[0]);
                    byte[] addressByte = (byte[]) valueClass.getMethod("getTextString", new Class[0]).invoke(encodedStringValue, new Object[0]);
                    int blockType2 = checkByAntiSpam((String) persisterClass.getMethod("toIsoString", byte[].class).invoke(null, addressByte), null, slotId);
                    if (blockType2 != 0) {
                        intent.putExtra("blockType", blockType2);
                        if (ExtraTelephony.getRealBlockType(blockType2) >= 3) {
                            Log.i(TAG, "This mms is intercepted by AntiSpam");
                            dispatchMmsToAntiSpam(intent);
                            blocked2 = true;
                        }
                    }
                } catch (Exception e3) {
                    e = e3;
                    e.printStackTrace();
                    return false;
                }
            }
            blocked2 = blocked;
        }
        Log.i(TAG, "leave checkSmsBlocked, blocked:" + blocked2);
        return blocked2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startInterceptSmsBySender(String pkgName, String sender, int count) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_SMS_INTERCEPT", TAG);
        int callerUid = Binder.getCallingUid();
        synchronized (this.mInterceptSmsLock) {
            if (this.mInterceptSmsCallerUid == 0) {
                this.mInterceptSmsCallerUid = callerUid;
                this.mInterceptSmsCallerPkgName = pkgName;
                this.mInterceptSmsSenderNum = sender;
                this.mInterceptSmsCount = count;
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopInterceptSmsBySender() {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_SMS_INTERCEPT", TAG);
        int callerUid = Binder.getCallingUid();
        synchronized (this.mInterceptSmsLock) {
            int i = this.mInterceptSmsCallerUid;
            if (i == 0) {
                return true;
            }
            if (i == callerUid) {
                releaseSmsIntercept();
                return true;
            }
            return false;
        }
    }

    private boolean checkWithInterceptedSender(String sender) {
        boolean result = false;
        synchronized (this.mInterceptSmsLock) {
            Log.i(TAG, String.format("checkWithInterceptedSender: callerUid:%d, senderNum:%s, count:%d", Integer.valueOf(this.mInterceptSmsCallerUid), this.mInterceptSmsSenderNum, Integer.valueOf(this.mInterceptSmsCount)));
            if (this.mInterceptSmsCallerUid != 0 && TextUtils.equals(this.mInterceptSmsSenderNum, sender)) {
                int i = this.mInterceptSmsCount;
                if (i > 0) {
                    this.mInterceptSmsCount = i - 1;
                    result = true;
                }
                if (this.mInterceptSmsCount == 0) {
                    releaseSmsIntercept();
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int checkByAntiSpam(String address, String content, int slotId) {
        long token = Binder.clearCallingIdentity();
        int blockType = ExtraTelephony.getSmsBlockType(this.mContext, address, content, slotId);
        Binder.restoreCallingIdentity(token);
        Log.i(TAG, "checkByAntiSpam : blockType = " + blockType);
        return blockType;
    }

    private void releaseSmsIntercept() {
        this.mInterceptSmsCallerUid = 0;
        this.mInterceptSmsCallerPkgName = null;
        this.mInterceptSmsSenderNum = null;
        this.mInterceptSmsCount = 0;
    }

    private void dispatchToInterceptApp(Intent intent) {
        Log.i(TAG, "dispatchToInterceptApp");
        intent.setFlags(0);
        intent.setComponent(null);
        intent.setPackage(this.mInterceptSmsCallerPkgName);
        intent.setAction("android.provider.Telephony.SMS_RECEIVED");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mInterceptedSmsResultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchSmsToAntiSpam(Intent intent) {
        Log.i(TAG, "dispatchSmsToAntiSpam");
        intent.setComponent(null);
        intent.setPackage("com.android.mms");
        intent.setAction("android.provider.Telephony.SMS_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
    }

    private void dispatchMmsToAntiSpam(Intent intent) {
        Log.i(TAG, "dispatchMmsToAntiSpam");
        intent.setComponent(null);
        intent.setPackage("com.android.mms");
        intent.setAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchNormalSms(Intent intent) {
        Log.i(TAG, "dispatchNormalSms");
        intent.setPackage(null);
        ComponentName componentName = SmsApplication.getDefaultSmsApplication(this.mContext, true);
        if (componentName != null) {
            intent.setComponent(componentName);
            Log.i(TAG, String.format("Delivering SMS to: %s", componentName.getPackageName()));
        }
        intent.addFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
        intent.setAction("android.provider.Telephony.SMS_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mNormalMsgResultReceiver);
    }

    private void dispatchNormalMms(Intent intent) {
        Log.i(TAG, "dispatchNormalMms");
        intent.setPackage(null);
        ComponentName componentName = SmsApplication.getDefaultMmsApplication(this.mContext, true);
        if (componentName != null) {
            intent.setComponent(componentName);
            Log.i(TAG, String.format("Delivering MMS to: %s", componentName.getPackageName()));
        }
        intent.addFlags(ActivityStarterInjector.FLAG_ASSOCIATED_SETTINGS_AV);
        intent.setAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, this.mNormalMsgResultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchIntent(Intent intent, String permission, int appOp, BroadcastReceiver resultReceiver) {
        this.mContext.sendOrderedBroadcast(intent, permission, appOp, resultReceiver, this.mHandler, -1, (String) null, (Bundle) null);
    }

    public static int getSlotIdFromIntent(Intent intent) {
        int slotId = 0;
        if (TelephonyManager.getDefault().getPhoneCount() > 1 && (slotId = intent.getIntExtra(SubscriptionManager.SLOT_KEY, 0)) < 0) {
            Log.e(TAG, "getSlotIdFromIntent slotId < 0");
        }
        return slotId;
    }
}

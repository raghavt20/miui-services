package com.miui.server.enterprise;

import android.content.Context;
import android.provider.MiuiSettings;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.miui.enterprise.IPhoneManager;
import com.miui.enterprise.settings.EnterpriseSettings;
import java.util.List;
import miui.telephony.PhoneNumberUtils;
import miui.telephony.TelephonyManagerEx;

/* loaded from: classes.dex */
public class PhoneManagerService extends IPhoneManager.Stub {
    private static final String TAG = "Enterprise-phone";
    private Context mContext;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PhoneManagerService(Context context) {
        this.mContext = context;
    }

    public void controlSMS(int flags, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_sms_status", flags, userId);
    }

    public void controlPhoneCall(int flags, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_phone_call_status", flags, userId);
    }

    public void controlCellular(int flag, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_cellular_status", flag, userId);
        if (shouldOpen(flag)) {
            TelephonyManager.from(this.mContext).setDataEnabled(true);
        }
        if (shouldClose(flag)) {
            TelephonyManager.from(this.mContext).setDataEnabled(false);
        }
    }

    private boolean shouldOpen(int state) {
        return state == 2 || state == 4;
    }

    private boolean shouldClose(int state) {
        return state == 0 || state == 3;
    }

    public int getSMSStatus(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_sms_status", 0, userId);
    }

    public int getPhoneCallStatus(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_phone_call_status", 0, userId);
    }

    public int getCellularStatus(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_cellular_status", 0, userId);
    }

    public String getIMEI(int slotId) {
        ServiceUtils.checkPermission(this.mContext);
        return miui.telephony.TelephonyManager.getDefault().getImeiForSlot(slotId);
    }

    public void setPhoneCallAutoRecord(boolean z, int i) {
        ServiceUtils.checkPermission(this.mContext);
        MiuiSettings.System.putBooleanForUser(this.mContext.getContentResolver(), "button_auto_record_call", z, i);
        EnterpriseSettings.putInt(this.mContext, "ep_force_auto_call_record", z ? 1 : 0, i);
    }

    public void setPhoneCallAutoRecordDir(String dir) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_force_auto_call_record_dir", dir);
    }

    public boolean isAutoRecordPhoneCall(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_force_auto_call_record", 0, userId) == 1;
    }

    public void setSMSBlackList(List<String> list, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_sms_black_list", EnterpriseSettings.generateListSettings(list), userId);
    }

    public void setSMSWhiteList(List<String> list, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_sms_white_list", EnterpriseSettings.generateListSettings(list), userId);
    }

    public List<String> getSMSBlackList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_sms_black_list", userId));
    }

    public List<String> getSMSWhiteList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_sms_white_list", userId));
    }

    public void setSMSContactRestriction(int mode, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_sms_restriction_mode", mode, userId);
    }

    public int getSMSContactRestriction(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_sms_restriction_mode", 0, userId);
    }

    public void setCallBlackList(List<String> list, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_call_black_list", EnterpriseSettings.generateListSettings(list), userId);
    }

    public void setCallWhiteList(List<String> list, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_call_white_list", EnterpriseSettings.generateListSettings(list), userId);
    }

    public List<String> getCallBlackList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_call_black_list", userId));
    }

    public List<String> getCallWhiteList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_call_white_list", userId));
    }

    public void setCallContactRestriction(int mode, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_call_restriction_mode", mode, userId);
    }

    public int getCallContactRestriction(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_call_restriction_mode", 0, userId);
    }

    public void endCall() {
        ServiceUtils.checkPermission(this.mContext);
        Slog.d(TAG, "End current call");
        TelephonyManagerEx.getDefault().endCall();
        throw new RuntimeException("Not implemented");
    }

    public void disableCallForward(boolean z) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_disable_call_forward", z ? 1 : 0);
    }

    public void disableCallLog(boolean z) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_disable_call_log", z ? 1 : 0);
    }

    public String getAreaCode(String phoneNumber) {
        ServiceUtils.checkPermission(this.mContext);
        return PhoneNumberUtils.PhoneNumber.parse(phoneNumber).getLocationAreaCode(this.mContext);
    }

    public String getMeid(int slotId) {
        ServiceUtils.checkPermission(this.mContext);
        return miui.telephony.TelephonyManager.getDefault().getMeidForSlot(slotId);
    }

    public void setIccCardActivate(int i, boolean z) {
        ServiceUtils.checkPermission(this.mContext);
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("Invalid slotId value " + i + ". Must be either PhoneManagerMode.SLOT_ID_1 or PhoneManagerMode.SLOT_ID_2.");
        }
        miui.telephony.TelephonyManager telephonyManager = miui.telephony.TelephonyManager.getDefault();
        if (telephonyManager.getSimStateForSlot(i) == 1) {
            return;
        }
        if (i == 0) {
            EnterpriseSettings.putInt("ep_icc_card_1_disable", !z ? 1 : 0);
        } else {
            EnterpriseSettings.putInt("ep_icc_card_2_disable", !z ? 1 : 0);
        }
        telephonyManager.setIccCardActivate(i, z);
    }
}

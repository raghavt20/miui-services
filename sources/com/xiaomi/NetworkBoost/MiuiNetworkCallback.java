package com.xiaomi.NetworkBoost;

import com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public abstract class MiuiNetworkCallback extends IAIDLMiuiNetworkCallback.Stub implements IAIDLMiuiNetQoECallback, IAIDLMiuiNetSelectCallback, IAIDLMiuiWlanQoECallback {
    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback
    public void avaliableBssidCb(List<String> list) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetSelectCallback
    public void connectionStatusCb(int i) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void dsdaStateChanged(boolean z) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void ifaceAdded(List<String> list) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void ifaceRemoved(List<String> list) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback
    public void masterQoECallBack(NetLinkLayerQoE netLinkLayerQoE) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void mediaPlayerPolicyNotify(int i, int i2, int i3) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onNetworkPriorityChanged(int i, int i2, int i3) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onScanSuccussed(int i) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onSetSlaveWifiResult(boolean z) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onSlaveWifiConnected(boolean z) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onSlaveWifiDisconnected(boolean z) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onSlaveWifiEnable(boolean z) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetworkCallback
    public void onSlaveWifiEnableV1(int i) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiNetQoECallback
    public void slaveQoECallBack(NetLinkLayerQoE netLinkLayerQoE) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback
    public void wlanQoEReportUpdateMaster(Map<String, String> map) {
    }

    @Override // com.xiaomi.NetworkBoost.IAIDLMiuiWlanQoECallback
    public void wlanQoEReportUpdateSlave(Map<String, String> map) {
    }
}

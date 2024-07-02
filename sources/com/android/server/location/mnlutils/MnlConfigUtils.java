package com.android.server.location.mnlutils;

import android.os.RemoteException;
import android.util.Log;
import com.android.server.location.hardware.mtk.engineermode.V1_3.IEmd;
import com.android.server.location.hardware.mtk.engineermode.aidl.IEmds;
import com.android.server.location.mnlutils.bean.MnlConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class MnlConfigUtils {
    private static final String TAG = "Glp-MnlConfigUtils";
    IEmd hidlService = null;
    IEmds aidlService = null;

    /* loaded from: classes.dex */
    private enum MnlFileType {
        MNL_FILE_CURRENT,
        MNL_FILE_BACKUP
    }

    private ArrayList<Byte> loadFile(MnlFileType fileType) {
        try {
            ArrayList<Integer> input = new ArrayList<>();
            input.add(Integer.valueOf(fileType.ordinal()));
            if (IEmdHidlUtils.getEmAidlService() != null) {
                this.aidlService = IEmdHidlUtils.getEmAidlService();
                int[] array = input.stream().mapToInt(new MnlConfigUtils$$ExternalSyntheticLambda0()).toArray();
                return arrayToByteList(this.aidlService.readMnlConfigFile(array));
            }
            if (this.hidlService == null) {
                this.hidlService = IEmdHidlUtils.getEmHidlService();
            }
            return this.hidlService.readMnlConfigFile(input);
        } catch (RemoteException e) {
            Log.d(TAG, "load mnl config fail");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private boolean saveFile(ArrayList<Byte> dataToWrite) {
        ArrayList<Integer> reserve = new ArrayList<>();
        try {
            if (IEmdHidlUtils.getEmAidlService() != null) {
                this.aidlService = IEmdHidlUtils.getEmAidlService();
                int[] array = reserve.stream().mapToInt(new MnlConfigUtils$$ExternalSyntheticLambda0()).toArray();
                return this.aidlService.writeMnlConfigFile(listToByteArray(dataToWrite), array);
            }
            if (this.hidlService == null) {
                this.hidlService = IEmdHidlUtils.getEmHidlService();
            }
            return this.hidlService.writeMnlConfigFile(dataToWrite, reserve);
        } catch (RemoteException e) {
            Log.d(TAG, "save mnl config fail");
            e.printStackTrace();
            return false;
        }
    }

    private ArrayList<Byte> arrayToByteList(byte[] bytesPrim) {
        if (bytesPrim == null) {
            return null;
        }
        ArrayList<Byte> result = new ArrayList<>();
        Byte[] bArr = new Byte[bytesPrim.length];
        for (byte b : bytesPrim) {
            result.add(Byte.valueOf(b));
        }
        return result;
    }

    private byte[] listToByteArray(ArrayList<Byte> bytesPrim) {
        if (bytesPrim == null) {
            return null;
        }
        Byte[] bytes = (Byte[]) bytesPrim.toArray(new Byte[bytesPrim.size()]);
        byte[] result = new byte[bytes.length];
        int i = 0;
        Iterator<Byte> it = bytesPrim.iterator();
        while (it.hasNext()) {
            Byte b = it.next();
            result[i] = b.byteValue();
            i++;
        }
        return result;
    }

    public MnlConfig getMnlConfig() {
        ArrayList<Byte> byteArrayList = loadFile(MnlFileType.MNL_FILE_CURRENT);
        if (byteArrayList.size() == 0) {
            return null;
        }
        byte[] bytes = new byte[byteArrayList.size()];
        int index = 0;
        Iterator<Byte> it = byteArrayList.iterator();
        while (it.hasNext()) {
            Byte bt = it.next();
            bytes[index] = bt.byteValue();
            index++;
        }
        return MnlConfigParseUtils.parseXml(new String(bytes));
    }

    public MnlConfig getBackUpMnlConfig() {
        ArrayList<Byte> byteArrayList = loadFile(MnlFileType.MNL_FILE_BACKUP);
        if (byteArrayList.size() == 0) {
            return null;
        }
        byte[] bytes = new byte[byteArrayList.size()];
        int index = 0;
        Iterator<Byte> it = byteArrayList.iterator();
        while (it.hasNext()) {
            Byte bt = it.next();
            bytes[index] = bt.byteValue();
            index++;
        }
        return MnlConfigParseUtils.parseXml(new String(bytes));
    }

    public boolean saveMnlConfig(MnlConfig mnlConfig) {
        String s = mnlConfig.toXmlString();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ArrayList<Byte> byteArrayList = new ArrayList<>();
        for (byte b : bytes) {
            byteArrayList.add(Byte.valueOf(b));
        }
        return saveFile(byteArrayList);
    }

    public boolean resetMnlConfig() {
        ArrayList<Byte> data = loadFile(MnlFileType.MNL_FILE_BACKUP);
        if (data != null && data.size() != 0) {
            return saveFile(data);
        }
        return false;
    }
}

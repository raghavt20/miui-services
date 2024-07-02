package database.SlaDbSchema;

import android.database.Cursor;
import android.database.CursorWrapper;
import com.xiaomi.NetworkBoost.slaservice.SLAApp;
import database.SlaDbSchema.SlaDbSchema;

/* loaded from: classes.dex */
public class SlaCursorWrapper extends CursorWrapper {
    private int mDay;
    private long mDayTraffic;
    private int mMonth;
    private long mMonthTraffic;
    private boolean mState;

    public SlaCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public SLAApp getSLAApp() {
        String uid = getString(getColumnIndex(SlaDbSchema.SlaTable.Uidlist.UID));
        long daytraffic = getLong(getColumnIndex(SlaDbSchema.SlaTable.Uidlist.DAYTRAFFIC));
        long monthtraffic = getLong(getColumnIndex(SlaDbSchema.SlaTable.Uidlist.MONTHTRAFFIC));
        int state = getInt(getColumnIndex("state"));
        int day = getInt(getColumnIndex(SlaDbSchema.SlaTable.Uidlist.DAY));
        int month = getInt(getColumnIndex(SlaDbSchema.SlaTable.Uidlist.MONTH));
        SLAApp slaApp = new SLAApp();
        slaApp.setUid(uid);
        slaApp.setDay(day);
        slaApp.setMonth(month);
        slaApp.setDayTraffic(daytraffic);
        slaApp.setMonthTraffic(monthtraffic);
        slaApp.setState(state != 0);
        return slaApp;
    }
}

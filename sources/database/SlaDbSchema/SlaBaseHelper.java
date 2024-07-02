package database.SlaDbSchema;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/* loaded from: classes.dex */
public class SlaBaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SlaBase.db";
    private static final String TAG = SlaBaseHelper.class.getName();
    private static final int VERSION = 1;

    public SlaBaseHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "create db cmd:create table SlaUid(_id integer primary key autoincrement, uid,dayTraffic,monthTraffic,state,day,month)");
        db.execSQL("create table SlaUid(_id integer primary key autoincrement, uid,dayTraffic,monthTraffic,state,day,month)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}

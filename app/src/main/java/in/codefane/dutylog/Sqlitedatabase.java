package in.codefane.dutylog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

    public class Sqlitedatabase extends SQLiteOpenHelper {
        public static final String DATABASE_NAME ="location.db";
        public static final int DATABASE_VERSION=1;
        public static final String TABLE_NAME="location";
        public static final String COLUMN_ID="_id";
        public static final String COLUMN_LATITUDE="latitude";
        public static final String COLUMN_LONGITUDE="longitude";
        public static final String COLUMN_TIMESTAMP="timestamp";

        private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_LATITUDE + " REAL, " +
                        COLUMN_LONGITUDE + " REAL, " +
                        COLUMN_TIMESTAMP + " INTEGER);";

        public Sqlitedatabase(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(TABLE_CREATE);
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public void insertLocation(double latitude,double longitude,long timestamp){
            SQLiteDatabase db=this.getWritableDatabase();
            db.execSQL("INSERT INTO "+TABLE_NAME+"(latitude,longitude,timestamp) VALUES (?,?,?)",new Object[]{latitude,longitude,timestamp});
            db.close();

        }

        public Cursor getAllLocations(){
            SQLiteDatabase db=this.getReadableDatabase();
            return db.rawQuery("SELECT * FROM "+TABLE_NAME,null);
        }

        public void clearDatabase() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_NAME, null, null); // Deletes all records from the table
            db.close();
        }

}

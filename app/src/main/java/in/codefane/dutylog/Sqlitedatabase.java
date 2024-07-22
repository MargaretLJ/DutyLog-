package in.codefane.dutylog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.*;

public class Sqlitedatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "location.db";
    public static final int DATABASE_VERSION = 3;
    public static final String TABLE_NAME = "location";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_TOKEN = "token";
    private static final String TAG = "Sqlitedatabase";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +

                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_TIMESTAMP + " INTEGER, " +
                    COLUMN_TOKEN + " TEXT, " +
                    "SESSION_ID TEXT" + ")";

    public Sqlitedatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_TOKEN + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN SESSION_ID TEXT");
        }
    }

    public void insertLocation(double latitude, double longitude, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO " + TABLE_NAME + "(latitude,longitude,timestamp) VALUES (?,?,?)", new Object[]{latitude, longitude, timestamp});
        db.close();

    }

    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null); // Deletes all records from the table
        db.close();
    }

    public void saveToken(String token) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TOKEN, token);

        // Update the existing row with the token
        int rowsUpdated = db.update(TABLE_NAME, values, null, null);
        if (rowsUpdated == 0) {
            // If no rows were updated, insert a new row
            db.insert(TABLE_NAME, null, values);
        }
        db.close();
    }

    public void saveSessionId(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("SESSION_ID", sessionId);

        // Update the existing row with the session ID
        int rowsUpdated = db.update(TABLE_NAME, contentValues, null, null);
        if (rowsUpdated == 0) {
            // If no rows were updated, insert a new row
            db.insert(TABLE_NAME, null, contentValues);
        }
        db.close();
    }

    public String getToken() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String token = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{COLUMN_TOKEN}, null, null, null, null, null, "1"); // LIMIT 1

            if (cursor != null && cursor.moveToFirst()) {
                token = cursor.getString(0);
                Log.d(TAG, "Retrieved token: " + token);
            } else {
                Log.d(TAG, "Cursor is null or empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving token", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        if (token == null) {
            Log.d(TAG, "TOKEN NOT FOUND");
        }

        return token;
    }

    public String getSessionId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String sessionId = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{"SESSION_ID"}, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "1"); // LIMIT 1

            if (cursor != null && cursor.moveToFirst()) {
                sessionId = cursor.getString(0);
                Log.d(TAG, "Retrieved session ID: " + sessionId);
            } else {
                Log.d(TAG, "Cursor is null or empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving session ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        if (sessionId == null) {
            Log.d(TAG, "SESSION ID NOT FOUND");
        }

        return sessionId;
    }
    public List<LocationData> getLocationData() {
        List<LocationData> locationDataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_TIMESTAMP}, null, null, null, null, COLUMN_TIMESTAMP + " DESC"); // Order by timestamp DESC

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Sqlitedatabase.COLUMN_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Sqlitedatabase.COLUMN_LONGITUDE));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Sqlitedatabase.COLUMN_TIMESTAMP));
                    locationDataList.add(new LocationData(latitude, longitude, timestamp));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving location data", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return locationDataList;
    }

    // Define LocationData class if not defined already
    public static class LocationData {
        private double latitude;
        private double longitude;
        private long timestamp;

        public LocationData(double latitude, double longitude, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}

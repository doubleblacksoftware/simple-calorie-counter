package com.doubleblacksoftware.caloriecounter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by x on 5/28/15.
 */
public class CalorieDataProvider extends ContentProvider {
    // fields for my content provider
    static final String PROVIDER_NAME = "com.doubleblacksoftware.caloriecounter";
    static final String URL = "content://" + PROVIDER_NAME + "/entries";
    static final Uri CONTENT_URI = Uri.parse(URL);

    // fields for the database
    static final String ID = "_id";
    static final String LABEL = "label";
    static final String VALUE = "value";
    static final String ADDED = "added";
    static final String TIMESTAMP = "timestamp";

    // integer values used in content URI
    static final int VALUES = 1;
    static final int VALUE_ID = 2;
    static final int DAY_ID = 3;
    static final int SUMMARY_BY_DAY = 4;
    static final int LAST_30_DAYS = 5;
    static final int UNIQUE = 6;

    DBHelper dbHelper;

    // projection map for a query
    private static HashMap<String, String> ValueMap;

    // maps content URI "patterns" to the integer values that were set above
    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "entries", VALUES);
        uriMatcher.addURI(PROVIDER_NAME, "entries/#", VALUE_ID);
        uriMatcher.addURI(PROVIDER_NAME, "entries/#/#/#", DAY_ID); // YYYY/MM/DD
        uriMatcher.addURI(PROVIDER_NAME, "entries/summary/day/#/#/#", SUMMARY_BY_DAY); // YYYY/MM/DD
        uriMatcher.addURI(PROVIDER_NAME, "entries/summary/last30days", LAST_30_DAYS); // YYYY/MM
        uriMatcher.addURI(PROVIDER_NAME, "entries/unique", UNIQUE); // YYYY/MM
    }

    // database declarations
    private SQLiteDatabase database;
    static final String DATABASE_NAME = "caloriecounter";
    static final String TABLE_NAME = "entries";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_TABLE =
            " CREATE TABLE " + TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " label VARCHAR(255) NOT NULL, " +
                    " value REAL NOT NULL, " +
                    " timestamp INTEGER NOT NULL," +
                    " added INTEGER NOT NULL);" +
                    " create index label on " + TABLE_NAME + " (label);" +
                    " create index timestamp on " + TABLE_NAME + " (timestamp);" +
                    " create index added on " + TABLE_NAME + " (added);";


    // class that creates and manages the provider's database
    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            Log.w(DBHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ". Old data will be destroyed");
            db.execSQL("DROP TABLE IF EXISTS " +  TABLE_NAME);
            onCreate(db);
        }

    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Context context = getContext();
        dbHelper = new DBHelper(context);
        // permissions to be writable
        database = dbHelper.getWritableDatabase();

        if(database == null)
            return false;
        else
            return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // the TABLE_NAME to query on
        queryBuilder.setTables(TABLE_NAME);
        List<String> segments;
        Calendar cal;
        long dateFrom;
        long dateTo;
        Cursor cursor;
        String groupBy = null;

        switch (uriMatcher.match(uri)) {
            // maps all database column names
            case VALUES:
                queryBuilder.setProjectionMap(ValueMap);
                break;
            case VALUE_ID:
                queryBuilder.appendWhere( ID + "=" + uri.getLastPathSegment());
                break;
            case DAY_ID:
                segments = uri.getPathSegments();

                cal = Calendar.getInstance();
                cal.set(
                        Integer.parseInt(segments.get(segments.size()-3)),
                        Integer.parseInt(segments.get(segments.size()-2))-1,
                        Integer.parseInt(segments.get(segments.size()-1)),
                        0,
                        0,
                        0);

                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                dateFrom = cal.getTimeInMillis()/1000;

                //Log.d("query date from", uri.toString() + " " + new SimpleDateFormat("yyyy-MM-dd h:m:s a").format(cal.getTime()) + dateFrom);
                cal.add(Calendar.DATE, 1);
                dateTo = cal.getTimeInMillis()/1000;
                //Log.d("query date to", new SimpleDateFormat("yyyy-MM-dd h:m:s a").format(cal.getTime()) + dateTo);
                queryBuilder.appendWhere( TIMESTAMP + " >= " + dateFrom + " and " + TIMESTAMP + " < " + dateTo + "");
                //Log.d("query: ", queryBuilder.buildQuery(projection, selection, selectionArgs,
                //        null, null, null, sortOrder));
                break;
            case SUMMARY_BY_DAY:
                segments = uri.getPathSegments();

                cal = Calendar.getInstance();
                cal.set(
                        Integer.parseInt(segments.get(segments.size()-3)),
                        Integer.parseInt(segments.get(segments.size()-2))-1,
                        Integer.parseInt(segments.get(segments.size()-1)),
                        0,
                        0,
                        0);

                dateFrom = cal.getTimeInMillis()/1000;
                cal.add(Calendar.DATE, 1);
                dateTo = cal.getTimeInMillis()/1000;

                cursor = database.rawQuery("select sum(" + VALUE + ") as total from " + TABLE_NAME + " where " + TIMESTAMP + " >= ? and " + TIMESTAMP + " < ?", new String[]{ dateFrom+"", dateTo+""});
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
                //break;
            case LAST_30_DAYS:
                segments = uri.getPathSegments();

                cal = Calendar.getInstance();
                dateTo = cal.getTimeInMillis()/1000;
                cal.add(Calendar.DATE, -30);
                dateFrom = cal.getTimeInMillis()/1000;

                cursor = database.rawQuery("select sum(" + VALUE + ") as total, strftime('%Y-%m-%d', " + TIMESTAMP + ", 'unixepoch', 'localtime') as timestamp from " + TABLE_NAME + " where " + TIMESTAMP + " >= ? and " + TIMESTAMP + " < ? group by strftime('%Y-%m-%d', " + TIMESTAMP + ", 'unixepoch', 'localtime')", new String[]{ dateFrom+"", dateTo+""});
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
            case UNIQUE:
                queryBuilder.setProjectionMap(ValueMap);
                queryBuilder.setDistinct(true);
                groupBy = "label, value";
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder == ""){
            // No sorting-> sort on names by default
            sortOrder = TIMESTAMP;
        }

        cursor = queryBuilder.query(database, projection, selection,
                selectionArgs, groupBy, null, sortOrder);
        /**
         * register to watch a content URI for changes
         */
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        long row = database.insert(TABLE_NAME, "", values);

        // If record is added successfully
        if(row > 0) {
            //Uri newUri = ContentUris.withAppendedId(CONTENT_URI, row);
            // this is kind of bad, because it notifies ALL observers that the data has changed.
            // it should instead only notify about the record it inserted, and maybe the summaries.
            Uri newUri = CONTENT_URI;
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Fail to add a new record into " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO Auto-generated method stub
        int count = 0;

        switch (uriMatcher.match(uri)){
            case VALUES:
                count = database.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case VALUE_ID:
                count = database.update(TABLE_NAME, values, ID +
                        " = " + uri.getLastPathSegment() +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri );
        }
        // this is kind of bad, because it notifies ALL observers that the data has changed.
        // it should instead only notify about the record it inserted, and maybe the summaries.
        //getContext().getContentResolver().notifyChange(uri, null);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        int count = 0;

        switch (uriMatcher.match(uri)){
            case VALUES:
                // delete all the records of the table
                count = database.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case VALUE_ID:
                String id = uri.getLastPathSegment();	//gets the id
                count = database.delete(TABLE_NAME, ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;


    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        switch (uriMatcher.match(uri)){
            // Get all friend-birthday records
            case VALUES:
                return "vnd.android.cursor.dir/vnd.laneviss.caloriecounter";
            // Get a particular friend
            case VALUE_ID:
                return "vnd.android.cursor.item/vnd.laneviss.caloriecounter";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}

package com.drync.android;

import com.drync.android.objects.Cork;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DryncDbAdapter 
{
    public static final String KEY_ROWID = "_id";
    public static final String KEY_CORK_ID = "cork_id";
    public static final String KEY_CORK_UUID = "cork_uuid";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_CORK_RATING = "cork_rating";
    public static final String KEY_CORK_WANT = "cork_want";
    public static final String KEY_CORK_OWN ="cork_own";
    public static final String KEY_CORK_DRANK = "cork_drank";
    public static final String KEY_CORK_ORDERED = "cork_ordered";
    public static final String KEY_CORK_PRICE = "cork_price";
    public static final String KEY_CORK_YEAR = "cork_year";
    public static final String KEY_CORK_POI = "cork_poi";
    public static final String KEY_CORK_BOTTLE_COUNT = "cork_bottle_count";
    public static final String KEY_CORK_CREATED_AT = "cork_created_at";
    public static final String KEY_CORK_LABEL = "cork_label";
    
    public static final String KEY_BOTTLE_ID = "bottle_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_YEAR = "year";
    public static final String KEY_REGION_PATH = "region_path";
    public static final String KEY_REGION = "region";
    public static final String KEY_GRAPE = "grape";
    public static final String KEY_LABEL = "label";
    public static final String KEY_LABEL_THUMB = "label_thumb";
    public static final String KEY_PRICE = "price";
    public static final String KEY_RATING = "rating";
    public static final String KEY_REVIEWCOUNT = "reviewCount";
    
    public static final String KEY_COUNT = "count";
    private static final String TAG = "DBAdapter";
    
    private static final String DATABASE_NAME = "drync";
    private static final String DATABASE_TABLE = "corks";
    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_CREATE =
        "create table corks ("
    	+ "_id integer primary key autoincrement, "
        + "cork_id integer, "
        + "cork_uuid text, "
        + "description text, "
        + "location text, "
        + "cork_rating integer, "
        + "cork_want boolean, "
        + "cork_own boolean, "
        + "cork_drank boolean, "
        + "cork_ordered boolean, "
        + "cork_price text, "
        + "cork_year integer, "
        + "cork_poi text, "
        + "cork_bottle_count integer, "
        + "cork_created_at integer, "
        + "cork_label text, "
        
        + "bottle_id integer, "
        + "name text, "
        + "year integer, "
        + "region_path text, "
        + "region text, "
        + "grape text, "
        + "label text, "
        + "label_thumb text, "
        + "price text, "
        + "rating integer, "
        + "reviewCount integer" 
        + ");";
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DryncDbAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS titles");
            onCreate(db);
        }
    }    
    
    //---opens the database---
    public DryncDbAdapter open() throws SQLException 
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }
    
    //---insert a title into the database---
    public long insertCork(Cork cork) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ROWID, cork.get_id());
        initialValues.put(KEY_CORK_ID, cork.getCork_id());
        initialValues.put(KEY_CORK_UUID, cork.getCork_uuid());
        initialValues.put(KEY_DESCRIPTION, cork.getDescription());
        initialValues.put(KEY_LOCATION, cork.getLocation());
        initialValues.put(KEY_CORK_RATING, cork.getCork_rating());
        initialValues.put(KEY_CORK_WANT, cork.isCork_want());
        initialValues.put(KEY_CORK_OWN, cork.isCork_own());
        initialValues.put(KEY_CORK_DRANK, cork.isCork_drank());
        initialValues.put(KEY_CORK_ORDERED, cork.isCork_ordered());
        initialValues.put(KEY_CORK_PRICE, cork.getCork_price());
        initialValues.put(KEY_CORK_YEAR, cork.getCork_year());
        initialValues.put(KEY_CORK_POI, cork.getCork_poi());
        initialValues.put(KEY_CORK_BOTTLE_COUNT, cork.getCork_bottle_count());
        initialValues.put(KEY_CORK_CREATED_AT, cork.getCork_created_at());
        initialValues.put(KEY_CORK_LABEL, cork.getCork_label());

        initialValues.put(KEY_BOTTLE_ID, cork.getBottle_Id());
        initialValues.put(KEY_NAME, cork.getName());
        initialValues.put(KEY_YEAR, cork.getYear());
        initialValues.put(KEY_REGION_PATH, cork.getRegion_path());
        initialValues.put(KEY_REGION, cork.getRegion());
        initialValues.put(KEY_GRAPE, cork.getGrape());
        initialValues.put(KEY_LABEL, cork.getLabel());
        initialValues.put(KEY_LABEL_THUMB, cork.getLabel_thumb());
        initialValues.put(KEY_PRICE, cork.getPrice());
        initialValues.put(KEY_RATING, cork.getRating());
        initialValues.put(KEY_REVIEWCOUNT, cork.getReviewCount());
        return db.insert(DATABASE_TABLE, "", initialValues);
    }

    //---deletes a particular title---
    public boolean deleteCork(long rowId) 
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }

    //---retrieves all the titles---
    public Cursor getAllCorks() 
    {
        return db.query(DATABASE_TABLE, new String[] {
        		KEY_ROWID,
        	    KEY_CORK_ID,
        	    KEY_CORK_UUID,
        	    KEY_DESCRIPTION,
        	    KEY_LOCATION,
        	    KEY_CORK_RATING,
        	    KEY_CORK_WANT,
        	    KEY_CORK_OWN,
        	    KEY_CORK_DRANK,
        	    KEY_CORK_ORDERED,
        	    KEY_CORK_PRICE,
        	    KEY_CORK_YEAR,
        	    KEY_CORK_POI,
        	    KEY_CORK_BOTTLE_COUNT,
        	    KEY_CORK_CREATED_AT,
        	    KEY_CORK_LABEL,
        	    
        	    KEY_BOTTLE_ID,
        	    KEY_NAME,
        	    KEY_YEAR,
        	    KEY_REGION_PATH,
        	    KEY_REGION,
        	    KEY_GRAPE,
        	    KEY_LABEL,
        	    KEY_LABEL_THUMB,
        	    KEY_PRICE,
        	    KEY_RATING,
        	    KEY_REVIEWCOUNT}, 
                null, 
                null, 
                null, 
                null, 
                null);
    }

    //---retrieves a particular title---
    public Cursor getCork(long rowId) throws SQLException 
    {
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {
                   		KEY_ROWID,
                	    KEY_CORK_ID,
                	    KEY_CORK_UUID,
                	    KEY_DESCRIPTION,
                	    KEY_LOCATION,
                	    KEY_CORK_RATING,
                	    KEY_CORK_WANT,
                	    KEY_CORK_OWN,
                	    KEY_CORK_DRANK,
                	    KEY_CORK_ORDERED,
                	    KEY_CORK_PRICE,
                	    KEY_CORK_YEAR,
                	    KEY_CORK_POI,
                	    KEY_CORK_BOTTLE_COUNT,
                	    KEY_CORK_CREATED_AT,
                	    KEY_CORK_LABEL,
                	    
                	    KEY_BOTTLE_ID,
                	    KEY_NAME,
                	    KEY_YEAR,
                	    KEY_REGION_PATH,
                	    KEY_REGION,
                	    KEY_GRAPE,
                	    KEY_LABEL,
                	    KEY_LABEL_THUMB,
                	    KEY_PRICE,
                	    KEY_RATING,
                	    KEY_REVIEWCOUNT
                		}, 
                		KEY_ROWID + "=" + rowId, 
                		null,
                		null, 
                		null, 
                		null, 
                		null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //---updates a cork---
    public boolean updateCork(Cork cork) 
    {
        ContentValues args = new ContentValues();
        args.put(KEY_ROWID, cork.get_id());
        args.put(KEY_CORK_ID, cork.getCork_id());
        args.put(KEY_CORK_UUID, cork.getCork_uuid());
        args.put(KEY_DESCRIPTION, cork.getDescription());
        args.put(KEY_LOCATION, cork.getLocation());
        args.put(KEY_CORK_RATING, cork.getCork_rating());
        args.put(KEY_CORK_WANT, cork.isCork_want());
        args.put(KEY_CORK_OWN, cork.isCork_own());
        args.put(KEY_CORK_DRANK, cork.isCork_drank());
        args.put(KEY_CORK_ORDERED, cork.isCork_ordered());
        args.put(KEY_CORK_PRICE, cork.getCork_price());
        args.put(KEY_CORK_YEAR, cork.getCork_year());
        args.put(KEY_CORK_POI, cork.getCork_poi());
        args.put(KEY_CORK_BOTTLE_COUNT, cork.getCork_bottle_count());
        args.put(KEY_CORK_CREATED_AT, cork.getCork_created_at());
        args.put(KEY_CORK_LABEL, cork.getCork_label());

        args.put(KEY_BOTTLE_ID, cork.getBottle_Id());
        args.put(KEY_NAME, cork.getName());
        args.put(KEY_YEAR, cork.getYear());
        args.put(KEY_REGION_PATH, cork.getRegion_path());
        args.put(KEY_REGION, cork.getRegion());
        args.put(KEY_GRAPE, cork.getGrape());
        args.put(KEY_LABEL, cork.getLabel());
        args.put(KEY_LABEL_THUMB, cork.getLabel_thumb());
        args.put(KEY_PRICE, cork.getPrice());
        args.put(KEY_RATING, cork.getRating());
        args.put(KEY_REVIEWCOUNT, cork.getReviewCount());
        return db.update(DATABASE_TABLE, args, 
                         KEY_ROWID + "=" + cork.get_id(), null) > 0;
    }
}


package com.drync.android;

import java.util.ArrayList;
import java.util.List;

import com.drync.android.objects.Cork;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
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
    public static final String KEY_PUBLIC_NOTE = "public_note";
    
    public static final String KEY_BOTTLE_ID = "bottle_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_YEAR = "year";
    public static final String KEY_REGION_PATH = "region_path";
    public static final String KEY_REGION = "region";
    public static final String KEY_GRAPE = "grape";
    public static final String KEY_STYLE = "style";
    public static final String KEY_LABEL = "label";
    public static final String KEY_LABEL_THUMB = "label_thumb";
    public static final String KEY_PRICE = "price";
    public static final String KEY_RATING = "rating";
    public static final String KEY_REVIEWCOUNT = "reviewCount";
    
    public static final String KEY_NEEDSSERVERUPDATE = "needsServerUpdate";
    public static final String KEY_UPDATETYPE = "updateType";
    
    public static final String KEY_COUNT = "count";
    private static final String TAG = "DBAdapter";
    
    private static final String DATABASE_PRO_NAME = "drync";
    private static final String DATABASE_FREE_NAME = "dryncfree";
    private static String DATABASE_NAME = DATABASE_PRO_NAME;
    private static final String DATABASE_TABLE = "corks";
    private static final int DATABASE_VERSION = 7;

    private static final String DATABASE_CREATE =
        "create table corks ("
    	+ "_id integer primary key autoincrement, "
        + "cork_id integer, "
        + "cork_uuid text, "
        + "description text, "
        + "location text, "
        + "cork_rating real, "
        + "cork_want boolean, "
        + "cork_own boolean, "
        + "cork_drank boolean, "
        + "cork_ordered boolean, "
        + "cork_price text, "
        + "cork_year integer, "
        + "cork_poi text, "
        + "cork_bottle_count integer, "
        + "cork_created_at text, "
        + "cork_label text, "
        + "public_note text, "
        + "bottle_id integer, "
        + "name text, "
        + "year integer, "
        + "region_path text, "
        + "region text, "
        + "grape text, "
        + "style text, "
        + "label text, "
        + "label_thumb text, "
        + "price text, "
        + "rating real, "
        + "reviewCount integer, "
        + "needsServerUpdate integer, "
        + "updateType integer "
        + ");";
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DryncDbAdapter(Context ctx) 
    {
        this.context = ctx;

    	if (DryncUtils.isFreeMode())
    		DATABASE_NAME = DATABASE_FREE_NAME;
    	else
    		DATABASE_NAME = DATABASE_PRO_NAME;
    	
        DBHelper = new DatabaseHelper(context);
    }
    
    public List<Cork> search(String query) {
    	return search(query, false);
    }
        
    public List<Cork> search(String query, boolean includePendingDeletes) {
    	
    	List<Cork> corks = new ArrayList<Cork>();
    	
    	String whereclause = 
			"description LIKE '%" + query + "%' OR " +
			"location LIKE '%" + query + "%' OR " + 
			"cork_label LIKE '%" + query + "%' OR " +
			"public_note LIKE '%" + query + "%' OR " +
			"name LIKE '%" + query + "%' OR " +
			"year LIKE '%" + query + "%' OR " + 
			"region LIKE '%" + query + "%' OR " +
			"grape LIKE '%" + query + "%' OR " + 
			"style LIKE '%" + query + "%'";
    
    	Cursor cur =  db.query(DATABASE_TABLE, new String[] {
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
        	    KEY_PUBLIC_NOTE,
        	    
        	    KEY_BOTTLE_ID,
        	    KEY_NAME,
        	    KEY_YEAR,
        	    KEY_REGION_PATH,
        	    KEY_REGION,
        	    KEY_GRAPE,
        	    KEY_STYLE,
        	    KEY_LABEL,
        	    KEY_LABEL_THUMB,
        	    KEY_PRICE,
        	    KEY_RATING,
        	    KEY_REVIEWCOUNT,
        	    KEY_NEEDSSERVERUPDATE,
        	    KEY_UPDATETYPE}, 
        	    whereclause, 
        	    null, 
                null, 
                null, 
                null);
        
        cur.moveToFirst();
        while (cur.isAfterLast() == false) {
        	if (! includePendingDeletes)
        	{
        		// using where wasn't working properly, so we're doing this in code for now.
        		if ((cur.getInt(cur.getColumnIndex(KEY_NEEDSSERVERUPDATE)) == 1) && 
        			(cur.getInt(cur.getColumnIndex(KEY_UPDATETYPE)) == Cork.UPDATE_TYPE_DELETE))
        		{
        			continue;
        		}
        	}
            Cork cork = buildCork(cur);
            corks.add(cork);
       	    cur.moveToNext();
        }
        
        cur.close();

        return corks;
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
                    + newVersion);
            
            
            if (oldVersion <= 6)
            {
            	try
            	{
            		db.execSQL("ALTER TABLE corks ADD COLUMN style text;");
            	}
            	catch (SQLiteException e)
            	{
            		//ignore
            	}
            }
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
    
    public long insertOrUpdateCork(Cork cork) throws DryncFreeCellarExceededException
    {
    	Cork localCopy = this.getCorkByUUID(cork.getCork_uuid());
    	if (localCopy == null) // then it's easy:
    	{
    		// if cork is not present, insert.
    		return this.insertCork(cork);
    	}
    	else
    	{
    		// if cork is present and marked for needsServerUpdate, keep local.
    		// however, if updateType is UPDATE_TYPE_NONE, take server version.
    		if (localCopy.isNeedsServerUpdate() && (localCopy.getUpdateType() != Cork.UPDATE_TYPE_NONE))
    		{
    			return localCopy.get_id();
    		}
    		else  // if cork is present and NOT marked for needsServerUpdate, mod for server version.
    		{
    			cork.set_id(localCopy.get_id());
    			this.updateCork(cork);
    			
    			return cork.get_id();
    		}
    	}
    }
    
    //---insert a title into the database---
    public long insertCork(Cork cork) throws DryncFreeCellarExceededException
    {
    	if (DryncUtils.isFreeMode() && (getCorkCount() >= DryncUtils.FREE_CELLAR_MAX_CORKS))
    		throw new DryncFreeCellarExceededException();
    	else
    		return insertCork(cork, false, 0);
    }
    
    public long insertCork(Cork cork, boolean needsUpdate, int updateType)
    {
        ContentValues initialValues = new ContentValues();
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
        initialValues.put(KEY_PUBLIC_NOTE, cork.getPublic_note());

        initialValues.put(KEY_BOTTLE_ID, cork.getBottle_Id());
        initialValues.put(KEY_NAME, cork.getName());
        initialValues.put(KEY_YEAR, cork.getYear());
        initialValues.put(KEY_REGION_PATH, cork.getRegion_path());
        initialValues.put(KEY_REGION, cork.getRegion());
        initialValues.put(KEY_GRAPE, cork.getGrape());
        initialValues.put(KEY_STYLE, cork.getStyle());
        initialValues.put(KEY_LABEL, cork.getLabel());
        initialValues.put(KEY_LABEL_THUMB, cork.getLabel_thumb());
        initialValues.put(KEY_PRICE, cork.getPrice());
        initialValues.put(KEY_RATING, cork.getRating());
        initialValues.put(KEY_REVIEWCOUNT, cork.getReviewCount());
        initialValues.put(KEY_NEEDSSERVERUPDATE, needsUpdate ? 1 : 0);
        initialValues.put(KEY_UPDATETYPE, updateType);
        return db.insert(DATABASE_TABLE, "", initialValues);
    }

    //---deletes a particular title---
    public boolean deleteCork(long rowId) 
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }
    
    public int clearCorks()
    {
    	return clearCorks(false);
    }
    
    public int clearCorks(boolean excludeThoseNeedingUpdate)
    {
    	String whereClause = null;
    	if (excludeThoseNeedingUpdate)
    	{
    		whereClause = KEY_NEEDSSERVERUPDATE + "<>1";
    	}
    	
    	return db.delete(DATABASE_TABLE, whereClause, null);
    }

    //---retrieves all the titles---
    public List<Cork> getAllCorks() 
    {
    	return getAllCorks(false);
    }
    
    public static final int FILTER_TYPE_NONE = 0;
    public static final int FILTER_TYPE_OWN = 1;
    public static final int FILTER_TYPE_DRANK = 2;
    public static final int FILTER_TYPE_WANT = 3;
    
    public List<Cork> getFilteredCorks(int filterType, String strFilter)
    {
    	ArrayList<Cork> filteredCorks = new ArrayList<Cork>();
    	
    	List<Cork> corks = null;
    	
    	if ((strFilter != null) && (!strFilter.equals("")))
    		corks = search(strFilter);
    	else
    		corks = getAllCorks();
    	
    	if (filterType == FILTER_TYPE_NONE)
    		return corks;
    	else
    	{
    		for (Cork cork : corks)
    		{
    			if ((filterType == FILTER_TYPE_OWN) && (cork.isCork_own()))
    			{
    				filteredCorks.add(cork);
    			}
    			else if ((filterType == FILTER_TYPE_WANT) && (cork.isCork_want()))
    			{
    				filteredCorks.add(cork);
    			}
    			else if ((filterType == FILTER_TYPE_DRANK) && (cork.isCork_drank()))
    			{
    				filteredCorks.add(cork);
    			}
    			
    		}
    	}
    	
    	return filteredCorks;
    }
    
    private void createFTS3Table()
    {
    	String createStr = "CREATE  VIRTUAL TABLE TableName USING " +
    			"FTS3(description TEXT, location text, cork_label text, public_note text, name text, year integer, region text, grape text, style text)";
    }
    
    public int getCorkCount()
    {
    	return getCorkCount(false);
    }
    
    public int getCorkCount(boolean includePendingDeletes)
    {
    	String pendingWhere = null; //KEY_NEEDSSERVERUPDATE + " != 1 && " + KEY_UPDATETYPE + " != " + Cork.UPDATE_TYPE_DELETE;
    	
    	Cursor cur =  db.query(DATABASE_TABLE, new String[] {
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
        	    KEY_PUBLIC_NOTE,
        	    
        	    KEY_BOTTLE_ID,
        	    KEY_NAME,
        	    KEY_YEAR,
        	    KEY_REGION_PATH,
        	    KEY_REGION,
        	    KEY_GRAPE,
        	    KEY_STYLE,
        	    KEY_LABEL,
        	    KEY_LABEL_THUMB,
        	    KEY_PRICE,
        	    KEY_RATING,
        	    KEY_REVIEWCOUNT,
        	    KEY_NEEDSSERVERUPDATE,
        	    KEY_UPDATETYPE}, 
        	    includePendingDeletes ? null : pendingWhere, 
                null, 
                null, 
                null, 
                null);
    	
    	return cur.getCount();
    }
    
    public List<Cork> getAllCorks(boolean includePendingDeletes) 
    {
    	List<Cork> corks = new ArrayList<Cork>();
    	/*String[] querymod = {KEY_NEEDSSERVERUPDATE + "!=" + 1, KEY_UPDATETYPE + "!=" + Cork.UPDATE_TYPE_DELETE};
    	
    	if (includePendingDeletes)
    	{
    		querymod = null;
    	}*/
    	
        Cursor cur =  db.query(DATABASE_TABLE, new String[] {
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
        	    KEY_PUBLIC_NOTE,
        	    
        	    KEY_BOTTLE_ID,
        	    KEY_NAME,
        	    KEY_YEAR,
        	    KEY_REGION_PATH,
        	    KEY_REGION,
        	    KEY_GRAPE,
        	    KEY_STYLE,
        	    KEY_LABEL,
        	    KEY_LABEL_THUMB,
        	    KEY_PRICE,
        	    KEY_RATING,
        	    KEY_REVIEWCOUNT,
        	    KEY_NEEDSSERVERUPDATE,
        	    KEY_UPDATETYPE}, 
        	    null, 
                null, 
                null, 
                null, 
                null);
        
        cur.moveToFirst();
        while (cur.isAfterLast() == false) {
        	if (! includePendingDeletes)
        	{
        		// using where wasn't working properly, so we're doing this in code for now.
        		if ((cur.getInt(cur.getColumnIndex(KEY_NEEDSSERVERUPDATE)) == 1) && 
        			(cur.getInt(cur.getColumnIndex(KEY_UPDATETYPE)) == Cork.UPDATE_TYPE_DELETE))
        		{
        			continue;
        		}
        	}
            Cork cork = buildCork(cur);
            corks.add(cork);
       	    cur.moveToNext();
        }
        
        cur.close();

        return corks;
    }
    
  //---retrieves all the titles---
    public List<Cork> getAllCorksNeedingUpdates() 
    {
    	List<Cork> corks = new ArrayList<Cork>();
    	
        Cursor cur =  db.query(DATABASE_TABLE, new String[] {
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
        	    KEY_PUBLIC_NOTE,
        	    
        	    KEY_BOTTLE_ID,
        	    KEY_NAME,
        	    KEY_YEAR,
        	    KEY_REGION_PATH,
        	    KEY_REGION,
        	    KEY_GRAPE,
        	    KEY_STYLE,
        	    KEY_LABEL,
        	    KEY_LABEL_THUMB,
        	    KEY_PRICE,
        	    KEY_RATING,
        	    KEY_REVIEWCOUNT, 
        	    KEY_NEEDSSERVERUPDATE,
        	    KEY_UPDATETYPE}, 
        	    KEY_NEEDSSERVERUPDATE + "=" + 1,  
                null, 
                null, 
                null, 
                null);
        
        cur.moveToFirst();
        while (cur.isAfterLast() == false) {
            Cork cork = buildCork(cur);
            corks.add(cork);
       	    cur.moveToNext();
        }

        cur.close();
        
        return corks;
    }
    
    private Cork buildCork(Cursor cur)
    {
    	Cork cork = new Cork();
    	cork.set_id(cur.getLong(cur.getColumnIndex(KEY_ROWID)));
    	cork.setCork_id(cur.getLong(cur.getColumnIndex(KEY_CORK_ID)));
    	cork.setCork_uuid(cur.getString(cur.getColumnIndex(KEY_CORK_UUID)));
    	cork.setDescription(cur.getString(cur.getColumnIndex(KEY_DESCRIPTION)));
    	cork.setLocation(cur.getString(cur.getColumnIndex(KEY_LOCATION)));
    	cork.setCork_rating(cur.getFloat(cur.getColumnIndex(KEY_CORK_RATING)));
    	cork.setCork_want(cur.getInt(cur.getColumnIndex(KEY_CORK_WANT)) == 0 ? false : true);
    	cork.setCork_own(cur.getInt(cur.getColumnIndex(KEY_CORK_OWN)) == 0 ? false : true);
    	cork.setCork_bottle_count(cur.getInt(cur.getColumnIndex(KEY_CORK_BOTTLE_COUNT)));
    	cork.setCork_drank(cur.getInt(cur.getColumnIndex(KEY_CORK_DRANK)) == 0 ? false : true);
    	cork.setCork_ordered(cur.getInt(cur.getColumnIndex(KEY_CORK_ORDERED)) == 0 ? false : true);
    	cork.setCork_price(cur.getString(cur.getColumnIndex(KEY_CORK_PRICE)));
    	cork.setCork_year(cur.getInt(cur.getColumnIndex(KEY_CORK_YEAR)));
    	cork.setCork_poi(cur.getString(cur.getColumnIndex(KEY_CORK_POI)));
    	cork.setCork_created_at(cur.getString(cur.getColumnIndex(KEY_CORK_CREATED_AT)));
    	
    	cork.setBottle_Id(cur.getLong(cur.getColumnIndex(KEY_BOTTLE_ID)));
    	cork.setCork_label(cur.getString(cur.getColumnIndex(KEY_CORK_LABEL)));
    	cork.setPublic_note(cur.getString(cur.getColumnIndex(KEY_PUBLIC_NOTE)));
    	cork.setName(cur.getString(cur.getColumnIndex(KEY_NAME)));
    	cork.setYear(cur.getInt(cur.getColumnIndex(KEY_YEAR)));
    	cork.setRegion_path(cur.getString(cur.getColumnIndex(KEY_REGION_PATH)));
    	cork.setRegion(cur.getString(cur.getColumnIndex(KEY_REGION)));
    	cork.setGrape(cur.getString(cur.getColumnIndex(KEY_GRAPE)));
    	cork.setStyle(cur.getString(cur.getColumnIndex(KEY_STYLE)));
    	cork.setLabel(cur.getString(cur.getColumnIndex(KEY_LABEL)));
    	cork.setLabel_thumb(cur.getString(cur.getColumnIndex(KEY_LABEL_THUMB)));
    	cork.setPrice(cur.getString(cur.getColumnIndex(KEY_PRICE)));
    	cork.setRating(cur.getString(cur.getColumnIndex(KEY_RATING)));
    	cork.setReviewCount(cur.getInt(cur.getColumnIndex(KEY_REVIEWCOUNT))); 
    	cork.setNeedsServerUpdate(
    			cur.getInt(cur.getColumnIndex(KEY_NEEDSSERVERUPDATE)) == 0 ? false : true);
    	cork.setUpdateType(cur.getInt(cur.getColumnIndex(KEY_UPDATETYPE)));
    	
    	return cork;
    }

    public Cork getCorkByUUID(String uuId) throws SQLException 
    {
    	Cursor mCursor = null;
    	try
    	{
    		mCursor =
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
    					KEY_PUBLIC_NOTE,

    					KEY_BOTTLE_ID,
    					KEY_NAME,
    					KEY_YEAR,
    					KEY_REGION_PATH,
    					KEY_REGION,
    					KEY_GRAPE,
    					KEY_STYLE,
    					KEY_LABEL,
    					KEY_LABEL_THUMB,
    					KEY_PRICE,
    					KEY_RATING,
    					KEY_REVIEWCOUNT,
    					KEY_NEEDSSERVERUPDATE,
    					KEY_UPDATETYPE
    			}, 
    			KEY_CORK_UUID + "='" + uuId + "'", 
    			null,
    			null, 
    			null, 
    			null, 
    			null);
    		if (mCursor != null && mCursor.getCount() > 0) {

    			mCursor.moveToFirst();
    			Cork returnCork = null;
    			if (mCursor.getColumnCount() > 0)
    			{
    				returnCork = buildCork(mCursor);	
    			}
    			return returnCork; 
    		}
    		return null;
    	}
    	finally
    	{
    		if (mCursor != null)
    			mCursor.close();
    	}
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
                	    KEY_PUBLIC_NOTE,
                	    
                	    KEY_BOTTLE_ID,
                	    KEY_NAME,
                	    KEY_YEAR,
                	    KEY_REGION_PATH,
                	    KEY_REGION,
                	    KEY_GRAPE,
                	    KEY_STYLE,
                	    KEY_LABEL,
                	    KEY_LABEL_THUMB,
                	    KEY_PRICE,
                	    KEY_RATING,
                	    KEY_REVIEWCOUNT,
                	    KEY_NEEDSSERVERUPDATE,
                	    KEY_UPDATETYPE
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
        mCursor.close();
        return mCursor;
    }

    //---updates a cork---
    public boolean updateCork(Cork cork) 
    {
    	return updateCork(cork, cork.isNeedsServerUpdate(), cork.getUpdateType());
    }
    
    public boolean updateCork(Cork cork, boolean needsUpdate, int updateType) 
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
        args.put(KEY_PUBLIC_NOTE, cork.getPublic_note());

        args.put(KEY_BOTTLE_ID, cork.getBottle_Id());
        args.put(KEY_NAME, cork.getName());
        args.put(KEY_YEAR, cork.getYear());
        args.put(KEY_REGION_PATH, cork.getRegion_path());
        args.put(KEY_REGION, cork.getRegion());
        args.put(KEY_GRAPE, cork.getGrape());
        args.put(KEY_STYLE, cork.getStyle());
        args.put(KEY_LABEL, cork.getLabel());
        args.put(KEY_LABEL_THUMB, cork.getLabel_thumb());
        args.put(KEY_PRICE, cork.getPrice());
        args.put(KEY_RATING, cork.getRating());
        args.put(KEY_REVIEWCOUNT, cork.getReviewCount());
        args.put(KEY_NEEDSSERVERUPDATE, needsUpdate ? 1 : 0);
        args.put(KEY_UPDATETYPE, updateType);
        
        int rowsModified = db.update(DATABASE_TABLE, args, 
        		KEY_ROWID + "=" + cork.get_id(), null);
        
        return rowsModified > 0;
    }   
}


package com.drync.android;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SearchTermDbAdapter 
{
    public static final String KEY_ROWID = "_id";
    public static final String KEY_WORD = "word";
    public static final String KEY_COUNT = "occ_count";
    
    private static final String TAG = "DBAdapter";
    
  //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/com.drync.android/databases/";

    
    private static String DATABASE_NAME = "dryncwords";
    private static final String DATABASE_TABLE = "searchterms";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
        "create table searchterms ("
    	+ "_id integer primary key autoincrement, "
        + "word text, "
        + "occ_count integer"
        + ");";
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public SearchTermDbAdapter(Context ctx) 
    {
        this.context = ctx;
    	
        DBHelper = new DatabaseHelper(context);
        
        try {
			DBHelper.createDataBase();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			 this.open();
	 		//DBHelper.openDataBase();
	 
	 	}catch(SQLException sqle){
	 
	 		throw sqle;
	 
	 	}

        
        
    }
    
   /* public List<Cork> search(String query) {
    	return search(query, false);
    }*/
        
    public List<String> search(String query) {

    	List<String> results = new ArrayList<String>();


    	String whereclause = null;
    	if ((query != null) && (!query.equals("")))
    	{
    		whereclause = "word LIKE '" + query + "%'";
    	}

    	String orderBy = "occ_count desc";
    	Cursor cur = null;
    	try
    	{
    		if (db == null)
        		db = DBHelper.getReadableDatabase();
    		
    		cur =  db.query(false, DATABASE_TABLE, new String[] {
    				KEY_WORD
    		}, 
    		whereclause, 
    		null, 
    		null, 
    		null, orderBy,  
    		"10");

    		cur.moveToFirst();
    		int i=0;
    		while (cur.isAfterLast() == false) {

    			// don't add any more than 5.
    			if (i >= 5) break;
    			
    			String result = cur.getString(cur.getColumnIndex(KEY_WORD));
    			// keep 'em unique
    			if (!results.contains(result))
    			{
    				results.add(result);
    				i++;
    			}
    			cur.moveToNext();
    		}
    	}
    	catch (SQLiteException e)
    	{

    	}
    	finally
    	{

    		if (cur != null)
    			cur.close();
    	}
        return results;
	}
    
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
    	private SQLiteDatabase myDataBase; 
    	 
        private final Context myContext;

        
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
			myContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
           // db.execSQL(DATABASE_CREATE);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion);            
        }
        
        /**
         * Check if the database already exist to avoid re-copying the file each time you open the application.
         * @return true if it exists, false if it doesn't
         */
        private boolean checkDataBase(){
     
        	SQLiteDatabase checkDB = null;
     
        	try{
        		String myPath = DB_PATH + DATABASE_NAME;
        		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
     
        	}catch(SQLiteException e){
     
        		//database does't exist yet.
     
        	}
     
        	if(checkDB != null){
     
        		checkDB.close();
     
        	}
     
        	return checkDB != null ? true : false;
        }

        /**
         * Creates a empty database on the system and rewrites it with your own database.
         * */
        public void createDataBase() throws IOException{
     
        	boolean dbExist = checkDataBase();
     
        	if(dbExist){
        		//do nothing - database already exist
        	}else{
     
        		//By calling this method and empty database will be created into the default system path
                   //of your application so we are gonna be able to overwrite that database with our database.
            	this.getReadableDatabase();
     
            	try {
     
        			copyDataBase();
     
        		} catch (IOException e) {
     
            		throw new Error("Error copying database");
     
            	}
        	}
     
        }
        
        /**
         * Copies your database from your local assets-folder to the just created empty database in the
         * system folder, from where it can be accessed and handled.
         * This is done by transfering bytestream.
         * */
        private void copyDataBase() throws IOException{
     
        	//Open your local db as the input stream
        	InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
     
        	// Path to the just created empty db
        	String outFileName = DB_PATH + DATABASE_NAME;
     
        	//Open the empty db as the output stream
        	OutputStream myOutput = new FileOutputStream(outFileName);
     
        	//transfer bytes from the inputfile to the outputfile
        	byte[] buffer = new byte[1024];
        	int length;
        	while ((length = myInput.read(buffer))>0){
        		myOutput.write(buffer, 0, length);
        	}
     
        	//Close the streams
        	myOutput.flush();
        	myOutput.close();
        	myInput.close();
     
        }
     
        public void openDataBase() throws SQLException{
     
        	//Open the database
            String myPath = DB_PATH + DATABASE_NAME;
        	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
     
        }
        
        @Override
    	public synchronized void close() {
     
        	    if(myDataBase != null)
        		    myDataBase.close();
     
        	    super.close();
     
    	}


    }    
    
    //---opens the database---
    public void open() throws SQLException 
    {
    	if (db == null)
    		db = DBHelper.getReadableDatabase();
    }

    //---closes the database---    
    public void close() 
    {
    	if (db != null)
    	{
    		db.close();
    		db = null;
    	}
    }
    
    public long insertOrUpdateWord(String word, int count) 
    {
    	return this.insertWord(word, count);
    }
    
    //---insert a title into the database---
 
    
    public long insertWord(String word, int count ) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_WORD, word);
        initialValues.put(KEY_COUNT, count);
        return db.insert(DATABASE_TABLE, "", initialValues);
    }

    //---deletes a particular title---
    public boolean deleteWord(long rowId) 
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }
    
    public int clearWords()
    {
    	String whereClause = null;
    	
    	return db.delete(DATABASE_TABLE, whereClause, null);
    }

    //---retrieves all the titles---
   /* public List<String> getAllCorks() 
    {
    	return getAllWords(false);
    }*/
    
    
    private void createFTS3Table()
    {
    	String createStr = "CREATE  VIRTUAL TABLE TableName USING " +
    			"FTS3(word TEXT)";
    }
    
    /*public int getCorkCount()
    {
    	return getCorkCount(false);
    }
    */
    public boolean isPopulated()
    {    	
    	Cursor cur =  db.query( DATABASE_TABLE, new String[] {
        		KEY_ROWID,
        	    KEY_WORD,
        	    KEY_COUNT
        	    }, 
        	    null, 
                null, 
                null, 
                null, 
                KEY_COUNT, "5");
    	
    	int count = cur.getCount();
    	
    	cur.close();
    	if (count > 0)
    		return true;
    	else
    		return false;
    }
    
   /* public List<String> getAllWords() 
    {
    	List<String> corks = new ArrayList<String>();
    	
        Cursor cur =  db.query(DATABASE_TABLE, new String[] {
        		KEY_ROWID,
        	    KEY_WORD,
        	    KEY_COUNT}, 
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
    }*/
    
    
   /* public Cork getCorkByUUID(String uuId) throws SQLException 
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
    					KEY_UPDATETYPE,
    					KEY_LOCALIMGRESOURCE,
    					KEY_CORK_LABEL_INLINE,
    					KEY_LOCATIONLAT,
    					KEY_LOCATIONLONG
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
    }*/
    
    //---retrieves a particular title---
    /*public Cursor getCork(long rowId) throws SQLException 
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
                	    KEY_UPDATETYPE,
                	    KEY_LOCALIMGRESOURCE,
                	    KEY_CORK_LABEL_INLINE,
                	    KEY_LOCATIONLAT,
                	    KEY_LOCATIONLONG
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
*/
    //---updates a cork---
/*    public boolean updateCork(Cork cork) 
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
        args.put(KEY_LOCALIMGRESOURCE, cork.getLocalImageResourceOnly());
        args.put(KEY_CORK_LABEL_INLINE, cork.getCork_labelInline());
        args.put(KEY_LOCATIONLAT, cork.getLocationLat());
        args.put(KEY_LOCATIONLONG, cork.getLocationLong());
        
        
        int rowsModified = db.update(DATABASE_TABLE, args, 
        		KEY_ROWID + "=" + cork.get_id(), null);
        
        return rowsModified > 0;
    }*/   
    
    

}


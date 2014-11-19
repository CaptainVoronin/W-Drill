package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;

import java.sql.SQLDataException;
import java.util.ArrayList;

/**
 * Created by MaxSh on 13.11.2014.
 */
public class DefaultDictionary
{
    WDdb database;
    static int id;

    public final static String DICT_UUID = "A74DE520-6B3B-11E4-9803-0800200C9A66";
    public final static String DICT_NAME = "The default dict, don't touch it!";
    public final static String DICT_LANG = "ru";

    static DefaultDictionary instance;
    Dictionary dict;

    static {
        instance = null;
        id = -1;
    }

    protected DefaultDictionary( WDdb _database )
    {
        database = _database;
    }

    public static final DefaultDictionary getInstance( WDdb _database )
    {
        if( instance == null )
            instance = new DefaultDictionary( _database );
        return instance;
    }

    public final boolean exists()
    {
        return id != -1;
    }

    public final void init()
    {
        id = DBDictionaryFactory.getInstance( database ).getDictionaryIDByUUID(DICT_UUID);
        if( id == -1 )
            createDefaultDict();
    }

    private void createDefaultDict() throws SQLiteException
    {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put( "name", DICT_NAME );
        cv.put( "language", DICT_LANG );
        cv.put( "uuid", DICT_UUID );

        id = (int) db.insertOrThrow(WDdb.T_DICTIONARY, null, cv);

        Log.d("DefaultDictionary::init", "Default dictionary has been created with ID=" + id);

        db.close();
    }

    public Dictionary getDictionary()
    {
        if( dict == null )
            dict = new Dictionary( id, DICT_NAME, DICT_UUID, DICT_LANG );

        return dict;
    }

    public int getId() throws SQLDataException
    {
        if( id == -1 )
            throw new SQLDataException( "The default dictionary hadn't been initialized properly");

        return id;
    }

    public void addWord( String word ) throws Exception {
        Word w = new Word( word );
        w.meanings().clear();
        w.addMeaning( new Meaning( "..." ) );
        DBWordFactory.getInstance( database, getDictionary() ).insertWord( w );
    }

}

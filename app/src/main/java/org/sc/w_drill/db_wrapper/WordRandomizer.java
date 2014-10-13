package org.sc.w_drill.db_wrapper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Max on 10/8/2014.
 */
public class WordRandomizer
{
    WDdb database;
    ArrayList<Integer> ids;
    Dictionary dict;
    int interval;
    Random random;
    String statement = "select id from words where dict_id = ? ";

    public WordRandomizer( WDdb _db, Dictionary _dict )
    {
        ids = new ArrayList<Integer>();
        database = _db;
        dict = _dict;
    }

    public void init( String whereStatement )
    {
        statement = statement + " and " + whereStatement;
        init();
    }

    public void init() throws ArrayIndexOutOfBoundsException
    {
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[] { Integer.valueOf( dict.getId() ).toString()} );

        while( crs.moveToNext() )
        {
            ids.add( Integer.valueOf( crs.getInt( 0 ) ) );
        }

        crs.close();
        db.close();

        if( ids.size() == 0 || ids.size() == 1 )
            throw new ArrayIndexOutOfBoundsException( );

        random = new Random();
    }

    public IWord gerRandomWord() throws ArrayIndexOutOfBoundsException
    {
        Collections.shuffle( ids, random );
        int id = ids.get( random.nextInt( ids.size() ) ).intValue();
        IWord word = DBWordFactory.getInstance( database, dict ).getWordEx(id);
        if( word == null )
            throw new ArrayIndexOutOfBoundsException( "For ID " + id );
        return word;
    }

    public int getAvalableElementsSize()
    {
        return ids.size();
    }
}

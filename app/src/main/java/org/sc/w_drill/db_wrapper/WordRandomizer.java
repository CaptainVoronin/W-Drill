package org.sc.w_drill.db_wrapper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;

import java.util.ArrayList;
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
    public WordRandomizer( WDdb _db, Dictionary _dict )
    {
        ids = new ArrayList<Integer>();
        database = _db;
    }

    public void init() throws ArrayIndexOutOfBoundsException
    {
        SQLiteDatabase db = database.getReadableDatabase();
        String statement = "select id from words where dict_id = ?";
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

    public IWord gerRandomWord()
    {
        int id = random.nextInt( ids.size()  ) - 1;
        return DBWordFactory.getInstance( database, dict ).getWordEx(id);
    }
}

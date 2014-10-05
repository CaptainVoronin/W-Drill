package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.dict.Word;

import java.util.ArrayList;

/**
 * Created by maxsh on 30.09.2014.
 */
public class DBWordFactory
{
    public static String WORD_ID_VALUE_NAME = "WORD_ID_VALUE_NAME";

    static DBWordFactory instance = null;
    WDdb database;
    Dictionary dict;

    public static DBWordFactory getInstance( WDdb _db, Dictionary _dict )
    {
        if( instance == null )
            instance = new DBWordFactory( _db, _dict );

        return instance;
    }

    protected DBWordFactory(WDdb _db, Dictionary _dict)
    {
        database = _db;
        dict = _dict;
    }

    public ArrayList<BaseWord> getBriefList()
    {
        String statement = "select id, word, percent, stage from words where dict_id = ?";
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId() ).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();

        while( crs.moveToNext() )
        {
            words.add( new BaseWord( crs.getInt( 0 ),
                                     crs.getString( 1 ),
                                     crs.getInt( 2 ),
                                     crs.getInt( 3 ) == 0 ? IBaseWord.LearnState.learn : IBaseWord.LearnState.check ));
        }
        return words;
    }

    public ArrayList<IWord> getExtList()
    {
        return new ArrayList<IWord>();
    }

    public void delete( IBaseWord word )
    {
        throw new UnsupportedOperationException( "DbWordFactory::delete" );
    }

    public int insertWord( IWord word )
    {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put( "word", word.getWord() );
        cv.put( "dict_id", dict.getId() );
        db.insert( WDdb.T_WORDS, null, cv );

        String statement = "select max( id ) from words";
        Cursor crs = db.rawQuery( statement, null );

        crs.moveToNext();
        int id = crs.getInt( 0 );
        crs.close();
        db.close();

        return id;
    }

    public void updateWord(IWord activeWord)
    {

    }

    public IWord getWord(int wordId)
    {
        String statement = "select id, word, percent, stage from words where id = ?";
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( wordId ).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();
        Word word = null;

        if( crs.moveToNext() )
        {
            word = new Word( crs.getInt( 0 ), crs.getString( 1 ) );
        }
        return word;
    }

    public void deleteWords(ArrayList<Integer> selectedWords)
    {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        for( Integer val : selectedWords )
             db.delete( WDdb.T_WORDS, "id=?", new String[] { val.toString() } );
        db.endTransaction();
    }
}

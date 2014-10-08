package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;
import org.sc.w_drill.utils.DBPair;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

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

        if( !_dict.equals( instance.getDict()) )
            instance = new DBWordFactory( _db, _dict );

        return instance;
    }

    public Dictionary getDict()
    {
        return dict;
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

    public IWord insertWord( IWord word ) throws Exception
    {

        SQLiteDatabase db = null;
        Exception e = null;
        try
        {
            db = database.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put("word", word.getWord());
            cv.put("dict_id", dict.getId());
            cv.put("uuid", UUID.randomUUID().toString());
            cv.put("transcription", word.getTranscription());

            db.beginTransaction();
            db.insertOrThrow(WDdb.T_WORDS, null, cv);

            String statement = "select max( id ) from words";
            Cursor crs = db.rawQuery(statement, null);

            crs.moveToNext();
            int id = crs.getInt(0);
            crs.close();

            word.setId(id);

            // Now insert meanings

            if( word.meanings() != null )
                for (IMeaning m : word.meanings())
                {
                    cv.clear();
                    cv.put("word_id", Integer.valueOf(id).toString());
                    cv.put("meaning", m.meaning());
                    // TODO: Values: isFormal, etc aren't inserted
                    db.insertOrThrow(WDdb.T_MEANINGS, null, cv);

                    Cursor c = db.rawQuery( "select max ( id ) from words ", null );

                    c.moveToNext();
                    int meaning_id = c.getInt( 0 );

                    // insert examples
                    if( m.examples() != null )
                        for (DBPair example : m.examples())
                        {
                            cv.clear();
                            cv.put("meaning_id", meaning_id);
                            cv.put("example", example.getValue());
                            db.insertOrThrow(WDdb.T_EXAMPLE, null, cv);
                        }
                }
            db.setTransactionSuccessful();
        }catch ( SQLiteException ex )
        {
            Log.e( "[DBWordFactory::insertWord]", "Exception: " + ex.getMessage() );
            e = ex;
        }
        finally
        {
            if( db != null )
            {
                db.endTransaction();
                db.close();
            }

            if( e != null )
                throw e;
        }

        return word;
    }

    public void updateWord(IWord activeWord)
    {

    }

    public IWord getWord(int wordId)
    {

        SQLiteDatabase db = database.getReadableDatabase();
        IWord word = internalGetWordBrief( db, wordId );
        db.close();
        return word;
    }

    private IWord internalGetWordBrief(SQLiteDatabase db, int wordId)
    {
        String statement = "select id, word, percent, stage from words where id = ?";

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( wordId ).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();
        Word word = null;

        if( crs.moveToNext() )
        {
            word = new Word( crs.getInt( 0 ), crs.getString( 1 ) );
        }
        crs.close();
        return word;
    }

    public IWord getWodEx( int wordId ) {
        SQLiteDatabase db = database.getReadableDatabase();
        IWord word = internalGetWordBrief(db, wordId );

        String statement = "select id, meaning, is_formal, is_disapproving, is_rude from meanings where word_id = ?";
        String examples = "select id, example from examples where meaning_id = ?";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf( wordId ).toString()});

        if (crs.getCount() != 0) {
            while (crs.moveToNext()) {
                IMeaning m = new Meaning(crs.getInt(0), crs.getString(1));
                word.meanings().add(m);
                Cursor crs1 = db.rawQuery(examples, new String[]{Integer.valueOf(m.getId()).toString()});
                if (crs.getCount() != 0) {
                    while (crs1.moveToNext()) {
                        DBPair pair = new DBPair(crs1.getInt(0), crs1.getString(1));
                        m.examples().add(pair);
                    }
                    crs1.close();
                }
            }
            crs.close();
        }
        db.close();

        return word;
    }

    public void deleteWords(ArrayList<Integer> selectedWords)
    {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        int cnt = 0;
        for( Integer val : selectedWords ) {
            if( db.delete(WDdb.T_WORDS, "id=?", new String[]{val.toString()}) != 0 )
                cnt++;
            else
                Log.w("[DBWordFactory::deleteWords]", "Word with id " + val.toString() + " wasn't delete");
        }

        Log.w("[DBWordFactory::deleteWords]", "Was deleted " + cnt + " from " + selectedWords.size() );

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public boolean findWord(String word)
    {
        boolean result = false;
        String statement = "select 1 from words where dict_id = ? and word = ?";
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[] { Integer.valueOf( dict.getId() ).toString(), word  } );
        result = crs.getCount() != 0;
        crs.close();
        db.close();
        return result;
    }
}

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

    public ArrayList<BaseWord> getBriefList( String whereSTMT, String orderSTMT )
    {
        String statement = "select id, word, percent, stage, avg_time, access_count, uuid from words where dict_id = ?";
        String addendum = " and %s";
        String order = " order by %s";

        if( whereSTMT != null && whereSTMT.length() != 0 )
        {
            statement = String.format(statement + addendum, whereSTMT);
        }

        if( orderSTMT != null && orderSTMT.length() != 0 )
        {
            statement = String.format(statement +  order, orderSTMT);
        }

        Log.d( "[DBWordFactory::getBriefList]", statement );

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId() ).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();

        while( crs.moveToNext() )
        {
            words.add( new BaseWord( crs.getInt( 0 ),
                    crs.getString( 1 ),
                    crs.getInt( 2 ),
                    crs.getInt( 3 ) == 0 ? IBaseWord.LearnState.learn : IBaseWord.LearnState.check,
                    crs.getInt( 4 ),
                    crs.getInt( 5 ),
                    crs.getString( 6 )));
        }
        return words;
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
            String uuid = UUID.randomUUID().toString();
            cv.put("uuid", uuid );
            cv.put("transcription", word.getTranscription());

            db.beginTransaction();
            int id = ( int ) db.insertOrThrow(WDdb.T_WORDS, null, cv);

            word.setId(id);
            word.setUUID( uuid );

            // Now insert meanings
            putMeaningsAndExamples( db, word );
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

    private void putMeaningsAndExamples( SQLiteDatabase db, IWord word ) throws SQLiteException
    {
        ContentValues cv = new ContentValues();
        int id = word.getId(); //Integer.valueOf( word.getId() ).toString();

        if( word.meanings() != null )
            for (IMeaning m : word.meanings())
            {
                cv.clear();
                cv.put("word_id", id );
                cv.put("meaning", m.meaning());
                cv.put( "part_of_speech", m.partOFSpeech() );
                // TODO: Values: isFormal, etc aren't inserted
                int meaning_id = ( int ) db.insertOrThrow(WDdb.T_MEANINGS, null, cv);

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
    }

    public void updateWord(IWord word) throws Exception
    {
        Exception e = null;
        SQLiteDatabase db = database.getWritableDatabase();

        try
        {
            db.beginTransaction();
            // Firstly, delete all meanings and examples
            db.delete( WDdb.T_MEANINGS, " word_id = ? ", new String[] { Integer.valueOf( word.getId() ).toString() });

            ContentValues cv = new ContentValues();

            cv.put( "word", word.getWord() );
            cv.put( "transcription", word.getTranscription() );
            db.update( WDdb.T_WORDS, cv, " id = ?", new String[]{ Integer.valueOf( word.getId() ).toString()} );

            putMeaningsAndExamples( db, word );

            db.setTransactionSuccessful();
        } catch( Exception ex )
        {
            e = ex;
        }
        finally
        {
            db.endTransaction();
            db.close();
            if( e != null )
                throw e;
        }
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
        String statement = "select id, word, percent, stage, avg_time, access_count, transcription, uuid from words where id = ?";

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( wordId ).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();
        Word word = null;

        if( crs.moveToNext() )
        {
            word = new Word( crs.getInt( 0 ), // id
                    crs.getString( 1 ),  // word
                    crs.getInt( 2 ), // percent
                    crs.getInt( 3 ) == 0 ? IBaseWord.LearnState.learn : IBaseWord.LearnState.check,  // stage
                    crs.getInt( 4 ), // avg_time
                    crs.getInt( 5 ),
                    crs.getString(7)); // access_time

            word.setTranscription( crs.getString( 6 ) );
        }
        crs.close();
        return word;
    }

    private IWord internalGetWordEx(SQLiteDatabase db, int wordId)
    {
        IWord word = internalGetWordBrief(db, wordId );

        String statement = "select id, meaning, is_formal, is_disapproving, is_rude, part_of_speech from meanings where word_id = ?";
        String examples = "select id, example from examples where meaning_id = ?";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf( wordId ).toString()});

        if (crs.getCount() != 0) {
            while (crs.moveToNext()) {
                //Meaning( String _meaning, boolean _isFormal, boolean _isDisapproving, boolean _isRude  )
                IMeaning m = new Meaning(crs.getInt(0),
                                         crs.getString(1),
                                         crs.getInt( 2 ) == 0 ? false : true,
                                         crs.getInt( 3 ) == 0 ? false : true,
                                         crs.getInt( 4 ) == 0 ? false : true );
                m.setPartOfSpeech( crs.getString(5) );
                word.meanings().add(m);
                Cursor crs1 = db.rawQuery(examples, new String[]{Integer.valueOf(m.getId()).toString()});
                if (crs1.getCount() != 0) {
                    while (crs1.moveToNext()) {
                        DBPair pair = new DBPair(crs1.getInt(0), crs1.getString(1));
                        m.examples().add(pair);
                    }

                }
                crs1.close();
            }
        }
        crs.close();
        return word;
    }

    public IWord getWordEx(int wordId) {
        SQLiteDatabase db = database.getReadableDatabase();
        IWord word = internalGetWordEx(db, wordId);
        db.close();
        return word;
    }

    public ArrayList<IWord> getWordsToLearn( int limit )
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();

        SQLiteDatabase db = database.getReadableDatabase();
        String statement =
        "select id, (julianday( 'now' ) - julianday( last_access )) as result " +
                "from words where " +
                "stage = 0 and " +
                "( result >= " +  WDdb.learnTimeOut + " or last_access IS NULL ) and  " +
                //"( result >= 0.08 or last_access IS NULL ) and  " +
                "dict_id = ? " +
                "order by access_count asc, percent asc, result desc, avg_time desc " +
                "limit ?; ";

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId() ).toString(), Integer.valueOf( limit ).toString()} );
        while( crs.moveToNext() )
        {
            ids.add( crs.getInt( 0 ));
        }
        crs.close();

        if( ids.size() == 0 )
            return null;

        ArrayList<IWord> words = new ArrayList<IWord>();

        for( Integer id : ids )
        {
            IWord word = internalGetWordEx(db, id.intValue());
            words.add( word );
        }
        db.close();
        return words;
    }

    public int deleteWords(ArrayList<IBaseWord> selectedWords) throws SQLiteException {
        int cnt = 0;
        SQLiteDatabase db = database.getWritableDatabase();
        SQLiteException ex = null;
        try
        {
            db.beginTransaction();

            for (IBaseWord val : selectedWords)
            {
                if (db.delete(WDdb.T_WORDS, "id=?", new String[]{ Integer.valueOf( val.getId() ).toString()}) != 0)
                    cnt++;
                else
                    Log.w("[DBWordFactory::deleteWords]", "Word with id " + val.toString() + " wasn't delete");
            }

            Log.w("[DBWordFactory::deleteWords]", "Was deleted " + cnt + " from " + selectedWords.size());

            db.setTransactionSuccessful();
        }
        catch ( SQLiteException e )
        {
            ex = e;
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
            db.close();
            if( ex != null )
                throw ex;
        }
        return cnt;
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

    public void updatePercentAndTime( int wordId, int percent, int time )
    {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put( "percent", percent );

        if( percent >= 100 )
        {
            cv.put( "avg_time", 0 );
            cv.put( "stage", 1 );
        }
        else
        {
            cv.put( "avg_time", time );
            cv.put( "stage", 0 );
        }
        cv.put( "last_access", "CURRENT_TIMESTAMP" );

        db.update( WDdb.T_WORDS, cv, "id = ?", new String [] {Integer.valueOf( wordId ).toString() } );

        db.close();
    }

    /*public void technicalInsert( SQLiteDatabase db, int dictId, String word, String meaning, String example )
    {
        ContentValues cv = new ContentValues();
        cv.put("word", word);
        cv.put("dict_id", dictId );
        cv.put("uuid", UUID.randomUUID().toString());

        int id = (int) db.insertOrThrow( WDdb.T_WORDS, null, cv);

        cv.clear();

        cv.put("word_id", Integer.valueOf(id).toString());
        cv.put("meaning", meaning);
        id = (int) db.insertOrThrow(WDdb.T_MEANINGS, null, cv);

        cv.clear();
        cv.put("meaning_id", id);
        cv.put("example", example);
        db.insertOrThrow(WDdb.T_EXAMPLE, null, cv);
    } */

    public void technicalInsert( SQLiteDatabase db, int dictId, IWord word )
    {
        ContentValues cv = new ContentValues();
        cv.put( "word", word.getWord());
        cv.put( "dict_id", dictId );
        cv.put( "transcription", word.getTranscription() );
        cv.put( "uuid", word.getUUID() );
        cv.put( "percent", word.getLearnPercent() );
        cv.put( "stage", word.getLearnState() == IBaseWord.LearnState.learn ? 0 : 1 );

        int id = (int) db.insertOrThrow( WDdb.T_WORDS, null, cv);
        word.setId( id );
        cv.clear();

        putMeaningsAndExamples( db, word );
    }


    public ArrayList<DBPair> technicalGetWordUnique()
    {
        String statement = "select id, uuid from words where dict_id = ?";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId() ).toString() } );

        ArrayList<DBPair> ids = new ArrayList<DBPair>();

        while( crs.moveToNext() )
            ids.add( new DBPair( Integer.valueOf( crs.getInt(0) ), crs.getString( 1 ) ) );

        crs.close();
        db.close();
        return ids;
   }

}
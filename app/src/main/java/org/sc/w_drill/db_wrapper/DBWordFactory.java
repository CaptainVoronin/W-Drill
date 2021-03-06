package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.content.Context;
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
import org.sc.w_drill.utils.datetime.DateTimeUtils;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by maxsh on 30.09.2014.
 */
public class DBWordFactory
{
    public static String WORD_ID_VALUE_NAME = "WORD_ID_VALUE_NAME";

    static DBWordFactory instance = null;
    Context context;
    Dictionary dict;

    public static final String wordsToCheckWhereClause = " stage = 1 and " +
            "( julianday( 'now' ) - julianday( last_access ) >= " + WDdb.checkTimeOut + " or last_access IS NULL ) and  dict_id = ? ";

    public static final String wordsToLearnWhereClause = " stage = 0 and " +
            "( julianday( 'now' ) - julianday( last_access ) >= " + WDdb.learnTimeOut + " or last_access IS NULL ) and  dict_id = ? ";

    public static DBWordFactory getInstance(Context _context, Dictionary _dict)
    {
        if (instance == null)
            instance = new DBWordFactory(_context, _dict);

        if (!_dict.equals(instance.getDict()))
            instance = new DBWordFactory(_context, _dict);

        return instance;
    }

    public Dictionary getDict()
    {
        return dict;
    }

    protected DBWordFactory(Context _context, Dictionary _dict)
    {
        context = _context;
        dict = _dict;
    }

    public ArrayList<BaseWord> getBriefList(String whereSTMT, String orderSTMT)
    {
        String statement = "select id, word, percent, stage, avg_time, access_count, uuid from words where dict_id = ?";
        String addendum = " and %s";
        String order = " order by %s";

        if (whereSTMT != null && whereSTMT.length() != 0)
        {
            statement = String.format(statement + addendum, whereSTMT);
        }

        if (orderSTMT != null && orderSTMT.length() != 0)
        {
            statement = String.format(statement + order, orderSTMT);
        }

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();
        BaseWord word;
        while (crs.moveToNext())
        {
            word = new BaseWord(crs.getInt(0),
                    crs.getString(1),
                    crs.getInt(2),
                    crs.getInt(3) == 0 ? IBaseWord.LearnState.learn : IBaseWord.LearnState.check,
                    crs.getInt(4),
                    crs.getInt(5),
                    crs.getString(6));
            words.add(word);
        }
        return words;
    }

    public void delete(IBaseWord word)
    {
        throw new UnsupportedOperationException("DbWordFactory::delete");
    }

    public IWord insertWord(IWord word) throws Exception
    {

        SQLiteDatabase db = null;
        Exception e = null;
        try
        {
            db = WDdb.getInstance(context).getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put("word", word.getWord());
            cv.put("dict_id", dict.getId());
            String uuid = UUID.randomUUID().toString();
            cv.put("uuid", uuid);
            cv.put("transcription", word.getTranscription());

            db.beginTransaction();
            int id = (int) db.insertOrThrow(WDdb.T_WORDS, null, cv);

            word.setId(id);
            word.setUUID(uuid);

            // Now insert meanings
            putMeaningsAndExamples(db, word);
            db.setTransactionSuccessful();

        }
        catch (SQLiteException ex)
        {
            Log.e("[DBWordFactory::insertWord]", "Exception: " + ex.getMessage());
            e = ex;
        }
        finally
        {
            if (db != null)
            {
                db.endTransaction();
                db.close();
            }

            if (e != null)
                throw e;
        }

        return word;
    }

    private void putMeaningsAndExamples(SQLiteDatabase db, IWord word) throws SQLiteException
    {
        ContentValues cv = new ContentValues();
        int id = word.getId(); //Integer.valueOf( word.getId() ).toString();

        if (word.meanings() != null)
            for (IMeaning m : word.meanings())
            {
                cv.clear();
                cv.put("word_id", id);
                cv.put("meaning", m.meaning());
                cv.put("part_of_speech", m.partOFSpeech());
                // TODO: Values: isFormal, etc aren't inserted
                int meaning_id = (int) db.insertOrThrow(WDdb.T_MEANINGS, null, cv);

                // insert examples
                if (m.examples() != null)
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
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();

        try
        {
            db.beginTransaction();
            // Firstly, delete all meanings and examples
            db.delete(WDdb.T_MEANINGS, " word_id = ? ", new String[]{Integer.valueOf(word.getId()).toString()});

            ContentValues cv = new ContentValues();

            cv.put("word", word.getWord());
            cv.put("transcription", word.getTranscription());
            db.update(WDdb.T_WORDS, cv, " id = ?", new String[]{Integer.valueOf(word.getId()).toString()});

            putMeaningsAndExamples(db, word);

            db.setTransactionSuccessful();
        }
        catch (Exception ex)
        {
            e = ex;
        }
        finally
        {
            db.endTransaction();
            if (e != null)
                throw e;
        }
    }

    public IWord getWord(int wordId)
    {
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        IWord word = internalGetWordBrief(db, wordId);
        return word;
    }

    private IWord internalGetWordBrief(SQLiteDatabase db, int wordId)
    {
        String statement = "select id, word, percent, stage, avg_time, access_count, transcription, uuid, updated, last_access from words where id = ?";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(wordId).toString()});
        ArrayList<BaseWord> words = new ArrayList<BaseWord>();
        Word word = null;

        if (crs.moveToNext())
        {
            word = new Word(crs.getInt(0), // id
                    crs.getString(1),  // word
                    crs.getInt(2), // percent
                    crs.getInt(3) == 0 ? IBaseWord.LearnState.learn : IBaseWord.LearnState.check,  // stage
                    crs.getInt(4), // avg_time
                    crs.getInt(5),
                    crs.getString(7)); // uuid
            word.setTranscription(crs.getString(6));

            word.setLastUpdate(DateTimeUtils.strToDate(crs.getString(8)));

            if (crs.getString(9) != null)
                word.setLastAccess(DateTimeUtils.strToDate(crs.getString(9)));

        }
        crs.close();
        return word;
    }

    private IWord internalGetWordEx(SQLiteDatabase db, int wordId)
    {
        IWord word = internalGetWordBrief(db, wordId);

        String statement = "select id, meaning, is_formal, is_disapproving, is_rude, part_of_speech from meanings where word_id = ?";

        String examples = "select id, example from examples where meaning_id = ?";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(wordId).toString()});

        if (crs.getCount() != 0)
        {
            while (crs.moveToNext())
            {
                //Meaning( String _meaning, boolean _isFormal, boolean _isDisapproving, boolean _isRude  )
                IMeaning m = new Meaning(crs.getInt(0),
                        crs.getString(1),
                        crs.getInt(2) == 0 ? false : true,
                        crs.getInt(3) == 0 ? false : true,
                        crs.getInt(4) == 0 ? false : true);
                m.setPartOfSpeech(crs.getString(5));
                word.meanings().add(m);
                Cursor crs1 = db.rawQuery(examples, new String[]{Integer.valueOf(m.getId()).toString()});
                if (crs1.getCount() != 0)
                {
                    while (crs1.moveToNext())
                    {
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

    public IWord getWordEx(int wordId)
    {
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        IWord word = internalGetWordEx(db, wordId);
        return word;
    }

    public ArrayList<IWord> getWordsToLearn(int limit)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        String statement = "select id, (julianday( 'now' ) - julianday( last_access )) as result " +
                "from words where " +
                wordsToLearnWhereClause +
                "order by access_count asc, percent asc, result desc, avg_time desc " +
                "limit ?; ";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString(), Integer.valueOf(limit).toString()});
        while (crs.moveToNext())
        {
            ids.add(crs.getInt(0));
        }
        crs.close();

        if (ids.size() == 0)
            return null;

        ArrayList<IWord> words = new ArrayList<IWord>();

        for (Integer id : ids)
        {
            IWord word = internalGetWordEx(db, id.intValue());
            words.add(word);
        }
        return words;
    }

    public ArrayList<IWord> getWordsToCheck(int limit)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        String statement = "select id, (julianday( 'now' ) - julianday( last_access )) as result " +
                "from words where " +
                wordsToCheckWhereClause +
                "order by result asc, percent asc " +
                "limit ?; ";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString(), Integer.valueOf(limit).toString()});

        while (crs.moveToNext())
            ids.add(crs.getInt(0));

        crs.close();

        if (ids.size() == 0)
            return null;

        ArrayList<IWord> words = new ArrayList<IWord>();

        for (Integer id : ids)
        {
            IWord word = internalGetWordEx(db, id.intValue());
            words.add(word);
        }
        return words;
    }

    public int deleteWords(ArrayList<IBaseWord> selectedWords) throws SQLiteException
    {
        int cnt = 0;
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        SQLiteException ex = null;
        try
        {
            db.beginTransaction();

            for (IBaseWord val : selectedWords)
            {
                if (db.delete(WDdb.T_WORDS, "id=?", new String[]{Integer.valueOf(val.getId()).toString()}) != 0)
                    cnt++;
                else
                    Log.w("[DBWordFactory::deleteWords]", "Word with id " + val.toString() + " wasn't delete");
            }

            Log.w("[DBWordFactory::deleteWords]", "Was deleted " + cnt + " from " + selectedWords.size());

            db.setTransactionSuccessful();
        }
        catch (SQLiteException e)
        {
            ex = e;
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
            if (ex != null)
                throw ex;
        }
        return cnt;
    }

    public boolean findWord(String word)
    {
        boolean result = false;
        String statement = "select 1 from words where dict_id = ? and word = ?";
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString(), word});
        result = crs.getCount() != 0;
        crs.close();
        return result;
    }

    public void updatePercentAndTime(int wordId, int percent, int time)
    {
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("percent", percent);

        if (percent >= 100)
        {
            cv.put("avg_time", 0);
            cv.put("stage", 1);
        }
        else
        {
            cv.put("avg_time", time);
            cv.put("stage", 0);
        }
        //cv.put( "last_access", "CURRENT_TIMESTAMP" );

        db.update(WDdb.T_WORDS, cv, "id = ?", new String[]{Integer.valueOf(wordId).toString()});

    }

    public void technicalInsert(SQLiteDatabase db, int dictId, IWord word)
    {
        ContentValues cv = new ContentValues();
        cv.put("word", word.getWord());
        cv.put("dict_id", dictId);
        cv.put("transcription", word.getTranscription());
        cv.put("uuid", word.getUUID());
        cv.put("percent", word.getLearnPercent());
        cv.put("stage", word.getLearnState() == IBaseWord.LearnState.learn ? 0 : 1);

        if (word.getLastUpdate() != null)
            cv.put("updated", DateTimeUtils.getSQLDateTimeString(word.getLastUpdate()));
        if (word.getLastAccess() != null)
            cv.put("last_access", DateTimeUtils.getSQLDateTimeString(word.getLastAccess()));

        int id = (int) db.insertOrThrow(WDdb.T_WORDS, null, cv);
        word.setId(id);
        cv.clear();

        putMeaningsAndExamples(db, word);
    }

    public ArrayList<DBPair> technicalGetWordUnique()
    {
        String statement = "select id, uuid from words where dict_id = ?";

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString()});

        ArrayList<DBPair> ids = new ArrayList<DBPair>();

        while (crs.moveToNext())
            ids.add(new DBPair(Integer.valueOf(crs.getInt(0)), crs.getString(1)));

        crs.close();
        return ids;
    }

    public int moveWords(Dictionary dict, ArrayList<IBaseWord> selectedWords)
    {
        int count = 0;
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        SQLiteException e = null;
        try
        {
            ContentValues cv = new ContentValues();
            for (IBaseWord word : selectedWords)
            {
                cv.clear();
                cv.put("dict_id", dict.getId());
                db.update(WDdb.T_WORDS, cv, "id = ?", new String[]{Integer.valueOf(word.getId()).toString()});
                count++;

            }
            db.setTransactionSuccessful();
        }
        catch (SQLiteException ex)
        {
            e = ex;
        }
        finally
        {
            db.endTransaction();
            if (e != null)
                throw e;
        }
        return count;
    }

    public int checkUUID(SQLiteDatabase db, String uuid)
    {
        int dict_id = -1;

        String stmt = "select dict_id from words where uuid = '" + uuid + "'";

        Cursor crs = db.rawQuery(stmt, null);

        if (crs.moveToNext())
        {
            dict_id = crs.getInt(0);
            crs.close();
        }

        return dict_id;
    }

    public ArrayList<Integer> getIdsListWithExclusion(int id)
    {
        ArrayList<Integer> ids = new ArrayList<Integer>();

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        String statement = "select id " +
                "from words where " +
                " dict_id = " + dict.getId() + " and " +
                " id != " + id;

        Cursor crs = db.rawQuery(statement, null);

        while (crs.moveToNext())
            ids.add(Integer.valueOf(crs.getInt(0)));

        crs.close();

        return ids;

    }

    public void clearLearnStatistic()
    {
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("stage", 0);
        cv.put("percent", 0);

        db.update(WDdb.T_WORDS, cv, "dict_id = ?", new String[]{Integer.valueOf(dict.getId()).toString()});

    }

    public void setAllLearned()
    {
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("stage", 1);
        cv.put("percent", 200);

        db.update(WDdb.T_WORDS, cv, "dict_id = ?", new String[]{Integer.valueOf(dict.getId()).toString()});

    }
}
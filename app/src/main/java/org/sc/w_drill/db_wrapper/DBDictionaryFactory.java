package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.joda.time.DateTime;
import org.sc.w_drill.db.Utils;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.datetime.DateTimeUtils;

import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by maxsh on 30.09.2014.
 */
public class DBDictionaryFactory
{
    public final static String DICTIONARY_ID_VALUE_NAME = "DICTIONARY_ID_VALUE_NAME";

    public final static int STAGE_LEARN = 0;
    public final static int STAGE_CHECK = 1;

    static DBDictionaryFactory instance = null;
    Context context;

    public static DBDictionaryFactory getInstance(Context _context)
    {
        if (instance == null)
            instance = new DBDictionaryFactory(_context);

        return instance;
    }

    protected DBDictionaryFactory(Context _context)
    {
        context = _context;
    }

    /**
     * It returns the list of dictionaries without default dict
     *
     * @return the list of dictionaries without default dict
     */
    public ArrayList<Dictionary> getList( int exlude_id ) throws android.database.sqlite.SQLiteException, SQLDataException
    {
        ArrayList<Dictionary> list = new ArrayList<Dictionary>();

        String statement = "select d.id, d.name, d.language, count( w.dict_id ), d.uuid " +
                "from dictionary d left outer join words w on d.id = w.dict_id " +
                " group by d.id order by name";

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        Cursor crs = db.rawQuery(statement, null);

        int dd_id = DefaultDictionary.getInstance(context).getId();

        while (crs.moveToNext())
        {
            int id = crs.getInt(0);

            // Skip the default dictionary
            if (id == dd_id || id == exlude_id )
                continue;
            String name = crs.getString(1);
            String lang = crs.getString(2);
            int count = crs.getInt(3);
            String uuid = crs.getString(4);
            Dictionary dict = new Dictionary(id, name, uuid, lang, count);
            list.add(dict);
        }

        crs.close();
        return list;
    }

    /**
     * Создает новый словарь
     *
     * @param name - название новго словаря
     * @return - идентификатор созданного словаря
     */
    public Dictionary createNew(String name, String language) throws android.database.sqlite.SQLiteException
    {
        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("language", language);
        String uuid = UUID.randomUUID().toString();
        cv.put("uuid", uuid);

        db.insertOrThrow(WDdb.T_DICTIONARY, null, cv);

        String statement = "select max( id ) from dictionary";
        Cursor crs = db.rawQuery(statement, null);

        crs.moveToNext();
        int id = crs.getInt(0);
        crs.close();

        Dictionary dict = new Dictionary(id, name, uuid, language);

        return dict;
    }

    public boolean dictionaryExists(String _uuid)
    {
        String statement = "select count(*) from dictionary where uuid = ?";
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        Cursor crs = db.rawQuery(statement, new String[]{_uuid});
        crs.moveToNext();
        int cnt = crs.getInt(0);
        crs.close();
        return cnt >= 1;
    }

    /**
     * this is special version for bulk operations
     * It doesn't close database, so it is useful in transactions
     *
     * @param name
     * @param language
     * @param db
     * @return
     * @throws android.database.sqlite.SQLiteException
     */
    public Dictionary createNewSpec(String name, String language, SQLiteDatabase db, String _uuid) throws android.database.sqlite.SQLiteException
    {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("language", language);
        if (_uuid == null)
            _uuid = UUID.randomUUID().toString();
        cv.put("uuid", _uuid);

        int id = (int) db.insertOrThrow(WDdb.T_DICTIONARY, null, cv);

        Dictionary dict = new Dictionary(id, name, _uuid, language);

        return dict;
    }

    /**
     * Проверяет наличие названия словаря в БД
     *
     * @param name имя, которое надо проверить
     * @return false - если есть дубли, true - если имя уникально
     */
    public boolean checkDuplicate(String name) throws android.database.sqlite.SQLiteException
    {
        String statement = "select count(*) from dictionary where name = ?";
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        Cursor crs = db.rawQuery(statement, new String[]{name});
        crs.moveToFirst();
        int cnt = crs.getInt(0);
        crs.close();

        return cnt == 0;
    }

    public Dictionary getDictionaryById(int id)
    {
        String statement = "select d.id, d.name, d.language, count( w.dict_id ), d.uuid " +
                "from dictionary d left outer join words w on d.id = w.dict_id " +
                "where d.id = ?";

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        Cursor crs = db.rawQuery(statement, new String[]{Integer.toString(id)});

        Dictionary dict = null;

        if (crs.moveToNext())
        {
            int cnt1 = internalGetWordsToLearn(db, id); // words to learn
            int cnt2 = internalGetWordsToCheck(db, id); // words to check
            dict = new Dictionary(id, crs.getString(1), crs.getString(4), crs.getString(2), crs.getInt(3), cnt1, cnt2);
        }
        else
        {
            crs.close();
            String message = "Dictionary with ID " + id + " not found in a database";
            Log.e("[DBDictionaryFactory::getDictionaryById]", message);
            throw new IllegalArgumentException(message);
        }

        crs.close();
        return dict;
    }

    public int getDictionaryIDByUUID(String uuid)
    {
        String statement = "select id from dictionary " +
                "where uuid = '" + uuid + "'";
        int id = -1;

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        Cursor crs = db.rawQuery(statement, null);

        if (crs.moveToNext())
            id = crs.getInt(0);

        crs.close();

        return id;
    }

    /**
     * It returns count of words in a dictionary, which specified
     * with dict_id. The type of words depends on stage param.
     * If stage = 0, the function returns count of words to learn.
     * If stage = 1, the function returns count of words to check.
     */
    public int getWordsTo(int dict_id, int stage)
    {
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        int count;
        if (stage == 0)
            count = internalGetWordsToLearn(db, dict_id);
        else
            count = internalGetWordsToCheck(db, dict_id);


        return count;
    }

    private int internalGetWordsToCheck(SQLiteDatabase db, int dict_id)
    {
        int count = 0;
        String statement = "select count( id ) " +
                "from words " +
                "where " + DBWordFactory.wordsToCheckWhereClause;

        Cursor crs = db.rawQuery(statement, new String[]{Integer.toString(dict_id)});

        Dictionary dict = null;

        if (crs.moveToNext())
            count = crs.getInt(0);

        crs.close();
        return count;
    }

    private int internalGetWordsToLearn(SQLiteDatabase db, int dict_id)
    {
        int count = 0;
        //TODO: There should be a parameter which contains a time interval
        // between learning sessions. Now it's hardcoded with value 0.08 - two hours.
        String statement = "select count( id ) " +
                "from words " +
                "where " + DBWordFactory.wordsToLearnWhereClause;

        Cursor crs = db.rawQuery(statement, new String[]{Integer.toString(dict_id)});

        Dictionary dict = null;

        if (crs.moveToNext())
            count = crs.getInt(0);

        crs.close();
        return count;
    }

    /**
     * It counts dictionaries in table and return result
     *
     * @return count of dictionaries
     */
    public int getDictCount() throws SQLDataException
    {
        String statement = "select count(id) from dictionary where dict_id != ?";
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        int id = DefaultDictionary.getInstance(context).getId();
        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(id).toString()});
        crs.moveToFirst();
        int cnt = crs.getInt(0);
        crs.close();
        return cnt;
    }

    /**
     * Удаляет словарь бесследно
     *
     * @param - словарь для удаления
     */
    public void delete(Dictionary dict) throws SQLDataException
    {
        if (dict.getId() == DefaultDictionary.getInstance(context).getId())
            throw new SQLiteConstraintException("Tha default dictionary can't be deleted");

        SQLiteDatabase db = WDdb.getInstance(context).getWritableDatabase();
        db.delete(WDdb.T_DICTIONARY, "id = " + dict.getId(), null);
    }

    /**
     * It gets additional information such as a count of words for learning
     * and a count of words for check up.
     *
     * @param dict
     * @return
     */
    public Dictionary getAdditionalInfo(Dictionary dict)
    {
        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();
        int count = internalGetWordsToLearn(db, dict.getId());
        dict.setWordsToLear(count);
        count = internalGetWordsToCheck(db, dict.getId());
        dict.setWordsToCheck(count);
        dict.setLastAccess(getLastAccess(db, dict));

        return dict;
    }

    public float getLearningEstimation(Dictionary dict)
    {
        String statement = "select count(*) from words where dict_id = ?";

        SQLiteDatabase db = WDdb.getInstance(context).getReadableDatabase();

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString()});

        crs.moveToNext();

        int count = crs.getInt(0);

        crs.close();

        statement = "select sum(*) from words where dict_id = ? and percent != 0";

        crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString()});

        crs.moveToNext();

        int percent_count = crs.getInt(0);

        crs.close();

        float result = ((float) percent_count) / count;

        return 0;
    }

    /**
     * @param dict
     * @return
     */
    public DateTime getLastAccess(SQLiteDatabase db, Dictionary dict)
    {
        DateTime dt = null;

        /*Utils.dumpQuery(db, "select id, word, updated, last_access, (julianday( 'now' ) - julianday( last_access )) as result" +
                " from words where dict_id = " + dict.getId() + " order by result asc, percent asc, avg_time desc "); */

        String statement = "select max( last_access ) from words where dict_id = ?";

        Cursor crs = db.rawQuery(statement, new String[]{Integer.valueOf(dict.getId()).toString()});

        if (crs.moveToNext())
        {
            String str = crs.getString(0);
            if (str != null)
                dt = DateTimeUtils.strToLocalDate(str);
            crs.close();
        }

        return dt;
    }

    public static int toughRestoreIntegrity(WDdb database)
    {

        String statement = "select dict_id, count( dict_id ) from words group by dict_id";

        SQLiteDatabase db = database.getWritableDatabase();

        Cursor crs = db.rawQuery(statement, null);

        while (crs.moveToNext())
        {
            Log.d("TEMP", "THere is dictionary id " + crs.getInt(0));
        }

        crs.close();

        return 0;
    }

}
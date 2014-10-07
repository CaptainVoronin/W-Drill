package org.sc.w_drill.db_wrapper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;

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
    WDdb database;

    public static DBDictionaryFactory getInstance( WDdb _db )
    {
        if( instance == null )
            instance = new DBDictionaryFactory( _db );

        return instance;
    }

    protected DBDictionaryFactory(WDdb _db)
    {
        database = _db;
    }

    /**
     * Возвращает список словарей
     * @return список словарей
     */
    public ArrayList<Dictionary> getList() throws android.database.sqlite.SQLiteException
    {
        ArrayList<Dictionary> list = new ArrayList<Dictionary>();

        String statement = "select d.id, d.name, d.language, count( w.dict_id ), d.uuid " +
                "from dictionary d left outer join words w on d.id = w.dict_id " +
                "group by d.id " +
                "order by name";

        SQLiteDatabase db = database.getReadableDatabase();
        Cursor crs = db.rawQuery( statement, null );

        while( crs.moveToNext() )
        {
            int id = crs.getInt( 0 );
            String name = crs.getString( 1 );
            String lang = crs.getString( 2 );
            int count = crs.getInt( 3 );
            String uuid = crs.getString( 4 );
            list.add(  new Dictionary( id, name, uuid, lang, count ) );
        }
        crs.close();
        db.close();
        return list;
    }

    /**
     * Создает новый словарь
     * @param name - название новго словаря
     * @return - идентификатор созданного словаря
     */
    public Dictionary createNew( String name, String language ) throws android.database.sqlite.SQLiteException
    {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put( "name", name );
        cv.put( "language", language );
        String uuid = UUID.randomUUID().toString();
        cv.put( "uuid", uuid );

        db.insert( WDdb.T_DICTIONARY, null, cv );

        String statement = "select max( id ) from dictionary";
        Cursor crs = db.rawQuery( statement, null );

        crs.moveToNext();
        int id = crs.getInt( 0 );
        crs.close();
        db.close();

        Dictionary dict = new Dictionary( id, name, uuid, language );

        return dict;
    }


    /**
     * Проверяет наличие названия словаря в БД
     * @param name имя, которое надо проверить
     * @return false - если есть дубли, true - если имя уникально
     */
    public boolean checkDuplicate( String name ) throws android.database.sqlite.SQLiteException
    {
        String statement = "select count(*) from dictionary where name = ?";
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor crs = db.rawQuery( statement, new String[] { name } );
        crs.moveToFirst();
        int cnt = crs.getInt(0);
        crs.close();
        db.close();
        return cnt == 0;
    }

     public Dictionary getDictionaryById( int id ) {
         String statement = "select d.id, d.name, d.language, count( w.dict_id ), d.uuid " +
                 "from dictionary d left outer join words w on d.id = w.dict_id " +
                 "where d.id = ?";

         SQLiteDatabase db = database.getReadableDatabase();
         Cursor crs = db.rawQuery(statement, new String[] { Integer.toString( id ) });

         Dictionary dict = null;

         if( crs.moveToNext() )
         {
             int cnt1 = internalGetWordsTo(  db, id, 0 ); // words to learn
             int cnt2 = internalGetWordsTo( db, id, 1 ); // words to check
             dict = new Dictionary(id, crs.getString(1), crs.getString(4), crs.getString(2), crs.getInt(3), cnt1, cnt2 );
         }
         else
         {
             crs.close();
             db.close();
             String message = "Dictionary with ID " + id + " not found in a database";
             Log.e("[DBDictionaryFactory::getDictionaryById]", message);
             throw new IllegalArgumentException( message );
         }

         crs.close();
         db.close();
         return dict;
     }

    /**
     * It returns count of words in a dictionary, which specified
     * with dict_id. The type of words depends on stage param.
     * If stage = 0, the function returns count of words to learn.
     * If stage = 1, the function returns count of words to check.
     */
    public int getWordsTo( int dict_id, int stage )
    {
        SQLiteDatabase db = database.getReadableDatabase();
        int count = internalGetWordsTo(db, dict_id, stage);
        db.close();
        return count;
    }

    private int internalGetWordsTo( SQLiteDatabase db, int dict_id, int stage )
    {
        int count = 0;
        String statement = "select count( id ) " +
                "from words " +
                "where dict_id = ? and stage = ?";

        Cursor crs = db.rawQuery(statement, new String[] { Integer.toString( dict_id ), Integer.toString( stage ) });

        Dictionary dict = null;

        if( crs.moveToNext() )
            count = crs.getInt( 0 );

        crs.close();
        return count;
    }

    /**
     * It counts dictionaries in table and return result
     * @return count of dictionaries
     */
     public int getDictCount()
     {
         String statement = "select count(id) from dictionary";
         SQLiteDatabase db = database.getReadableDatabase();
         Cursor crs = db.rawQuery( statement, null );
         crs.moveToFirst();
         int cnt = crs.getInt(0);
         crs.close();
         db.close();
         return cnt;
     }
    /**
     * Удаляет словарь бесследно
     * @param - словарь для удаления
     */
    public void delete( Dictionary dict )
    {
        SQLiteDatabase db = database.getWritableDatabase();
        db.delete( WDdb.T_DICTIONARY, "id = " + dict.getId(), null );
        db.close();
    }

    /**
     * Переносит слова из одного словаря в другой и удалает первый
     * @param destDict - словарь, куда надо перенести слова
     * @param sourceDict - словарь, из которого надо перенести слова и потом удалить
     */
    public void move_and_delete( Dictionary destDict, Dictionary sourceDict )
    {
        throw new UnsupportedOperationException( "Dictionary::move_and_delete" );
    }

    /**
     * It gets additional information such as a count of words for learning
     * and a count of words for check up.
     * @param dict
     * @return
     */
    public Dictionary getAdditionalInfo ( Dictionary dict )
    {
        SQLiteDatabase db = database.getReadableDatabase();
        int count = internalGetWordsTo( db, dict.getId(), STAGE_LEARN );
        dict.setWordsToLear( count );
        count = internalGetWordsTo( db, dict.getId(), STAGE_CHECK );
        dict.setWordsToCheck(count);
        db.close();
        return dict;
    }

    public float getLearningEstimation(Dictionary dict )
    {
        String statement = "select count(*) from words where dict_id = ?";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId()).toString() } );

        crs.moveToNext();

        int count = crs.getInt( 0 );

        crs.close();

        statement = "select sum(*) from words where dict_id = ? and percent != 0";

        crs = db.rawQuery( statement, new String[]{ Integer.valueOf( dict.getId()).toString() } );

        crs.moveToNext();

        int percent_count = crs.getInt( 0 );

        crs.close();

        float result = ((float) percent_count ) / count;

        return 0;
    }
}
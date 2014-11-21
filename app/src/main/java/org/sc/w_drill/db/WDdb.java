package org.sc.w_drill.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by maxsh on 30.09.2014.
 */
public class WDdb extends SQLiteOpenHelper
{

    public final static int SCHEME_VERSION = 18;
    public final static String learnTimeOut = "0.08"; // It's two hours
    public final static String checkTimeOut = "0.8"; // It's 10 hours
    //public final static String checkTimeOut = "0"; // It's 10 hours

    public final static String T_DICTIONARY = "dictionary";
    public final static String T_WORDS = "words";
    public final static String T_MEANINGS = "meanings";
    public final static String T_EXAMPLE = "examples";

    final static String CREATE_DICTIONARY = "CREATE TABLE dictionary( id INTEGER PRIMARY KEY autoincrement, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "name TEXT NOT NULL UNIQUE, " +
            "language TEXT NOT NULL," +
            "type INTEGER NOT NULL DEFAULT 0 );";

    final static String CREATE_WORDS = "CREATE TABLE " +
            "words( id INTEGER PRIMARY KEY autoincrement, " +
            "dict_id INTEGER NOT NULL, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "word TEXT NOT NULL, " +
            "transcription TEXT, " +
            "stage INTEGER NOT NULL DEFAULT 0, " +
            "percent INTEGER NOT NULL DEFAULT 0," +
            "access_count INTEGER NOT NULL DEFAULT 0," +
            "avg_time INTEGER NOT NULL DEFAULT 0," +
            "created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "last_access TIMESTAMP," +
            "picture_file TEXT," +
            "sound_file TEXT," +
            "CONSTRAINT uniq_within_dict UNIQUE ( word, dict_id ) ON CONFLICT ROLLBACK," +
            "FOREIGN KEY(dict_id) REFERENCES dictionary( id ) ON DELETE CASCADE );";

    final static String CREATE_WORDS_UPDATE_TRIGGER = "CREATE TRIGGER au_words AFTER UPDATE ON words " +
            "FOR EACH ROW " +
            "BEGIN " +
            " update words set updated = CURRENT_TIMESTAMP where id = old.id;" +
            "END;";

    final static String CREATE_WORDS_UPDATE_TRIGGER1 = "CREATE TRIGGER au_update_percent AFTER UPDATE ON words " +
            "WHEN old.percent <> new.percent " +
            "BEGIN " +
            "   update words set last_access = CURRENT_TIMESTAMP, access_count = old.access_count + 1 where id = old.id;" +
            "END;";

    final static String CREATE_MEANINGS = "CREATE TABLE " +
            "meanings ( id INTEGER PRIMARY KEY autoincrement," +
            "word_id INTEGER NOT NULL," +
            "meaning TEXT NOT NULL," +
            "part_of_speech TEXT, " +
            "is_formal INTEGER NOT NULL DEFAULT 0," +
            "is_disapproving INTEGER NOT NULL DEFAULT 0," +
            "is_rude INTEGER NOT NULL DEFAULT 0," +
            "FOREIGN KEY ( word_id ) REFERENCES words ( id ) ON DELETE CASCADE );";

    final static String CREATE_EXAMPLES = "CREATE TABLE " +
            "examples ( id INTEGER PRIMARY KEY autoincrement," +
            "meaning_id INTEGER NOT NULL," +
            "example TEXT NOT NULL," +
            "FOREIGN KEY ( meaning_id ) REFERENCES meanings ( id ) ON DELETE CASCADE );";

    final static String DB_NAME = "wd.db";

    private static WDdb instance;

    static
    {
        instance = null;
    }

    Context mContext;

    protected WDdb(Context context)
    {
        super(context, DB_NAME, null, SCHEME_VERSION);
        mContext = context;
    }

    public static synchronized WDdb getInstance(Context context)
    {
        if (instance == null)
            instance = new WDdb(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_DICTIONARY);
        db.execSQL(CREATE_WORDS);
        db.execSQL(CREATE_WORDS_UPDATE_TRIGGER);
        db.execSQL(CREATE_WORDS_UPDATE_TRIGGER1);
        db.execSQL(CREATE_MEANINGS);
        db.execSQL(CREATE_EXAMPLES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

        if (!needUpdate(oldVersion, newVersion))
            return;



  /*      ContentValues cv = new ContentValues();
        cv.put( "language", "eng" );
        db.update( T_DICTIONARY, cv, null, null );

/*        String stmt = "alter table words add picture_file TEXT;";
        db.execSQL( stmt );

        stmt = "alter table words add sound_file TEXT;";
        db.execSQL( stmt );

        stmt = "alter table dictionary add type INTEGER NOT NULL DEFAULT 0;";
        db.execSQL( stmt ); */

        /*db.execSQL("DROP TABLE IF EXISTS examples");
        db.execSQL("DROP TABLE IF EXISTS meanings");
        db.execSQL("DROP TABLE IF EXISTS words");
        db.execSQL("DROP TABLE IF EXISTS dictionary");
        onCreate(db); */
    }

    private boolean needUpdate(int oldVersion, int newVersion)
    {
        return oldVersion < newVersion;
    }

    @Override
    public SQLiteDatabase getWritableDatabase()
    {
        SQLiteDatabase db = super.getWritableDatabase();
        Cursor crs = db.rawQuery("PRAGMA foreign_keys=ON", null);
        crs.close();
        return db;
    }
}

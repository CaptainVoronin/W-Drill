package org.sc.w_drill.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by maxsh on 30.09.2014.
 */
public class WDdb extends SQLiteOpenHelper
{
    public final static int SCHEME_VERSION = 10;

    public final static String T_DICTIONARY = "dictionary";
    public final static String T_WORDS = "words";
    public final static String T_MEANINGS = "meanings";
    public final static String T_EXAMPLE = "examples";

    final static String CREATE_DICTIONARY = "CREATE TABLE dictionary( id INTEGER PRIMARY KEY autoincrement, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "name TEXT NOT NULL UNIQUE, " +
            "language TEXT NOT NULL  );";

    final static String CREATE_WORDS = "CREATE TABLE " +
            "words( id INTEGER PRIMARY KEY autoincrement, " +
            "dict_id INTEGER NOT NULL, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "word TEXT NOT NULL UNIQUE, " +
            "transcription TEXT, " +
            "stage INTEGER NOT NULL DEFAULT 0, " +
            "percent INTEGER NOT NULL DEFAULT 0," +
            "created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "last_access TIMESTAMP," +
            "FOREIGN KEY(dict_id) REFERENCES dictionary( id ) ON DELETE CASCADE );";

    final static String CREATE_WORDS_UPDATE_TRIGGER = "CREATE TRIGGER au_words AFTER UPDATE ON words " +
            "FOR EACH ROW " +
            "BEGIN " +
            " update words set updated = CURRENT_TIMESTAMP where id = old.id;" +
            "END;";

    final static String CREATE_MEANINGS ="CREATE TABLE " +
            "meanings ( id INTEGER PRIMARY KEY autoincrement," +
            "word_id INTEGER NOT NULL," +
            "meaning TEXT NOT NULL," +
            "is_formal INTEGER NOT NULL DEFAULT 0," +
            "is_disapproving INTEGER NOT NULL DEFAULT 0," +
            "is_rude INTEGER NOT NULL DEFAULT 0," +
            "FOREIGN KEY ( word_id ) REFERENCES words ( id ) ON DELETE CASCADE );";

    final static String CREATE_EXAMPLES ="CREATE TABLE " +
            "examples ( id INTEGER PRIMARY KEY autoincrement," +
            "meaning_id INTEGER NOT NULL," +
            "example TEXT NOT NULL," +
            "FOREIGN KEY ( meaning_id ) REFERENCES meanings ( id ) ON DELETE CASCADE );";


    final static String DB_NAME = "wd.db";

    Context mContext;

    public WDdb( Context context )
    {
        super(context, DB_NAME, null, SCHEME_VERSION );
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(CREATE_DICTIONARY);
        db.execSQL(CREATE_WORDS);
        db.execSQL(CREATE_WORDS_UPDATE_TRIGGER);
        db.execSQL(CREATE_MEANINGS);
        db.execSQL(CREATE_EXAMPLES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //проверяете какая версия сейчас и делаете апдейт

        if( !needUpdate( oldVersion, newVersion ) )
            return;

        db.execSQL("DROP TABLE IF EXISTS examples");
        db.execSQL("DROP TABLE IF EXISTS meanings");
        db.execSQL("DROP TABLE IF EXISTS words");
        db.execSQL("DROP TABLE IF EXISTS dictionary");
        onCreate(db);
    }

    private boolean needUpdate( int oldVersion, int newVersion  )
    {
        return oldVersion < newVersion ;
    }
}

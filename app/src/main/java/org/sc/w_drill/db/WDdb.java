package org.sc.w_drill.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by maxsh on 30.09.2014.
 */
public class WDdb extends SQLiteOpenHelper
{
    public final static int SCHEME_VERSION = 5;

    public final static String T_DICTIONARY = "dictionary";
    public final static String T_WORDS = "words";

    final static String CREATE_DICTIONARY = "CREATE TABLE dictionary( id INTEGER PRIMARY KEY autoincrement, name TEXT NOT NULL UNIQUE, language TEXT NOT NULL  );";
    final static String CREATE_WORDS = "CREATE TABLE " +
            "words( id INTEGER PRIMARY KEY autoincrement, " +
            "dict_id INTEGER NOT NULL, " +
            "word TEXT NOT NULL UNIQUE, " +
            "stage INTEGER NOT NULL DEFAULT 0, " +
            " percent INTEGER NOT NULL DEFAULT 0," +
            "FOREIGN KEY(dict_id) REFERENCES dictionary( id ) ON DELETE CASCADE );";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //проверяете какая версия сейчас и делаете апдейт

        if( !needUpdate( oldVersion, newVersion ) )
            return;

        //db.execSQL("DROP TABLE IF EXISTS dictionary");
        onCreate(db);
    }

    private boolean needUpdate( int oldVersion, int newVersion  )
    {
        return oldVersion < newVersion ;
    }
}

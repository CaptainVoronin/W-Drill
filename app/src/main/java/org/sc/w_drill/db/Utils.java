package org.sc.w_drill.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by MaxSh on 10.11.2014.
 */
public class Utils {

    protected Utils(){

    }


    public static final void dumpQuery( SQLiteDatabase db, String statement )
    {
        Cursor crs = db.rawQuery( statement, null );

        if( crs.getCount() == 0 ) {
            crs.close();
            return;
        }

        String[] cols = crs.getColumnNames();

        StringBuilder buff = new StringBuilder();

        for( int i = 0; i < cols.length; i++ )
        {
            buff.append( cols[i] ).append( '\t' );
        }

        Log.d("QUERY DUMP", buff.toString());
        Log.d("QUERY DUMP", "_________________________________________________________________________________");

        while( crs.moveToNext() )
        {
            buff.setLength( 0 );
            for (int i = 0; i < cols.length; i++ )
            {
                buff.append( crs.getString( crs.getColumnIndex( cols[i] ) ) ).append( '\t' );
            }
            Log.d("QUERY DUMP", buff.toString());
        }
        crs.close();
    }
}

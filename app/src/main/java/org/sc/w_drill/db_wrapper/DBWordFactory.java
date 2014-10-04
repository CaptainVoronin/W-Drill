package org.sc.w_drill.db_wrapper;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;

import java.util.ArrayList;

/**
 * Created by maxsh on 30.09.2014.
 */
public class DBWordFactory
{
    static DBWordFactory instance = null;
    WDdb db;
    Dictionary dict;

    public static DBWordFactory getInstance( WDdb _db, Dictionary _dict )
    {
        if( instance == null )
            instance = new DBWordFactory( _db, _dict );

        return instance;
    }

    protected DBWordFactory(WDdb _db, Dictionary _dict)
    {
        db = _db;
        dict = _dict;
    }

    public ArrayList<IBaseWord> getBriefList()
    {
        return new ArrayList<IBaseWord>();
    }

    public ArrayList<IWord> getExtList()
    {
        return new ArrayList<IWord>();
    }

    public void delete( IBaseWord word )
    {
        throw new UnsupportedOperationException( "DbWordFactory::delete" );
    }

}

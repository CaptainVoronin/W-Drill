package org.sc.w_drill.dict;

import org.sc.w_drill.db_wrapper.DBWordFactory;

/**
 * Created by MaxSh on 07.10.2014.
 */
public class WordChecker
{
    public static boolean isCorrect( DBWordFactory factory, String word )
    {
        if( word == null )
            return false;

        if( word.length() == 0 )
            return false;

        if( factory.findWord( word ) )
            return false;

        return true;
    }
}

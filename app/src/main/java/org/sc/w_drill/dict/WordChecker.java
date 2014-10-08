package org.sc.w_drill.dict;

import org.sc.w_drill.db_wrapper.DBWordFactory;

/**
 * Created by MaxSh on 07.10.2014.
 */
public class WordChecker
{
    public static boolean isCorrect( DBWordFactory factory, IWord word )
    {
        if( word == null )
            return false;

        if( word.getWord().length() == 0 )
            return false;

        if( word.meanings() == null )
            return false;

        if( word.meanings().size() == 0 )
            return false;

        IMeaning meaning = word.meanings().get( 0 );

        if( meaning.meaning().length() == 0 )
            return false;

        if( factory.findWord( word.getWord() ) )
            return false;

        return true;
    }
}

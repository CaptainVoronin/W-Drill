package org.sc.w_drill.dict;

import org.sc.w_drill.db_wrapper.DBWordFactory;

/**
 * Created by MaxSh on 07.10.2014.
 */
public class WordChecker
{

    static char[] incorrectCharacters;

    static
    {
        incorrectCharacters = "<>\"".toCharArray();
    }

    public static void isCorrect(DBWordFactory factory, IWord word) throws MalformedWord, UniqueException, MeaningException
    {
        if (word == null)
            throw new MalformedWord();

        if (word.getWord().length() == 0)
            throw new MalformedWord();

        if( !checkChars( word.getWord() ) )
            throw new MalformedWord();

        //TODO: I'm not certain
        if (word.meanings() == null)
            throw new MeaningException();

        // TODO: That same case
        if (word.meanings().size() == 0)
            throw new MeaningException();

        IMeaning meaning = word.meanings().get(0);

        // TODO: And there
        if (meaning.meaning().length() == 0)
            throw new MeaningException();

        // YJis search is needed if only it's a new word
        // in this case it has id = -1
        if (word.getId() == -1)
            if (factory.findWord(word.getWord()))
                throw new UniqueException();
    }

    public static void isCorrect(DBWordFactory factory, String word) throws MalformedWord, UniqueException
    {
        if (word == null)
            throw new MalformedWord();

        if (word.length() == 0)
            throw new MalformedWord();

        if( !checkChars( word ) )
            throw new MalformedWord();

        if (factory.findWord(word))
            throw new UniqueException();
    }

    /**
     * It return true if string doesn't contain incorrect characters
     * and false otherwise
     * @param word
     * @return
     */
    private static boolean checkChars( String word )
    {
        for( int i = 0; i < word.length(); i++ )
            for( int j = 0; j < incorrectCharacters.length; j++ )
                if( word.charAt( i ) == incorrectCharacters[j] )
                    return false;

        return true;
    }
}
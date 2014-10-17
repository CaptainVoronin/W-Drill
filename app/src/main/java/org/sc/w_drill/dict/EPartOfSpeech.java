package org.sc.w_drill.dict;

/**
 * Created by maxsh on 30.09.2014.
 */
public enum EPartOfSpeech
{
    adj, adv, conj, interj, noun, prep, pron, verb;

    public static boolean check(String sample)
    {
        for( Object s : EPartOfSpeech.values() )
        {
            if( s.toString().equals( sample ) )
                return true;
        }

        return false;
    }
}

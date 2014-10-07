package org.sc.w_drill.dict;

import java.util.ArrayList;

/**
 * Created by Max on 10/4/2014.
 */
public class Word implements IWord
{
    int id;
    String word;
    ArrayList<IMeaning> meanings;
    String transcription;
    LearnState state;
    int percent;

    public Word ( String _word )
    {
        word = _word;
        id = -1;
    }

    public Word ( int _id, String _word )
    {
        word = _word;
        id = _id;
    }

    @Override
    public ArrayList<IMeaning> meanings()
    {
        return meanings;
    }

    public void addMeaning( IMeaning meaning )
    {
        if( meanings == null )
            meanings = new ArrayList<IMeaning>();

        meanings.add( meaning );
    }

    @Override
    public String transcription()
    {
        return transcription;
    }

    @Override
    public void setWord(String _word) {
        word = _word;
    }

    @Override
    public int getId()
    {
        return id;
    }

    public void setId( int _id )
    {
        id = _id;
    }

    @Override
    public String getWord()
    {
        return word;
    }

    @Override
    public int LearnPercent()
    {
        return 0;
    }

    public void setLearnPercent( int _percent )
    {
        percent = _percent;
    }

    @Override
    public LearnState getLearnState()
    {
        return state;
    }

    @Override
    public void setLearnState(LearnState _state)
    {
        state = _state;
    }

    public static Word getDummy()
    {
        return new Word( -1, "" );
    }

}

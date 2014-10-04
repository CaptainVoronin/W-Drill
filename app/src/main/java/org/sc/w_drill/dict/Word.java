package org.sc.w_drill.dict;

import java.util.ArrayList;

/**
 * Created by Max on 10/4/2014.
 */
public class Word implements IWord
{
    int id;
    String word;

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
        return null;
    }

    @Override
    public String transcription()
    {
        return null;
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

    @Override
    public LearnState getLearnState()
    {
        return null;
    }

    @Override
    public void setLearnState(LearnState state)
    {

    }
}

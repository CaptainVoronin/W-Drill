package org.sc.w_drill.dict;

/**
 * Created by Max on 10/4/2014.
 */
public class BaseWord implements IBaseWord
{
    int id;
    String word;
    int learnPercent;
    LearnState state;

    public BaseWord( int _id, String _word, int _learnPercent, LearnState _state )
    {
        id = _id;
        word = _word;
        learnPercent = _learnPercent;
        state = _state;
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

    @Override
    public void setId(int _id)
    {
        id = _id;
    }

    @Override
    public String getWord()
    {
        return word;
    }

    @Override
    public int getLearnPercent()
    {
        return learnPercent;
    }

    @Override
    public void setLearnPercent(int percent)
    {
        learnPercent = percent;
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

}

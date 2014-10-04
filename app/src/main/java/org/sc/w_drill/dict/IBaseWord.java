package org.sc.w_drill.dict;

/**
 * Created by maxsh on 30.09.2014.
 */
public interface IBaseWord
{
    public static enum LearnState { learn, check };
    public int getId();
    public void setId( int id );
    public String getWord();
    public int LearnPercent();
    public LearnState getLearnState();
    public void setLearnState( LearnState state );
}

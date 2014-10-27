package org.sc.w_drill.dict;

/**
 * Created by maxsh on 30.09.2014.
 */
public interface IBaseWord
{
    public void setWord(String word);
    public String getUUID();
    public void setUUID( String uuid );
    public static enum LearnState { learn, check };
    public int getId();
    public void setId( int id );
    public String getWord();
    public int getLearnPercent();
    public void setLearnPercent( int percent );
    public LearnState getLearnState();
    public void setLearnState( LearnState state );
    public void setAvgTime( int time );
    public int getAvgTime();
    public int getAccessCount();

    @Override
    public boolean equals( Object obj );
}

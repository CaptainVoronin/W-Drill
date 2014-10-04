package org.sc.w_drill.dict;

/**
 * Created by maxsh on 30.09.2014.
 */
public interface IMeaning
{
    public EPartOfSpeech partOFSpeech();
    public String translation();
    public boolean isFormal();
    public boolean isDisapproving();
    public boolean isRude();
    public String example();
}

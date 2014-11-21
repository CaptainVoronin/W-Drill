package org.sc.w_drill.dict;

import org.sc.w_drill.utils.DBPair;

import java.util.ArrayList;

/**
 * Created by maxsh on 30.09.2014.
 */
public interface IMeaning
{
    public String partOFSpeech();

    public void setPartOfSpeech(String part);

    public String meaning();

    public boolean isFormal();

    public boolean isDisapproving();

    public boolean isRude();

    public ArrayList<DBPair> examples();

    public int getId();
}

package org.sc.w_drill.dict;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by maxsh on 30.09.2014.
 */
public interface IWord extends IBaseWord
{
    public ArrayList<IMeaning> meanings();
    public String getTranscription();
    public void   setTranscription( String value );
    public DateTime getLastAccess();
    public void setLastAccess( DateTime dt );
    public DateTime getLastUpdate();
    public void setLastUpdate( DateTime dt );
}

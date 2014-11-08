package org.sc.w_drill.dict;

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
    public Date getLastAccess();
    public void setLastAccess( Date dt );
    public Date getLastUpdate();
    public void setLastUpdate( Date dt );
}

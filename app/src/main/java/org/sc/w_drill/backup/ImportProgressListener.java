package org.sc.w_drill.backup;

/**
 * Created by Max on 11/2/2014.
 */
public interface ImportProgressListener
{
    public final static int STATE_BEFORE_UNZIP = -1;
    public final static int STATE_LOAD_TEXT = -2;
    public final static int STATE_LOAD_DB = -3;

    public void setMaxValue(Integer value);

    public void setProgress(Integer progress);

    public void setState(int state);

}

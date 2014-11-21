package org.sc.w_drill.backup;

/**
 * Created by Max on 11/1/2014.
 */
public interface ExportProgressListener
{
    void setMaxValue(Integer max);

    void setCurrentProgress(Integer current);
}

package org.sc.w_drill.utils;

import java.math.BigDecimal;

/**
 * Created by Max on 11/4/2014.
 */
public class SizeUtils
{
    protected SizeUtils(){};

    public static final DisplaySize getDisplaySize( long size )
    {
        DisplaySize dsz = new DisplaySize();

        float scaler = 1;

        if( size < 1000 )
        {
            dsz.measure = DisplaySize.Measurement.b;
        }
        else if( size > 1000 && size < 1000000 )
        {
            scaler = 1000;
            dsz.measure = DisplaySize.Measurement.Kb;
        }
        else if( size > 1000000 && size < 1000000000 )
        {
            scaler = 1000000;
            dsz.measure = DisplaySize.Measurement.Mb;
        }
        else if( size > 1000000000 && size < 1000000000000. )
        {
            scaler = 1000000000;
            dsz.measure = DisplaySize.Measurement.Tb;
        }

        float sz = size / scaler;
        BigDecimal bd = new BigDecimal(Double.toString(sz));

        bd = bd.setScale( 2 , BigDecimal.ROUND_HALF_UP);

        dsz.size = ( float ) bd.doubleValue();
        return dsz;
    }
}

package org.sc.w_drill.utils;

import java.util.Calendar;

/**
 * Created by Max on 10/24/2014.
 */
public class DateTimeUtils
{
    protected DateTimeUtils()
    {

    }

    public static String getDateTimeString()
    {
        String str = "%d-%d-%d %d.%d";
        Calendar cl = Calendar.getInstance();
        return String.format( str, cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.MONTH ),
                cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.HOUR ),
                cl.get( Calendar.MINUTE ));
    }
}

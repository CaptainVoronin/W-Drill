package org.sc.w_drill.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
        String str = "%d-%02d-%02d %02d:%02d";
        Calendar cl = Calendar.getInstance();
        return String.format( str, cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.MONTH ),
                cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.HOUR ),
                cl.get( Calendar.MINUTE ));
    }

    public static String getDateTimeString( Date dt )
    {
        //String str = "%d-%d-%d %d.%d";
        String str = "%d-%02d-%02d %02d:%02d";
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis( dt.getTime() );

        return String.format( str, cl.get( Calendar.YEAR ),
                cl.get( Calendar.MONTH ),
                cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.HOUR_OF_DAY ),
                cl.get( Calendar.MINUTE ));
    }

    /**
     *
     * @param str date in format yyyy-mm-dd hh:mm:ss
     * @return
     */
    public static Date strToDate(String str)
    {
        Date dt = null;
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        try
        {
            dt = sdf.parse( str );
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return dt;
    }
}

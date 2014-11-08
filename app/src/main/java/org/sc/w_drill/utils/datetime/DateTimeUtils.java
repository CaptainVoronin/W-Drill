package org.sc.w_drill.utils.datetime;

import android.content.Context;

import org.sc.w_drill.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

    public static String mkFilenameForTime()
    {
        String str = "%d-%02d-%02d %02d.%02d";
        Calendar cl = Calendar.getInstance();
        return String.format( str, cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.MONTH ),
                cl.get( Calendar.DAY_OF_MONTH ),
                cl.get( Calendar.HOUR ),
                cl.get( Calendar.MINUTE ));
    }

    public static String getSQLDateTimeString( Date dt )
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        return sdf.format( dt ).toString();
    }

    public static String getDateTimeString( Date dt, boolean seconds )
    {

        String str;

        if( seconds )
            str = getSQLDateTimeString(  dt );
        else
        {
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm" );
            str = sdf.format( dt );
        }
        return str;
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
        TimeZone tz = TimeZone.getDefault();
        sdf.setTimeZone( tz );
        try
        {
            dt = sdf.parse( str );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return dt;
    }

    public static String timeIntervalToString( Context context, Date dt )
    {
        Date now = new Date();

        long diff = now.getTime() - dt.getTime();

        TimeInterval ti = calculateTimeInterval( diff );

        try
        {
            return intervalToString(context, ti);
        } catch ( IntervalToBigException ex )
        {
            return getDateTimeString( dt, false );
        }
    }

    public static String intervalToString(Context context, TimeInterval ti) throws IntervalToBigException
    {
       String message = "";

        if( ti.hours > 20 )
        {
            ti.days++;
            ti.hours = 0;
        }

        // I don't want convert intervals which are longer than 20 lays
        if( ti.days > 20 )
            throw new IntervalToBigException();
        else if( ti.days >= 2  )
            message = String.format( "%d %s", ti.days, getDayWord( context, ti.days ) );
        else if( ti.days == 1 )
            if( ti.hours > 6 )
                message = String.format( "%d %s %d %s", ti.days, getDayWord( context, ti.days ),
                                                        ti.hours, getHourWord(context, ti.hours));
            else
                message = String.format( "%d %s", ti.days, getDayWord( context, ti.days ) );
        else
        if( ti.hours >= 1)
            message = String.format( context.getString( R.string.about_one_hour ) );
        else
            message = String.format( "%d %s", ti.hours, getHourWord( context, ti.hours ) );

        return message;
    }

    private  static TimeInterval calculateTimeInterval( long interval )
    {
        TimeInterval ti = new TimeInterval();

        float days = interval / (24f * 60 * 60 * 1000);
        float dummy = days - ( float ) Math.floor( days );
        float hours = dummy * 24f;
        ti.days = ( int ) Math.floor( days );
        ti.hours = ( int ) Math.floor( hours );
        return ti;
    }

    private static String getHourWord( Context context, int val )
    {
        String message = "";

        if( val == 1 )
            message = context.getString( R.string.hour );
        else if( val >= 2 && val < 5)
            message = context.getString( R.string.hours1 );
        else
            message = context.getString( R.string.hours2 );

        return message;
    }

    private static String getDayWord( Context context, int val )
    {
        String message = "";

        if( val == 1 )
            message = context.getString( R.string.day );
        else if( val >= 2 && val < 5 )
            message = context.getString( R.string.days1 );
        else
            message = context.getString( R.string.days2 );

        return message;
    }

    private  static String getMonthWord( Context context, int val )
    {
        String message = "";

        if( val == 1 )
            message = context.getString( R.string.month );
        else if( val >= 2 && val < 5 )
            message = context.getString( R.string.months1 );
        else
            message = context.getString( R.string.months2 );

        return message;
    }
}

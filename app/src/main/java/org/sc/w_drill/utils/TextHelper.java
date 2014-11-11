package org.sc.w_drill.utils;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by MaxSh on 10.11.2014.
 */
public class TextHelper
{

    protected TextHelper()
    {}

    public static String decorate( String text, String colorCode )
    {
        String[] arr = splitWithBrackets( text );
        StringBuilder buff = new StringBuilder();

        for( int i = 0; i < arr.length; i++ )
        {
            if( arr[i].startsWith( "[") )
                buff.append( "<i><font color=\"" + colorCode + "\">" ).append( arr[i]).append( "</font></i>" );
            else
                buff.append( arr[i] );
        }

        return buff.toString();
    }


    public static final boolean compare( String pattern, String text )
    {

        String[] textTokens = text.split( "\\[(.*?)\\]" );

        StringBuilder buff1 = new StringBuilder();

        for( int i = 0; i < textTokens.length; i++ )
        {
            StringTokenizer st = new StringTokenizer( textTokens[i] );
            while( st.hasMoreElements() )
                buff1.append(st.nextToken());
        }

        StringBuilder buff2 = new StringBuilder();

        String[] patTokens = pattern.split( "\\[(.*?)\\]" );

        for( int i = 0; i < patTokens.length; i++ )
        {
            StringTokenizer st = new StringTokenizer( patTokens[i] );
            while( st.hasMoreElements() )
                buff2.append(st.nextToken());
        }

        return buff1.toString().equals( buff2.toString() );
    }

    public static final String[] splitWithBrackets( String text )
    {
        ArrayList<StringBuilder> buffs = new ArrayList<StringBuilder>();
        char[] chars = new char[text.length()];

        text.getChars( 0, text.length(), chars, 0 );

        int pos = 0;
        int length = text.length();

        StringBuilder buff = new StringBuilder();
        buffs.add( buff );
        while( pos < length )
        {
            if( chars[pos] == '[')
            {
                buff = new StringBuilder();
                buff.append( chars[pos] );
                buffs.add( buff );
            } else if( chars[pos] == ']' )
            {
                buff.append( chars[pos] );
                buff = new StringBuilder();
                buffs.add( buff );
            }else
                buff.append( chars[pos] );
            pos++;
        }
        int cnt = 0;

        for( int i = 0; i < buffs.size(); i++ )
            if( buffs.get( i ).toString().trim().length() != 0 )
                cnt++;

        String[] arr = new String[cnt];
        cnt = 0;
        for( int i = 0; i < buffs.size(); i++ )
            if( buffs.get( i ).toString().trim().length() != 0 )
            {
                arr[cnt] = buffs.get(i).toString();
                cnt++;
            }

        return arr;
    }
}

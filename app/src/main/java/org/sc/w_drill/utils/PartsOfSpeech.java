package org.sc.w_drill.utils;

import android.content.Context;

import org.sc.w_drill.R;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by Max on 10/17/2014.
 */
public class PartsOfSpeech
{
    Context context;

    private static PartsOfSpeech instance;

    Hashtable<String, String> parts;

    static
    {
        instance = null;
    }

    public static PartsOfSpeech getInstance(Context _context)
    {
        if (instance == null)
            instance = new PartsOfSpeech(_context);
        return instance;
    }

    private PartsOfSpeech(Context _context)
    {
        context = _context;

        init();
    }

    void init()
    {
        parts = new Hashtable<String, String>();
        parts.put("noun", context.getString(R.string.noun));
        parts.put("adj", context.getString(R.string.adj));
        parts.put("verb", context.getString(R.string.verb));
        parts.put("adv", context.getString(R.string.adv));
        parts.put("pron", context.getString(R.string.pron));
        parts.put("conj", context.getString(R.string.conj));
        parts.put("interj", context.getString(R.string.interj));
    }

    public String getCode(String name)
    {
        String code = null;

        if (parts.containsValue(name))
        {
            Enumeration<String> en = parts.keys();
            while (en.hasMoreElements())
            {
                String key = en.nextElement();
                String str = parts.get(key);

                if (name.equals(str))
                {
                    code = key;
                    break;
                }
            }
        }
        return code;
    }

    public String getName(String code)
    {
        if (code == null)
            return "";
        else
            return parts.get(code);
    }

    public String[] getNames()
    {
        String[] arr = new String[parts.size()];
        Enumeration<String> en = parts.elements();
        int i = 0;
        while (en.hasMoreElements())
        {
            String str = en.nextElement();
            arr[i] = str;
            i++;
        }

        return arr;
    }

    public int indexOf(String code)
    {
        int index = 0;
        Enumeration<String> en = parts.keys();

        while (en.hasMoreElements())
        {
            if (en.nextElement().toString().equals(code))
                return index;
            else
                index++;
        }
        return -1;
    }
}
package org.sc.w_drill;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.Langs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 11/3/2014.
 */
public class DictListAdapter extends ArrayAdapter<Dictionary>
{
    public enum ListForm { SHORT, FULL };

    ListForm form;

    ArrayList<Dictionary> dictList;
    Context context;
    Langs langs;
    boolean shortForm = false;

    public DictListAdapter(Context _context, List objects, ListForm _form )
    {
        super(_context, R.layout.row_dict_list, objects);

        dictList = ( ArrayList<Dictionary>) objects;

        context = _context;

        langs = Langs.getInstance( context );

        form = _form;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent )
    {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView;

        Dictionary dict = dictList.get(position);

        if( form.equals( ListForm.FULL ) )
        {
            rowView = inflater.inflate(R.layout.row_dict_list, parent, false);

            TextView lbName = (TextView) rowView.findViewById(R.id.row_dict_list_dict_name);
            TextView lbLang = (TextView) rowView.findViewById(R.id.row_dict_list_dict_lang);
            TextView lbWordsCount = (TextView) rowView.findViewById(R.id.row_dict_list_dict_word_count);
            lbName.setText(dict.getName());
            lbLang.setText(langs.get(dict.getLang()));
            lbWordsCount.setText(context.getString(R.string.row_dict_list_dict_word_count, dict.getWordCount()));
        }
        else
        {
            rowView = inflater.inflate(R.layout.row_short_dict_list, parent, false);
            TextView lbName = (TextView) rowView.findViewById(R.id.tvDictName);
            lbName.setText(dict.getName());
        }

        rowView.setTag(dict);
        return rowView;
    }
}

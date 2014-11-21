package org.sc.w_drill;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.TextView;

import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.Langs;
import org.sc.w_drill.utils.MessageDialog;

import java.util.HashMap;

/**
 * The autocomplete text editor was created using an approach that is
 * described here:
 * http://drzon.net/how-to-create-a-clearable-autocomplete-dropdown-with-autocompletetextview/
 */

public class DlgDictionary extends Dialog implements android.view.View.OnClickListener
{
    public Button btnOk, btnCancel;
    EditText edName;
    AutoCompleteTextView edSearch;
    LangsAdapter langsAdapter;

    OnDictionaryOkClickListener listener;
    Langs langs;

    public DlgDictionary(Context context)
    {
        super(context);
        langs = Langs.getInstance(context);
    }

    @Override
    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);
        setTitle(R.string.new_dictionary_comment);
        setContentView(R.layout.dlg_new_dictionary);

        edName = (EditText) findViewById(R.id.edName);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);
        btnOk = (Button) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(this);
        String[] langNames = getContext().getResources().getStringArray(R.array.languages);

        edSearch = (AutoCompleteTextView) findViewById(R.id.edSearch);

        langsAdapter = new LangsAdapter(getContext(), langs);
        edSearch.setAdapter(langsAdapter);
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.btnCancel)
            dismiss();
        else
        {
            processOkBtn();
        }
    }

    private void processOkBtn()
    {
        String name = edName.getText().toString();
        String langName = edSearch.getText().toString();

        Langs langs = Langs.getInstance(getContext());
        String code = langs.getCode(langName);

        if (code == null)
        {
            //TODO: Handle incorrect input
            return;
        }

        dismiss();
        if (checkDictionaryValues(name, code))
        {
            Dictionary newDict = DBDictionaryFactory.getInstance(getContext()).createNew(name, code);
            if (listener != null)
                listener.onNewDictOkClick(newDict.getId());
        }
        else
            MessageDialog.showError(getContext(), R.string.dict_name_already_exists, null, null);
    }

    public interface OnDictionaryOkClickListener
    {
        public void onNewDictOkClick(int dictId);
    }

    public void setOkListsner(OnDictionaryOkClickListener _listener)
    {
        listener = _listener;
    }

    private boolean checkDictionaryValues(String name, String lang)
    {
        if (name.length() == 0)
        {
            MessageDialog.showError(getContext(), R.string.incorrect_name, null, null);
            return false;
        }

        // проверить на повторяемость названия
        DBDictionaryFactory factory = DBDictionaryFactory.getInstance(getContext());
        boolean res = factory.checkDuplicate(name);
        return res;
    }

    class LangsAdapter extends ArrayAdapter<String>
    {
        Langs _langs;
        Context context;
        Object[] keyArray;
        Filter filter;
        HashMap<String, String> subset;

        public LangsAdapter(Context _context, Langs _langs)
        {
            super(_context, R.layout.row_lang_list, _langs.getList());
            langs = _langs;
            context = _context;
            keyArray = langs.keysArray();
            subset = langs.getSubset(null);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_lang_list, parent, false);

            String l = subset.get(keyArray[position].toString());

            TextView tv = (TextView) rowView.findViewById(R.id.tvLangDispName);
            tv.setText(l);
            tv.setTag(keyArray[position]);
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {

            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.row_lang_list, parent,
                    false);
            TextView tv = (TextView) row.findViewById(R.id.tvLangDispName);
            tv.setText(subset.get(keyArray[position].toString()));
            return row;
        }

        @Override
        public Filter getFilter()
        {
            if (filter == null)
                filter = new LangFilter();
            return filter;
        }

        public void setSubset(HashMap<String, String> _subset)
        {
            subset = _subset;
            keyArray = subset.keySet().toArray();
            addAll(subset.values());
            //notifyDataSetChanged();
        }

    }

    private class LangFilter extends Filter
    {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence)
        {
            FilterResults result = new FilterResults();
            if (charSequence == null || charSequence.length() == 0)
            {
                HashMap<String, String> res = langs.getSubset(null);
                result.values = res;
                result.count = res.size();
            }
            else
            {
                String search = charSequence.toString();
                HashMap<String, String> res = langs.getSubset(search);
                result.values = res;
                result.count = res.size();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults results)
        {
            langsAdapter.clear();
            langsAdapter.setSubset((HashMap<String, String>) results.values);
        }
    }
}
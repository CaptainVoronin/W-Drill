package org.sc.w_drill;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.sql.SQLDataException;
import java.util.ArrayList;

/**
 * Created by Max on 11/3/2014.
 */
public class DialogSelectDict extends Dialog
{
    Dictionary excludeDict;
    Context context;
    DictionaryDialogListener listener;
    Dictionary selectedDict = null;
    protected ArrayList<Dictionary> dicts;

    public DialogSelectDict(Context _context, Dictionary _excludeDict, DictionaryDialogListener _listener)
    {
        super(_context);
        excludeDict = _excludeDict;
        context = _context;
        listener = _listener;
    }

    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);
        setTitle(R.string.txt_dlg_select_dict);
        setContentView(R.layout.dlg_select_dict);
        setCancelable(true);
        try
        {
            fillList();
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
        }
    }

    private void fillList() throws SQLDataException
    {
        dicts = DBDictionaryFactory.getInstance(getContext()).getList();
        dicts.remove(excludeDict);
        DictListAdapter adapter = new DictListAdapter(context, dicts, DictListAdapter.ListForm.SHORT);
        ListView list = (ListView) findViewById(R.id.listDicts);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new OnDictItemClickListener());
    }

    public Dictionary getSelectedDictionary()
    {
        return selectedDict;
    }

    class OnDictItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            selectedDict = dicts.get(i);
            dismiss();
            if (listener != null)
                listener.onDictSelected(selectedDict);
        }
    }

    public void onBackPressed()
    {
        super.onBackPressed();
        if (listener != null)
            listener.onCanceled();
    }

    public interface DictionaryDialogListener
    {
        public void onDictSelected(Dictionary dict);

        public void onCanceled();
    }
}
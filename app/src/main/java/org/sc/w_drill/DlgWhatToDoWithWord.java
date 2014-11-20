package org.sc.w_drill;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.MessageDialog;

import java.sql.SQLDataException;
import java.util.ArrayList;

/**
 * Created by MaxSh on 20.11.2014.
 */
public class DlgWhatToDoWithWord extends Dialog implements View.OnClickListener, AdapterView.OnItemClickListener
{

    WhatToDoWithWordListener listener;
    WDdb database;
    protected ArrayList<Dictionary> dicts;

    public DlgWhatToDoWithWord(Context context, WhatToDoWithWordListener _listener)
    {
        super(context);
        listener = _listener;
    }

    @Override
    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);
        setTitle(R.string.txt_dlg_what_to_do_with_word);
        setContentView(R.layout.dlg_what_to_to_with_word);
        setCancelable(true);
        Button btn = ( Button ) findViewById( R.id.btnSaveAsIs );
        btn.setOnClickListener( this );
        try
        {
            fillList();
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
            MessageDialog.showError( getContext(), e.getMessage(), null, null );
        }
    }

    private void fillList() throws SQLDataException
    {
        database = new WDdb(getContext());
        dicts = DBDictionaryFactory.getInstance(database).getList();
        DictListAdapter adapter = new DictListAdapter( getContext(), dicts, DictListAdapter.ListForm.SHORT);
        ListView list = (ListView) findViewById(R.id.listDictionaries);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        dismiss();
        if( listener != null )
            listener.onSaveAsIs();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        dismiss();
        Dictionary dict = dicts.get(i);
        if (listener != null)
            listener.saveInDictionary(dict);
    }

    public interface WhatToDoWithWordListener
    {
        public void onSaveAsIs();
        public void saveInDictionary( Dictionary dict );
    }
}

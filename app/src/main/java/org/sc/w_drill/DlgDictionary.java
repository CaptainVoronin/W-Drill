package org.sc.w_drill;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

/**
 * Created by MaxSh on 13.10.2014.
 */
public class DlgDictionary extends Dialog implements android.view.View.OnClickListener
{
    public Button btnOk, btnCancel;
    EditText edName;
    Spinner spin;
    WDdb database;

    OnDictionaryOkClickListener listener;

    public DlgDictionary(Context context )
    {
        super(context);
        database = new WDdb( context );
    }

    @Override
    public void onCreate( Bundle savedInstance )
    {
        super.onCreate(savedInstance);
        setTitle( R.string.new_dictionary_comment );
        setContentView(R.layout.dlg_new_dictionary);

        edName = ( EditText ) findViewById( R.id.edName );
        spin = (Spinner) findViewById( R.id.listLangs );
        btnCancel = ( Button ) findViewById( R.id.btnCancel );
        btnCancel.setOnClickListener( this );
        btnOk = ( Button ) findViewById( R.id.btnOk );
        btnOk.setOnClickListener( this );
        String[] langNames = getContext().getResources().getStringArray(R.array.languages);
        spin.setAdapter( new ArrayAdapter<String>( getContext(), android.R.layout.simple_list_item_1, langNames ) );
    }

    @Override
    public void onClick(View view)
    {
        if( view.getId() == R.id.btnCancel )
            dismiss();
        else {
            processOkBtn();
        }
    }

    private void processOkBtn()
    {
        String name = edName.getText().toString();
        String lang = spin.getSelectedItem().toString();
        dismiss();
        if( checkDictionaryValues( name, lang ) )
        {
            Dictionary newDict = DBDictionaryFactory.getInstance( database ).createNew( name, lang );
            if( listener != null )
                listener.onNewDictOkClick(newDict.getId());
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
            builder.setMessage(R.string.dict_name_already_exists).setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            });

            builder.setCancelable( true );
            builder.create();
            builder.show();
        }
    }

    public interface OnDictionaryOkClickListener
    {
        public void onNewDictOkClick(int dictId);
    }

    public void setOkListsner( OnDictionaryOkClickListener _listener )
    {
        listener = _listener;
    }

    private boolean checkDictionaryValues( String name, String lang )
    {
        if( name.length() == 0 )
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
            builder.setMessage(R.string.incorrect_name).setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            });

            builder.setCancelable( true );
            builder.create();
            builder.show();
            return false;
        }

        // проверить на повторяемость названия
        DBDictionaryFactory factory = DBDictionaryFactory.getInstance( database );
        boolean res = factory.checkDuplicate( name  );
        return res;
    }
}

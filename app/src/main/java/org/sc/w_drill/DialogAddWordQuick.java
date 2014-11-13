package org.sc.w_drill;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.db_wrapper.DefaultDictionary;

/**
 * Created by MaxSh on 13.11.2014.
 */
public class DialogAddWordQuick extends Dialog implements View.OnClickListener
{
    WDdb database;

    public DialogAddWordQuick(Context context )
    {
        super(context);
        database = new WDdb( context );
    }

    public void onCreate( Bundle savedInstance )
    {
        super.onCreate(savedInstance);
        setTitle(R.string.txt_dlg_add_word_quick);
        setContentView(R.layout.dlg_add_word_quick);
        setCancelable( true );

        Button btn = ( Button ) findViewById( R.id.btnOk );
        btn.setOnClickListener( this );
        btn = ( Button ) findViewById( R.id.btnCancel );
        btn.setOnClickListener( this );
    }

    @Override
    public void onClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.btnCancel:
                dismiss();
                break;
            case R.id.btnOk:
                if( !checkInput() )
                    showError();
                else {
                    dismiss();
                    insertWord();
                }
                break;
        }
    }

    private void showError()
    {

    }

    private boolean checkInput()
    {
        String word = ( ( EditText ) findViewById( R.id.edWord ) ).getText().toString();
        if( word.trim().length() == 0 )
            return false;

        return !DBWordFactory.getInstance( database,
                        DefaultDictionary.getInstance( database ).getDictionary() ).findWord( word );

    }

    private void insertWord()
    {
        String word = ( ( EditText ) findViewById( R.id.edWord ) ).getText().toString();
        try {
            DefaultDictionary.getInstance( database ).addWord( word );
        } catch (Exception e)
        {
            e.printStackTrace();
            showError();
        }
    }
}

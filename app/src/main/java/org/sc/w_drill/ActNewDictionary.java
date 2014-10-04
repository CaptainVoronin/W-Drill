package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

public class ActNewDictionary extends ActionBarActivity
{
    public static final String CANCELABLE_PAR_NAME = "CAN_CANCEL";
    public static final String NAME_CORRECT = "NAME_CORRECT";
    public static final String NAME_UNIQUE = "NAME_UNIQUE";
    public static final String SAVED = "SAVED";

    Spinner spin;

    WDdb db;

    Dictionary newDict;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_dictionary);

        WDdb db = new WDdb( getApplicationContext() );

        spin = ( Spinner ) this.findViewById( R.id.listLanguages );

        String[] langNames = getResources().getStringArray( R.array.languages );

        spin.setAdapter( new ArrayAdapter<String>( getApplicationContext(), android.R.layout.simple_list_item_1, langNames ) );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_new_dictionary, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void createNewDictionary(View view)
    {
        if( checkValues() )
        {
            String name = ((EditText) this.findViewById( R.id.ed_new_dict_name )).getText().toString();
            String lang = spin.getSelectedItem().toString();
            newDict = DBDictionaryFactory.getInstance( db ).createNew( name, lang );
            Intent resultData = new Intent();
            resultData.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, Integer.valueOf(newDict.getId()) );
            setResult(Activity.RESULT_OK, resultData);
            finish();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( this );
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

    /**
     * It checks the name for doubles
     * @return true - if everything is OK, false - otherwise
     */
    private boolean checkValues()
    {
        String name = ((EditText) this.findViewById( R.id.ed_new_dict_name )).getText().toString();
        if( name.length() == 0 )
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( this );
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
        DBDictionaryFactory factory = DBDictionaryFactory.getInstance(db);
        boolean res = factory.checkDuplicate( name  );
        return res;
    }

    public void cancel(View view)
    {
        setResult(Activity.RESULT_CANCELED, null );
        finish();
    }

    public void onBackPressed()
    {
        setResult(Activity.RESULT_CANCELED, null );
        finish();
    }
}
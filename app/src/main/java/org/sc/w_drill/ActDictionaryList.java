package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.sc.w_drill.backup.BackupHelper;
import org.sc.w_drill.backup.RestoreHelper;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActDictionaryList extends ActionBarActivity implements DlgDictionary.OnDictionaryOkClickListener
{

    public static final int CHOOSE_FILE_CODE = Activity.RESULT_FIRST_USER + 1;

    WDdb db;

    protected ArrayList<Dictionary> dicts;

    ListView listDicts;

    DistListAdapter adapter = null;

    Dictionary dictForOperation = null;

    int activeDictId;

    boolean activeDictDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary_list);

        db = new WDdb( getApplicationContext() );
        Intent data = getIntent();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if( data != null )
            activeDictId = data.getIntExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1 );
        prepareList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_dictionary_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_new_dictionary)
        {
            goShowNewDictionaryDialog();
            return true;
        }
        else
         if( id == R.id.action_load_dictionary )
         {
             chooseZipFile();
         }
        return super.onOptionsItemSelected(item);
    }

    private void chooseZipFile()
    {
        openFile("*/*");
    }

    private void exportDictionary()
    {
        Context context = getApplicationContext();
        File dir= Environment.getExternalStorageDirectory();

        File destdir = new File( dir.getPath() + File.separator + "Scholar" );
        if( !destdir.exists() )
            destdir.mkdir();

        try
        {
            BackupHelper.backup(context, destdir.getPath(), dictForOperation);
            showMessage( getString( R.string.txt_dict_export_complete, destdir.getPath()
                                                                        + File.separator
                                                                        + dictForOperation.getName()
                                                                        + ".zip" ) );
        } catch( Exception ex )
        {
            ex.printStackTrace();
            showError( ex.getMessage() );
        }
    }

    private void showMessage(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( message ).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {

            }
        });
        builder.setTitle( R.string.txt_dictionary_export );
        builder.setIcon( android.R.drawable.ic_dialog_info );
        builder.setCancelable( true );
        builder.create();
        builder.show();

    }

    private void showError(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( message ).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {

            }
        });
        builder.setTitle( R.string.txt_dictionary_export );
        builder.setIcon( android.R.drawable.ic_dialog_alert );
        builder.setCancelable( true );
        builder.create();
        builder.show();
    }

    /**
     * The function call ActWordList and pesses
     * ID of the selected dictionary
     * @param dict
     */
    protected void setActiveDictAndGotoWordList( Dictionary dict )
    {
        Intent resultData = new Intent( );
        resultData.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, Integer.valueOf(dict.getId()) );
        setResult(Activity.RESULT_OK, resultData);
        finish();
    }

    /**
     * This function makes a transition to an activity,
     * which creates a new dictionary.
     */
    protected void goShowNewDictionaryDialog()
    {
        DlgDictionary dlg = new DlgDictionary( this );
        dlg.setOkListsner( this );
        dlg.show();
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        switch( requestCode )
        {
            case CHOOSE_FILE_CODE:

                if( data != null )
                {
                    Uri retUri = data.getData();
                    File f = new File( retUri.getPath() );
                    loadFile(f);
                }

                break;
            default:
                break;
        }
    }

    private void loadFile(File file)
    {
        RestoreHelper rh = new RestoreHelper( new WDdb( this ) );
        try
        {
            rh.load(this, file);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void prepareList()
    {
        listDicts = (ListView) findViewById( R.id.listDictionaries );

        dicts = DBDictionaryFactory.getInstance( db ).getList();

        // If we have some dictionaries, we will use a the real adapter
        if( dicts.size() != 0 )
        {
            adapter = new DistListAdapter( this, dicts );
            listDicts.setAdapter( adapter );
        }
        else // In other case we use fictive
        {
            String[] stringList = new String[1];
            stringList[0] = getApplicationContext().getString( R.string.txt_no_dicts_at_all);
            listDicts.setAdapter( new ArrayAdapter<String>( getApplicationContext(), android.R.layout.simple_list_item_1 ) );
        }

        listDicts.setOnItemClickListener(new OnDictItemClickListener());
        listDicts.setOnItemLongClickListener(new OnDictItemLongClickListener());
    }

    @Override
    public void onNewDictOkClick(int dictId)
    {
        prepareList();
    }

    /**
     * Адаптер списка словарей
     */
    class DistListAdapter extends ArrayAdapter<Dictionary>
    {
        ArrayList<Dictionary> dictList;
        Context context;
        public DistListAdapter(Context _context, List objects)
        {
            super(_context, R.layout.row_dict_list, objects);

            dictList = ( ArrayList<Dictionary>) objects;

            context = _context;
        }
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_dict_list, parent, false);

            TextView lbName = ( TextView ) rowView.findViewById( R.id.row_dict_list_dict_name );
            TextView lbLang = ( TextView ) rowView.findViewById( R.id.row_dict_list_dict_lang );
            TextView lbWordsCount = ( TextView ) rowView.findViewById( R.id.row_dict_list_dict_word_count );
            Dictionary dict = dictList.get(position);

            lbName.setText( dict.getName() );
            lbLang.setText( dict.getLang() );
            lbWordsCount.setText( ActDictionaryList.this.getString( R.string.row_dict_list_dict_word_count, dict.getWordCount() ));
            rowView.setTag( dict );

            return rowView;
        }
    }

    class OnDictItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            // A simple click chooses an active dictionary
            // consequently after the a click we set up the selected
            // dictionary as active and should go to its word listDicts.
            if( dicts.size() != 0 ) {
                Dictionary dict = dicts.get(i);
                Log.d("[ActDictionaryList::OnDictItemClickListener::onItemClick]", "Selected " + dict.getName());
                setActiveDictAndGotoWordList(dict);
            }
            // There aren't any dictionaries and we must create one therefore
            // a simple click should send us to ActNewDictionary activity
            else
            {
                goShowNewDictionaryDialog();
            }
        }
    }

    class OnDictItemLongClickListener implements AdapterView.OnItemLongClickListener
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            dictForOperation = ( Dictionary ) view.getTag();

            PopupMenu popup = new PopupMenu( ActDictionaryList.this, view);
            popup.getMenuInflater().inflate(R.menu.dict_list_popup_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch( item.getItemId() )
                    {
                        case R.id.action_move_and_delete:
                            dictForOperation = null;
                            return false;
                        case R.id.action_delete:
                            if( dictForOperation != null )
                                preDeleteDict( dictForOperation.getName() );
                            return true;
                        case R.id.action_export:
                            exportDictionary();
                            return true;
                    }
                    return true;
                }
            });
            popup.show();
            return true;
        }
    }

    private void preDeleteDict( String name )
    {
        String message = getString( R.string.delete_dictionary_question, name );

        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( message ).setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled the dialog
            }
        }).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                deleteDictionary();
           }
        });

        builder.setCancelable( true );
        builder.create();
        builder.show();
    }

    private void deleteDictionary()
    {
        if( dictForOperation != null )
            DBDictionaryFactory.getInstance( db ).delete(dictForOperation);
        if( dictForOperation.getId() == activeDictId )
            activeDictDeleted = true;

        dictForOperation = null;

        prepareList();
    }

    public void onBackPressed()
    {
        if( activeDictDeleted )
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( this );
            builder.setMessage( R.string.txt_have_to_choose_dict).setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            }).setCancelable( true ).create().show();
        }
        else
            super.onBackPressed();
    }

    public void openFile(String minmeType)
    {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(minmeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        // if you want any file type, you can skip next line
        sIntent.putExtra("CONTENT_TYPE", minmeType);
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        }
        else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try {
            startActivityForResult(chooserIntent, CHOOSE_FILE_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }
}
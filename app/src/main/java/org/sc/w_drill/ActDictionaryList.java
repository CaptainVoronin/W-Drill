package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.MessageDialog;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;

import java.io.File;
import java.sql.SQLDataException;
import java.util.ArrayList;

import static java.lang.System.exit;

public class ActDictionaryList extends ActionBarActivity implements DlgDictionary.OnDictionaryOkClickListener
{

    public static final int CHOOSE_FILE_CODE = Activity.RESULT_FIRST_USER + 1;
    public static final int CODE_ActImportDictionary = 12334;

    protected ArrayList<Dictionary> dicts;

    ListView listDicts;

    DictListAdapter adapter = null;

    Dictionary dictForOperation = null;

    int activeDictId;

    boolean activeDictDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary_list);
        Intent data = getIntent();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if (data != null)
            activeDictId = data.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
        prepareList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_dictionary_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_new_dictionary)
        {
            goShowNewDictionaryDialog();
            return true;
        }
        else if (id == R.id.action_load_dictionary)
        {
            chooseZipFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void chooseZipFile()
    {
        openFile("*/*");
    }

    private void exportDictionary()
    {
        int id = dictForOperation.getId();
        Intent intent = new Intent(this, ActExportDictionary.class);


        intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, id);

        startActivity(intent);
    }

    /*private void showMessage(String message)
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

    } */

    private void showError(String message)
    {
        String title = getString(R.string.txt_dictionary_export);
        MessageDialog.showError(this, message, null, title);
    }

    /**
     * The function call ActWordList and pesses
     * ID of the selected dictionary
     *
     * @param dict
     */
    protected void setActiveDictAndGotoWordList(Dictionary dict)
    {
        Intent resultData = new Intent();
        resultData.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, Integer.valueOf(dict.getId()));
        setResult(Activity.RESULT_OK, resultData);
        finish();
    }

    /**
     * This function makes a transition to an activity,
     * which creates a new dictionary.
     */
    protected void goShowNewDictionaryDialog()
    {
        DlgDictionary dlg = new DlgDictionary(this);
        dlg.setOkListsner(this);
        dlg.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case CHOOSE_FILE_CODE:

                if (data != null)
                {
                    Uri retUri = data.getData();
                    File f = new File(retUri.getPath());
                    loadFile(f);
                }

                break;
            case CODE_ActImportDictionary:
                if (resultCode == Activity.RESULT_OK)
                    prepareList();
                break;
            default:
                break;

        }
    }

    private void loadFile(File file)
    {
        Intent intent = new Intent(this, ActImportDictionary.class);

        intent.putExtra(ActImportDictionary.SRC_FILE_PARAM_NAME, file.getPath());

        startActivityForResult(intent, CODE_ActImportDictionary);
    }

    private void prepareList()
    {
        listDicts = (ListView) findViewById(R.id.listDictionaries);


        try
        {
            dicts = DBDictionaryFactory.getInstance(this).getList( -1 );
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
            showError(getString(R.string.txt_fatal_internal_error, e.getMessage()));
            exit(-1);
        }

        // If we have some dictionaries, we will use a the real adapter
        if (dicts.size() != 0)
        {
            adapter = new DictListAdapter(this, dicts, DictListAdapter.ListForm.FULL);
            listDicts.setAdapter(adapter);
        }
        else // In other case we use fictive
        {
            String[] stringList = new String[1];
            stringList[0] = getApplicationContext().getString(R.string.txt_no_dicts_at_all);
            listDicts.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1));
        }

        listDicts.setOnItemClickListener(new OnDictItemClickListener());
        listDicts.setOnItemLongClickListener(new OnDictItemLongClickListener());
    }

    @Override
    public void onNewDictOkClick(int dictId)
    {
        prepareList();
    }

    class OnDictItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            // A simple click chooses an active dictionary
            // consequently after the a click we set up the selected
            // dictionary as active and should go to its word listDicts.
            if (dicts.size() != 0)
            {
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
            dictForOperation = (Dictionary) view.getTag();

            PopupMenu popup = new PopupMenu(ActDictionaryList.this, view);
            popup.getMenuInflater().inflate(R.menu.dict_list_popup_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch (item.getItemId())
                    {
                        case R.id.action_delete:
                            if (dictForOperation != null)
                                preDeleteDict(dictForOperation.getName());
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

    private void preDeleteDict(String name)
    {
        String message = getString(R.string.delete_dictionary_question, name);

        MessageDialog.showQuestion(this, message, null, new MessageDialog.Handler()
        {
            @Override
            public void doAction()
            {
                deleteDictionary();
            }
        }, null);
    }

    private void deleteDictionary()
    {
        if (dictForOperation != null)
        {
            DictionaryImageFileManager manager = new DictionaryImageFileManager(this, dictForOperation);
            try
            {
                manager.deleteDictDir();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                //TODO: there should be something more clever
            }
            try
            {
                DBDictionaryFactory.getInstance(this).delete(dictForOperation);
            }
            catch (SQLDataException e)
            {
                e.printStackTrace();
                showError(getString(R.string.txt_error_dectionary_deletition, e.getMessage()));
            }
        }
        if (dictForOperation.getId() == activeDictId)
            activeDictDeleted = true;

        dictForOperation = null;

        prepareList();
    }

    public void onBackPressed()
    {
        if (activeDictDeleted)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.txt_have_to_choose_dict).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // User cancelled the dialog
                }
            }).setCancelable(true).create().show();
        }
        else
            super.onBackPressed();
    }

    public void openFile(String mimeType)
    {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        // if you want any file type, you can skip next line
        sIntent.putExtra("CONTENT_TYPE", mimeType);
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null)
        {
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intent});
        }
        else
        {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try
        {
            startActivityForResult(chooserIntent, CHOOSE_FILE_CODE);
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }
}
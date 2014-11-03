package org.sc.w_drill;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sc.w_drill.backup.BackupHelper;
import org.sc.w_drill.backup.ImportProgressListener;
import org.sc.w_drill.backup.RestoreHelper;
import org.sc.w_drill.db.WDdb;

import java.io.File;
import java.util.logging.Handler;


public class ActImportDictionary extends ActionBarActivity
{
    public static final String SRC_FILE_PARAM_NAME = "SRC_FILE_PARAM_NAME";

    File sourceFile;
    Button btnStart;
    boolean bImportStats, bImportImages;
    ImportTask task;
    ProgressBar prgBar;
    TextView tvMessage;
    RestoreMessageHandler handler;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_dictionary);

        Intent data = getIntent();

        if( data != null )
        {
            String filename = data.getStringExtra(SRC_FILE_PARAM_NAME);
            if( filename == null )
                // TODO: There should be a message
                showErrorAndFinish( "" );
            else
                sourceFile = new File( filename );
        }
        else
        {
            // TODO: There should be a message
            showErrorAndFinish( "" );
        }

        btnStart = ( Button ) findViewById( R.id.btnStart );
        btnStart.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                preStartImport();
            }
        });

        prgBar = ( ProgressBar ) findViewById( R.id.prgBar );
        tvMessage = ( TextView  ) findViewById( R.id.stateMessage );

        prefs = getPreferences( MODE_PRIVATE );

        bImportImages = prefs.getBoolean(BackupHelper.PREF_IMPORT_IMAGES, true);
        bImportStats = prefs.getBoolean(BackupHelper.PREF_IMPORT_STATS, true);

        CheckBox chb = ( CheckBox ) findViewById( R.id.chbImportStats );
        chb.setChecked( bImportStats );

        chb = ( CheckBox ) findViewById( R.id.chbImportImages );
        chb.setChecked( bImportImages );
    }

    private void preStartImport()
    {
        CheckBox chb = ( CheckBox ) findViewById( R.id.chbImportStats );
        bImportStats = chb.isChecked();

        chb = ( CheckBox ) findViewById( R.id.chbImportImages );
        bImportImages = chb.isChecked();
        
        startImport();
    }

    private void startImport()
    {
        ImportTaskParams params = new ImportTaskParams();
        params.bImportImages = bImportImages;
        params.bImportStats = bImportStats;
        params.srcFile = sourceFile;
        params.database = new WDdb( getBaseContext() );
        handler = new RestoreMessageHandler();
        task = new ImportTask();
        task.execute( params );
    }

    private void showErrorAndFinish( String message )
    {
        finish();
    }

    void setMaxValue( Integer value )
    {
        prgBar.setMax( value );
    }

    void setProgress( Integer value )
    {
        prgBar.setProgress( value );
    }

    void beforeExecProc()
    {
        prgBar.setVisibility( View.INVISIBLE );
        tvMessage.setText( "" );
        btnStart.setEnabled( false );
    }

    void showState( int code )
    {
        String message = "";
        switch( code )
        {
            case ImportProgressListener.STATE_BEFORE_UNZIP:
                message = getString( R.string.txt_state_unzip );
                break;
            case ImportProgressListener.STATE_LOAD_TEXT:
                message = getString( R.string.txt_state_parsing);
                break;
            case ImportProgressListener.STATE_LOAD_DB:
                prgBar.setVisibility( View.VISIBLE );
                message = getString( R.string.txt_state_load);
                break;
        }
        tvMessage.setText( message );
    }

    class ImportTaskParams
    {
        boolean bImportStats;
        boolean bImportImages;
        File srcFile;
        WDdb database;
    }

    private class ImportTask extends AsyncTask<ImportTaskParams, Integer, Long> implements ImportProgressListener
    {

        @Override
        protected void onPreExecute()
        {
            beforeExecProc();
        }

        @Override
        protected Long doInBackground(ImportTaskParams... importTaskParams)
        {
            long res = 0;
            ImportTaskParams params = importTaskParams[0];

            RestoreHelper helper = new RestoreHelper( getBaseContext(), params.database,
                                                      params.srcFile,
                                                      this, params.bImportImages,
                                                      params.bImportStats,
                                                      handler );
            try
            {
                helper.load();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                res = -1;
            }

            return Long.valueOf( res  );
        }

        @Override
        public void setMaxValue(Integer value)
        {
            ActImportDictionary.this.setMaxValue( value );
        }

        @Override
        public void setProgress(Integer progress)
        {
            ActImportDictionary.this.setProgress(progress);
        }

        @Override
        public void setState(int state)
        {
            showState( state );
        }

        @Override
        protected void onPostExecute( Long result )
        {
            ActImportDictionary.this.onFinish(result);
        }
    }

    private void onFinish(Long result)
    {
        String message = "";

        prgBar.setVisibility( View.INVISIBLE );

        switch( ( int ) result.longValue() )
        {
            case 0:
                message = getString(R.string.txt_import_complete);
                setResult( Activity.RESULT_OK );
                SharedPreferences.Editor ed = prefs.edit();
                ed.putBoolean( BackupHelper.PREF_IMPORT_IMAGES, bImportImages );
                ed.putBoolean( BackupHelper.PREF_IMPORT_STATS, bImportStats );
                ed.commit();
                break;
            default:
                setResult( Activity.RESULT_CANCELED );
                message = getString(R.string.txt_import_error);
        }

        tvMessage.setText( message );
        btnStart.setEnabled( true );

    }

    class RestoreMessageHandler extends android.os.Handler
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            showState( msg.what );
        };
    }
}
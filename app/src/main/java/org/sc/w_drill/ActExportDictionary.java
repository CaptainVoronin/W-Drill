package org.sc.w_drill;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import org.sc.w_drill.backup.BackupHelper;
import org.sc.w_drill.backup.ExportProgressListener;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.io.File;


public class ActExportDictionary extends ActionBarActivity {

    Dictionary activeDict;
    WDdb database;
    ProgressBar prgBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_dictionary);

        database = new WDdb( this );

        Intent data = getIntent();
        if( data != null )
        {
            int id = data.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
            if( id == -1 )
                showErrorAndExit();
            else
                activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( id );
        }
        else
            showErrorAndExit();

        Button btn = (Button) findViewById( R.id.btnStart );
        btn.setOnClickListener(  new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                preStartExport();
            }
        });

        prgBar = ( ProgressBar  ) findViewById( R.id.prgBar );
    }

    private void showErrorAndExit()
    {
        super.onBackPressed();
    }

    private void preStartExport()
    {
        boolean bExportStats = true, bExportImages = true;

        CheckBox chb = ( CheckBox ) findViewById( R.id.chbExportStats );
        bExportStats = chb.isChecked();

        chb = ( CheckBox ) findViewById( R.id.chbExportImages );
        bExportImages = chb.isChecked();

        startExport( bExportStats, bExportImages );
    }

    private void startExport( boolean bExportStats, boolean  bExportImages )
    {
        ExportTask tsk = new ExportTask( );

    }

    public void setProgressMax( int maxValue )
    {
        prgBar.setMax( maxValue );
    }

    public void setCurrentProgress( Integer value )
    {
        prgBar.setProgress(value.intValue());
    }

    public void onFinish( Long value )
    {

    }


    private class ExportTaskParams
    {
        public boolean bExportStats;
        public boolean bExportImages;
        public Dictionary dict;
    }

    private class ExportTask extends AsyncTask<ExportTaskParams, Integer, Long> implements ExportProgressListener
    {

        @Override
        protected Long doInBackground(ExportTaskParams... dicts)
        {
            ExportTaskParams params = dicts[0];
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

            return Long.valueOf( 0 );
        }

        protected void onProgressUpdate(Integer... progress) {
            setCurrentProgress(progress[0]);
        }

        @Override
        public void setMaxValue(Integer max)
        {
            setProgressMax( max );
        }

        @Override
        public void setCurrentProgress(Integer current)
        {
            ActExportDictionary.this.setCurrentProgress( current );
        }
    }
}

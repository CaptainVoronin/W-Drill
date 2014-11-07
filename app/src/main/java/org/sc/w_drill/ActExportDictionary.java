package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sc.w_drill.backup.ExportHelper;
import org.sc.w_drill.backup.ExportProgressListener;
import org.sc.w_drill.backup.ImportHelper;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.io.File;


public class ActExportDictionary extends ActionBarActivity {

    Dictionary activeDict;
    WDdb database;
    ProgressBar prgBar;
    String destdir;
    ExportTask task = null;
    SharedPreferences prefs;
    boolean bExportImages = false;
    boolean bExportStats  = false;
    String errorString;
    int exportedCount;
    ExportHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_dictionary);

        errorString = null;
        exportedCount = 0;

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

        handler = new ExportHandler();

        prefs = getPreferences(Activity.MODE_PRIVATE);
        bExportImages = prefs.getBoolean( ExportHelper.PREF_EXPORT_IMAGES, false );
        bExportStats  = prefs.getBoolean( ExportHelper.PREF_EXPORT_STATS, false );

        CheckBox chb = ( CheckBox ) findViewById( R.id.chbExportStats );
        chb.setChecked( bExportStats );

        chb = ( CheckBox ) findViewById( R.id.chbExportImages );
        chb.setChecked( bExportImages );

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

        TextView tv = ( TextView ) findViewById( R.id.tvResultMessage );
        tv.setText( "" );

        CheckBox chb = ( CheckBox ) findViewById( R.id.chbExportStats );
        bExportStats = chb.isChecked();

        chb = ( CheckBox ) findViewById( R.id.chbExportImages );
        bExportImages = chb.isChecked();

        prefs = getPreferences(Activity.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean( ExportHelper.PREF_EXPORT_IMAGES, bExportImages );
        ed.putBoolean( ExportHelper.PREF_EXPORT_STATS, bExportStats );
        ed.commit();

        startExport(bExportStats, bExportImages);
    }

    private void startExport( boolean bExportStats, boolean  bExportImages )
    {
        ExportTaskParams params = new ExportTaskParams();
        params.context = this;
        params.destdir = new File( Environment.getExternalStorageDirectory() + File.separator + "Scholar" );
        destdir = params.destdir.getPath();
        params.dict = activeDict;
        params.bExportImages = bExportImages;
        params.bExportStats = bExportStats;

        task = new ExportTask( );
        task.execute( params );
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
        prgBar.setVisibility( View.INVISIBLE );
        Button btn = ( Button ) findViewById( R.id.btnStart );
        btn.setEnabled(true);
        TextView tv = ( TextView ) findViewById( R.id.tvResultMessage );
        if( value.longValue() == 0 )
        {
            tv.setText( getString( R.string.txt_dict_export_complete, destdir + File.separator
                                                                      + activeDict.getName() + ".zip" ));
        }
        else
        {
            String message = "";

            if( errorString != null )
                message = errorString;

            tv.setText( getString( R.string.txt_dict_export_error, message ));
        }

        task = null;
    }

    void onProcessStarted()
    {
        prgBar.setVisibility( View.VISIBLE );
        Button btn = ( Button ) findViewById( R.id.btnStart );
        btn.setEnabled(false);
    }

    private class ExportTaskParams
    {
        public boolean bExportStats;
        public boolean bExportImages;
        public Dictionary dict;
        public Context context;
        public File destdir;
    }

    private class ExportTask extends AsyncTask<ExportTaskParams, Integer, Long> implements ExportProgressListener
    {

        @Override
        protected Long doInBackground(ExportTaskParams... dicts)
        {
            ExportTaskParams params = dicts[0];
            long res = 0;
            try
            {
                ExportHelper helper = new ExportHelper( params.context,
                                                        params.destdir.getPath(),
                                                        params.dict, params.bExportImages,
                                                        params.bExportStats, this, handler );
                helper.backup();
            } catch( Exception ex )
            {
                ex.printStackTrace();
                res = -1;
            }

             return Long.valueOf( res );
        }


        @Override
        protected void onPreExecute()
        {
            onProcessStarted();
        }

        @Override
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

        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            onFinish( result );
        }
    }

    @Override
    public void onBackPressed()
    {
        if( task != null )
            showMessage( getString( R.string.txt_need_wait_for_end ) );
        else
            super.onBackPressed();
    }

    private void showMessage(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        builder.setTitle( R.string.txt_warning);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    class ExportHandler extends Handler
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch( msg.arg1 )
            {
                case ImportHelper.MSG_IMPORT_ERROR:
                    ActExportDictionary.this.setErrorInfo( msg.obj.toString() );
                    break;
                case ImportHelper.MSG_IMPORT_COMPLETE:
                    ActExportDictionary.this.setExportedWordCount((Integer) msg.obj);
                    break;
                default:
                    break;
            }
        };
    }

    private void setExportedWordCount(Integer obj)
    {
        exportedCount = obj.intValue();
    }

    private void setErrorInfo(String s)
    {
        errorString = s;
    }
}

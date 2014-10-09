package org.sc.w_drill;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class ExportWordsActivity extends ActionBarActivity {

    WordsDBHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_wirds);
        Button btn = (Button) findViewById( R.id.btnConnect );
        dbh = new WordsDBHelper();
        btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testDB();
            }
        });

        btn = ( Button ) findViewById( R.id.btnExport );
        btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 export();
            }
        });
    }

    private void export()
    {
        SQLiteDatabase db = dbh.getReadableDatabase();
        String statement = "select word, desc, translate from words where collection_id = 53";
        String pattern = "\t\t<row><word>%s</word><translation>%s</translation><description>%s</description></row>\n";
        Cursor crs = db.rawQuery(statement, null);


        StringBuffer buff = new StringBuffer();
        buff.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        buff.append( "\t<dictionary lang=\"en\" name=\"Main dictionary\">\n");

        while( crs.moveToNext() )
        {
            buff.append(String.format(pattern, crs.getString(0), crs.getString(2), crs.getString(1)));
        }
        buff.append( "</dictionary>");

        crs.close();
        db.close();

        try {
            File root = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES );
            File f = new File( root + File.separator + "dictionary.xml");
            if( !f.exists())
                f.createNewFile();
            else
            {
                f.delete();
                f.createNewFile();
            }
            FileWriter fr = new FileWriter( f );
            fr.write(buff.toString());
            fr.close();
            setText( "Закончено нормально" );
        }
        catch( IOException ex )
        {
            setText( ex.getMessage() );
        }
    }

    private void testDB()
    {
        SQLiteDatabase db = dbh.getReadableDatabase();
        String statement = "select count(*) from words";

        Cursor crs = db.rawQuery(statement, null);

        crs.moveToNext();

        int count = crs.getInt( 0 );

        crs.close();
        db.close();

        String text = "Database has been opened sucessfully.\n Words total " + count;
        setText(text);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.export_wirds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setText( String text )
    {
        TextView tv = ( TextView ) findViewById( R.id.info );

        tv.setText( text );
    }

    class WordsDBHelper extends SQLiteOpenHelper
    {

        public WordsDBHelper() {
            super(getApplicationContext(), "words.db", null, 13);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

        }
    }
}

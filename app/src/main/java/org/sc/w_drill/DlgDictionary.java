package org.sc.w_drill;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by MaxSh on 13.10.2014.
 *
 * Arabic, Egypt (ar_EG)
 Arabic, Israel (ar_IL)
 Bulgarian, Bulgaria (bg_BG)
 Catalan, Spain (ca_ES)
 Czech, Czech Republic (cs_CZ)
 Danish, Denmark(da_DK)
 German, Austria (de_AT)
 German, Switzerland (de_CH)
 German, Germany (de_DE)
 German, Liechtenstein (de_LI)
 Greek, Greece (el_GR)
 English, Australia (en_AU)
 English, Canada (en_CA)
 English, Britain (en_GB)
 English, Ireland (en_IE)
 English, India (en_IN)
 English, New Zealand (en_NZ)
 English, Singapore(en_SG)
 English, US (en_US)
 English, South Africa (en_ZA)
 Spanish (es_ES)
 Spanish, US (es_US)
 Finnish, Finland (fi_FI)
 French, Belgium (fr_BE)
 French, Canada (fr_CA)
 French, Switzerland (fr_CH)
 French, France (fr_FR)
 Hebrew, Israel (he_IL)
 Hindi, India (hi_IN)
 Croatian, Croatia (hr_HR)
 Hungarian, Hungary (hu_HU)
 Indonesian, Indonesia (id_ID)
 Italian, Switzerland (it_CH)
 Italian, Italy (it_IT)
 Japanese (ja_JP)
 Korean (ko_KR)
 Lithuanian, Lithuania (lt_LT)
 Latvian, Latvia (lv_LV)
 Norwegian bokmål, Norway (nb_NO)
 Dutch, Belgium (nl_BE)
 Dutch, Netherlands (nl_NL)
 Polish (pl_PL)
 Portuguese, Brazil (pt_BR)
 Portuguese, Portugal (pt_PT)
 Romanian, Romania (ro_RO)
 Russian (ru_RU)
 Slovak, Slovakia (sk_SK)
 Slovenian, Slovenia (sl_SI)
 Serbian (sr_RS)
 Swedish, Sweden (sv_SE)
 Thai, Thailand (th_TH)
 Tagalog, Philippines (tl_PH)
 Turkish, Turkey (tr_TR)
 Ukrainian, Ukraine (uk_UA)
 Vietnamese, Vietnam (vi_VN)
 Chinese, PRC (zh_CN)
 Chinese, Taiwan (zh_TW)
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
        getLocales();
        spin.setAdapter( new ArrayAdapter<String>( getContext(), android.R.layout.simple_list_item_1, langNames ) );
    }

    private void getLocales() {
        String[] langs = Locale.getISOLanguages();
        String[] countries = Locale.getISOLanguages();
        Locale[] locales = Locale.getAvailableLocales();

/*        for( int i = 0; i < langs.length; i++ )
            Log.d("W-DRILL langs", langs[i] + "\n");

        for( int i = 0; i < countries.length; i++ )
            Log.d("W-DRILL countries", countries[i] + "\n"); */
        String str;

        StringBuilder buff = new StringBuilder();

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < locales.length; i++) {
            //<item name="1">@string/noun</item>
            str = "<item name=\"locale_" + locales[i].getISO3Language() + "\">"
                    + locales[i].getISO3Language()
                    + ";" + locales[i].getDisplayLanguage()
                    + "</item>";
            if (!list.contains(str))
                list.add( str );
        }

        for( int i = 0; i< list.size(); i++ )
            buff.append( list.get(i) ).append( '\n' );

        try {
            File f = Environment.getExternalStorageDirectory();
            File f1 = new File( f.getPath() + File.separator + "Scholar/locales.txt");
            FileWriter fw = new FileWriter( f1  );
            fw.write( buff.toString() );
            fw.close();
            fw = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

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
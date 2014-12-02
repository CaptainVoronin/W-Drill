package org.sc.w_drill;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.utils.DBPair;
import org.sc.w_drill.utils.PartsOfSpeech;


/**
 * Created by Max on 10/18/2014.
 */
public class MeaningEditView implements View.OnClickListener
{
    IMeaning meaning;
    Context context;
    View view;
    Spinner listPartOfSpeech;
    PartsOfSpeech parts;
    private OnRemoveMeaningViewClickListener listener;
    private ImageView btnRemove;

    public MeaningEditView(Context _context, IMeaning _meaning)
    {
        context = _context;
        meaning = _meaning;
        init();
    }

    protected void init()
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Application.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.meaning_edit_view, null);

        listPartOfSpeech = (Spinner) view.findViewById(R.id.listPartOfSpeech);
        parts = PartsOfSpeech.getInstance(context);
        String[] items = parts.getNames();
        listPartOfSpeech.setAdapter(new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item, items));

        ((EditText) view.findViewById(R.id.ed_meaning)).setText(meaning.meaning());
        int index = parts.indexOf(meaning.partOFSpeech());
        listPartOfSpeech.setSelection(index);

        if (meaning.examples().size() != 0)
            for (DBPair p : meaning.examples())
                ((EditText) view.findViewById(R.id.ed_example)).setText(p.getValue());
        else
            ((EditText) view.findViewById(R.id.ed_example)).setText("");

        btnRemove = (ImageView) view.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(this);
    }

    public String getMeaning()
    {
        return ((EditText) view.findViewById(R.id.ed_meaning)).getText().toString().trim();
    }

    public String getExample()
    {
        return ((EditText) view.findViewById(R.id.ed_example)).getText().toString().trim();
    }

    public String getPartOfSpeech()
    {
        String name = (String) listPartOfSpeech.getSelectedItem();

        return parts.getCode(name);
    }

    public View getView()
    {
        return view;
    }

    @Override
    public void onClick(View view)
    {
        if (listener != null)
            listener.onClick(this);
    }

    public void setInitialFocus()
    {
        EditText ed = ( EditText ) view.findViewById( R.id.ed_meaning );
        ed.requestFocus();
    }

    public void setPartOfSpeach(String pos)
    {
        int index = parts.indexOf( pos );
        listPartOfSpeech.setSelection(index);
    }

    public interface OnRemoveMeaningViewClickListener
    {
        public void onClick(MeaningEditView meaningView);
    }

    public void setOnRemoveClickListener(OnRemoveMeaningViewClickListener _listener)
    {
        listener = _listener;
    }

    public void setRemovable(boolean removable)
    {
        btnRemove.setVisibility(removable ? View.VISIBLE : View.INVISIBLE);
    }
}

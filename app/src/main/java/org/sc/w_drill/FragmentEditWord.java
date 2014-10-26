package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.EPartOfSpeech;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;
import org.sc.w_drill.dict.WordChecker;
import org.sc.w_drill.utils.PartsOfSpeech;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentEditWord.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentEditWord#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class FragmentEditWord extends Fragment
        implements MeaningEditView.OnRemoveMeaningViewClickListener
{
    private Dictionary activeDict;
    private IWord activeWord;
    WDdb database;
    EditText edWord;
    EditText edTranscription;

    private OnFragmentInteractionListener mListener;
    private boolean isVisible;
    private boolean needBringWord;
    View rootView;
    Spinner listPartOfSpeech;
    PartsOfSpeech parts;
    LinearLayout viewContainer;
    ArrayList<MeaningEditView> meaningViewList;
    ImageView btnAddMeaning;

    public static FragmentEditWord newInstance(  )
    {
        FragmentEditWord fragment = new FragmentEditWord();
        return fragment;
    }

    public FragmentEditWord() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new WDdb( getActivity().getApplicationContext() );
        meaningViewList = new ArrayList<MeaningEditView>();

    }

/*    private void prepareForNewWord()
    {
        edWord.setText("");
    } */

    public void bringWordToScreen()
    {
        edWord.setText(activeWord.getWord());
        edTranscription.setText(activeWord.getTranscription());

        if( activeWord.meanings().size() != 0 )
        {
            meaningViewList.clear();
            viewContainer.removeAllViews();
            boolean removable = activeWord.meanings().size() > 1;
            for (IMeaning m : activeWord.meanings())
            {
                MeaningEditView med = new MeaningEditView(getActivity(), m);
                meaningViewList.add( med );
                viewContainer.addView(med.getView());
                med.setOnRemoveClickListener( this  );
                med.setRemovable( removable );
            }
        }

        needBringWord = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_edit_word, container, false);
        edWord = ( EditText ) rootView.findViewById( R.id.ed_word);
        edTranscription = ((EditText) rootView.findViewById(R.id.ed_transcription));
        viewContainer = ( LinearLayout ) rootView.findViewById( R.id.meanings_table );
        btnAddMeaning = ( ImageView ) rootView.findViewById( R.id.btnAddMeaning );
        btnAddMeaning.setOnClickListener( new OnAddMeaningClickListener() );
        int id = -1;
        Bundle args = getArguments();
        if( args != null )
            id = args.getInt(DBWordFactory.WORD_ID_VALUE_NAME, -1);

        setActiveWord(id);
        bringWordToScreen();

        return rootView;
    }

    public void startSaveWord()
    {
        /**
         * Gathering information
         */
        String word = String.valueOf(edWord.getText()).trim();

        // It's a dummy, obviously
        if( activeWord == null )
            activeWord = new Word( word );
        else
        {
            activeWord.setWord(word);
            activeWord.meanings().clear();
        }

        String transc = (( EditText )rootView.findViewById( R.id.ed_transcription )).getText().toString().trim();

        activeWord.setTranscription( transc  );

        for( MeaningEditView view : meaningViewList )
        {
            String meaning = view.getMeaning();
            String example = view.getExample();
            String posCode = view.getPartOfSpeech();
            if( !EPartOfSpeech.check( posCode ) )
            {
                // The code of a part of speech hasn't been found
                //TODO: Do something with the mistake
                continue;
            }

            meaning = meaning.trim();
            if( meaning.length() == 0 )
                continue;
            Meaning m = new Meaning( meaning );
            m.setPartOfSpeech( posCode );
            m.addExample(example.trim());
            activeWord.meanings().add( m );
        }
        saveWord();
    }

    private void saveWord()
    {
        int id;

        if( !WordChecker.isCorrect( DBWordFactory.getInstance(database, activeDict), activeWord ) )
        {
            showError( getString( R.string.incorrect_word ) );
            return;
        }

        if( ( id = activeWord.getId() ) != -1 )
        {
            try
            {
                DBWordFactory.getInstance(database, activeDict).updateWord(activeWord);
                mListener.onWordUpdated( activeWord.getId() );
            }catch( Exception e )
            {
                // TODO: It must be a correct exception handler.
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                activeWord = DBWordFactory.getInstance(database, activeDict).insertWord(activeWord);
            }
            catch (Exception e)
            {
                // TODO: It must be a correct exception handler.
                e.printStackTrace();
            }
        }

        /**
         * We should update the word list
         * in any case due to word or a new word was added
         * or an old word has been changed.
         */
        mListener.onWordAdded( id );

        clear();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void clear()
    {
        activeWord = new Word("");
        bringWordToScreen();
    }

    @Override
    public void onClick(MeaningEditView meaningView)
    {
        View view = meaningView.getView();
        meaningViewList.remove( meaningView );
        viewContainer.removeView( view );
        setRemovable();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        public void onWordAdded( int id );
        public void onWordUpdated( int id );
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible )
            return;

        // If is becomes visible, refresh list
        if( isVisibleToUser && needBringWord )
        {
            bringWordToScreen();
        }

        isVisible = isVisibleToUser;

    }

    public void setActiveWord( int wordId )
    {
        if( wordId != -1 )
            activeWord = DBWordFactory.getInstance( database, activeDict  ).getWordEx(wordId);
        else
            activeWord = Word.getDummy();
        needBringWord = true;
    }

    public void setParams( int dictId, int wordId)
    {
        activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );

        if( wordId != -1 )
            activeWord = DBWordFactory.getInstance( database, activeDict  ).getWord( wordId );
        else
            activeWord = Word.getDummy();
    }

    void showError( String message )
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );

        builder.setMessage( message ).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        }).setCancelable( true ).create().show();
    }

    class OnAddMeaningClickListener implements View.OnClickListener
    {

        @Override
        public void onClick(View view)
        {
            addMeaningView();
        }
    }

    private void addMeaningView()
    {
        Meaning m = new Meaning( "" );
        MeaningEditView view = new MeaningEditView(  getActivity(), m );
        meaningViewList.add( view );
        viewContainer.addView( view.getView() );
        view.setOnRemoveClickListener( this );

        setRemovable();
    }

    void setRemovable()
    {
        boolean removable =  meaningViewList.size() > 1;
        for( MeaningEditView mv : meaningViewList )
            mv.setRemovable( removable );
    }
}
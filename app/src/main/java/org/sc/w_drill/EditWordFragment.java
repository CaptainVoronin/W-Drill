package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;
import org.sc.w_drill.dict.WordChecker;
import org.sc.w_drill.utils.DBPair;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EditWordFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EditWordFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class EditWordFragment extends Fragment
{

    private Dictionary activeDict;
    private IWord activeWord;
    WDdb database;
    EditText edWord;

    private OnFragmentInteractionListener mListener;
    private boolean isVisible;
    private boolean needBringWord;
    View rootView;

    public static EditWordFragment newInstance(  )
    {
        EditWordFragment fragment = new EditWordFragment();
        return fragment;
    }

    public EditWordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new WDdb( getActivity().getApplicationContext() );
    }

    private void prepareForNewWord()
    {
        edWord.setText("");
    }

    public void bringWordToScreen()
    {
        edWord.setText(activeWord.getWord());
        ((EditText) rootView.findViewById(R.id.ed_transcription)).setText(activeWord.getTranscription());

        if( activeWord.meanings().size() != 0 )
            for( IMeaning m : activeWord.meanings() )
            {
                ((EditText) rootView.findViewById(R.id.ed_meaning)).setText(m.meaning());
                if( m.examples().size() != 0 )
                    for(DBPair p : m.examples() )
                        ((EditText) rootView.findViewById(R.id.ed_example)).setText( p.getValue() );
                else
                    ((EditText) rootView.findViewById(R.id.ed_example)).setText( "" );
            }
        else {
            ((EditText) rootView.findViewById(R.id.ed_meaning)).setText("");
            ((EditText) rootView.findViewById(R.id.ed_example)).setText( "" );
        }

        needBringWord = false;
    }

    private void fatalError()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_edit_word, container, false);
        Button btnOk = ( Button ) rootView.findViewById( R.id.btnOk );
        btnOk.setOnClickListener(  new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startSaveWord();
            }
        });
        edWord = ( EditText ) rootView.findViewById( R.id.the_word );

        Bundle args = getArguments();
        if( args != null )
        {
            int id = args.getInt(DBWordFactory.WORD_ID_VALUE_NAME, -1);
            if( id != -1 )
            {
                setActiveWord(id);
                bringWordToScreen();
            }
        }

        return rootView;
    }

    private void startSaveWord()
    {
        /**
         * Gather information
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

        String strMeaning = (( EditText )rootView.findViewById( R.id.ed_meaning )).getText().toString();

        Meaning meaning = new Meaning( strMeaning.trim() );

        String transc = (( EditText )rootView.findViewById( R.id.ed_transcription )).getText().toString().trim();

        activeWord.setTranscription( transc  );

        String example = (( EditText )rootView.findViewById( R.id.ed_example )).getText().toString();

        meaning.addExample( example.trim() );
        activeWord.meanings().add( meaning );

        saveWord();
    }

    private void saveWord()
    {
        int id;

        if( !WordChecker.isCorrect( DBWordFactory.getInstance(database, activeDict), activeWord ) )
        {
            AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );

            builder.setMessage( R.string.incorrect_word).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            }).setCancelable( true ).create().show();

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

        activeWord = new Word("");

        bringWordToScreen();
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
            bringWordToScreen();

        isVisible = isVisibleToUser;

    }

    public void setActiveWord( int wordId )
    {
        activeWord = DBWordFactory.getInstance( database, activeDict  ).getWordEx(wordId);
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
}
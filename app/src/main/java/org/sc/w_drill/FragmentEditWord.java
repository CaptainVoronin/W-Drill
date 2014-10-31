package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import org.sc.w_drill.utils.image.DictionaryImageFileManager;
import org.sc.w_drill.utils.image.ImageConstraints;
import org.sc.w_drill.utils.image.ImageFileHelper;
import org.sc.w_drill.utils.image.ImageHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentEditWord.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentEditWord#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentEditWord extends Fragment
        implements MeaningEditView.OnRemoveMeaningViewClickListener {
    private Dictionary activeDict;
    private IWord activeWord;
    WDdb database;
    EditText edWord;
    EditText edTranscription;

    private OnFragmentInteractionListener mListener;
    private boolean isVisible;
    private boolean needBringWord;
    View rootView;
    LinearLayout viewContainer;
    ArrayList<MeaningEditView> meaningViewList;
    ImageView btnAddMeaning, btnAddImg;
    String cachedImageFilename = null;
    ImageView wordIllustration;
    ImageFileHelper imageHelper;
    DictionaryImageFileManager dictImageManager;

    public static FragmentEditWord newInstance() {
        FragmentEditWord fragment = new FragmentEditWord();
        return fragment;
    }

    public FragmentEditWord() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new WDdb(getActivity().getApplicationContext());
        meaningViewList = new ArrayList<MeaningEditView>();

    }

/*    private void prepareForNewWord()
    {
        edWord.setText("");
    } */

    public void bringWordToScreen() {
        edWord.setText(activeWord.getWord());
        edTranscription.setText(activeWord.getTranscription());
        wordIllustration.setImageBitmap(null);
        Bitmap bmp;
        try {
            if( dictImageManager.getImageFile( activeWord ) != null )
            {
                bmp = dictImageManager.getImageBitmap(activeWord);
                wordIllustration.setImageBitmap(bmp);
                btnAddImg.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
            }
            else
                btnAddImg.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_menu_gallery));
        } catch (FileNotFoundException fnfex) {
            btnAddImg.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_menu_gallery));
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: there must be something more clever
        }

        if (activeWord.meanings().size() != 0) {
            meaningViewList.clear();
            viewContainer.removeAllViews();
            boolean removable = activeWord.meanings().size() > 1;
            for (IMeaning m : activeWord.meanings()) {
                MeaningEditView med = new MeaningEditView(getActivity(), m);
                meaningViewList.add(med);
                viewContainer.addView(med.getView());
                med.setOnRemoveClickListener(this);
                med.setRemovable(removable);
            }
        }

        needBringWord = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_edit_word, container, false);
        edWord = (EditText) rootView.findViewById(R.id.ed_word);
        edTranscription = ((EditText) rootView.findViewById(R.id.ed_transcription));
        viewContainer = (LinearLayout) rootView.findViewById(R.id.meanings_table);

        btnAddImg = (ImageView) rootView.findViewById(R.id.imgAddImg);
        btnAddImg.setOnClickListener(new AddImageListener());

        btnAddMeaning = (ImageView) rootView.findViewById(R.id.btnAddMeaning);
        btnAddMeaning.setOnClickListener(new OnAddMeaningClickListener());
        int id = -1;
        Bundle args = getArguments();
        if (args != null)
            id = args.getInt(DBWordFactory.WORD_ID_VALUE_NAME, -1);

        wordIllustration = (ImageView) rootView.findViewById(R.id.wordImage);
        imageHelper = ImageFileHelper.getInstance(getActivity());
        dictImageManager = new DictionaryImageFileManager(getActivity(), activeDict);

        setActiveWord(id);
        bringWordToScreen();

        return rootView;
    }

    public void startSaveWord() {
        /**
         * Gathering information
         */
        String word = String.valueOf(edWord.getText()).trim();

        // It's a dummy, obviously
        if (activeWord == null)
            activeWord = new Word(word);
        else {
            activeWord.setWord(word);
            activeWord.meanings().clear();
        }

        String transc = ((EditText) rootView.findViewById(R.id.ed_transcription)).getText().toString().trim();

        activeWord.setTranscription(transc);

        for (MeaningEditView view : meaningViewList) {
            String meaning = view.getMeaning();
            String example = view.getExample();
            String posCode = view.getPartOfSpeech();
            if (!EPartOfSpeech.check(posCode)) {
                // The code of a part of speech hasn't been found
                //TODO: Do something with the mistake
                continue;
            }

            meaning = meaning.trim();
            if (meaning.length() == 0)
                continue;
            Meaning m = new Meaning(meaning);
            m.setPartOfSpeech(posCode);
            m.addExample(example.trim());
            activeWord.meanings().add(m);
        }
        saveWord();
    }

    private void saveWord() {
        int id;

        if (!WordChecker.isCorrect(DBWordFactory.getInstance(database, activeDict), activeWord)) {
            showError(getString(R.string.incorrect_word));
            return;
        }

        if ((id = activeWord.getId()) != -1) {
            try {
                DBWordFactory.getInstance(database, activeDict).updateWord(activeWord);
                mListener.onWordUpdated(activeWord.getId());
            } catch (Exception e) {
                e.printStackTrace();
                showError( getString( R.string.error_on_word_update) + "\n" + e.getMessage() );
                return;
            }
        } else {
            try {
                activeWord = DBWordFactory.getInstance(database, activeDict).insertWord(activeWord);
            }
            catch( Exception e)
            {
                e.printStackTrace();
                showError( getString( R.string.error_on_word_insert) + "\n" + e.getMessage() );
                return;
            }

            try {
                if( cachedImageFilename != null  )
                {
                    // if an image was saved in cache it should be moved in an appropriate directory
                    ImageFileHelper helper = ImageFileHelper.getInstance(getActivity());
                    dictImageManager.moveImageFromCache( activeWord, cachedImageFilename);
                    deleteCachedImage();
                }
            } catch (Exception e) {
                DBWordFactory.getInstance( database, activeDict ).delete( activeWord );
                e.printStackTrace();
                showError( getString( R.string.error_on_image_saving) + "\n" + e.getMessage() );
                return;
            }
        }

        /**
         * We should update the word list
         * in any case due to word or a new word was added
         * or an old word has been changed.
         */
        mListener.onWordAdded(id);

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

    public void clear() {
        activeWord = new Word("");
        bringWordToScreen();
    }

    @Override
    public void onClick(MeaningEditView meaningView) {
        View view = meaningView.getView();
        meaningViewList.remove(meaningView);
        viewContainer.removeView(view);
        setRemovable();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        public void onWordAdded(int id);

        public void onWordUpdated(int id);

        public void selectImage();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible)
            return;

        // If is becomes visible, refresh list
        if (isVisibleToUser && needBringWord) {
            bringWordToScreen();
        }

        isVisible = isVisibleToUser;

    }

    public void setActiveWord(int wordId) {
        if (wordId != -1)
            activeWord = DBWordFactory.getInstance(database, activeDict).getWordEx(wordId);
        else
            activeWord = Word.getDummy();
        needBringWord = true;
    }

    public void setParams(int dictId, int wordId) {
        activeDict = DBDictionaryFactory.getInstance(database).getDictionaryById(dictId);

        if (wordId != -1)
            activeWord = DBWordFactory.getInstance(database, activeDict).getWord(wordId);
        else
            activeWord = Word.getDummy();
    }

    void showError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(message).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        }).setCancelable(true).create().show();
    }

    class OnAddMeaningClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            addMeaningView();
        }
    }

    private void addMeaningView() {
        Meaning m = new Meaning("");
        MeaningEditView view = new MeaningEditView(getActivity(), m);
        meaningViewList.add(view);
        viewContainer.addView(view.getView());
        view.setOnRemoveClickListener(this);

        setRemovable();
    }

    void setRemovable() {
        boolean removable = meaningViewList.size() > 1;
        for (MeaningEditView mv : meaningViewList)
            mv.setRemovable(removable);
    }

    private class AddImageListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (dictImageManager.getImageFile(activeWord) != null )
            {
                removeImage();
            } else {
                if (mListener != null)
                    mListener.selectImage();
            }
        }
    }

    public void onImageSelected(Uri uri) throws IOException {
        deleteCachedImage();
        removeImage();

        ImageConstraints constraints = ImageConstraints.getInstance(getActivity());

        Bitmap orig_bmp = imageHelper.pickImage(uri);

        // There we resize image if needed
        Bitmap bmp = ImageHelper.resizeBitmapForStorage( constraints, orig_bmp);

        if (activeWord.getId() == -1) {
            // The active word isn't saved so put picture in the cache
            cachedImageFilename = imageHelper.putBitmapInCache(bmp);
        } else {
            dictImageManager.checkDir();
            imageHelper.putBitmapInInternalStorage(dictImageManager, activeWord, bmp);
        }

        bmp = null;

        wordIllustration.setImageBitmap( ImageHelper.resizeBitmapForShow( constraints, orig_bmp ));

        btnAddImg.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
    }

    public void deleteCachedImage() {
        if (cachedImageFilename != null) {
            File f = new File(cachedImageFilename);
            if( f.exists() )
                if (f.delete())
                    Log.d("FragmentEditWord::deleteCachedImage", "file " + cachedImageFilename + " has been deleted");
                else
                    Log.e("FragmentEditWord::deleteCachedImage", "file " + cachedImageFilename + " hasn't been deleted");
            cachedImageFilename = null;
        }
    }

    @Override
    public void onDestroyView() {
        deleteCachedImage();
        super.onDestroyView();
    }

    protected void removeImage() {
        try {
            dictImageManager.deleteImage(activeWord);
            wordIllustration.setImageBitmap(null);
            btnAddImg.setImageDrawable(getActivity().getResources().getDrawable(android.R.drawable.ic_menu_gallery));
        } catch (FileNotFoundException fnfex) {

        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: must be a handler
        }
    }
}
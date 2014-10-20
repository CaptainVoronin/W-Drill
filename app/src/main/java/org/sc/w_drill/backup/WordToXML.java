package org.sc.w_drill.backup;

import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.DBPair;

/**
 * Created by MaxSh on 20.10.2014.
 */
public class WordToXML
{

    public static final void toXML( StringBuilder buff, IWord word, String uuid )
    {
        buff.append( "<word uuid=\"").append( uuid ).append( "\" ");
        buff.append( "state=\"").append( word.getLearnState() == IBaseWord.LearnState.learn ? 0 : 1 ).append( "\" ");
        buff.append( "percent=\"").append( word.getLearnPercent() ).append( "\"> ");
        // TODO: other properties has been omitted

        buff.append( "\t<value>").append( word.getWord() ).append("\t</value>");

        if( word.getTranscription() != null && word.getTranscription().length() != 0 )
            buff.append( "\t<transcription>").append( word.getTranscription() ).append( "\t</transcription>");

        for( IMeaning m : word.meanings())
        {
            toXML( buff, m );
        }
        buff.append( "</word>");

    }

    static final void toXML( StringBuilder buff, IMeaning meaning )
    {
        buff.append( "\t<meaning pos=\">").append( meaning.partOFSpeech()).append( "\">\n");
            buff.append( "\t<value>");
                buff.append( meaning.meaning() );
            buff.append( "</value>\n");
            if( meaning.examples()!= null && meaning.examples().size() != 0 )
            {
                for( DBPair ex : meaning.examples() )
                    toXML( buff, ex.getValue() );
            }
        buff.append( "\t</meaning>\n");

    }

    static final void toXML( StringBuilder buff, String example )
    {
        buff.append( "\t<example>");
            buff.append( example );
        buff.append( "</example>\n");
    }
}

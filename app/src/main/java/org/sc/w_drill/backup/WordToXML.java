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
        buff.append( "\t\t\t<word uuid=\"").append( uuid ).append( "\" ");
        buff.append( "state=\"").append( word.getLearnState() == IBaseWord.LearnState.learn ? 0 : 1 ).append( "\" ");
        buff.append( "percent=\"").append( word.getLearnPercent() ).append( "\">\n");

        buff.append( "\t\t\t\t<value>").append( word.getWord() ).append("</value>\n");

        if( word.getTranscription() != null && word.getTranscription().length() != 0 )
            buff.append( "\t\t\t\t<transcription>").append( word.getTranscription() ).append( "</transcription>\n");

        for( IMeaning m : word.meanings())
        {
            toXML( buff, m );
        }
        buff.append( "\t\t\t</word>\n");

    }

    static final void toXML( StringBuilder buff, IMeaning meaning )
    {
        buff.append( "\t\t\t\t\t<meaning pos=\"").append( meaning.partOFSpeech()).append( "\" ");
        buff.append( "formal=\"").append( meaning.isFormal() ).append( "\" ");
        buff.append( "rude=\"").append( meaning.isRude() ).append( "\" ");
        buff.append( "disapproving=\"").append( meaning.isDisapproving() ).append( "\">\n");
            buff.append( "\t\t\t\t\t\t<value>");
                buff.append( meaning.meaning() );
            buff.append( "</value>\n");
            if( meaning.examples()!= null && meaning.examples().size() != 0 )
            {
                for( DBPair ex : meaning.examples() )
                    toXML( buff, ex.getValue() );
            }
        buff.append( "\t\t\t\t\t</meaning>\n");

    }

    static final void toXML( StringBuilder buff, String example )
    {
        buff.append( "\t\t\t\t\t\t<example>");
            buff.append( example );
        buff.append( "</example>\n");
    }
}

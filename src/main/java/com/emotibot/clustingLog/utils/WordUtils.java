package com.emotibot.clustingLog.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.common.element.Word;
import com.emotibot.middleware.response.nlu.Segment;

public class WordUtils
{
    private static final String[] VERB = {"v", "vf"};
    private static final String[] NOUN = {"n", "nud", "nz", "nf"};    
    private static final String[] MODAL = {"e", "y"};
    private static final String[] PRONOUN = {"rr"};
    
    private static Set<String> verbSet;
    private static Set<String> nounSet;
    private static Set<String> modalSet;
    private static Set<String> pronounSet;
    
    static
    {
        verbSet = new HashSet<String>();
        for(String verb : VERB)
        {
            verbSet.add(verb);
        }
        
        nounSet = new HashSet<String>();
        for(String noun : NOUN)
        {
            nounSet.add(noun);
        }
        
        modalSet = new HashSet<String>();
        for(String modal : MODAL)
        {
            modalSet.add(modal);
        }
        
        pronounSet = new HashSet<String>();
        for(String modal : PRONOUN)
        {
            pronounSet.add(modal);
        }
    }
    
    public static boolean isVerb(Word word)
    {
        return isVerb(word.getPos());
    }
    
    public static boolean isVerb(String pos)
    {
        if (verbSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isNoun(Word word)
    {
        return isNoun(word.getPos());
    }
    
    public static boolean isNoun(String pos)
    {
        if (nounSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isModal(Word word)
    {
        return isModal(word.getPos());
    }
    
    public static boolean isModal(String pos)
    {
        if (modalSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isPronoun(Word word)
    {
        return isPronoun(word.getPos());
    }
    
    public static boolean isPronoun(String pos)
    {
        if (pronounSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isContainNud(Element element)
    {
        if (element == null || element.getSegments() == null)
        {
            return false;
        }
        List<Segment> segments = element.getSegments();
        for (Segment segment : segments)
        {
            if(segment.getPos().equals("nud"))
            {
                return true;
            }
        }
        return false;
    }
}

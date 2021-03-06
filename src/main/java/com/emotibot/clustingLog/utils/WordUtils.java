package com.emotibot.clustingLog.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.common.element.Word;
import com.emotibot.middleware.response.nlu.Segment;

public class WordUtils
{
    private static final String[] VERB = {"v", "vf", "vn"};
    private static final String[] VERB_1 = {"vi", "vyou"};
    private static final String[] NOUN = {"n", "nud", "nz", "nr", "nis"};
    private static final String[] NOUN_1 = {"nt", "nf", "ng", "nnt", "nba", "nt", "nx"};
    private static final String[] PERSON = {"nr", "nrf"};
    private static final String[] LOCATION = {"ns"};
    private static final String[] MODAL = {"e", "y"};
    private static final String[] PRONOUN = {"rr"};
    private static final String[] QUANTIFIER = {"q"};
    private static final String[] NUMERAL = {"m"};
    //后加的
    private static final String[] ADJECTIVE = {"a", "an"};
    private static final String[] OTHER = {"t", "s", "i"};
    
    private static Set<String> verbSet;
    private static Set<String> verbSet_1;
    private static Set<String> nounSet;
    private static Set<String> nounSet_1;
    private static Set<String> personSet;
    private static Set<String> modalSet;
    private static Set<String> pronounSet;
    private static Set<String> locationSet;
    private static Set<String> quantifierSet;
    private static Set<String> numeralSet;
    private static Set<String> adjectiveSet;
    private static Set<String> otherSet;
    
    public static final String PERSON_TEXT = "含人名/角色";
    public static final String PERSON_TAG = "persion";
    public static final String LOCATION_TEXT = "含地名";
    public static final String LOCATION_TAG = "location";
    public static final String APP_TEXT = "含APP";
    public static final String APP_TAG = "app";
    public static final String QUANTIFIER_TEXT = "含集/部/季";
    public static final String QUANTIFIER_TAG = "quantifier";
    public static final String VIDEO_TEXT = "含电影/电视剧";
    public static final String VIDEO_TAG = "video";
    public static final String NUMERAL_TEXT = "含数字";
    public static final String NUMERAL_TAG = "numeral";
    public static final String AREA_TEXT = "含国家";
    public static final String AREA_TAG = "area";
    
    public static final String[] SPECIAL_TAGS = {PERSON_TAG, LOCATION_TAG, APP_TAG, QUANTIFIER_TAG, VIDEO_TAG, NUMERAL_TAG, AREA_TAG};
    private static Set<String> specitalTagSet;
    
    private static final String num_parttern = "^[0-9]*$";
    
    static
    {
        verbSet = new HashSet<String>();
        for(String verb : VERB)
        {
            verbSet.add(verb);
        }
        
        verbSet_1 = new HashSet<String>();
        for(String verb : VERB_1)
        {
            verbSet_1.add(verb);
        }
        
        nounSet_1 = new HashSet<String>();
        for(String noun : NOUN_1)
        {
            nounSet_1.add(noun);
        }
        
        nounSet = new HashSet<String>();
        for(String noun : NOUN)
        {
            nounSet.add(noun);
        }
        
        personSet = new HashSet<String>();
        for(String noun : PERSON)
        {
            personSet.add(noun);
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
        
        locationSet = new HashSet<String>();
        for(String modal : LOCATION)
        {
            locationSet.add(modal);
        }
        
        quantifierSet = new HashSet<String>();
        for(String modal : QUANTIFIER)
        {
            quantifierSet.add(modal);
        }
        
        specitalTagSet = new HashSet<String>();
        for(String modal : SPECIAL_TAGS)
        {
            specitalTagSet.add(modal);
        }

        numeralSet = new HashSet<String>();
        for(String modal : NUMERAL)
        {
            numeralSet.add(modal);
        }
        
        adjectiveSet = new HashSet<String>();
        for(String adjective : ADJECTIVE)
        {
            adjectiveSet.add(adjective);
        }
        
        otherSet = new HashSet<String>();
        for(String other : OTHER)
        {
            otherSet.add(other);
        }
    }
    
    public static boolean isVerb(Word word)
    {
        return isVerb(word.getPos());
    }
    
    public static boolean isVerb(String pos)
    {
        if (verbSet.contains(pos) || verbSet_1.contains(pos))
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
        if (nounSet.contains(pos) || nounSet_1.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isPerson(Word word)
    {
        return isPerson(word.getPos());
    }
    
    public static boolean isPerson(String pos)
    {
        if (personSet.contains(pos))
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
    
    public static boolean isLocation(Word word)
    {
        return isLocation(word.getPos(), word.getWord());
    }
    
    public static boolean isLocation(String pos, String text)
    {
        if (locationSet.contains(pos) || CityUtils.cityNameSet.contains(text))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isLocation(String pos)
    {
        if (locationSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isQuantifier(Word word)
    {
        return isQuantifier(word.getPos());
    }
    
    public static boolean isQuantifier(String pos)
    {
        if (quantifierSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isNumeral(Word word)
    {
        return isNumeral(word.getPos());
    }
    
    public static boolean isNumeral(String pos)
    {
        if (numeralSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isAdjective(Word word)
    {
        return isAdjective(word.getPos());
    }
    
    public static boolean isAdjective(String pos)
    {
        if (adjectiveSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isOther(Word word)
    {
        return isOther(word.getPos());
    }
    
    public static boolean isOther(String pos)
    {
        if (otherSet.contains(pos))
        {
            return true;
        }
        return false;
    }
    
    public static boolean isSpecialWord(Word word)
    {
        if (word == null)
        {
            return false;
        }
        return isSpecialWord(word.getPos());
    }
    
    public static boolean isSpecialWord(String pos)
    {
        return specitalTagSet.contains(pos);
    }
    
    public static float getAdjustRate(Word word)
    {
        return getAdjustRate(word.getPos());
    }
    
    public static float getAdjustRate(String pos)
    {
        if (verbSet.contains(pos))
        {
            return Constants.ADJUST_VERB_RATE;
        }
        else if (verbSet_1.contains(pos))
        {
            return Constants.ADJUST_VERB_1_RATE;
        }
        else if (nounSet.contains(pos))
        {
            return Constants.ADJUST_NONE_RATE;
        }
        else if (nounSet_1.contains(pos))
        {
            return Constants.ADJUST_NONE_1_RATE;
        }
        else if (modalSet.contains(pos))
        {
            return Constants.ADJUST_MODAL_RATE;
        }
        else if (pronounSet.contains(pos))
        {
            return Constants.ADJUST_PRONOUN_RATE;
        }
        else if (personSet.contains(pos))
        {
            return Constants.ADJUST_PERSON_RATE;
        }
        else if (locationSet.contains(pos))
        {
            return Constants.ADJUST_LOCATION_RATE;
        }
        return 1.0f;
    }
    
    public static boolean isAllNum(String str)
    {
        Pattern pattern = Pattern.compile(num_parttern);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
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

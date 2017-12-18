package com.emotibot.clustingLog.nlp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.emotibot.middleware.response.nlu.NLUResponse;
import com.emotibot.middleware.response.nlu.Segment;

public class NlpUtilsTest
{
    public static final String TEXT = "陈赫恩";
    private static String[] PERSON_LEVEL_INFO = {"专有词库>长虹>影视>actor", "专有词库>长虹>影视>角色名"};
    public static Set<String> persionLevelInfoSet = new HashSet<String>();
    static
    {
        for (int i = 0; i < PERSON_LEVEL_INFO.length; i ++)
        {
            persionLevelInfoSet.add(PERSON_LEVEL_INFO[i]);
        }
    }
    
    
    @Test
    public void test()
    {
        NlpUtils.test();
        while(!NlpUtils.isNlpReady())
        {
            try
            {
                Thread.sleep(1000);
            } 
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        NLUResponse response = NlpUtils.getNlp(TEXT);
        List<Segment> segments = response.getSegment();
        for (Segment segment : segments)
        {
            if (persionLevelInfoSet.contains(segment.getLevelInfo()))
            {
                System.out.println(segment);
            }
        }
        System.out.println(response);
    }

}

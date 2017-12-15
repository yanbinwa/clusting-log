package com.emotibot.clustingLog.nlp;

import org.junit.Test;

import com.emotibot.middleware.response.nlu.NLUResponse;

public class NlpUtilsTest
{
    public static final String TEXT = "我想看周星驰的电影";
    
    @Test
    public void test()
    {
        NLUResponse response = NlpUtils.getNlp(TEXT);
        System.out.println(response);
    }

}

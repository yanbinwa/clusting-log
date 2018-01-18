package com.emotibot.clustingLog.utils;

import org.junit.Test;

public class WordUtilsTest
{

    @Test
    public void test()
    {
        String str = "123123一";
        System.out.println(WordUtils.isAllNum(str));
    }

}

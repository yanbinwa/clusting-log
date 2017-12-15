package com.emotibot.clustingLog.step;

import org.junit.Test;

public class ClustingLogStepTest
{

    @Test
    public void test()
    {
        int totalNum = 20000;
        float rate = 3 / (float)totalNum;
        if (rate < 0.001f)
        {
            System.out.println(true);
        }
        System.out.println(rate);
    }

}

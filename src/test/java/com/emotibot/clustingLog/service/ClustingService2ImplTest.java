package com.emotibot.clustingLog.service;

import org.junit.Test;

public class ClustingService2ImplTest
{

    public static final String CSV_FILE = "/Users/emotibot/Documents/workspace/other/clusting-log/file/长虹日志.csv";
    public static final String XLS_FILE = "/Users/emotibot/Documents/workspace/other/clusting-log/file/长虹日志部分.xlsx";
    
    @Test
    public void test()
    {
        ClustingService service = new ClustingService2Impl();
        service.clustingLog(XLS_FILE);
    }

}

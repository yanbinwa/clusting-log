package com.emotibot.clustingLog.service;

import org.junit.Test;

public class ClustingServiceImplTest
{

    public static final String CSV_FILE = "/Users/emotibot/Documents/workspace/other/clusting-log/file/长虹日志.csv";
    public static final String XLS_FILE = "/Users/emotibot/Documents/workspace/other/clusting-log/file/对话日志.xlsx";
    
    @Test
    public void test()
    {
        ClustingService service = new ClustingServiceImpl();
        service.clustingLog(XLS_FILE);
    }

}

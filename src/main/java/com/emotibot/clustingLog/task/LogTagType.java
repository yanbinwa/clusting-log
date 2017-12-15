package com.emotibot.clustingLog.task;

import com.emotibot.middleware.utils.StringUtils;

public enum LogTagType
{
    NONE, VIDEO;
    
    public static LogTagType getLogTagType(String verb, String noun)
    {
        if (StringUtils.isEmpty(verb) && StringUtils.isEmpty(noun))
        {
            return NONE;
        }
        return VIDEO;
    }
}

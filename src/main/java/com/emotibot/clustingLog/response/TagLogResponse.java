package com.emotibot.clustingLog.response;

import java.util.Map;

import com.emotibot.clustingLog.common.element.LogTagEle;
import com.emotibot.clustingLog.task.MyResponseType;
import com.emotibot.middleware.response.AbstractResponse;

public class TagLogResponse extends AbstractResponse
{

    Map<Integer, LogTagEle> indexToTagEleMap = null;
    
    public TagLogResponse()
    {
        super(MyResponseType.TAGLOG);
    }
    
    public TagLogResponse(Map<Integer, LogTagEle> indexToTagEleMap)
    {
        super(MyResponseType.TAGLOG);
        this.indexToTagEleMap = indexToTagEleMap;
    }

    public Map<Integer, LogTagEle> getIndexToTagEleMap()
    {
        return this.indexToTagEleMap;
    }
}

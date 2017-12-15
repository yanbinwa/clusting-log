package com.emotibot.clustingLog.response;

import java.util.List;
import java.util.Map;

import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.task.LogSelectType;
import com.emotibot.clustingLog.task.MyResponseType;
import com.emotibot.middleware.response.AbstractResponse;

public class SentencesToVectorResponse extends AbstractResponse
{
    private Map<LogSelectType, List<Element>> selectTypeToElementMap;
    
    public SentencesToVectorResponse()
    {
        super(MyResponseType.SENTENCE2VECTER);
    }
    
    public SentencesToVectorResponse(Map<LogSelectType, List<Element>> selectTypeToElementMap)
    {
        super(MyResponseType.SENTENCE2VECTER);
        this.selectTypeToElementMap = selectTypeToElementMap;
    }

    public Map<LogSelectType, List<Element>> getSelectTypeToElementMap()
    {
        return this.selectTypeToElementMap;
    }
    
    public List<Element> getElementByType(LogSelectType type)
    {
        if (selectTypeToElementMap == null)
        {
            return null;
        }
        return this.selectTypeToElementMap.get(type);
    }
}

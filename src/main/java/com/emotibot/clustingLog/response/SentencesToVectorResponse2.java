package com.emotibot.clustingLog.response;

import java.util.Set;

import com.emotibot.clustingLog.meanShift.MyDataPoint;
import com.emotibot.clustingLog.task.MyResponseType;
import com.emotibot.middleware.response.AbstractResponse;

public class SentencesToVectorResponse2 extends AbstractResponse
{
    private Set<MyDataPoint> elements = null;
    
    public SentencesToVectorResponse2()
    {
        super(MyResponseType.SENTENCE2VECTER);
    }
    
    public SentencesToVectorResponse2(Set<MyDataPoint> elements)
    {
        super(MyResponseType.SENTENCE2VECTER);
        this.elements = elements;
    }

    public Set<MyDataPoint> getElements()
    {
        return this.elements;
    }
}

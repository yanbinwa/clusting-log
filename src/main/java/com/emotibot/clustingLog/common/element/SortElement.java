package com.emotibot.clustingLog.common.element;

import java.util.Set;

import com.emotibot.middleware.utils.JsonUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SortElement
{
    @SerializedName("tagEle")
    @Expose
    private LogTagEle tagEle;
    
    @SerializedName("elements")
    @Expose
    private Set<Element> elements;
    
    public SortElement()
    {
        
    }
    
    public SortElement(LogTagEle tagEle, Set<Element> elements)
    {
        this.tagEle = tagEle;
        this.elements = elements;
    }
    
    public void setTagEle(LogTagEle tagEle)
    {
        this.tagEle = tagEle;
    }
    
    public LogTagEle getTagEle()
    {
        return this.tagEle;
    }
    
    public void setElements(Set<Element> elements)
    {
        this.elements = elements;
    }
    
    public Set<Element> getElements()
    {
        return this.elements;
    }
    
    @Override
    public String toString()
    {
        return JsonUtils.getJsonStr(this);
    }
}

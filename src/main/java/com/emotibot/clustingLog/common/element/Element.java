package com.emotibot.clustingLog.common.element;

import java.util.List;

import com.emotibot.middleware.response.nlu.Segment;
import com.emotibot.middleware.utils.JsonUtils;
import com.emotibot.middleware.utils.StringUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Element
{
    @SerializedName("text")
    @Expose
    private String text;
    
    @SerializedName("vectors")
    @Expose
    private float[] vectors;
    
    @SerializedName("segments")
    @Expose
    List<Segment> segments;
    
    public Element()
    {
        
    }
    
    public Element(String text, float[] vectors, List<Segment> segments)
    {
        this.text = text;
        this.vectors = vectors;
        this.segments = segments;
    }
    
    public void setText(String text)
    {
        this.text = text;
    }
    
    public String getText()
    {
        return this.text;
    }
    
    public void setVectors(float[] vectors)
    {
        this.vectors = vectors;
    }
    
    public float[] getVectors()
    {
        return this.vectors;
    }
    
    public void setSegments(List<Segment> segments)
    {
        this.segments = segments;
    }
    
    public List<Segment> getSegments()
    {
        return this.segments;
    }
    
    public String getSegmentLevelInfo()
    {
        if (segments == null)
        {
            return null;
        }
        for (Segment segment : segments)
        {
            if (!StringUtils.isEmpty(segment.getLevelInfo()))
            {
                return segment.getLevelInfo();
            }
        }
        return null;
    }
    
    @Override
    public int hashCode()
    {
        if (text == null)
        {
            return Integer.MAX_VALUE;
        }
        return text.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || !(obj instanceof Element))
        {
            return false;
        }
        Element other = (Element) obj;
        if (text == null || other.text == null)
        {
            return false;
        }
        return text.equals(other.text);
    }
    
    @Override
    public String toString()
    {
        return JsonUtils.getJsonStr(this);
    }
}

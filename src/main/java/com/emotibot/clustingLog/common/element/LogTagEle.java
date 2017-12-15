package com.emotibot.clustingLog.common.element;

import com.emotibot.clustingLog.task.LogTagType;
import com.emotibot.middleware.utils.JsonUtils;
import com.emotibot.middleware.utils.StringUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class LogTagEle
{
    @SerializedName("verb")
    @Expose
    private String verb;
    
    @SerializedName("noun")
    @Expose
    private String noun;
    
    @SerializedName("type")
    @Expose
    private LogTagType type;
    
    public LogTagEle()
    {
        
    }
    
    public LogTagEle(String verb, String noun, LogTagType type)
    {
        this.verb = verb;
        this.noun = noun;
        this.type = type;
    }
    
    public void setVerb(String verb)
    {
        this.verb = verb;
    }
    
    public String getVerb()
    {
        return this.verb;
    }
    
    public void setNoun(String noun)
    {
        this.noun = noun;
    }
    
    public String getNoun()
    {
        return this.noun;
    }
    
    public void setType(LogTagType type)
    {
        this.type = type;
    }
    
    public LogTagType getType()
    {
        return this.type;
    }
    
    public boolean isEmptyTag()
    {
        if (StringUtils.isEmpty(verb) && StringUtils.isEmpty(noun))
        {
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return this.toString().hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof LogTagEle))
        {
            return false;
        }
        LogTagEle other = (LogTagEle) obj;
        return this.toString().equals(other.toString());
    }
    
    @Override
    public String toString()
    {
        return JsonUtils.getJsonStr(this);
    }
}

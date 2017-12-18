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
    private Word verb;
    
    @SerializedName("noun")
    @Expose
    private Word noun;
    
    @SerializedName("type")
    @Expose
    private LogTagType type;
    
    public LogTagEle()
    {
        
    }
    
    public LogTagEle(Word verb, Word noun, LogTagType type)
    {
        this.verb = verb;
        this.noun = noun;
        this.type = type;
    }
    
    public void setVerb(Word verb)
    {
        this.verb = verb;
    }
    
    public Word getVerb()
    {
        return this.verb;
    }
    
    public void setNoun(Word noun)
    {
        this.noun = noun;
    }
    
    public Word getNoun()
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
    
    public String getTagStr()
    {
        String ret = "";
        if (verb != null)
        {
            ret += verb.getWord() + ",";
        }
        if (noun != null)
        {
            ret += noun.getWord() + ",";
        }
        if (!StringUtils.isEmpty(ret))
        {
            ret = ret.substring(0, ret.length() - 1);
        }
        else
        {
            ret = "无";
        }
        return ret;
    }
    
    public boolean isEmptyTag()
    {
        if (verb == null && noun == null)
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

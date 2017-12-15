package com.emotibot.clustingLog.common.element;

import com.emotibot.middleware.utils.JsonUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Word
{
    @SerializedName("word")
    @Expose
    private String word;
    
    @SerializedName("pos")
    @Expose
    private String pos;
    
    public Word()
    {
        
    }
    
    public Word(String word, String pos)
    {
        this.word = word;
        this.pos = pos;
    }
    
    public void setWord(String word)
    {
        this.word = word;
    }
    
    public String getWord()
    {
        return this.word;
    }
    
    public void setPos(String pos)
    {
        this.pos = pos;
    }
    
    public String getPos()
    {
        return this.pos;
    }
    
    @Override
    public int hashCode()
    {
        return this.toString().hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof Word))
        {
            return false;
        }
        Word other = (Word) obj;
        return this.toString().equals(other.toString());
    }
    
    @Override
    public String toString()
    {
        return JsonUtils.getJsonStr(this);
    }
}

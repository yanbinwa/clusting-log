package com.emotibot.clustingLog.meanShift;

import jsat.classifiers.DataPoint;
import jsat.linear.Vec;

public class MyDataPoint extends DataPoint
{

    private static final long serialVersionUID = 933889623932287446L;

    private String sentence;
    
    public MyDataPoint(Vec numericalValues, String sentence)
    {
        super(numericalValues);
        this.sentence = sentence;
    }
    
    public String getSentence()
    {
        return this.sentence;
    }
    
    public void setSentence(String sentence)
    {
        this.sentence = sentence;
    }
    
    @Override
    public int hashCode()
    {
        if (sentence == null)
        {
            return Integer.MAX_VALUE;
        }
        return sentence.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(obj == null || !(obj instanceof MyDataPoint))
        {
            return false;
        }
        MyDataPoint other = (MyDataPoint) obj;
        if (sentence == null || other.sentence == null)
        {
            return false;
        }
        return sentence.equals(other.sentence);
    }
    
    @Override
    public String toString()
    {
        return sentence + ": " + numericalValues;
    }
}

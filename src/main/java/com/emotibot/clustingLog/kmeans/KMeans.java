package com.emotibot.clustingLog.kmeans;

import java.util.List;
import java.util.Set;

import com.emotibot.clustingLog.common.element.Element;

public interface KMeans
{
    public List<Set<Element>> kmeans(Set<Element> elements);
    
    public void setKeamsNum(int keamsNum);
    
    public int getKeamsNum();
}

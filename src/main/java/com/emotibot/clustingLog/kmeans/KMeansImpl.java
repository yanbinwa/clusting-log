package com.emotibot.clustingLog.kmeans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.common.element.Element;

public class KMeansImpl implements KMeans
{
    private static Logger logger = Logger.getLogger(KMeansImpl.class);
    
    private List<Set<Element>> lastKmeansCluster = new ArrayList<Set<Element>>();
    private List<float[]> lastKmeansCenter = new ArrayList<float[]>();
    private List<Set<Element>> currentKmeansCluster = new ArrayList<Set<Element>>();
    private List<float[]> currentKmeansCenter = new ArrayList<float[]>();
    private List<Element> totalElement = new ArrayList<Element>();
    private int keamsNum = -1;
    private float threshold = 0;
    private int currentKeamsNum = -1;
    private int countTime = 0;
    private List<Float> errorList = new ArrayList<Float>();
    private Random random = new Random();
    
    public void setLastKmeansCluster(List<Set<Element>> lastKmeansCluster)
    {
        this.lastKmeansCluster = lastKmeansCluster;
    }
    
    public List<Set<Element>> getLastKmeansCluster()
    {
        return this.lastKmeansCluster;
    }
    
    public void setLastKmeansCenter(List<float[]> lastKmeansCenter)
    {
        this.lastKmeansCenter = lastKmeansCenter;
    }
    
    public List<float[]> getLastKmeansCenter()
    {
        return this.lastKmeansCenter;
    }
    
    public void setCurrentKmeansCluster(List<Set<Element>> currentKmeansCluster)
    {
        this.currentKmeansCluster = currentKmeansCluster;
    }
    
    public List<Set<Element>> getCurrentKmeansCluster()
    {
        return this.currentKmeansCluster;
    }
    
    public void setCurrentKmeansCenter(List<float[]> currentKmeansCenter)
    {
        this.currentKmeansCenter = currentKmeansCenter;
    }
    
    public List<float[]> getCurrentKmeansCenter()
    {
        return this.currentKmeansCenter;
    }
    
    public void setKeamsNum(int keamsNum)
    {
        this.keamsNum = keamsNum;
    }
    
    public int getKeamsNum()
    {
        return this.keamsNum;
    }
    
    public void setThreshold(float threshold)
    {
        this.threshold = threshold;
    }
    
    public float getThreshold()
    {
        return this.threshold;
    }

    /**
     * 即使新加的内容与原先的重复，kmean不会做过多的计算
     * 
     * 先遍历新加入的内容，如果之前都不存在，找到与之最近的center，加入到对应的Set中，之后开始计算
     * 
     * 如果之前没有任何的数据，需要重新初始化结果
     */
    @Override
    public List<Set<Element>> kmeans(Set<Element> elements)
    {
        if (elements == null || elements.isEmpty())
        {
            logger.error("input element is null or empty");
            return null;
        }
        countTime = 0;
        errorList.clear();
        Set<Element> elementsTmp = adjustInputElement(elements);
        initKeamsNum(elementsTmp);
        initCenters(elementsTmp);
        initClusters(elementsTmp);
        kmeans();
        List<Set<Element>> ret = getKmeansResult(elements);
        lastKmeansCluster = currentKmeansCluster;
        lastKmeansCenter = currentKmeansCenter;
        logger.info("countTime is: " + countTime + "; errorList is: " + errorList);
        return ret;
    }
    
    private Set<Element> adjustInputElement(Set<Element> elements)
    {
        Set<Element> newElement = new HashSet<Element>();
        for (Element element : elements)
        {
            boolean isInLastCluster = false;
            for (Set<Element> clusterEle : lastKmeansCluster)
            {
                if(clusterEle.contains(element))
                {
                    isInLastCluster = true;
                    break;
                }
            }
            if (!isInLastCluster)
            {
                newElement.add(element);
            }
        }
        return newElement;
    }
    
    private void initKeamsNum(Set<Element> elements)
    {
        int size = 0;
        for (Set<Element> clusterElement : lastKmeansCluster)
        {
            size += clusterElement.size();
        }
        size += elements.size();
        if (size < keamsNum)
        {
            currentKeamsNum = size;
        }
        else
        {
            currentKeamsNum = keamsNum;
        }
    }
    
    private void initCenters(Set<Element> elements)
    {
        currentKmeansCenter = lastKmeansCenter;
        List<Element> elementsTmp = new ArrayList<Element>(elements);
        int createCenterNum = currentKeamsNum - currentKmeansCenter.size();
        if (createCenterNum == 0)
        {
            return;
        }
        int arange = elementsTmp.size();
        int[] randoms = new int[createCenterNum];
        int temp = random.nextInt(arange);
        randoms[0] = temp;
        boolean flag;
        for (int i = 1; i < createCenterNum; i ++)
        {
            flag = true;
            while(flag)
            {
                temp = random.nextInt(arange);
                int j = 0;
                while(j < i)
                {
                    if (temp == randoms[j])
                    {
                        break;
                    }
                    j ++;
                }
                if (j == i)
                {
                    flag = false;
                }
            }
            randoms[i] = temp;
        }
        for (int i = 0; i < createCenterNum; i ++)
        {
            currentKmeansCenter.add(elementsTmp.get(randoms[i]).getVectors());
        }
    }
    
    private void initClusters(Set<Element> elements)
    {
        totalElement.addAll(elements);
        initClusters();
    }
    
    private void initClusters()
    {
        currentKmeansCluster.clear();
        for (int i = 0; i < currentKeamsNum; i ++)
        {
            currentKmeansCluster.add(new HashSet<Element>());
        }
    }
    
    private List<Set<Element>> getKmeansResult(Set<Element> elements)
    {
        List<Set<Element>> ret = new ArrayList<Set<Element>>();
        for (int i = 0; i < currentKeamsNum; i ++)
        {
            ret.add(new HashSet<Element>());
        }
        for (Element element : elements)
        {
            for (int i = 0; i < currentKeamsNum; i ++)
            {
                if (currentKmeansCluster.get(i).contains(element))
                {
                    ret.get(i).add(element);
                }
            }
        }
        return ret;
    }
    
    private void kmeans()
    {
        while(true)
        {
            clusterSet();
            countRule();
            if (countTime != 0)
            {
                if (errorList.get(countTime) - errorList.get(countTime - 1) == threshold)
                {
                    break;
                }
            }
            setNewCenter();
            countTime ++;
            initClusters();
        }
    }
    
    private void clusterSet()
    {
        float[] distance = new float[currentKeamsNum];
        for (int i = 0; i < totalElement.size(); i ++)
        {
            for (int j = 0; j < currentKeamsNum; j ++)
            {
                distance[j] = distance(currentKmeansCenter.get(j), totalElement.get(i));
            }
            int minLocation = minDistance(distance);
            currentKmeansCluster.get(minLocation).add(totalElement.get(i));
        }
    }
    
    private int minDistance(float[] distance)
    {
        float minDistance = distance[0];    
        int minLocation = 0;    
        for (int i = 1; i < distance.length; i ++) 
        {    
            if (distance[i] < minDistance) 
            {    
                minDistance = distance[i];    
                minLocation = i;    
            } 
            else if (distance[i] == minDistance)    
            {    
                if (random.nextInt(10) < 5) 
                {    
                    minLocation = i;    
                }    
            }    
        }    
        return minLocation; 
    }
    
    private void countRule()
    {
        float error = 0;
        for (int i = 0; i < currentKmeansCluster.size(); i ++)
        {
            for (Element element : currentKmeansCluster.get(i))
            {
                error += errorSquare(currentKmeansCenter.get(i), element);
            }
        }
        errorList.add(error);
    }
    
    private float errorSquare(float[] center, Element element)
    {
        float[] vector = element.getVectors();
        float sum = 0.0f;
        for (int i = 0; i < center.length; i ++)
        {
            sum += (center[i] - vector[i]) * (center[i] - vector[i]);
        }
        return sum;
    }
    
    private float distance(float[] center, Element element)
    {
        float[] vector = element.getVectors();
        float sum = 0.0f;
        for (int i = 0; i < center.length; i ++)
        {
            sum += (center[i] - vector[i]) * (center[i] - vector[i]);
        }
        return (float) Math.sqrt(sum);
    }
    
    private void setNewCenter()
    {
        int centerLen = totalElement.get(0).getVectors().length;
        for (int i = 0; i < currentKeamsNum; i ++)
        {
            int len = currentKmeansCluster.get(i).size();
            if (len > 0)
            {
                float[] newCenter = new float[centerLen];
                for (int j = 0; j < centerLen; j ++)
                {
                    newCenter[j] = 0;
                }
                for (Element element : currentKmeansCluster.get(i))
                {
                    for (int k = 0; k < centerLen; k ++)
                    {
                        newCenter[k] += element.getVectors()[k];
                    }
                }
                for (int j = 0; j < centerLen; j ++)
                {
                    newCenter[j] = newCenter[j] / len;
                }
                currentKmeansCenter.set(i, newCenter);
            }
        }
    }
}

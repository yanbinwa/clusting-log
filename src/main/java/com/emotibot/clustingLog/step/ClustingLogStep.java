package com.emotibot.clustingLog.step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.kmeans.KMeans;
import com.emotibot.clustingLog.kmeans.KMeansImpl;
import com.emotibot.clustingLog.response.SentencesToVectorResponse;
import com.emotibot.clustingLog.task.LogSelectType;
import com.emotibot.clustingLog.task.SentencesToVectorTask;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.step.AbstractStep;

/**
 * 分多个task线程进行句向量的处理，之后汇总结果进行kmeans操作
 * 
 * @author emotibot
 *
 */
public class ClustingLogStep extends AbstractStep
{
    private static final int TASK_MAX_NUM = 10;
    private static final int KMEANS_CLUSTER_NUM = 400;
    private KMeans kMeans = null;
    
    public ClustingLogStep()
    {
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
    }
    
    public ClustingLogStep(ExecutorService executorService)
    {
        super(executorService);
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void beforeRun(Context context)
    {
        Set<String> sentences = (Set<String>) context.getValue(Constants.CLUSTING_LOG_SENTENCES_KEY);
        if (sentences == null || sentences.isEmpty())
        {
            return;
        }
        List<String> sentenceList = new ArrayList<String>(sentences);
        int task_num = TASK_MAX_NUM;
        if (sentenceList.size() < task_num)
        {
            task_num = sentenceList.size();
        }
        int sentencePerTask = sentenceList.size() / task_num;
        for (int i = 0; i < task_num; i ++)
        {
            int start = i * sentencePerTask;
            int end = (i + 1) * sentencePerTask;
            if (i == task_num - 1)
            {
                end = sentenceList.size();
            }
            SentencesToVectorTask task = new SentencesToVectorTask(sentenceList.subList(start, end));
            this.addTask(context, task);
        }
    }

    @Override
    public void afterRun(Context context)
    {
        List<Response> responseList = this.getOutputMap(context).get(com.emotibot.clustingLog.task.MyResponseType.SENTENCE2VECTER);
        if (responseList == null)
        {
            return;
        }
        Map<LogSelectType, List<Element>> summarySelectMap = new HashMap<LogSelectType, List<Element>>();
        for (Response response : responseList)
        {
            SentencesToVectorResponse sentencesToResponse = (SentencesToVectorResponse) response;
            if (sentencesToResponse.getSelectTypeToElementMap() != null)
            {
                for (Map.Entry<LogSelectType, List<Element>> entry : sentencesToResponse.getSelectTypeToElementMap().entrySet())
                {
                    LogSelectType type = entry.getKey();
                    List<Element> summaryElements = summarySelectMap.get(type);
                    if (summaryElements == null)
                    {
                        summaryElements = new ArrayList<Element>();
                        summarySelectMap.put(type, summaryElements);
                    }
                    summaryElements.addAll(entry.getValue());
                }
            }
        }
        List<Set<Element>> cluster = null;
        if (summarySelectMap.get(LogSelectType.NORMAL_LOG) != null)
        {
            Set<Element> normalElement = new HashSet<Element>(summarySelectMap.get(LogSelectType.NORMAL_LOG));
            cluster = getCluster(context, normalElement);
        }
        
        context.setValue(Constants.CLUSTING_LOG_OUTPUT_KEY, cluster);
        context.setValue(Constants.CLUSTING_LOG_SUMMARY_MAP_KEY, summarySelectMap);
    }
    
    @SuppressWarnings("unused")
    private List<Set<Element>> getCluster(Context context, Set<Element> elements)
    {
        initKMeans(KMEANS_CLUSTER_NUM);
        List<Set<Element>> clustingResult = kMeans.kmeans(elements);
        int totalNum = 0;
        for (Set<Element> retElements : clustingResult)
        {
            totalNum += retElements.size();
        }
        if (totalNum == 0)
        {
            return null;
        }
        List<Element> dropEle = new ArrayList<Element>();
        Set<Element> chooseEle = new HashSet<Element>();
        clustingResult = sortClustingResult(clustingResult);
        int dropCount = 0;
        for (int i = 0; i < clustingResult.size(); i ++)
        {
            Set<Element> retElements = clustingResult.get(i);
            float rate = retElements.size() / (float) totalNum;
            if (rate < Constants.CHOOSE_THRESHOLD)
            {
                dropEle.addAll(retElements);
                dropCount ++;
            }
            else
            {
                chooseEle.addAll(retElements);
            }
        }
        context.setValue(Constants.CLUSTING_LOG_DROP_KEY, dropEle);
        initKMeans(KMEANS_CLUSTER_NUM - dropCount);
        clustingResult = kMeans.kmeans(chooseEle);
        clustingResult = sortClustingResult(clustingResult);
        return clustingResult;
    }
    
    private void initKMeans(int clusterNum)
    {
        kMeans = new KMeansImpl();
        kMeans.setKeamsNum(clusterNum);
    }
    
    private List<Set<Element>> sortClustingResult(List<Set<Element>> clustingResult)
    {
        Collections.sort(clustingResult, new Comparator<Set<Element>>() {

            @Override
            public int compare(Set<Element> o1, Set<Element> o2)
            {
                if (o1.size() > o2.size())
                {
                    return -1;
                }
                else if (o1.size() < o2.size())
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
            
        });
        return clustingResult;
    }

}

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
import com.emotibot.clustingLog.common.element.LogTagEle;
import com.emotibot.clustingLog.common.element.SortElement;
import com.emotibot.clustingLog.response.TagLogResponse;
import com.emotibot.clustingLog.task.MyResponseType;
import com.emotibot.clustingLog.task.TagLogTask;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.step.AbstractStep;

/**
 * 可以多线程进行的，每个task可以分配一些List的index，返回结果是该index中的特征词
 * 
 * 之后再根据index可以找到相应的日志内容
 * 
 * 1. 筛选出符合要求的cluster（cluster中的数量占总数量的0.1%以上的），不满足的自动归为一类，不做处理
 * 
 * 2. 将cluster分配给不同的task进行计算处理（设定task的数量）
 * 
 * 3. 汇总计算的结果，key为cluster的index，value为得到的特征词
 * 
 * @author emotibot
 *
 */
public class TagLogStep extends AbstractStep
{

    private static final int THREAD_NUM = 10;
    
    public TagLogStep()
    {
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
    }
    
    public TagLogStep(ExecutorService executorService)
    {
        super(executorService);
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void beforeRun(Context context)
    {
        //还是需要对每个cluster的大小有限制，过于小的需要删除掉
        List<Set<Element>> clusterEle = (List<Set<Element>>) context.getValue(Constants.CLUSTING_LOG_OUTPUT_KEY);
        if (clusterEle == null)
        {
            return;
        }
        List<Element> dropEle = (List<Element>) context.getValue(Constants.CLUSTING_LOG_DROP_KEY);
        if (dropEle == null)
        {
            dropEle = new ArrayList<Element>();
        }
        int totalNum = 0;
        for (Set<Element> retElements : clusterEle)
        {
            totalNum += retElements.size();
        }
        if (totalNum == 0)
        {
            return;
        }
        Map<Integer, Set<Element>> indexToChooseEle = new HashMap<Integer, Set<Element>>();
        for (int i = 0; i < clusterEle.size(); i ++)
        {
            Set<Element> retElements = clusterEle.get(i);
            float rate = retElements.size() / (float) totalNum;
            if (rate < Constants.CHOOSE_THRESHOLD)
            {
                dropEle.addAll(retElements);
            }
            else
            {
                indexToChooseEle.put(i, retElements);
            }            
        }
        context.setValue(Constants.CLUSTING_LOG_DROP_KEY, dropEle);
        
        //多线程进行分类工作
        int threadNum = Math.min(indexToChooseEle.size(), THREAD_NUM);
        List<Map<Integer, Set<Element>>> clusterEleBucket = new ArrayList<Map<Integer, Set<Element>>>();
        for (int i = 0; i < threadNum; i ++)
        {
            clusterEleBucket.add(new HashMap<Integer, Set<Element>>());
        }
        int count = 0;
        for (Map.Entry<Integer, Set<Element>> entry : indexToChooseEle.entrySet())
        {
            Map<Integer, Set<Element>> indexToEleMap = clusterEleBucket.get(count % threadNum);
            if (indexToEleMap == null)
            {
                indexToEleMap = new HashMap<Integer, Set<Element>>();
            }
            indexToEleMap.put(entry.getKey(), entry.getValue());
            count ++;
        }
        for (int i = 0; i < clusterEleBucket.size(); i ++)
        {
            TagLogTask task = new TagLogTask(clusterEleBucket.get(i));
            this.addTask(context, task);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterRun(Context context)
    {
        List<Response> responseList = this.getOutputMap(context).get(MyResponseType.TAGLOG);
        if (responseList == null)
        {
            return;
        }
        Map<Integer, LogTagEle> indexToTagEleMap = new HashMap<Integer, LogTagEle>();
        for (Response response : responseList)
        {
            TagLogResponse tagLogResponse = (TagLogResponse) response;
            if (tagLogResponse.getIndexToTagEleMap() != null)
            {
                indexToTagEleMap.putAll(tagLogResponse.getIndexToTagEleMap());
            }
        }
        
        //这里需要按照LogTagELe来进行汇总
        List<Set<Element>> clusterEle = (List<Set<Element>>) context.getValue(Constants.CLUSTING_LOG_OUTPUT_KEY);
        Map<LogTagEle, Set<Element>> summaryClusterEleMap = new HashMap<LogTagEle, Set<Element>>();
        //如果tag为空，不能盲目合并
        List<Set<Element>> emptyEles = new ArrayList<Set<Element>>();
        for (Map.Entry<Integer, LogTagEle> entry : indexToTagEleMap.entrySet())
        {
            LogTagEle tagEle = entry.getValue();
            if (tagEle.isEmptyTag())
            {
                emptyEles.add(clusterEle.get(entry.getKey()));
                continue;
            }
            Set<Element> summaryClusterEles = summaryClusterEleMap.get(tagEle);
            if (summaryClusterEles == null)
            {
                summaryClusterEles = new HashSet<Element>();
                summaryClusterEleMap.put(tagEle, summaryClusterEles);
            }
            summaryClusterEles.addAll(clusterEle.get(entry.getKey()));
        }
        
        //按照由多到少进行排序
        List<SortElement> sortElements = new ArrayList<SortElement>();
        for (Map.Entry<LogTagEle, Set<Element>> entry : summaryClusterEleMap.entrySet())
        {
            sortElements.add(new SortElement(entry.getKey(), entry.getValue()));
        }
        for (Set<Element> elements : emptyEles)
        {
            sortElements.add(new SortElement(new LogTagEle(), elements));
        }
        Collections.sort(sortElements, new Comparator<SortElement>() {

            @Override
            public int compare(SortElement o1, SortElement o2)
            {
                if (o1.getElements().size() > o2.getElements().size())
                {
                    return -1;
                }
                else if (o1.getElements().size() < o2.getElements().size())
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
            
        });
        
        List<LogTagEle> logTagList = new ArrayList<LogTagEle>();
        List<Set<Element>> clusterResultEle = new ArrayList<Set<Element>>();
        for (SortElement sortElement : sortElements)
        {
            logTagList.add(sortElement.getTagEle());
            clusterResultEle.add(sortElement.getElements());
        }
        
        context.setValue(Constants.CLUSTING_LOG_OUTPUT_KEY, clusterResultEle);
        context.setValue(Constants.CLUSTING_LOG_TYPE_KEY, logTagList);
    }
    
}

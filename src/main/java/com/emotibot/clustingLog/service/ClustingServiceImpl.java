package com.emotibot.clustingLog.service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.common.element.LogTagEle;
import com.emotibot.clustingLog.nlp.NlpUtils;
import com.emotibot.clustingLog.step.ClustingLogStep;
import com.emotibot.clustingLog.step.TagLogStep;
import com.emotibot.clustingLog.task.LogSelectType;
import com.emotibot.clustingLog.utils.XlsUtils;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.utils.StringUtils;

/**
 * 将结果输出到xls中
 * 
 * @author emotibot
 *
 */

public class ClustingServiceImpl implements ClustingService
{
    private static final Logger logger = Logger.getLogger(ClustingServiceImpl.class);
    
    private ExecutorService executorService = Executors.newFixedThreadPool(100);
    private ClustingLogStep clustingLogStep = new ClustingLogStep(executorService);
    private TagLogStep tagLogStep = new TagLogStep(executorService);
    private String outputXls = ConfigManager.INSTANCE.getPropertyString(Constants.CLUSTING_LOG_XLS_FILE_KEY);
    public String[] outputLogType = {Constants.CLUSTING_LOG_OUTPUT_KEY, Constants.CLUSTING_LOG_DROP_KEY, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY, 
                                     Constants.CLUSTING_LOG_EMPTY_TAG_KEY, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY, Constants.CLUSTING_LOG_SHORT_KEY, 
                                     Constants.CLUSTING_LOG_LONG_KEY, Constants.CLUSTING_LOG_TOO_MANNY_M_KEY}; 
    
    public ClustingServiceImpl()
    {
        init();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Set<String>> clustingLog(String csvFile)
    {
        logger.info("");
        logger.info("-------------- start -------------");
        Set<String> sentences = XlsUtils.loadLogFromXls1(csvFile);
        if (sentences == null || sentences.isEmpty())
        {
            return null;
        }
        Context context = new Context();
        context.setValue(Constants.CLUSTING_LOG_SENTENCES_KEY, sentences);
        getClustingLog(context);
        getLogTag(context);
        getReturnLog(context);
        storeOutput(context);
        logger.info("-------------- end -------------");
        logger.info("");
        return (List<Set<String>>)context.getValue(Constants.CLUSTING_LOG_RESULT_KEY);
    }
    
    @SuppressWarnings("unchecked")
    private void getClustingLog(Context context)
    {
        long startTime = System.currentTimeMillis();
        clustingLogStep.execute(context);
        long endTime = System.currentTimeMillis();
        logger.info("clustingLogStep: [" + (endTime - startTime) + "]ms");

        Map<LogSelectType, List<Element>> summarySelectMap = (Map<LogSelectType, List<Element>>) context.getValue(Constants.CLUSTING_LOG_SUMMARY_MAP_KEY);
        if (summarySelectMap == null)
        {
            return;
        }
        //SHORT_LOG
        List<Element> shortLogList = summarySelectMap.get(LogSelectType.SHORT_LOG);
        context.setValue(Constants.CLUSTING_LOG_SHORT_KEY, shortLogList);
        //LONG_LOG
        List<Element> longLogList = summarySelectMap.get(LogSelectType.LONG_LOG);
        context.setValue(Constants.CLUSTING_LOG_LONG_KEY, longLogList);
        //WITHOUT_V_N
        List<Element> withoutVNList = summarySelectMap.get(LogSelectType.WITHOUT_V_N);
        context.setValue(Constants.CLUSTING_LOG_WITHOUT_V_N_KEY, withoutVNList);
        //TOO_MANNY_M
        List<Element> tooManyMList = summarySelectMap.get(LogSelectType.TOO_MANNY_M);
        context.setValue(Constants.CLUSTING_LOG_TOO_MANNY_M_KEY, tooManyMList);
        //WITHOUT_VECTOR_WITH_NUD 需要根据NUD进行分类后再输出
        List<Element> withoutVectorWithNudList = summarySelectMap.get(LogSelectType.WITHOUT_VECTOR_WITH_NUD);
        if (withoutVectorWithNudList != null)
        {
            Map<String, List<Element>> withoutVectorWithNudMap = new HashMap<String, List<Element>>();
            for (Element element : withoutVectorWithNudList)
            {
                String nudLevelInfo = element.getSegmentLevelInfo();
                if (!StringUtils.isEmpty(nudLevelInfo))
                {
                    List<Element> elements = withoutVectorWithNudMap.get(nudLevelInfo);
                    if (elements == null)
                    {
                        elements = new ArrayList<Element>();
                        withoutVectorWithNudMap.put(nudLevelInfo, elements);
                    }
                    elements.add(element);
                }
            }
            context.setValue(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY, withoutVectorWithNudMap);
        }
        //WITHOUT_VECTOR_WITHOUT_NUD
        List<Element> withoutVectorWithoutNudList = summarySelectMap.get(LogSelectType.WITHOUT_VECTOR_WITHOUT_NUD);
        context.setValue(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY, withoutVectorWithoutNudList);
    }
    
    private void init()
    {
        NlpUtils.test();
        int count = 0;
        while (count < Constants.CLUSTING_SERVICE_INIT_NUM)
        {
            if (NlpUtils.isNlpReady())
            {
                break;
            }
            try
            {
                Thread.sleep(Constants.CLUSTING_SERVICE_INIT_INTERVAL);
                count ++;
            } 
            catch (InterruptedException e)
            {
                e.printStackTrace();
                return;
            }
        }
        logger.info("clusting service is init");
    }
    
    private void getLogTag(Context context)
    {
        long startTime = System.currentTimeMillis();
        tagLogStep.execute(context);
        long endTime = System.currentTimeMillis();
        logger.info("tagLogStep: [" + (endTime - startTime) + "]ms");
    }
    
    @SuppressWarnings("unchecked")
    private void getReturnLog(Context context)
    {
        List<Set<Element>> clustingResult = (List<Set<Element>>) context.getValue(Constants.CLUSTING_LOG_OUTPUT_KEY);
        if (clustingResult != null)
        {
            List<Set<String>> ret = new ArrayList<Set<String>>();
            for (int i = 0; i < clustingResult.size(); i ++)
            {
                Set<String> retStrSet = new HashSet<String>();
                Set<Element> cluster = clustingResult.get(i);
                for (Element element : cluster)
                {
                    retStrSet.add(element.getText());
                }
                ret.add(retStrSet);
            }
            context.setValue(Constants.CLUSTING_LOG_RESULT_KEY, ret);
        }
    }
    
    /**
     * 1. 分类结果
     * 2. 短日志，没有nud
     * 3. 没有动词和名词的日志
     * 4. 太多语气词的日志
     * 5. 没有词向量有nud的日志
     * 6. 没有词向量没有nud的日志
     * 7. 分类较少被丢弃的日志
     * 8. 分类type日志
     * 
     * @param context
     */
    private void storeOutput(Context context)
    {   
        Map<String, List<String>> logsMap = new HashMap<String, List<String>>();
        
        //这里需要合并cluster与type
        List<String> logs = getClustingLog(context, Constants.CLUSTING_LOG_OUTPUT_KEY, Constants.CLUSTING_LOG_TYPE_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_OUTPUT_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_SHORT_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_SHORT_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_LONG_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_LONG_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_WITHOUT_V_N_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_WITHOUT_V_N_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_TOO_MANNY_M_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_TOO_MANNY_M_KEY, logs);
        }
        
        logs = getClustingLog2(context, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY, logs);
        }
        
        logs = getClustingLog1(context, Constants.CLUSTING_LOG_DROP_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_DROP_KEY, logs);
        }
        
        logs = getClustingLogType(context, Constants.CLUSTING_LOG_TYPE_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_TYPE_KEY, logs);
        }
        
        logs = getClustingLog3(context, Constants.CLUSTING_LOG_EMPTY_TAG_KEY);
        if (logs != null)
        {
            logsMap.put(Constants.CLUSTING_LOG_EMPTY_TAG_KEY, logs);
        }
        
        XlsUtils.writeLogForXls(outputXls, logsMap, outputLogType);
    }
    
    @SuppressWarnings("unused")
    private String getStoreFile(String containKey)
    {
        String filePathKey = null;
        switch(containKey)
        {
        case Constants.CLUSTING_LOG_OUTPUT_KEY:
            filePathKey = Constants.CLUSTING_LOG_OUTPUT_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_SHORT_KEY:
            filePathKey = Constants.CLUSTING_LOG_SHORT_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_LONG_KEY:
            filePathKey = Constants.CLUSTING_LOG_LONG_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_WITHOUT_V_N_KEY:
            filePathKey = Constants.CLUSTING_LOG_WITHOUT_V_N_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_TOO_MANNY_M_KEY:
            filePathKey = Constants.CLUSTING_LOG_TOO_MANNY_M_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY:
            filePathKey = Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY:
            filePathKey = Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_DROP_KEY:
            filePathKey = Constants.CLUSTING_LOG_DROP_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_TYPE_KEY:
            filePathKey = Constants.CLUSTING_LOG_TYPE_FILE_KEY;
            break;
        case Constants.CLUSTING_LOG_EMPTY_TAG_KEY:
            filePathKey = Constants.CLUSTING_LOG_EMPTY_TAG_FILE_KEY;
            break;
        }
        if (StringUtils.isEmpty(filePathKey))
        {
            return null;
        }
        return ConfigManager.INSTANCE.getPropertyString(filePathKey);
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getClustingLog(Context context, String clusterKey, String typeKey)
    {
        List<String> outputLog = new ArrayList<String>();
        List<Set<Element>> clusterLog = (List<Set<Element>>) context.getValue(clusterKey);
        List<LogTagEle> clusterLogTag = (List<LogTagEle>) context.getValue(typeKey);
        if (clusterLog == null || clusterLogTag == null)
        {
            return null;
        }
        else if (clusterLog.size() != clusterLogTag.size())
        {
            logger.error("clusterLog size is not equal to clusterLogTag size");
            return null;
        }
        String filePath = getStoreFile(clusterKey);
        for (int i = 0; i < clusterLog.size(); i ++)
        {
            Set<Element> logs = clusterLog.get(i);
            for (Element log : logs)
            {
                outputLog.add(i + Constants.CLUSTING_LOG_SPLIT_KEY + log.getText());
            }
        }
        storeFile(outputLog, filePath);
        
        outputLog.clear();
        for (int i = 0; i < clusterLog.size(); i ++)
        {
            Set<Element> logs = clusterLog.get(i);
            LogTagEle tagEle = clusterLogTag.get(i);
            for (Element log : logs)
            {
                outputLog.add(i + Constants.CLUSTING_LOG_SPLIT_KEY + log.getText() + Constants.CLUSTING_LOG_SPLIT_KEY + tagEle.getTagStr());
            }
        }
        return outputLog;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getClustingLog1(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<Element> clusterLog = (List<Element>) context.getValue(key);
        if (clusterLog == null)
        {
            return null;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLog.size(); i ++)
        {
            outputLog.add(clusterLog.get(i).getText());
        }
        storeFile(outputLog, filePath);
        return outputLog;
        
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getClustingLog2(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        Map<String, List<Element>> clusterLogMap = (Map<String, List<Element>>) context.getValue(key);
        if (clusterLogMap == null)
        {
            return null;
        }
        String filePath = getStoreFile(key);
        for (Map.Entry<String, List<Element>> entry : clusterLogMap.entrySet())
        {
            List<Element> elements = entry.getValue();
            if (elements == null)
            {
                continue;
            }
            String levelInfo = entry.getKey();
            for (Element element : elements)
            {
                outputLog.add(levelInfo + Constants.CLUSTING_LOG_SPLIT_KEY + element.getText()); 
            }
        }
        storeFile(outputLog, filePath);
        return outputLog;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getClustingLog3(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<Set<Element>> clusterLogList = (List<Set<Element>>) context.getValue(key);
        if (clusterLogList == null)
        {
            return null;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLogList.size(); i ++)
        {
            Set<Element> elements = clusterLogList.get(i);
            if (elements == null)
            {
                continue;
            }
            for (Element element : elements)
            {
                outputLog.add(i + Constants.CLUSTING_LOG_SPLIT_KEY + element.getText());
            }
        }
        storeFile(outputLog, filePath);
        return outputLog;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getClustingLogType(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<LogTagEle> clusterLogTag = (List<LogTagEle>) context.getValue(key);
        if (clusterLogTag == null)
        {
            return null;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLogTag.size(); i ++)
        {
            outputLog.add(i + ": " + clusterLogTag.get(i));
        }
        storeFile(outputLog, filePath);
        return outputLog;
    }
    
    @SuppressWarnings("unused")
    private void storeFile(List<String> lines, String fileName)
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(fileName);
            for (int i = 0; i < lines.size(); i ++)
            {
                fw.write(lines.get(i) + "\r\n");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }
        finally
        {
            try
            {
                fw.close();
            } 
            catch (IOException e)
            {
                
            }
        }
    }
}

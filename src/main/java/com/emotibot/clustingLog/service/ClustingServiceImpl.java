package com.emotibot.clustingLog.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.common.element.LogTagEle;
import com.emotibot.clustingLog.step.ClustingLogStep;
import com.emotibot.clustingLog.step.TagLogStep;
import com.emotibot.clustingLog.task.LogSelectType;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.utils.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * 将结果输出到xls中
 * 
 * @author emotibot
 *
 */

public class ClustingServiceImpl implements ClustingService
{
    private static final int MAX_LOG_NUM = 100000;
    private static final int LOG_COLUME = 5;
    private static final int OUTPUT_COLUME = 4;
    private static final String EMPTY_OUTPUT = "{}";
    private static final Logger logger = Logger.getLogger(ClustingServiceImpl.class);
    
    
    private ExecutorService executorService = Executors.newFixedThreadPool(100);
    private ClustingLogStep clustingLogStep = new ClustingLogStep(executorService);
    private TagLogStep tagLogStep = new TagLogStep(executorService);
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Set<String>> clustingLog(String csvFile)
    {
        logger.info("");
        logger.info("-------------- start -------------");
        Set<String> sentences = loadLogFromXls(csvFile);
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
    
    @SuppressWarnings("unused")
    private Set<String> loadLogFromCsv(String csvFile)
    {
        File file = new File(csvFile);
        FileReader fReader = null; 
        CSVReader csvReader = null;
        try
        {
            fReader = new FileReader(file);  
            csvReader = new CSVReader(fReader); 
            Set<String> sentences = new HashSet<String>();
            List<String[]> list = csvReader.readAll();
            int count = 0;
            for (String[] ss : list)
            {
                String sentence = ss[LOG_COLUME];
                if (!StringUtils.isEmpty(sentence) && !sentences.contains(sentence))
                {
                    sentences.add(sentence);
                    count ++;
                    if (count > MAX_LOG_NUM)
                    {
                        break;
                    }
                }
            }
            return sentences;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            try
            {
                csvReader.close();
                fReader.close();
            } 
            catch (IOException e)
            {
                
            }
        }
    }
    
    @SuppressWarnings("unused")
    private Set<String> loadLogFromXls(String xlsFile)
    {
        InputStream is = null;
        XSSFWorkbook xssfWorkbook = null;
        try
        {
            Set<String> sentences = new HashSet<String>();
            is = new FileInputStream(xlsFile);
            xssfWorkbook = new XSSFWorkbook(is);
            XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(0);
            if (xssfSheet == null)
            {
                return null;
            }
            int count = 0;
            for (int i = 1; i <= xssfSheet.getLastRowNum(); i ++)
            {
                XSSFRow xssfRow = xssfSheet.getRow(i);
                if (xssfRow != null)
                {
                    XSSFCell output = xssfRow.getCell(OUTPUT_COLUME);
                    String outputStr = output.getStringCellValue();
                    if (EMPTY_OUTPUT.equals(outputStr))
                    {
                        XSSFCell sentenceCell = xssfRow.getCell(LOG_COLUME);
                        int cellType = sentenceCell.getCellType();
                        String sentence = null;
                        switch(cellType)
                        {
                        case XSSFCell.CELL_TYPE_NUMERIC:
                            sentence = String.valueOf(sentenceCell.getNumericCellValue());
                            break;
                        case XSSFCell.CELL_TYPE_STRING:
                            sentence = sentenceCell.getStringCellValue();
                            break;
                        default:
                            break;    
                        }
                        if (!StringUtils.isEmpty(sentence) && !sentences.contains(sentence))
                        {
                            sentences.add(sentence);
                            count ++;
                            if (count > MAX_LOG_NUM)
                            {
                                break;
                            }
                        }
                    }
                }
            }
            return sentences;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            try
            {
                is.close();
            } 
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
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
        //WITHOUT_VECTOR_WITH_NUD
        List<Element> withoutVectorWithNudList = summarySelectMap.get(LogSelectType.WITHOUT_VECTOR_WITH_NUD);
        context.setValue(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY, withoutVectorWithNudList);
        //WITHOUT_VECTOR_WITHOUT_NUD
        List<Element> withoutVectorWithoutNudList = summarySelectMap.get(LogSelectType.WITHOUT_VECTOR_WITHOUT_NUD);
        context.setValue(Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY, withoutVectorWithoutNudList);
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
        storeClustingLog(context, Constants.CLUSTING_LOG_OUTPUT_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_SHORT_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_LONG_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_WITHOUT_V_N_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_TOO_MANNY_M_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY);
        storeClustingLog1(context, Constants.CLUSTING_LOG_DROP_KEY);
        storeClustingLogType(context, Constants.CLUSTING_LOG_TYPE_KEY);
    }
    
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
        }
        if (StringUtils.isEmpty(filePathKey))
        {
            return null;
        }
        return ConfigManager.INSTANCE.getPropertyString(filePathKey);
    }
    
    @SuppressWarnings("unchecked")
    private void storeClustingLog(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<Set<Element>> clusterLog = (List<Set<Element>>) context.getValue(key);
        if (clusterLog == null)
        {
            return;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLog.size(); i ++)
        {
            Set<Element> logs = clusterLog.get(i);
            for (Element log : logs)
            {
                outputLog.add(i + ": " + log.getText());
            }
        }
        storeFile(outputLog, filePath);
    }
    
    @SuppressWarnings("unchecked")
    private void storeClustingLog1(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<Element> clusterLog = (List<Element>) context.getValue(key);
        if (clusterLog == null)
        {
            return;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLog.size(); i ++)
        {
            outputLog.add(clusterLog.get(i).getText());
        }
        storeFile(outputLog, filePath);
    }
    
    @SuppressWarnings("unchecked")
    private void storeClustingLogType(Context context, String key)
    {
        List<String> outputLog = new ArrayList<String>();
        List<LogTagEle> clusterLogTag = (List<LogTagEle>) context.getValue(key);
        if (clusterLogTag == null)
        {
            return;
        }
        String filePath = getStoreFile(key);
        for (int i = 0; i < clusterLogTag.size(); i ++)
        {
            outputLog.add(i + ": " + clusterLogTag.get(i));
        }
        storeFile(outputLog, filePath);
    }
    
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

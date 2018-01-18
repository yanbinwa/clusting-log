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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.meanShift.MyDataPoint;
import com.emotibot.clustingLog.step.ClustingLogStep2;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.utils.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import jsat.classifiers.DataPoint;

@Deprecated
public class ClustingService2Impl implements ClustingService
{
    private static final int MAX_LOG_NUM = 50;
    private static final int LOG_COLUME = 5;
    private static final int OUTPUT_COLUME = 8;
    private static final String EMPTY_OUTPUT = "{}";
    private static final Logger logger = Logger.getLogger(ClustingServiceImpl.class);
    
    
    private ExecutorService executorService = Executors.newFixedThreadPool(100);
    private ClustingLogStep2 clustingLogStep = new ClustingLogStep2(executorService);
    
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
        List<Set<String>> ret = getClustingLog(context);
        storeClustingLog(ret);
        logger.info("-------------- end -------------");
        logger.info("");
        return ret;
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
    private List<Set<String>> getClustingLog(Context context)
    {
        long startTime = System.currentTimeMillis();
        clustingLogStep.execute(context);
        long endTime = System.currentTimeMillis();
        logger.info("ParseSentenceTypeStep: [" + (endTime - startTime) + "]ms");
        List<List<DataPoint>> clustingResult = (List<List<DataPoint>>) context.getValue(Constants.CLUSTING_LOG_RESULT_KEY);
        List<Set<String>> ret = new ArrayList<Set<String>>();
        for (int i = 0; i < clustingResult.size(); i ++)
        {
            Set<String> retStrSet = new HashSet<String>();
            List<DataPoint> cluster = clustingResult.get(i);
            for (DataPoint element : cluster)
            {
                MyDataPoint myDataPoint = (MyDataPoint) element;
                retStrSet.add(myDataPoint.getSentence());
            }
            ret.add(retStrSet);
        }
        return ret;
    }
    
    private void storeClustingLog(List<Set<String>> clusterLog)
    {
        if (clusterLog == null || clusterLog.isEmpty())
        {
            return;
        }
        String clusterFile = ConfigManager.INSTANCE.getPropertyString(Constants.CLUSTING_LOG_OUTPUT_FILE_KEY);
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(clusterFile);
            for (int i = 0; i < clusterLog.size(); i ++)
            {
                Set<String> logs = clusterLog.get(i);
                if (logs == null)
                {
                    continue;
                }
                for(String log : logs)
                {
                    fw.write(i + ", " + log + "\r\n");
                }
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

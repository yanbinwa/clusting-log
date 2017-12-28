package com.emotibot.clustingLog.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.middleware.utils.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

public class XlsUtils
{
    private static Logger logger = Logger.getLogger(XlsUtils.class);
    private static final int MAX_LOG_NUM = 100000;
    private static final int LOG_COLUME = 5;
    private static final int OUTPUT_COLUME = 4;
    private static final String EMPTY_OUTPUT = "{}";
    
    public static Set<String> loadLogFromCsv(String csvFile)
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
    
    public static Set<String> loadLogFromXls(String xlsFile)
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
    
    public static Set<String> loadLogFromXls1(String xlsFile)
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
                    XSSFCell output = xssfRow.getCell(0);
                    String outputStr = output.getStringCellValue();
                    if (!StringUtils.isEmpty(outputStr) && !sentences.contains(outputStr))
                    {
                        sentences.add(outputStr);
                        count ++;
                        if (count > MAX_LOG_NUM)
                        {
                            break;
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
    
    //Map的key为sheetName，value为日志
    public static void writeLogForXls(String xlsFile, Map<String, List<String>> logMap)
    {
        File file = new File(xlsFile);
        if (file.exists())
        {
            file.delete();
        }
        OutputStream os = null;
        XSSFWorkbook xssfWorkbook = null;
        try
        {
            os = new FileOutputStream(xlsFile);
            xssfWorkbook = new XSSFWorkbook();
            for (Map.Entry<String, List<String>> entry : logMap.entrySet())
            {
                String sheetName = entry.getKey();
                List<String> logs = entry.getValue();
                if (logs == null)
                {
                    continue;
                }
                logger.info("create sheetName " + sheetName);
                Sheet sheet = xssfWorkbook.createSheet(sheetName);
                int rowCount = 0;
                for (String log : logs)
                {
                    String[] cellStrs = log.split(Constants.CLUSTING_LOG_SPLIT_KEY);
                    Row row = sheet.createRow(rowCount);
                    for (int j = 0; j < cellStrs.length; j ++)
                    {
                        Cell cell = row.createCell(j);
                        cell.setCellValue(cellStrs[j]);
                    }
                    rowCount ++;
                }
            }
            xssfWorkbook.write(os);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (Exception e)
            {
                
            }
        }
    }
    
    public static void writeLogForXls(String xlsFile, Map<String, List<String>> logMap, String[] sheetKey)
    {
        File file = new File(xlsFile);
        if (file.exists())
        {
            file.delete();
        }
        OutputStream os = null;
        XSSFWorkbook xssfWorkbook = null;
        try
        {
            os = new FileOutputStream(xlsFile);
            xssfWorkbook = new XSSFWorkbook();
            for (int i = 0; i < sheetKey.length; i ++)
            {
                String sheetName = sheetKey[i];
                List<String> logs = logMap.get(sheetName);
                if (logs == null)
                {
                    continue;
                }
                Sheet sheet = xssfWorkbook.createSheet(sheetName);
                int rowCount = 0;
                for (String log : logs)
                {
                    String[] cellStrs = log.split(Constants.CLUSTING_LOG_SPLIT_KEY);
                    Row row = sheet.createRow(rowCount);
                    for (int j = 0; j < cellStrs.length; j ++)
                    {
                        Cell cell = row.createCell(j);
                        cell.setCellValue(cellStrs[j]);
                    }
                    rowCount ++;
                }
            }
            xssfWorkbook.write(os);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (Exception e)
            {
                
            }
        }
    }
}

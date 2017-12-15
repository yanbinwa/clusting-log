package com.emotibot.clustingLog.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.common.element.LogTagEle;
import com.emotibot.clustingLog.common.element.Word;
import com.emotibot.clustingLog.response.TagLogResponse;
import com.emotibot.clustingLog.utils.WordUtils;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.response.nlu.Segment;
import com.emotibot.middleware.task.AbstractTask;

/**
 * 可以对多组聚合的句子进行标签处理，具体逻辑如下（考虑intent）
 * 
 * 1. 根据分词结果统计所有的分词情况
 * 2. 计算所有分词的出现频率，并进行排序，取topN (只取动词和名词)
 * 3. 对于得到的词安装词性进行归纳（只保留动词和名词），再次计算频率，做出排序，取topN，
 *    如果没有动词和名词，则认为该cluster无意义，直接丢弃
 * 4. 通过得到的动词和名词找到相应的目录，之后归类，返回是index: tag
 * 
 * @author emotibot
 *
 */
public class TagLogTask extends AbstractTask
{
    private static final double HF_THRESHOLD = 0.5;
    
    Map<Integer, Set<Element>> indexToElementMap;
    
    public TagLogTask(Map<Integer, Set<Element>> indexToElementMap)
    {
        this.indexToElementMap = indexToElementMap;
    }
    
    @Override
    public Response call() throws Exception
    {
        if (indexToElementMap == null)
        {
            return null;
        }
        Map<Integer, LogTagEle> ret = new HashMap<Integer, LogTagEle>();
        for(Map.Entry<Integer, Set<Element>> entry : indexToElementMap.entrySet())
        {
            LogTagEle tagEle = getLogType(entry.getValue(), entry.getKey());
            ret.put(entry.getKey(), tagEle);
        }
        TagLogResponse response = new TagLogResponse(ret);
        return response;
    }

    private LogTagEle getLogType(Set<Element> logEles, int index)
    {
        Map<Word, Integer> hfWord = getHFWord(logEles);
        String verb = getTopVerb(hfWord);
        String noun = getTopNone(hfWord);
        LogTagType type = LogTagType.getLogTagType(verb, noun);
        LogTagEle tagEle = new LogTagEle(verb, noun, type);
        return tagEle;
    }
    
    /**
     * 在一个cluster中获取高频词（high frequency），只看动词和名词
     */
    
    private Map<Word, Integer> getHFWord(Set<Element> logEles)
    {
        Map<Word, Integer> wordToCountMap = new HashMap<Word, Integer>();
        for (Element element : logEles)
        {
            List<Segment> segments = element.getSegments();
            if (segments == null)
            {
                continue;
            }
            for (Segment segment : segments)
            {
                Word word = new Word(segment.getWord(), segment.getPos());
                if (WordUtils.isVerb(word) || WordUtils.isNoun(word))
                {
                    Integer count = wordToCountMap.get(word);
                    if (count == null)
                    {
                        count = new Integer(0);
                    }
                    count ++;
                    wordToCountMap.put(word, count);
                }
            }
        }
        Map<Word, Integer> hfWord = new HashMap<Word, Integer>();
        for (Map.Entry<Word, Integer> entry : wordToCountMap.entrySet())
        {
            double rate = entry.getValue() / (double) logEles.size();
            if (rate > HF_THRESHOLD)
            {
                hfWord.put(entry.getKey(), entry.getValue());
            }
        }
        return hfWord;
    }
    
    /**
     * 取出排名第一的动词和排名第一的名词
     */
    private String getTopVerb(Map<Word, Integer> hfWord)
    {
        int maxCount = Integer.MIN_VALUE;
        String retVerb = null;
        for (Map.Entry<Word, Integer> entry : hfWord.entrySet())
        {
            if (!WordUtils.isVerb(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() > maxCount)
            {
                retVerb = entry.getKey().getWord();
            }
        }
        return retVerb;
    }
    
    private String getTopNone(Map<Word, Integer> hfWord)
    {
        int maxCount = Integer.MIN_VALUE;
        String retNoun = null;
        for (Map.Entry<Word, Integer> entry : hfWord.entrySet())
        {
            if (!WordUtils.isNoun(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() > maxCount)
            {
                retNoun = entry.getKey().getWord();
            }
        }
        return retNoun;
    }
}

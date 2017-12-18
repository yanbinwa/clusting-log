package com.emotibot.clustingLog.task;

import java.util.HashMap;
import java.util.HashSet;
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
import com.emotibot.middleware.utils.StringUtils;

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
    private static final double HF_THRESHOLD = 0.35;
    private static final double HF_THRESHOLD_1 = 0.75;
    
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
        Word verb = getTopVerb(hfWord);
        Word noun = getTopNone(hfWord);
        String verbWord = verb != null ? verb.getWord() : null;
        String nounWord = noun != null ? noun.getWord() : null;
        LogTagType type = LogTagType.getLogTagType(verbWord, nounWord);
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
                //优先判断人名和地名，之后判断动词和名词
                if (WordUtils.isPerson(word) || isPersionLevelInfo(segment.getLevelInfo()))
                {
                    word = new Word(WordUtils.PERSON_TEXT, WordUtils.PERSON_TAG);
                    Integer count = wordToCountMap.get(word);
                    if (count == null)
                    {
                        count = new Integer(0);
                    }
                    count ++;
                    wordToCountMap.put(word, count);
                }
                else if (WordUtils.isLocation(word))
                {
                    word = new Word(WordUtils.LOCATION_TEXT, WordUtils.LOCATION_TAG);
                    Integer count = wordToCountMap.get(word);
                    if (count == null)
                    {
                        count = new Integer(0);
                    }
                    count ++;
                    wordToCountMap.put(word, count);
                }
                else if (isAppLevelInfo(segment.getLevelInfo()))
                {
                    word = new Word(WordUtils.APP_TEXT, WordUtils.APP_TAG);
                    Integer count = wordToCountMap.get(word);
                    if (count == null)
                    {
                        count = new Integer(0);
                    }
                    count ++;
                    wordToCountMap.put(word, count);
                }
                else if (WordUtils.isVerb(word) || WordUtils.isNoun(word))
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
            Word word = entry.getKey();
            //如果是人名或者地名，需要特定的threshold
            if (WordUtils.isSpecialWord(word) && rate > HF_THRESHOLD_1)
            {
                hfWord.put(word, entry.getValue());
            }
            else if (rate > HF_THRESHOLD)
            {
                hfWord.put(word, entry.getValue());
            }
        }
        return hfWord;
    }
    
    /**
     * 取出排名第一的动词和排名第一的名词
     */
    private Word getTopVerb(Map<Word, Integer> hfWord)
    {
        int maxCount = Integer.MIN_VALUE;
        Word retVerb = null;
        for (Map.Entry<Word, Integer> entry : hfWord.entrySet())
        {
            if (!WordUtils.isVerb(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() > maxCount)
            {
                retVerb = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        return retVerb;
    }
    
    /**
     * 这里要分为是否是special tag，作为backup
     * 
     * @param hfWord
     * @return
     */
    private Word getTopNone(Map<Word, Integer> hfWord)
    {
        int maxCount = Integer.MIN_VALUE;
        Word retNoun = null;
        for (Map.Entry<Word, Integer> entry : hfWord.entrySet())
        {
            if (!WordUtils.isNoun(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() > maxCount)
            {
                retNoun = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        if (retNoun != null)
        {
            return retNoun;
        }
        
        maxCount = Integer.MIN_VALUE;
        for (Map.Entry<Word, Integer> entry : hfWord.entrySet())
        {
            if (!WordUtils.isSpecialWord(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() > maxCount)
            {
                retNoun = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        return retNoun;
    }
    
    private boolean isPersionLevelInfo(String levelInfo)
    {
        if (StringUtils.isEmpty(levelInfo))
        {
            return false;
        }
        return MyConstants.persionLevelInfoSet.contains(levelInfo);
    }
    
    private boolean isAppLevelInfo(String levelInfo)
    {
        if (StringUtils.isEmpty(levelInfo))
        {
            return false;
        }
        return MyConstants.appLevelInfoSet.contains(levelInfo);
    }
    
    static class MyConstants
    {
        private static String[] PERSON_LEVEL_INFO = {"专有词库>长虹>影视>actor", "专有词库>长虹>影视>角色名"};
        private static String[] APP_LEVEL_INFO = {"专有词库>长虹>APP>name", "专有词库>长虹>其他>search", "专有词库>长虹>其他>site"};
        public static Set<String> persionLevelInfoSet = new HashSet<String>();
        public static Set<String> appLevelInfoSet = new HashSet<String>();
        static
        {
            for (int i = 0; i < PERSON_LEVEL_INFO.length; i ++)
            {
                persionLevelInfoSet.add(PERSON_LEVEL_INFO[i]);
            }
            
            for (int i = 0; i < APP_LEVEL_INFO.length; i ++)
            {
                appLevelInfoSet.add(APP_LEVEL_INFO[i]);
            }
        }
    }
}

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
 * 
 * TODO: 天气在长虹是一个APP名，需要在判断APP时去掉
 * @author emotibot
 *
 */
public class TagLogTask extends AbstractTask
{
    private static final double HF_THRESHOLD = 0.4;
    private static final double HF_THRESHOLD_1 = 0.8;
    
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
        Set<Word> spectialWordSet = new HashSet<Word>();
        for (Element element : logEles)
        {
            List<Segment> segments = element.getSegments();
            if (segments == null)
            {
                continue;
            }
            
            spectialWordSet.clear();
            for (Segment segment : segments)
            {
                Word word = new Word(segment.getWord(), segment.getPos());
                //优先是正常的词
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
                //之后是特殊词
                if (WordUtils.isPerson(word) || isPersionLevelInfo(segment))
                {
                    word = new Word(WordUtils.PERSON_TEXT, WordUtils.PERSON_TAG);
                }
                else if (WordUtils.isLocation(word))
                {
                    word = new Word(WordUtils.LOCATION_TEXT, WordUtils.LOCATION_TAG);
                }
                else if (isAppLevelInfo(segment))
                {
                    word = new Word(WordUtils.APP_TEXT, WordUtils.APP_TAG);
                }
                else if (WordUtils.isQuantifier(word) && MyConstants.quantifierIncludeSet.contains(word.getWord()))
                {
                    word = new Word(WordUtils.QUANTIFIER_TEXT, WordUtils.QUANTIFIER_TAG);
                }
                else if (isVideoLevelInfo(segment))
                {
                    word = new Word(WordUtils.VIDEO_TEXT, WordUtils.VIDEO_TAG);
                }
                else if (WordUtils.isNumeral(word))
                {
                    word = new Word(WordUtils.NUMERAL_TEXT, WordUtils.NUMERAL_TAG);
                }
                else if (isAreaLevelInfo(segment))
                {
                    word = new Word(WordUtils.AREA_TEXT, WordUtils.AREA_TEXT);
                }
                else
                {
                    word = null;
                }
                
                if (word != null)
                {
                    /**
                     * 对于特殊word，一句话只能提取一种中的一个
                     */
                    if (spectialWordSet.contains(word))
                    {
                        continue;
                    }
                    spectialWordSet.add(word);
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
    
    private boolean isPersionLevelInfo(Segment segment)
    {
        if (StringUtils.isEmpty(segment.getLevelInfo()))
        {
            return false;
        }
        return MyConstants.persionLevelInfoSet.contains(segment.getLevelInfo()) &&
                !MyConstants.persionExcludeSet.contains(segment.getOrgWord());
    }
    
    private boolean isAppLevelInfo(Segment segment)
    {
        if (StringUtils.isEmpty(segment.getLevelInfo()))
        {
            return false;
        }
        return MyConstants.appLevelInfoSet.contains(segment.getLevelInfo()) &&
                !MyConstants.appExcludeSet.contains(segment.getOrgWord());
    }
    
    private boolean isVideoLevelInfo(Segment segment)
    {
        if (StringUtils.isEmpty(segment.getLevelInfo()))
        {
            return false;
        }
        return MyConstants.videoLevelInfoSet.contains(segment.getLevelInfo()) &&
                !MyConstants.videoExcludeSet.contains(segment.getOrgWord());
    }
    
    private boolean isAreaLevelInfo(Segment segment)
    {
        if (StringUtils.isEmpty(segment.getLevelInfo()))
        {
            return false;
        }
        return MyConstants.areaLevelInfoSet.contains(segment.getLevelInfo());
    }
    
    static class MyConstants
    {
        private static String[] PERSON_LEVEL_INFO = {"专有词库>长虹>影视>actor", "专有词库>长虹>影视>角色名"};
        private static String[] APP_LEVEL_INFO = {"专有词库>长虹>APP>name", "专有词库>长虹>其他>search", "专有词库>长虹>其他>site"};
        private static String[] VIDEO_LEVEL_INFO = {"专有词库>长虹>影视>电视剧", "专有词库>长虹>影视>电影"};
        private static String[] AREA_LEVEL_INFO = {"专有词库>长虹>VIDEO>area"};
        private static String[] APP_EXCLUDE = {"天气", "一点"};
        private static String[] VIDEO_EXCLUDE = {"今天", "手机", "我们", "昨天", "恶魔", "我是", "搜索", "我知道", "他说", "不知道", 
                "想你", "谢谢", "祝福", "幸福", "人生", "孤独", "成长", "高兴", "喜悦", "回家", "过年", "老头", "出门", "秘密", "地下",
                "红色", "秘密", "热门", "绝密", "蓝色", "风云", "革命", "无情", "黑夜", "情歌", "一天", "钓鱼", "苍蝇", "鳗鱼", "狼",
                "一条", "小鱼", "世界", "小偷", "混蛋", "迷失", "恶人", "真实", "骗子", "天才", "20", "孩子", "司机", "大哥", "警察",
                "他们", "朋友", "宝贝", "时间", "傻瓜", "美丽", "恩情", "恩典", "长城", "阳光", "太阳", "结婚", "遇见", "女孩", "男孩",
                "少女", "美人", "明星", "女人", "如果", "聊天", "回头"};
        private static String[] PERSON_EXCLUDE = {"希望", "满意", "向往", "天天", "小雨", "青蛙", "兔子", "鸭子", "大米", "好人", "警官",
                "弯弯", "亲戚", "王子", "美女"};
        private static String[] QUANTIFIER_INCLUDE = {"集", "部", "季"};
        
        public static Set<String> persionLevelInfoSet = new HashSet<String>();
        public static Set<String> persionExcludeSet = new HashSet<String>();
        public static Set<String> appLevelInfoSet = new HashSet<String>();
        public static Set<String> appExcludeSet = new HashSet<String>();
        public static Set<String> videoLevelInfoSet = new HashSet<String>();
        public static Set<String> videoExcludeSet = new HashSet<String>();
        public static Set<String> areaLevelInfoSet = new HashSet<String>();
        public static Set<String> quantifierIncludeSet = new HashSet<String>();
        
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
            
            for (int i = 0; i < APP_EXCLUDE.length; i ++)
            {
                appExcludeSet.add(APP_EXCLUDE[i]);
            }
            
            for (int i = 0; i < QUANTIFIER_INCLUDE.length; i ++)
            {
                quantifierIncludeSet.add(QUANTIFIER_INCLUDE[i]);
            }
            
            for (int i = 0; i < VIDEO_LEVEL_INFO.length; i ++)
            {
                videoLevelInfoSet.add(VIDEO_LEVEL_INFO[i]);
            }
            
            for (int i = 0; i < VIDEO_EXCLUDE.length; i ++)
            {
                videoExcludeSet.add(VIDEO_EXCLUDE[i]);
            }
            
            for (int i = 0; i < AREA_LEVEL_INFO.length; i ++)
            {
                areaLevelInfoSet.add(AREA_LEVEL_INFO[i]);
            }
            
            for (int i = 0; i < PERSON_EXCLUDE.length; i ++)
            {
                persionExcludeSet.add(PERSON_EXCLUDE[i]);
            }
        }
    }
}

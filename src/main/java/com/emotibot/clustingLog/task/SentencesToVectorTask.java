package com.emotibot.clustingLog.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.common.element.Element;
import com.emotibot.clustingLog.model.ModelUtils;
import com.emotibot.clustingLog.nlp.NlpUtils;
import com.emotibot.clustingLog.response.SentencesToVectorResponse;
import com.emotibot.clustingLog.utils.WordUtils;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.response.nlu.NLUResponse;
import com.emotibot.middleware.response.nlu.Segment;
import com.emotibot.middleware.task.AbstractTask;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

/**
 * 首先依次调用NLU，获取分词结果，之后将结果汇总到Response中
 * 
 * 1. 当句子的长度小于特定长度（4），且不含有nud
 * 2. 过长的句子
 * 3. 句子中没有动词和名词 (名词就包含nud的)
 * 4. 句子中语气词出现三次以上
 * 
 * 5. 剩下的就是正常的句子，在计算句向量时对于动词和名词进行加权，对于语气词进行减权
 * 
 * a. 可以计算词向量的
 * b. 计算不出来词向量的(还要分是否有nud的)
 * 
 * 
 * 补充：1. 补充动词和名词，对于不同类型的动词和名词有不同的rate权重
 *      2. 对于withoutVectorWithNud，需要根据nud的levelInfo来分类
 * 
 * @author emotibot
 *
 */
public class SentencesToVectorTask extends AbstractTask
{

    private static Logger logger = Logger.getLogger(SentencesToVectorTask.class);
    private List<String> sentences = null;
    private Map<LogSelectType, List<Element>> resultElementMap = new HashMap<LogSelectType, List<Element>>();
    
    public SentencesToVectorTask()
    {
        
    }
    
    public SentencesToVectorTask(List<String> sentences)
    {
        this.sentences = sentences;
    }
    
    @Override
    public Response call() throws Exception
    {
        if (sentences == null || sentences.isEmpty())
        {
            return new SentencesToVectorResponse();
        }
        for (String sentence : sentences)
        {
            NLUResponse nlpResponse = NlpUtils.getNlp(sentence);
            nlpResponse = adjustNluResponse(nlpResponse);
            float[] vector = getVector(nlpResponse);
            Element element = new Element(sentence, vector, nlpResponse.getSegment());
            if (isValidElement(element))
            {
                selectLog(element);
            }
        }
        return new SentencesToVectorResponse(resultElementMap);
    }
    
    /**
     * 1. 如果nud长度为1，调用hanlp回归词性
     * 
     * 2. 如果句子中有nud内容全为数字（0-9，一-十），则统一制为m
     * 
     * @param nlpResponse
     * @return
     */
    private NLUResponse adjustNluResponse(NLUResponse nlpResponse)
    {
        if (nlpResponse == null)
        {
            return null;
        }
        List<Segment> segments = nlpResponse.getSegment();
        if (segments == null)
        {
            return nlpResponse;
        }
        for (Segment segment : segments)
        {
            if (segment.getPos().equals("nud"))
            {
                if (segment.getOrgWord().length() == 1)
                {
                    List<Term> termList = StandardTokenizer.segment(segment.getOrgWord());
                    Term term = termList.get(0);
                    segment.setPos(term.nature.name());
                }
                else if (WordUtils.isAllNum(segment.getOrgWord()))
                {
                    segment.setPos("m");
                }
            }
        }
        return nlpResponse;
    }
    
    private void selectLog(Element element)
    {
        //1. 当句子的长度小于特定长度（4），且不含有nud
        if (element.getText().length() <= Constants.SHORT_SENTENCE_THRESHOLD && !WordUtils.isContainNud(element))
        {
            setResultElementMap(element, LogSelectType.SHORT_LOG);
            return;
        }
        //2. 句子过长
        if (element.getText().length() >= Constants.LONG_SENTENCE_THRESHOLD)
        {
            setResultElementMap(element, LogSelectType.LONG_LOG);
            return;
        }
        //3. 句子中没有动词,名词和人名
        boolean tag = false;
        for (Segment segment : element.getSegments())
        {
            if (WordUtils.isVerb(segment.getPos()) || WordUtils.isNoun(segment.getPos()) 
                    || WordUtils.isPerson(segment.getPos()) || WordUtils.isLocation(segment.getPos(), segment.getOrgWord()))
            {
                tag = true;
                break;
            }
        }
        if (!tag)
        {
            setResultElementMap(element, LogSelectType.WITHOUT_V_N);
            return;
        }
        //4. 句子中语气词超过多个
        int count = 0;
        for (Segment segment : element.getSegments())
        {
            if (WordUtils.isModal(segment.getPos()))
            {
                count ++;
            }
        }
        if (count >= Constants.MODAL_WORD_NUM_THRESHOLD)
        {
            setResultElementMap(element, LogSelectType.TOO_MANNY_M);
            return;
        }
        //5. 是否有词向量，同时考虑是否有nud
        if (element.getVectors() == null)
        {
            if (WordUtils.isContainNud(element))
            {
                setResultElementMap(element, LogSelectType.WITHOUT_VECTOR_WITH_NUD);
            }
            else
            {
                setResultElementMap(element, LogSelectType.WITHOUT_VECTOR_WITHOUT_NUD);
            }
            return;
        }
        setResultElementMap(element, LogSelectType.NORMAL_LOG);
    }

    /**
     * 对于名词和动词，需要提示权重，对于语气词，需要降低权重
     * 
     * @param response
     * @return
     */
    private float[] getVector(NLUResponse response)
    {
        List<Segment> segments = response.getSegment();
        if (segments == null || segments.isEmpty())
        {
            return null;
        }
        float[] sentenceVector = null;
        for (Segment segment : segments)
        {
            float[] vector = ModelUtils.getWordVector(segment.getWord());
            if (vector != null)
            {
                if (sentenceVector == null)
                {
                    sentenceVector = initSentenceVector(vector.length);
                }
                float rate = WordUtils.getAdjustRate(segment.getPos());
                
                for (int i = 0; i < vector.length; i ++)
                {
                    sentenceVector[i] += vector[i] * rate;
                }
            }
        }
        if (sentenceVector == null)
        {
            logger.error("can not get vector from sentence: " + response.getQuery());
            return null;
        }
        return normalizeVector(sentenceVector);
    }
    
    private float[] normalizeVector(float[] originalVector)
    {
        if (originalVector == null)
        {
            return null;
        }
        float[] ret = new float[originalVector.length];
        float factor = 0;
        for (float vec : originalVector)
        {
            factor += vec * vec;
        }
        factor = (float) Math.sqrt(factor);
        for (int i = 0; i < originalVector.length; i ++)
        {
            ret[i] = originalVector[i] / factor;
        }
        return ret;
    }
    
    private void setResultElementMap(Element element, LogSelectType type)
    {
        List<Element> elementList = resultElementMap.get(type);
        if (elementList == null)
        {
            elementList = new ArrayList<Element>();
            resultElementMap.put(type, elementList);
        }
        elementList.add(element);
    }
    
    private boolean isValidElement(Element element)
    {
        if (element.getSegments() == null)
        {
            return false;
        }
        return true;
    }
    
    private float[] initSentenceVector(int len)
    {
        float[] sentenceVector = new float[len];
        for (int i = 0; i < len; i ++)
        {
            sentenceVector[i] = 0;
        }
        return sentenceVector;
    }
}

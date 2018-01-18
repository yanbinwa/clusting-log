package com.emotibot.clustingLog.task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.meanShift.MyDataPoint;
import com.emotibot.clustingLog.model.ModelUtils;
import com.emotibot.clustingLog.nlp.NlpUtils;
import com.emotibot.clustingLog.response.SentencesToVectorResponse2;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.response.nlu.NLUResponse;
import com.emotibot.middleware.response.nlu.Segment;
import com.emotibot.middleware.task.AbstractTask;

import jsat.linear.DenseVector;
import jsat.linear.Vec;

@Deprecated
public class SentencesToVectorTask2 extends AbstractTask
{
    private static Logger logger = Logger.getLogger(SentencesToVectorTask2.class);
    private List<String> sentences = null;
    
    public SentencesToVectorTask2()
    {
        
    }
    
    public SentencesToVectorTask2(List<String> sentences)
    {
        this.sentences = sentences;
    }
    
    @Override
    public Response call() throws Exception
    {
        if (sentences == null || sentences.isEmpty())
        {
            return new SentencesToVectorResponse2();
        }
        Set<MyDataPoint> elements = new HashSet<MyDataPoint>();
        for (String sentence : sentences)
        {
            NLUResponse nlpResponse = NlpUtils.getNlp(sentence);
            double[] vector = getVector(nlpResponse);
            if (vector != null)
            {
                elements.add(getDataPoint(vector, sentence));
            }
        }
        return new SentencesToVectorResponse2(elements);
    }

    private double[] getVector(NLUResponse response)
    {
        List<Segment> segments = response.getSegment();
        if (segments == null || segments.isEmpty())
        {
            return null;
        }
        List<double[]> wordVectors = new ArrayList<double[]>();
        for (Segment segment : segments)
        {
            float[] vector = ModelUtils.getWordVector(segment.getWord());
            if (vector != null)
            {
                wordVectors.add(floatToDouble(vector));
            }
        }
        if (wordVectors.isEmpty())
        {
            logger.error("can not get vector from sentence: " + response.getQuery());
            return null;
        }
        int vectorLen = wordVectors.get(0).length;
        double[] sentenceVector = new double[vectorLen];
        for (int i = 0; i < vectorLen; i ++)
        {
            sentenceVector[i] = 0;
        }
        for (double[] vector : wordVectors)
        {
            for (int i = 0; i < vectorLen; i ++)
            {
                sentenceVector[i] += vector[i];
            }
        }
        for (int i = 0; i < vectorLen; i ++)
        {
            sentenceVector[i] = sentenceVector[i] / wordVectors.size();
        }
        return normalizeVector(sentenceVector);
    }
    
    private double[] floatToDouble(float[] vectors)
    {
        if (vectors == null)
        {
            return null;
        }
        double[] ret= new double[vectors.length];
        for (int i = 0; i < vectors.length; i ++)
        {
            ret[i] = new Float(vectors[i]).doubleValue();
        }
        return ret;
    }
    
    private MyDataPoint getDataPoint(double[] vectors, String sentence)
    {
        List<Double> vecList = new ArrayList<Double>();
        for (int i = 0; i < vectors.length; i ++)
        {
            vecList.add(vectors[i]);
        }
        Vec vec = new DenseVector(vecList);
        MyDataPoint ret = new MyDataPoint(vec, sentence);
        return ret;
    }
    
    private double[] normalizeVector(double[] originalVector)
    {
        if (originalVector == null)
        {
            return null;
        }
        double[] ret = new double[originalVector.length];
        double factor = 0;
        for (double vec : originalVector)
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
}

package com.emotibot.clustingLog.meanShift;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.emotibot.clustingLog.model.ModelUtils;

import jsat.linear.DenseVector;
import jsat.linear.Vec;

public class VecTest
{
    
    public static String word = "的";
    
    @Test
    public void test()
    {
        float[] vectors = ModelUtils.getWordVector(word);
        List<Double> vecList = new ArrayList<Double>();
        for (float vector : vectors)
        {
            vecList.add(new Float(vector).doubleValue());
        }
        Vec vec = new DenseVector(vecList);
        System.out.println(vec);
    }

}

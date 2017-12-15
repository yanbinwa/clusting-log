package com.emotibot.clustingLog.model;

public class ModelUtils
{
    private static Word2VecModel model = new Word2VecModelImpl();
    
    public static float[] getWordVector(String word)
    {
        return model.getVectors(word);
    }
}

package com.emotibot.clustingLog.model;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.middleware.conf.ConfigManager;

public class Word2VecModelImpl implements Word2VecModel
{
    private static Logger logger = Logger.getLogger(Word2VecModelImpl.class);
    private static final int MAX_SIZE = 50;
    
    private Map<String, float[]> wordMap = new HashMap<String, float[]>();
    private String word2VecBinFile = null;
    private String word2VecRetFile = null;
    
    public Word2VecModelImpl()
    {
        word2VecBinFile = ConfigManager.INSTANCE.getPropertyString(Constants.WORD_TO_VECTOR_BIN_FILE_KEY);
        word2VecRetFile = ConfigManager.INSTANCE.getPropertyString(Constants.WORD_TO_VECTOR_RET_FILE_KEY);
        loadModel();
    }
    
    @Override
    public float[] getVectors(String word)
    {
        return wordMap.get(word);
    }
    
    private void loadModel()
    {
        DataInputStream dis = null;
        BufferedInputStream bis = null;
        double len = 0;
        float vector = 0;
        FileWriter fw = null;
        try
        {
            bis = new BufferedInputStream(new FileInputStream(word2VecBinFile));
            fw = new FileWriter(word2VecRetFile);
            dis = new DataInputStream(bis);
            int words = Integer.parseInt(readString(dis));
            int size = Integer.parseInt(readString(dis));
            String word;
            float[] vectors = null;
            for (int i = 0; i < words; i++) 
            {
                word = readString(dis);
                vectors = new float[size];
                len = 0;
                for (int j = 0; j < size; j++) 
                {
                    vector = readFloat(dis);
                    len += vector * vector;
                    vectors[j] = (float) vector;
                }
                len = Math.sqrt(len);

                for (int j = 0; j < size; j++) 
                {
                    vectors[j] /= len;
                }

                wordMap.put(word, vectors);
                fw.write(word + "\r\n");
                dis.read();
            }
        }
        catch(Exception e)
        {
            logger.error("Fail to load model");
        }
        finally
        {
            try
            {
                bis.close();
                dis.close();
                fw.close();
            }
            catch(Exception e)
            {
                
            }
        }
    }
    
    private String readString(DataInputStream dis) throws IOException 
    {
        byte[] bytes = new byte[MAX_SIZE];
        byte b = dis.readByte();
        int i = -1;
        StringBuilder sb = new StringBuilder();
        while (b != 32 && b != 10) 
        {
            i++;
            bytes[i] = b;
            b = dis.readByte();
            if (i == 49) 
            {
                sb.append(new String(bytes));
                i = -1;
                bytes = new byte[MAX_SIZE];
            }
        }
        sb.append(new String(bytes, 0, i + 1));
        return sb.toString();
    }
    
    private float readFloat(InputStream is) throws IOException 
    {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }
    
    private float getFloat(byte[] b) 
    {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }
}

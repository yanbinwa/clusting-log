package com.emotibot.clustingLog.nlp;

import org.apache.log4j.Logger;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.dictionary.exception.ServiceNotReadyException;
import com.emotibot.dictionary.request.DictionaryRequest;
import com.emotibot.dictionary.service.DictionaryService;
import com.emotibot.dictionary.service.DictionaryServiceImpl;
import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.response.nlu.NLUResponse;

public class NlpUtils
{
    private static DictionaryService service = null;
    private static Logger logger = Logger.getLogger(NlpUtils.class);
    private static String appId = ConfigManager.INSTANCE.getPropertyString(Constants.APPID_KEY);
    
    static
    {
        try
        {
            service = new DictionaryServiceImpl();
            service.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            service = null;
        }
    }
    
//    public static NLUResponse getNlp(String text)
//    {
//        NLUTask task = new NLUTask();
//        String params = "?f=synonymSegment&appid=5a200ce8e6ec3a6506030e54ac3b970e&q=" + UrlUtils.urlEncode(text);
//        String hostname = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_HOST_KEY);
//        String port = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_PORT_KEY);
//        String endpoint = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_ENDPOINT_KEY);
//        String url = UrlUtils.getUrl(hostname, port, endpoint, params);
//        HttpRequest request = new HttpRequest(url, null, HttpRequestType.GET);
//        task.setRequest(request);
//        NLUResponse nluResponse = null;
//        try
//        {
//            nluResponse = (NLUResponse) task.call();
//        } 
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//        return nluResponse;
//    }
    
    public static NLUResponse getNlp(String text)
    {
        if (service == null)
        {
            logger.error("DictionaryService is not ready");
            return null;
        }
        DictionaryRequest request = new DictionaryRequest();
        request.setAppid(appId);
        request.setText(text);
        try
        {
            String retJsonStr = service.getWord(request);
            if (retJsonStr == null)
            {
                logger.error("can not get nlp response for " + text);
                return null;
            }
            retJsonStr = "[" + retJsonStr + "]";
            NLUResponse response = new NLUResponse(retJsonStr);
            return response;
        } 
        catch (ServiceNotReadyException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean isNlpReady()
    {
        if (service == null)
        {
            logger.error("DictionaryService is not build");
            return false;
        }
        return service.isReady();
    }
    
    public static void test()
    {
        
    }
    
    static class MyConstants
    {
        public static String NLU_HOST_KEY = "NLU_HOST";
        public static String NLU_PORT_KEY = "NLU_PORT";
        public static String NLU_ENDPOINT_KEY = "NLU_ENDPOINT";
    }
}

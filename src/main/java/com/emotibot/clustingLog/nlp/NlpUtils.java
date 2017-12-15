package com.emotibot.clustingLog.nlp;

import com.emotibot.middleware.conf.ConfigManager;
import com.emotibot.middleware.request.HttpRequest;
import com.emotibot.middleware.request.HttpRequestType;
import com.emotibot.middleware.response.nlu.NLUResponse;
import com.emotibot.middleware.task.restCallTask.NLUTask;
import com.emotibot.middleware.utils.UrlUtils;

public class NlpUtils
{
    public static NLUResponse getNlp(String text)
    {
        NLUTask task = new NLUTask();
        String params = "?f=segment&appid=5a200ce8e6ec3a6506030e54ac3b970e&q=" + UrlUtils.urlEncode(text);
        String hostname = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_HOST_KEY);
        String port = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_PORT_KEY);
        String endpoint = ConfigManager.INSTANCE.getPropertyString(MyConstants.NLU_ENDPOINT_KEY);
        String url = UrlUtils.getUrl(hostname, port, endpoint, params);
        HttpRequest request = new HttpRequest(url, null, HttpRequestType.GET);
        task.setRequest(request);
        NLUResponse nluResponse = null;
        try
        {
            nluResponse = (NLUResponse) task.call();
        } 
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return nluResponse;
    }
    
    static class MyConstants
    {
        public static String NLU_HOST_KEY = "NLU_HOST";
        public static String NLU_PORT_KEY = "NLU_PORT";
        public static String NLU_ENDPOINT_KEY = "NLU_ENDPOINT";
    }
}

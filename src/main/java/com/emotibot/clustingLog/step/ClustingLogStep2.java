package com.emotibot.clustingLog.step;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.emotibot.clustingLog.common.constants.Constants;
import com.emotibot.clustingLog.response.SentencesToVectorResponse2;
import com.emotibot.clustingLog.task.SentencesToVectorTask2;
import com.emotibot.middleware.context.Context;
import com.emotibot.middleware.response.Response;
import com.emotibot.middleware.step.AbstractStep;

import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.MeanShift;
import jsat.distributions.empirical.kernelfunc.GaussKF;
import jsat.distributions.multivariate.MetricKDE;
import jsat.linear.Vec;
import jsat.linear.VecPaired;
import jsat.linear.distancemetrics.EuclideanDistance;
import jsat.linear.vectorcollection.DefaultVectorCollectionFactory;

public class ClustingLogStep2 extends AbstractStep
{

    private static final int TASK_MAX_NUM = 10;
    private MeanShift meanShift = null;
    ExecutorService meanShiftThreadPool = Executors.newFixedThreadPool(10);
    
    public ClustingLogStep2()
    {
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
        initKMeans();
    }
    
    public ClustingLogStep2(ExecutorService executorService)
    {
        super(executorService);
        this.timeout = Constants.CLUSTING_LOG_STEP_TIMEOUT;
        initKMeans();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void beforeRun(Context context)
    {
        Set<String> sentences = (Set<String>) context.getValue(Constants.CLUSTING_LOG_SENTENCES_KEY);
        if (sentences == null || sentences.isEmpty())
        {
            return;
        }
        List<String> sentenceList = new ArrayList<String>(sentences);
        int task_num = TASK_MAX_NUM;
        if (sentenceList.size() < task_num)
        {
            task_num = sentenceList.size();
        }
        int sentencePerTask = sentenceList.size() / task_num;
        for (int i = 0; i < task_num; i ++)
        {
            int start = i * sentencePerTask;
            int end = (i + 1) * sentencePerTask;
            if (i == task_num - 1)
            {
                end = sentenceList.size();
            }
            SentencesToVectorTask2 task = new SentencesToVectorTask2(sentenceList.subList(start, end));
            this.addTask(context, task);
        }
    }

    @Override
    public void afterRun(Context context)
    {
        List<Response> responseList = this.getOutputMap(context).get(com.emotibot.clustingLog.task.MyResponseType.SENTENCE2VECTER);
        if (responseList == null)
        {
            return;
        }
        Set<DataPoint> totalElements = new HashSet<DataPoint>();
        for (Response response : responseList)
        {
            SentencesToVectorResponse2 sentencesToResponse = (SentencesToVectorResponse2) response;
            if (sentencesToResponse.getElements() != null)
            {
                totalElements.addAll(sentencesToResponse.getElements());
            }
        }
        SimpleDataSet dataSet = new SimpleDataSet(new ArrayList<DataPoint>(totalElements));
        List<List<DataPoint>> cluster = meanShift.cluster(dataSet, meanShiftThreadPool);
        context.setValue(Constants.CLUSTING_LOG_RESULT_KEY, cluster);
    }
    
    private void initKMeans()
    {
        MetricKDE kde = new MetricKDE(GaussKF.getInstance(), new EuclideanDistance(), new DefaultVectorCollectionFactory<VecPaired<Vec, Integer>>(), 3, 2);
        meanShift = new MeanShift(kde);
        meanShift.setScaleBandwidthFactor(0.05);
    }

}

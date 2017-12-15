package com.emotibot.clustingLog.common.constants;

public class Constants
{
    public static final String WORD_TO_VECTOR_BIN_FILE_KEY = "WORD_TO_VECTOR_BIN_FILE";
    public static final String WORD_TO_VECTOR_RET_FILE_KEY = "WORD_TO_VECTOR_RET_FILE";
    
    public static final int CLUSTING_LOG_STEP_TIMEOUT = 10000000;
    public static final int TAG_LOG_STEP_TIMEOUT = 1000;
    
    public static final String CLUSTING_LOG_SENTENCES_KEY = "CLUSTING_LOG_SENTENCES";
    public static final String CLUSTING_LOG_RESULT_KEY = "CLUSTING_LOG_RESULT";
    public static final String CLUSTING_LOG_SUMMARY_MAP_KEY = "CLUSTING_LOG_SUMMARY_MAP";
    
    public static final String CLUSTING_LOG_RETURN_KEY = "CLUSTING_LOG_RETURN";
    
    public static final String CLUSTING_LOG_SHORT_KEY = "CLUSTING_LOG_SHORT";
    public static final String CLUSTING_LOG_LONG_KEY = "CLUSTING_LOG_LONG";
    public static final String CLUSTING_LOG_WITHOUT_V_N_KEY = "CLUSTING_LOG_WITHOUT_V_N";
    public static final String CLUSTING_LOG_TOO_MANNY_M_KEY = "CLUSTING_LOG_TOO_MANNY_M";
    public static final String CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_KEY = "CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD";
    public static final String CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_KEY = "CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD";
    public static final String CLUSTING_LOG_OUTPUT_KEY = "CLUSTING_LOG_BADCASE";
    public static final String CLUSTING_LOG_DROP_KEY = "CLUSTING_LOG_DROP";
    public static final String CLUSTING_LOG_TYPE_KEY = "CLUSTING_LOG_TYPE";
    
    public static final String CLUSTING_LOG_SHORT_FILE_KEY = "CLUSTING_LOG_SHORT_FILE";
    public static final String CLUSTING_LOG_LONG_FILE_KEY = "CLUSTING_LOG_LONG_FILE";
    public static final String CLUSTING_LOG_WITHOUT_V_N_FILE_KEY = "CLUSTING_LOG_WITHOUT_V_N_FILE";
    public static final String CLUSTING_LOG_TOO_MANNY_M_FILE_KEY = "CLUSTING_LOG_TOO_MANNY_M_FILE";
    public static final String CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_FILE_KEY = "CLUSTING_LOG_WITHOUT_VECTOR_WITH_NUD_FILE";
    public static final String CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_FILE_KEY = "CLUSTING_LOG_WITHOUT_VECTOR_WITHOUT_NUD_FILE";
    public static final String CLUSTING_LOG_OUTPUT_FILE_KEY = "CLUSTING_LOG_OUTPUT_FILE";
    public static final String CLUSTING_LOG_DROP_FILE_KEY = "CLUSTING_LOG_DROP_FILE";
    public static final String CLUSTING_LOG_TYPE_FILE_KEY = "CLUSTING_LOG_TYPE_FILE";
    
    //SentencesToVectorTask
    public static final int SHORT_SENTENCE_THRESHOLD = 4;
    public static final int LONG_SENTENCE_THRESHOLD = 20;
    public static final int MODAL_WORD_NUM_THRESHOLD = 3;
    public static final float ADJUST_VERB_RATE = 3.0f;
    public static final float ADJUST_NONE_RATE = 2.0f;
    public static final float ADJUST_MODAL_RATE = 0.1f;
    public static final float ADJUST_PRONOUN_RATE = 0.1f;
    
    
    public static final float CHOOSE_THRESHOLD = 0.001f;
}
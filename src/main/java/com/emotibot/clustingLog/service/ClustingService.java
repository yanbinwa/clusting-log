package com.emotibot.clustingLog.service;

import java.util.List;
import java.util.Set;

public interface ClustingService
{
    public List<Set<String>> clustingLog(String csvFile);
}

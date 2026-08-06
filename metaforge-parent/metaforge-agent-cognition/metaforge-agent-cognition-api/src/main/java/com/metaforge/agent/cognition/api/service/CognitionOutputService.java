package com.metaforge.agent.cognition.api.service;

public interface CognitionOutputService {

    String formatJson(Object result);

    String formatPrompt(Object result);
}

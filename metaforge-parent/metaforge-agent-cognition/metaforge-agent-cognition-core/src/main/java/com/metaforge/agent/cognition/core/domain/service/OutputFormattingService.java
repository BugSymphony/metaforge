package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;

public interface OutputFormattingService {

    String format(GuidanceResult result, OutputFormat format);
}

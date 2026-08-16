package com.metaforge.agent.cognition.api.spi;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;

public interface CognitionOperator {

    String operatorId();

    DimensionCategory category();

    CognitionResult execute(CognitionQueryContext context);
}

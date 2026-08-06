package com.metaforge.agent.cognition.core.domain.port;

import java.util.List;

public interface GraphClientPort {

    Object getOutboundRelations(String entityFqn, List<String> relationTypes, List<String> targetEntityTypes);

    Object getInboundRelations(String entityFqn, List<String> relationTypes, List<String> sourceEntityTypes);

    Object multiFilter(Object criteria);

    Object getDependentRelations(String entityFqn);

    int getRelationCount(String entityFqn);
}

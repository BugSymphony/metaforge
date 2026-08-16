package com.metaforge.agent.cognition.operator.deontic;

import com.metaforge.agent.cognition.api.enums.DimensionCategory;
import com.metaforge.agent.cognition.api.spi.CognitionQueryContext;
import com.metaforge.agent.cognition.api.spi.CognitionResult;
import com.metaforge.agent.cognition.operator.common.AbstractCognitionOperator;
import com.metaforge.agent.cognition.operator.common.MetaforgeLibraryFqns;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则清单——按关系 schema 精确绑定 {@code RuleAppliesTo}（ExecutionRule → ExecutionStep），
 * 入边即得适用于 X 的约束规则。
 */
@Component
public class DeonticRuleListingOperator extends AbstractCognitionOperator {

    private static final List<String> DEFAULT_RULE_RELATIONS = List.of(
            MetaforgeLibraryFqns.Relation.RULE_APPLIES_TO);

    @Override
    public String operatorId() {
        return "deontic.rule-listing";
    }

    @Override
    public DimensionCategory category() {
        return DimensionCategory.DEONTIC;
    }

    @Override
    public CognitionResult execute(CognitionQueryContext context) {
        String entityFqn = context.entityFqn();
        if (entityFqn == null || entityFqn.isBlank()) {
            return wrapFailure("缺少 entityFqn 参数");
        }

        Map<String, Object> config = context.operatorConfig();
        Object relResult = executeWithPort(() -> queryRelationsBySchema(
                entityFqn, RelationDirection.INBOUND, config, DEFAULT_RULE_RELATIONS, null));
        if (relResult instanceof CognitionResult cr) return cr;

        List<String> ruleFqns = new ArrayList<>();
        if (relResult instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof RelationInstanceDto dto) {
                    String ruleFqn = resolvePeerFqn(dto, RelationDirection.INBOUND);
                    if (ruleFqn != null && !ruleFqn.isBlank()) {
                        ruleFqns.add(ruleFqn);
                    }
                }
            }
        }

        List<Map<String, Object>> detailedRules = new ArrayList<>();
        for (String ruleFqn : ruleFqns) {
            Object detail = executeWithPort(() -> metadataReadPort.getByFqn(ruleFqn));
            if (detail instanceof MetadataEntityDto dto) {
                detailedRules.add(toContentMap(dto));
            }
        }

        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("rules", detailedRules);
        resultData.put("count", detailedRules.size());
        resultData.put("entityFqn", entityFqn);

        return CognitionResult.success(operatorId(), category(), resultData);
    }
}

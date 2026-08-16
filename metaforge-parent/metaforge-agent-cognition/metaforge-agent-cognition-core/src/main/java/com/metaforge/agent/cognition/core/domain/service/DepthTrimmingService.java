package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepthTrimmingService {

    private static final Logger log = LoggerFactory.getLogger(DepthTrimmingService.class);

    private static final int MIN_KEEP = 3;

    public TrimResult trim(List<OperatorDefinition> operators, CognitionDepth depth) {
        if (operators == null || operators.isEmpty()) {
            return new TrimResult(List.of(), List.of());
        }

        if (depth == CognitionDepth.L3) {
            return new TrimResult(new ArrayList<>(operators), List.of());
        }

        List<OperatorDefinition> requiredOps = operators.stream()
                .filter(OperatorDefinition::isRequired)
                .toList();

        List<OperatorDefinition> optionalOps = operators.stream()
                .filter(op -> !op.isRequired())
                .sorted(Comparator.comparingInt(OperatorDefinition::getPriority).reversed())
                .toList();

        int optionalCount = optionalOps.size();
        double ratio = depth.getTrimRatio();
        int keepCount = Math.max(MIN_KEEP, (int) Math.ceil(optionalCount * ratio));
        keepCount = Math.min(keepCount, optionalCount);

        List<OperatorDefinition> trimmed = new ArrayList<>(requiredOps);
        List<String> truncated = new ArrayList<>();

        if (keepCount < optionalCount) {
            for (int i = keepCount; i < optionalCount; i++) {
                String opId = optionalOps.get(i).getOperatorId();
                String prefix = opId.contains(".") ? opId.substring(0, opId.indexOf('.')) : opId;
                truncated.add(prefix);
            }
        }

        if (keepCount > 0) {
            trimmed.addAll(optionalOps.subList(0, keepCount));
        }

        log.debug("深度裁剪: depth={}, total={}, required={}, optional={}/{}, truncated={}",
                depth, operators.size(), requiredOps.size(), keepCount, optionalCount, truncated.size());

        return new TrimResult(trimmed, truncated);
    }

    public static class TrimResult {
        public final List<OperatorDefinition> trimmedOperators;
        public final List<String> truncatedPerspectives;

        public TrimResult(List<OperatorDefinition> trimmedOperators, List<String> truncatedPerspectives) {
            this.trimmedOperators = trimmedOperators;
            this.truncatedPerspectives = truncatedPerspectives;
        }
    }
}

package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.CognitionDepth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepthTrimmingService {

    private static final Logger log = LoggerFactory.getLogger(DepthTrimmingService.class);

    public int getMaxPerspectives(CognitionDepth depth) {
        if (depth == null) {
            log.warn("认知深度为空，回退默认 L2");
            return CognitionDepth.L2.maxPerspectives();
        }
        return depth.maxPerspectives();
    }

    public List<String> trimPerspectives(List<String> perspectiveIds, CognitionDepth depth) {
        if (perspectiveIds == null || perspectiveIds.isEmpty()) {
            return perspectiveIds;
        }

        int maxCount = getMaxPerspectives(depth);
        if (perspectiveIds.size() <= maxCount) {
            return perspectiveIds;
        }

        log.debug("深度裁剪: 从 {} 个视角裁剪至 {} (depth={})", perspectiveIds.size(), maxCount, depth);
        return perspectiveIds.subList(0, maxCount);
    }

    public CognitionDepth resolveDepth(String depthValue) {
        if (depthValue == null) {
            return CognitionDepth.L2;
        }
        try {
            return CognitionDepth.valueOf(depthValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知认知深度值 '{}'，回退默认 L2", depthValue);
            return CognitionDepth.L2;
        }
    }
}

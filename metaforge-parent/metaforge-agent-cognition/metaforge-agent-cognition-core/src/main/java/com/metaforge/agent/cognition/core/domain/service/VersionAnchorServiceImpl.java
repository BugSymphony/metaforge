package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.valueobject.DataVersionAnchor;
import com.metaforge.agent.cognition.core.domain.port.MetamodelClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VersionAnchorServiceImpl implements VersionAnchorService {

    private static final Logger log = LoggerFactory.getLogger(VersionAnchorServiceImpl.class);

    private final MetamodelClientPort metamodelClientPort;

    public VersionAnchorServiceImpl(MetamodelClientPort metamodelClientPort) {
        this.metamodelClientPort = metamodelClientPort;
    }

    @Override
    public List<DataVersionAnchor> resolveAnchors(List<String> bundleFqns) {
        if (bundleFqns == null || bundleFqns.isEmpty()) {
            return List.of();
        }

        List<DataVersionAnchor> anchors = new ArrayList<>();
        Instant queriedAt = Instant.now();

        for (String bundleFqn : bundleFqns) {
            try {
                String publishedVersionFqn = metamodelClientPort.getLatestPublishedVersion(bundleFqn);
                int latestVersionNumber = extractVersionNumber(publishedVersionFqn);

                anchors.add(new DataVersionAnchor(
                        bundleFqn,
                        publishedVersionFqn,
                        latestVersionNumber,
                        queriedAt));
            } catch (Exception e) {
                log.warn("无法获取 Bundle 版本信息: bundleFqn={}, error={}", bundleFqn, e.getMessage());
                anchors.add(new DataVersionAnchor(
                        bundleFqn,
                        null,
                        0,
                        queriedAt));
            }
        }

        log.debug("解析版本锚完成: anchors={}", anchors.size());
        return anchors;
    }

    private int extractVersionNumber(String publishedVersionFqn) {
        if (publishedVersionFqn == null) return 0;
        try {
            int colon = publishedVersionFqn.indexOf(':');
            String version = colon >= 0 ? publishedVersionFqn.substring(colon + 1) : publishedVersionFqn;
            String firstSegment = version.split("\\.")[0].replaceAll("[^0-9]", "");
            return firstSegment.isEmpty() ? 0 : Integer.parseInt(firstSegment);
        } catch (NumberFormatException e) {
            log.debug("无法从版本 FQN 提取版本号: {}", publishedVersionFqn);
        }
        return 0;
    }
}

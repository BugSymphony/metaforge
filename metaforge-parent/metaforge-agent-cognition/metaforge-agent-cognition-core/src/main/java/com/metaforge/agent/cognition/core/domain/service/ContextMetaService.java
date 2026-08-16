package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.dto.response.ContextMeta;
import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContextMetaService {

    private static final Logger log = LoggerFactory.getLogger(ContextMetaService.class);

    private static final Pattern VERSION_SUFFIX_PATTERN =
            Pattern.compile("^(\\w[\\w-]*)(:\\d+\\.\\d+\\.\\d+)$");

    private final MetamodelReadPort metamodelReadPort;

    public ContextMetaService(@Autowired(required = false) MetamodelReadPort metamodelReadPort) {
        this.metamodelReadPort = metamodelReadPort;
    }

    public ContextMeta generate(String templateId, Scope scopeApplied,
                                 int tokenEstimate, List<String> skippedEntities,
                                 List<String> truncatedPerspectives) {
        List<String> versionAnchors = resolveVersionAnchors(scopeApplied);

        return new ContextMeta(
                templateId,
                versionAnchors,
                scopeApplied != null ? scopeApplied : Scope.EMPTY,
                tokenEstimate,
                Instant.now(),
                skippedEntities != null ? skippedEntities : Collections.emptyList(),
                truncatedPerspectives != null ? truncatedPerspectives : Collections.emptyList()
        );
    }

    private List<String> resolveVersionAnchors(Scope scope) {
        if (scope == null || scope.isEmpty() || scope.bundles() == null) {
            return Collections.emptyList();
        }

        List<String> anchors = new ArrayList<>();
        for (String bundle : scope.bundles()) {
            if (metamodelReadPort != null) {
                try {
                    String baseName = extractBaseBundleName(bundle);
                    Object versions = metamodelReadPort.listBundleVersions(baseName);
                    if (versions != null) {
                        anchors.add(bundle + "@latest");
                    } else {
                        anchors.add(bundle);
                    }
                } catch (Exception e) {
                    log.debug("版本锚解析失败 (P1 降级): bundle={}", bundle);
                    anchors.add(bundle);
                }
            } else {
                anchors.add(bundle);
            }
        }
        return anchors;
    }

    private String extractBaseBundleName(String bundleFqn) {
        Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(bundleFqn);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return bundleFqn;
    }
}

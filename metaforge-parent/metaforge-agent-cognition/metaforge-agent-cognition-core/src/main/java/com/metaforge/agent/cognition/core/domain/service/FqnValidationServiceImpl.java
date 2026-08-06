package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.exception.EmptyBundleFqnsException;
import com.metaforge.agent.cognition.core.domain.exception.InvalidBundleFqnException;
import com.metaforge.agent.cognition.core.domain.exception.InvalidEntityFqnException;
import com.metaforge.agent.cognition.core.domain.port.MetamodelClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class FqnValidationServiceImpl implements FqnValidationService {

    private static final Logger log = LoggerFactory.getLogger(FqnValidationServiceImpl.class);

    private static final Pattern FQN_PATTERN = Pattern.compile(
            "^[a-zA-Z][a-zA-Z0-9_-]*(?:[.:][a-zA-Z0-9_-]+)*$");

    private final MetamodelClientPort metamodelClientPort;

    public FqnValidationServiceImpl(MetamodelClientPort metamodelClientPort) {
        this.metamodelClientPort = metamodelClientPort;
    }

    @Override
    public void validateBundleFqns(List<String> bundleFqns) {
        if (bundleFqns == null || bundleFqns.isEmpty()) {
            throw new EmptyBundleFqnsException("bundle_fqns 参数不能为空");
        }
        for (String fqn : bundleFqns) {
            if (!FQN_PATTERN.matcher(fqn).matches()) {
                throw new InvalidBundleFqnException("Bundle FQN 格式非法: " + fqn);
            }
        }
        log.debug("Bundle FQN 校验通过: {}", bundleFqns);
    }

    @Override
    public String resolveBundleFromEntityFqn(String entityFqn) {
        if (entityFqn == null || entityFqn.isBlank()) {
            throw new InvalidEntityFqnException("entity_fqn 不能为空");
        }
        if (!FQN_PATTERN.matcher(entityFqn).matches()) {
            throw new InvalidEntityFqnException("实体 FQN 格式非法: " + entityFqn);
        }

        String bundleFqn = extractBundlePrefix(entityFqn);
        if (bundleFqn == null) {
            throw new InvalidEntityFqnException(
                    "无法从实体 FQN 提取 Bundle 前缀: " + entityFqn);
        }

        String resolved = metamodelClientPort.resolveBundleFqnByPrefix(bundleFqn);
        if (resolved == null) {
            throw new InvalidEntityFqnException(
                    "实体 FQN 前缀不属于任何已发布 Bundle: " + entityFqn);
        }

        log.debug("从实体 FQN 解析 Bundle: entityFqn={}, bundleFqn={}", entityFqn, resolved);
        return resolved;
    }

    private String extractBundlePrefix(String fqn) {
        if (fqn == null) return null;
        int colon = fqn.indexOf(':');
        if (colon > 0) {
            return fqn.substring(0, colon);
        }
        int dot = fqn.indexOf('.');
        if (dot > 0) {
            return fqn.substring(0, dot);
        }
        return fqn;
    }
}

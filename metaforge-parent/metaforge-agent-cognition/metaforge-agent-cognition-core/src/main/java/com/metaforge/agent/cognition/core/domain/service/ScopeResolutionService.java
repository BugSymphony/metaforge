package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.dto.request.Scope;
import com.metaforge.agent.cognition.api.port.MetamodelReadPort;
import com.metaforge.agent.cognition.core.domain.exception.EntityOutOfScopeException;
import com.metaforge.agent.cognition.core.domain.exception.InvalidScopeException;
import com.metaforge.agent.cognition.core.domain.exception.MissingScopeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ScopeResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ScopeResolutionService.class);

    private static final Pattern BUNDLE_FQN_PATTERN =
            Pattern.compile("[a-z][a-z0-9_-]{2,63}(:\\d+\\.\\d+\\.\\d+)?");

    private static final Pattern VERSION_SUFFIX_PATTERN =
            Pattern.compile("^(\\w[\\w-]*)(:\\d+\\.\\d+\\.\\d+)$");

    private final MetamodelReadPort metamodelReadPort;

    public ScopeResolutionService(@Autowired(required = false) MetamodelReadPort metamodelReadPort) {
        this.metamodelReadPort = metamodelReadPort;
    }

    public ScopeValidationResult validateScope(Scope scope, boolean scopeRequired, String entityFqn) {
        ScopeValidationResult result = new ScopeValidationResult();
        result.validatedScope = scope != null ? scope : Scope.EMPTY;

        if (scopeRequired && (scope == null || scope.isEmpty())) {
            throw new MissingScopeException("(未提供 templateId)");
        }

        if (scope == null || scope.isEmpty()) {
            return result;
        }

        if (scope.bundles() != null) {
            for (String bundle : scope.bundles()) {
                if (bundle == null || bundle.isBlank()) {
                    result.skippedEntities.add("bundle:(blank)");
                    continue;
                }

                if (!BUNDLE_FQN_PATTERN.matcher(bundle).matches()) {
                    log.warn("Bundle FQN 格式无效: {}", bundle);
                    throw new InvalidScopeException("Bundle FQN 格式无效: " + bundle);
                }

                if (metamodelReadPort != null) {
                    try {
                        String bundleName = extractBaseBundleName(bundle);
                        Object bundleDto = metamodelReadPort.getBundle(bundleName);
                        if (bundleDto == null) {
                            log.warn("Bundle 不存在: {}", bundle);
                            throw new InvalidScopeException("Bundle 不存在或未发布: " + bundle);
                        }
                    } catch (InvalidScopeException ex) {
                        throw ex;
                    } catch (Exception e) {
                        log.warn("Bundle 查询失败 (P1 降级为语法校验通过): {}", e.getMessage());
                    }
                } else {
                    log.debug("MetamodelReadPort 未就绪，Bundle FQN 仅执行语法校验: {}", bundle);
                }
            }
        }

        if (scope.packages() != null) {
            for (String pkg : scope.packages()) {
                if (pkg == null || pkg.isBlank()) {
                    result.skippedEntities.add("package:(blank)");
                    continue;
                }
                if (metamodelReadPort != null) {
                    try {
                        boolean exported = metamodelReadPort.isPackageExported(pkg, pkg);
                        if (!exported) {
                            log.debug("Package 未导出: {}", pkg);
                        }
                    } catch (Exception e) {
                        log.debug("Package 查询失败 (P1 降级): {}", e.getMessage());
                    }
                }
            }
        }

        if (scope.entitySchemas() != null) {
            for (String es : scope.entitySchemas()) {
                if (es == null || es.isBlank()) {
                    result.skippedEntities.add("entitySchema:(blank)");
                    continue;
                }
                if (metamodelReadPort != null) {
                    try {
                        Object schema = metamodelReadPort.getEntitySchema(es);
                        if (schema == null) {
                            log.warn("EntitySchema 不存在: {}", es);
                            throw new InvalidScopeException("EntitySchema 不存在: " + es);
                        }
                    } catch (InvalidScopeException ex) {
                        throw ex;
                    } catch (Exception e) {
                        log.warn("EntitySchema 查询失败 (P1 降级为语法校验通过): {}", e.getMessage());
                    }
                }
            }
        }

        if (entityFqn != null && !entityFqn.isBlank()) {
            boolean inScope = isEntityInScope(entityFqn, scope);
            if (!inScope) {
                result.skippedEntities.add(entityFqn);
                throw new EntityOutOfScopeException(entityFqn, scope.toString());
            }
        }

        return result;
    }

    private boolean isEntityInScope(String entityFqn, Scope scope) {
        if (scope.isEmpty()) {
            return true;
        }

        if (scope.bundles() != null && !scope.bundles().isEmpty()) {
            boolean matchesBundle = scope.bundles().stream()
                    .anyMatch(entityFqn::startsWith);
            if (!matchesBundle) {
                log.debug("entityFqn {} 不在 scope.bundles 范围内: {}", entityFqn, scope.bundles());
                return false;
            }
        }

        if (scope.entitySchemas() != null && !scope.entitySchemas().isEmpty()) {
            boolean matchesSchema = scope.entitySchemas().stream()
                    .anyMatch(entityFqn::contains);
            if (!matchesSchema) {
                log.debug("entityFqn {} 不在 scope.entitySchemas 范围内", entityFqn);
                return false;
            }
        }

        return true;
    }

    private String extractBaseBundleName(String bundleFqn) {
        java.util.regex.Matcher matcher = VERSION_SUFFIX_PATTERN.matcher(bundleFqn);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return bundleFqn;
    }

    public static class ScopeValidationResult {
        public Scope validatedScope;
        public final List<String> skippedEntities = new ArrayList<>();
    }
}

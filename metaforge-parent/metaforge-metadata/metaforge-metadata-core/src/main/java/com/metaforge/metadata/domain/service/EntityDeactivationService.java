package com.metaforge.metadata.domain.service;

import com.metaforge.metadata.api.dto.response.DeactivationCheckResult;
import com.metaforge.metadata.domain.exception.DeactivationBlockedException;
import com.metaforge.metadata.domain.exception.EntityNotFoundException;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class EntityDeactivationService {

    private static final Logger log = LoggerFactory.getLogger(EntityDeactivationService.class);

    private final MetadataEntityRepository entityRepository;

    public EntityDeactivationService(MetadataEntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Transactional
    public void deactivate(String fqn) {
        log.info("执行下线操作: fqn={}", fqn);

        if (!entityRepository.existsByFqn(fqn)) {
            throw new EntityNotFoundException("生效元数据不存在: " + fqn);
        }

        DeactivationCheckResult checkResult = checkPreconditions(fqn);
        if (!checkResult.isCanDeactivate()) {
            List<String> reasons = checkResult.getBlockReasons() != null
                    ? checkResult.getBlockReasons()
                    : List.of("存在拦截条件");
            throw new DeactivationBlockedException("下线被拦截", reasons);
        }

        entityRepository.deleteByFqn(fqn);
        log.info("下线操作完成: fqn={}", fqn);
    }

    public DeactivationCheckResult checkPreconditions(String fqn) {
        log.debug("检查下线前置条件: fqn={}", fqn);

        DeactivationCheckResult result = new DeactivationCheckResult();
        List<String> blockReasons = new ArrayList<>();
        List<String> activeChildren = new ArrayList<>();
        List<String> activeReferences = new ArrayList<>();

        String childPrefix = fqn + ".";
        List<String> childFqns = entityRepository.findFqnsByParentFqnPrefix(fqn);
        if (childFqns != null && !childFqns.isEmpty()) {
            activeChildren.addAll(childFqns);
            blockReasons.add("存在 " + childFqns.size() + " 个生效子实体: " + String.join(", ", childFqns));
        }

        log.debug("外部活跃引用检查（上游语义关系网络 BC 占位）: fqn={}", fqn);

        result.setActiveChildren(activeChildren);
        result.setActiveReferences(activeReferences);
        result.setBlockReasons(blockReasons);
        result.setCanDeactivate(blockReasons.isEmpty());

        return result;
    }
}

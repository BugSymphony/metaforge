package com.metaforge.graph.domain.service;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 依赖关系校验领域服务。
 * 判断目标关系是否被其他 DEPENDENCY_INFLUENCE 类型的生效关系引用。
 */
@Component
public class DependencyCheckService {

    private final RelationInstanceRepository instanceRepository;

    public DependencyCheckService(RelationInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    /**
     * 检查目标 FQN 是否被下游强依赖引用。
     *
     * @param targetFqn 被检查的关系 FQN
     * @return 下游强依赖关系 FQN 列表
     */
    public List<String> findBlockingDependencies(String targetFqn) {
        // 查询所有目标为 targetFqn 的 DEPENDENCY_INFLUENCE 类型的生效关系
        return instanceRepository.findByTargetEntityFqn(targetFqn).stream()
                .filter(r -> "DEPENDENCY_INFLUENCE".equals(r.getRelationType()))
                .map(r -> r.getFqnValue())
                .toList();
    }

    /**
     * 校验下线前置条件，若存在强依赖则抛出异常。
     *
     * @param relationFqn 待下线的关系 FQN
     * @throws DependencyBlockedException 如果存在下游强依赖
     */
    public void checkDeprecationPreconditions(String relationFqn) {
        List<String> blocking = findBlockingDependencies(relationFqn);
        if (!blocking.isEmpty()) {
            throw new DependencyBlockedException(
                    "存在下游强依赖，阻塞下线: " + relationFqn + ", 依赖关系: " + String.join(", ", blocking));
        }
    }

    public static class DependencyBlockedException extends GraphBizException {
        public DependencyBlockedException(String message) {
            super(GraphErrorCode.DEPENDENCY_BLOCKING, message);
        }
        @Override
        public String getErrorCodeName() { return "DEPENDENCY_BLOCKING"; }
    }
}

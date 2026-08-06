package com.metaforge.graph.domain.service;

import com.metaforge.graph.api.constant.GraphConstants;
import com.metaforge.graph.domain.model.valueobject.EntityFQN;
import com.metaforge.graph.domain.model.valueobject.FQN;

/**
 * FQN 生成器——统一生成与解析关系实例的 FQN。
 *
 * <p>格式：{源实体FQN}#{关系类型FQN}#{目标实体FQN}
 */
public final class FqnGenerator {

    private FqnGenerator() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 生成关系 FQN。
     *
     * @param source     源实体 FQN
     * @param relationTypeFqn 关系类型 FQN
     * @param target     目标实体 FQN
     * @return 完整的 FQN
     */
    public static FQN generate(EntityFQN source, String relationTypeFqn, EntityFQN target) {
        if (source == null || relationTypeFqn == null || target == null) {
            throw new IllegalArgumentException("FQN 组成部分不能为空");
        }
        if (relationTypeFqn.isBlank()) {
            throw new IllegalArgumentException("关系类型 FQN 不能为空");
        }
        String value = source.getValue()
                + GraphConstants.FQN_DELIMITER
                + relationTypeFqn
                + GraphConstants.FQN_DELIMITER
                + target.getValue();
        return FQN.of(value);
    }

    /**
     * 解析 FQN 为各组成部分。
     *
     * @param fqn 完整的 FQN
     * @return FqnComponents 包含源实体 FQN、关系类型 FQN、目标实体 FQN
     */
    public static FqnComponents parse(FQN fqn) {
        return new FqnComponents(
                fqn.getSourceEntityFqn(),
                fqn.getRelationTypeFqn(),
                fqn.getTargetEntityFqn()
        );
    }

    /**
     * FQN 解析结果。
     */
    public record FqnComponents(EntityFQN sourceEntityFqn, String relationTypeFqn, EntityFQN targetEntityFqn) {
        public FqnComponents {
            if (sourceEntityFqn == null || relationTypeFqn == null || targetEntityFqn == null) {
                throw new IllegalArgumentException("FQN 组成部分不能为空");
            }
        }
    }
}

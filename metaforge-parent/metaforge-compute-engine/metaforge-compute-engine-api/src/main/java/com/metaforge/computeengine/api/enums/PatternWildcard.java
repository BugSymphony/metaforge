package com.metaforge.computeengine.api.enums;

/**
 * 模式匹配通配符常量。
 *
 * <p>'*' 匹配完整的 EntitySchema FQN，'?' 匹配完整的 RelationSchema FQN。
 * 通配符匹配不拆分 FQN 的名称段。
 *
 * @author metaforge
 */
public final class PatternWildcard {

    public static final String ENTITY_WILDCARD = "*";

    public static final String RELATION_WILDCARD = "?";

    private PatternWildcard() {
    }
}

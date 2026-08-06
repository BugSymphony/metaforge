package com.metaforge.computeengine.domain.model.valueobject;

import com.metaforge.computeengine.api.enums.AssociationType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 遍历深度约束值对象。
 *
 * <p>封装全局最大深度与各 AssociationType 差异化的深度上限。
 * 有效深度取全局深度与类型深度中的较小值。
 *
 * @author metaforge
 */
public final class TraversalDepth {

    private final int globalMaxDepth;
    private final Map<AssociationType, Integer> perTypeMaxDepths;

    public TraversalDepth(int globalMaxDepth) {
        this(globalMaxDepth, Collections.emptyMap());
    }

    public TraversalDepth(int globalMaxDepth, Map<AssociationType, Integer> perTypeMaxDepths) {
        if (globalMaxDepth < 1 || globalMaxDepth > 10) {
            throw new IllegalArgumentException("全局最大深度必须在 1-10 之间，当前值: " + globalMaxDepth);
        }
        this.globalMaxDepth = globalMaxDepth;
        this.perTypeMaxDepths = Collections.unmodifiableMap(
                perTypeMaxDepths != null ? new HashMap<>(perTypeMaxDepths) : new HashMap<>());
    }

    public int getGlobalMaxDepth() {
        return globalMaxDepth;
    }

    public Map<AssociationType, Integer> getPerTypeMaxDepths() {
        return perTypeMaxDepths;
    }

    /**
     * 获取指定 AssociationType 的有效遍历深度。
     * 取全局深度与类型深度中的较小值。
     *
     * @param type 关联类型
     * @return 有效深度
     */
    public int effectiveDepth(AssociationType type) {
        Integer typeMax = perTypeMaxDepths.get(type);
        if (typeMax != null) {
            return Math.min(globalMaxDepth, typeMax);
        }
        return globalMaxDepth;
    }
}

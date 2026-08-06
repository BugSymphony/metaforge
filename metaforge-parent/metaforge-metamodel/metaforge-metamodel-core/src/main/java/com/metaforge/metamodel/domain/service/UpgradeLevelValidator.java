package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.api.enums.UpgradeLevel;
import com.metaforge.metamodel.domain.exception.UpgradeLevelMismatchException;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;

import org.springframework.stereotype.Component;

/**
 * 升级等级匹配校验器。
 * 发布时对比源版本与草稿版本的变更差异，判定是否匹配声明的升级等级。
 *
 * <p>规则矩阵：
 * <pre>
 * 变更类型                     PATCH  MINOR  MAJOR
 * description/name 修改        ✓      ✓      ✓
 * 新增 EntitySchema/Relation   ✗      ✓      ✓
 * 新增 AttributeTemplate       ✗      ✓      ✓
 * 删除元素                     ✗      ✗      ✓
 * 修改关联类型                  ✗      ✗      ✓
 * 新增必填属性                  ✗      ✗      ✓
 * 枚举值删除                   ✗      ✗      ✓
 * </pre>
 */
@Component
public class UpgradeLevelValidator {

    /**
     * 校验升级等级与变更报告的兼容性。
     * MVP 阶段提供骨架实现，phase 5/6 完成后通过 ChangeReport diff 完成完整逻辑。
     */
    public void validate(BundleVersion sourceVersion, BundleVersion draftVersion,
                          UpgradeLevel declared, ChangeReport report) {
        if (declared == UpgradeLevel.MAJOR) {
            return;
        }

        if (declared == UpgradeLevel.PATCH) {
            if (report.hasElementAddition() || report.hasElementDeletion()
                    || report.hasBreakingChange()) {
                throw new UpgradeLevelMismatchException(declared,
                        "PATCH 级别不允许增删元素或破坏性变更");
            }
        }

        if (declared == UpgradeLevel.MINOR) {
            if (report.hasElementDeletion() || report.hasBreakingChange()) {
                throw new UpgradeLevelMismatchException(declared,
                        "MINOR 级别不允许删除元素或破坏性变更");
            }
        }
    }

    /**
     * 变更差异报告。
     */
    public static class ChangeReport {
        private boolean hasElementAddition;
        private boolean hasElementDeletion;
        private boolean hasBreakingChange;

        public ChangeReport() {}

        public ChangeReport(boolean hasElementAddition, boolean hasElementDeletion,
                            boolean hasBreakingChange) {
            this.hasElementAddition = hasElementAddition;
            this.hasElementDeletion = hasElementDeletion;
            this.hasBreakingChange = hasBreakingChange;
        }

        public boolean hasElementAddition() { return hasElementAddition; }
        public void setHasElementAddition(boolean hasElementAddition) {
            this.hasElementAddition = hasElementAddition;
        }
        public boolean hasElementDeletion() { return hasElementDeletion; }
        public void setHasElementDeletion(boolean hasElementDeletion) {
            this.hasElementDeletion = hasElementDeletion;
        }
        public boolean hasBreakingChange() { return hasBreakingChange; }
        public void setHasBreakingChange(boolean hasBreakingChange) {
            this.hasBreakingChange = hasBreakingChange;
        }

        /**
         * 创建无变更报告。
         */
        public static ChangeReport noChanges() {
            return new ChangeReport(false, false, false);
        }
    }
}

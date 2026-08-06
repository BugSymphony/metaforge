package com.metaforge.metamodel.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 元模型治理 BC 级配置属性。
 * 前缀: metaforge.metamodel
 */
@Component
@ConfigurationProperties(prefix = "metaforge.metamodel")
public class MetamodelProperties {

    /** Package 嵌套最大深度（根层 + 4 级子层 = 5） */
    private int maxPackageDepth = 5;

    /** 是否启用 FQN 格式自动校验 */
    private boolean fqnAutoValidation = true;

    /** 是否启用发布前全量校验 */
    private boolean publishValidation = true;

    public int getMaxPackageDepth() {
        return maxPackageDepth;
    }

    public void setMaxPackageDepth(int maxPackageDepth) {
        this.maxPackageDepth = maxPackageDepth;
    }

    public boolean isFqnAutoValidation() {
        return fqnAutoValidation;
    }

    public void setFqnAutoValidation(boolean fqnAutoValidation) {
        this.fqnAutoValidation = fqnAutoValidation;
    }

    public boolean isPublishValidation() {
        return publishValidation;
    }

    public void setPublishValidation(boolean publishValidation) {
        this.publishValidation = publishValidation;
    }
}

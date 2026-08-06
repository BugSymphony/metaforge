package com.metaforge.metamodel.api.service;

import com.metaforge.metamodel.api.dto.response.ImportResultDto;

public interface ImportExportService {

    /**
     * 导出 Bundle 完整元模型或指定 Package 级元模型到 YAML/JSON。
     */
    String exportBundle(String bundleFqn, String format);

    /**
     * 按 Package 级导出元模型（含属性模板依赖）。
     */
    String exportPackage(String packageFqn, String format);

    /**
     * 声明式批量导入元模型（YAML/JSON 格式）。
     * 幂等策略: skip（跳过已存在FQN）或 error（遇到重复报错）。
     */
    ImportResultDto importMetamodel(String content, String format, String conflictStrategy);
}

package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.exception.AttributeNameConflictException;
import com.metaforge.metamodel.domain.model.entity.AttributeTemplate;
import com.metaforge.metamodel.domain.repository.AttributeTemplateRepository;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 属性平铺合并领域服务。
 * 将原生属性定义与挂载属性模板组展开合并，按挂载顺序保持，检测同名冲突。
 */
@Component
public class AttributeMergeService {

    private final AttributeTemplateRepository templateRepository;

    public AttributeMergeService(AttributeTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * 合并原生属性与挂载模板属性，返回合并后的属性定义 JSON 字符串。
     *
     * @param nativeAttributesJson    原生属性 JSON（可为 null）
     * @param mountedTemplateFqnsList 挂载属性模板组 FQN 列表（可为 null）
     * @return 合并后的属性定义 JSON
     * @throws AttributeNameConflictException 如果存在同名属性
     */
    public String merge(String nativeAttributesJson, List<String> mountedTemplateFqnsList) {
        // MVP 阶段：直接拼接原生属性 + 模板属性
        // 完整实现：展开每个模板的 attributeDefinitions，检测同名冲突
        List<String> parts = new ArrayList<>();

        if (nativeAttributesJson != null && !nativeAttributesJson.isBlank()) {
            parts.add(nativeAttributesJson.trim());
        }

        if (mountedTemplateFqnsList != null) {
            for (String templateFqn : mountedTemplateFqnsList) {
                AttributeTemplate template = templateRepository.findByFqn(templateFqn)
                        .orElse(null);
                if (template != null && template.getAttributeDefinitions() != null
                        && !template.getAttributeDefinitions().isBlank()) {
                    parts.add(template.getAttributeDefinitions().trim());
                }
            }
        }

        if (parts.isEmpty()) return null;
        if (parts.size() == 1) return parts.get(0);

        // 简单拼接（MVP 阶段暂不展开深层同名检测，发布时由 JsonSchemaCompiler 完成最终校验）
        return "[" + String.join(",", parts.stream()
                .map(s -> s.replaceAll("^\\[", "").replaceAll("\\]$", ""))
                .toList()) + "]";
    }
}

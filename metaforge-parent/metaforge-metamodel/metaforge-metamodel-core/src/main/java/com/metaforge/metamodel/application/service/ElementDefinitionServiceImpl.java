package com.metaforge.metamodel.application.service;

import com.metaforge.metamodel.api.dto.AttributeDefinitionDto;
import com.metaforge.metamodel.api.dto.ElementQueryRequest;
import com.metaforge.metamodel.api.dto.NativeAttributeDto;
import com.metaforge.metamodel.api.dto.request.*;
import com.metaforge.metamodel.api.dto.response.*;
import com.metaforge.metamodel.api.enums.AssociationType;
import com.metaforge.metamodel.api.enums.Cardinality;
import com.metaforge.metamodel.api.service.ElementDefinitionService;
import com.metaforge.metamodel.domain.exception.AttributeNameConflictException;
import com.metaforge.metamodel.domain.exception.FqnDuplicateException;
import com.metaforge.metamodel.domain.exception.FqnNotFoundException;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.model.entity.*;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.domain.repository.*;
import com.metaforge.metamodel.domain.service.AttributeMergeService;
import com.metaforge.metamodel.domain.service.FqnGenerator;
import com.metaforge.metamodel.domain.service.JsonSchemaCompiler;

import com.metaforge.common.util.JsonbUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ElementDefinitionServiceImpl implements ElementDefinitionService {

    private final EntitySchemaRepository entityRepository;
    private final RelationSchemaRepository relationRepository;
    private final AttributeTemplateRepository templateRepository;
    private final FqnGenerator fqnGenerator;
    private final AttributeMergeService mergeService;
    private final JsonSchemaCompiler schemaCompiler;
    private final BundleVersionRepository versionRepository;

    public ElementDefinitionServiceImpl(EntitySchemaRepository entityRepository,
                                         RelationSchemaRepository relationRepository,
                                         AttributeTemplateRepository templateRepository,
                                         FqnGenerator fqnGenerator,
                                         AttributeMergeService mergeService,
                                         JsonSchemaCompiler schemaCompiler,
                                         BundleVersionRepository versionRepository) {
        this.entityRepository = entityRepository;
        this.relationRepository = relationRepository;
        this.templateRepository = templateRepository;
        this.fqnGenerator = fqnGenerator;
        this.mergeService = mergeService;
        this.schemaCompiler = schemaCompiler;
        this.versionRepository = versionRepository;
    }

    // ========== EntitySchema ==========

    @Override
    public EntitySchemaDto createEntitySchema(CreateEntitySchemaRequest request) {
        String fqnStr = fqnGenerator.entitySchema(request.getPackageFqn(), request.getSegment());
        if (entityRepository.findByFqn(fqnStr).isPresent()) {
            throw new FqnDuplicateException(fqnStr);
        }
        String bundleVersionFqn = resolveBundleVersionFqn(request.getPackageFqn(),
                request.getBundleVersionFqn());
        validateAttributeConflicts(request.getNativeAttributes(),
                request.getMountedTemplateFqns(), fqnStr);
        EntitySchema entity = EntitySchema.create(Fqn.of(fqnStr), request.getPackageFqn(),
                bundleVersionFqn, request.getName(), request.getDescription());
        entity.setNativeAttributes(toNativeAttributesJson(request.getNativeAttributes()));
        entity.setMountedTemplateFqns(request.getMountedTemplateFqns() != null
                ? JsonbUtils.toJsonb(request.getMountedTemplateFqns()) : null);
        EntitySchema saved = entityRepository.save(entity);
        return toEntityDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntitySchemaDto> findEntitySchemaByFqn(String fqn) {
        return entityRepository.findByFqn(fqn).map(this::toEntityDto);
    }

    @Override
    public EntitySchemaDto updateEntitySchema(String fqn, UpdateEntitySchemaRequest request) {
        EntitySchema entity = entityRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getNativeAttributes() != null) {
            entity.setNativeAttributes(toNativeAttributesJson(request.getNativeAttributes()));
        }
        if (request.getMountedTemplateFqns() != null) {
            entity.setMountedTemplateFqns(JsonbUtils.toJsonb(request.getMountedTemplateFqns()));
            validateAttributeConflicts(null, request.getMountedTemplateFqns(), fqn);
        }
        EntitySchema saved = entityRepository.save(entity);
        return toEntityDto(saved);
    }

    @Override
    public void deleteEntitySchema(String fqn) {
        EntitySchema entity = entityRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));
        entityRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntitySchemaDto> listEntitySchemas(ElementQueryRequest request) {
        if (request.getFqnPrefixes() != null && !request.getFqnPrefixes().isEmpty()) {
            return request.getFqnPrefixes().stream()
                    .flatMap(prefix -> entityRepository.findByFqnStartingWith(prefix).stream())
                    .distinct()
                    .map(this::toEntityDto)
                    .collect(Collectors.toList());
        }
        return entityRepository.findByBundleVersionFqn(null).stream()
                .map(this::toEntityDto).collect(Collectors.toList());
    }

    // ========== RelationSchema ==========

    @Override
    public RelationSchemaDto createRelationSchema(CreateRelationSchemaRequest request) {
        String fqnStr = fqnGenerator.relationSchema(request.getPackageFqn(), request.getSegment());
        if (relationRepository.findByFqn(fqnStr).isPresent()) {
            throw new FqnDuplicateException(fqnStr);
        }
        String bundleVersionFqn = resolveBundleVersionFqn(request.getPackageFqn(),
                request.getBundleVersionFqn());
        RelationSchema entity = RelationSchema.create(Fqn.of(fqnStr), request.getPackageFqn(),
                bundleVersionFqn, request.getName(), request.getDescription(),
                request.getSourceFqn(), request.getTargetFqn(),
                AssociationType.fromDisplayName(request.getAssociationType()),
                Cardinality.fromNotation(request.getCardinalitySource()),
                Cardinality.fromNotation(request.getCardinalityTarget()));
        entity.setNativeAttributes(toNativeAttributesJson(request.getNativeAttributes()));
        entity.setMountedTemplateFqns(request.getMountedTemplateFqns() != null
                ? JsonbUtils.toJsonb(request.getMountedTemplateFqns()) : null);
        RelationSchema saved = relationRepository.save(entity);
        return toRelDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RelationSchemaDto> findRelationSchemaByFqn(String fqn) {
        return relationRepository.findByFqn(fqn).map(this::toRelDto);
    }

    @Override
    public void deleteRelationSchema(String fqn) {
        RelationSchema entity = relationRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));
        relationRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationSchemaDto> listRelationSchemas(ElementQueryRequest request) {
        if (request.getFqnPrefixes() != null && !request.getFqnPrefixes().isEmpty()) {
            return request.getFqnPrefixes().stream()
                    .flatMap(prefix -> relationRepository.findByFqnStartingWith(prefix).stream())
                    .distinct()
                    .map(this::toRelDto)
                    .collect(Collectors.toList());
        }
        return relationRepository.findByBundleVersionFqn(null).stream()
                .map(this::toRelDto).collect(Collectors.toList());
    }

    // ========== AttributeTemplate ==========

    @Override
    public AttributeTemplateDto createAttributeTemplate(CreateAttributeTemplateRequest request) {
        String fqnStr = fqnGenerator.attributeTemplate(
                request.getBundleVersionFqn(), request.getSegment());
        if (templateRepository.findByFqn(fqnStr).isPresent()) {
            throw new FqnDuplicateException(fqnStr);
        }
        AttributeTemplate entity = AttributeTemplate.create(Fqn.of(fqnStr),
                request.getBundleVersionFqn(), request.getName(), request.getDescription());
        entity.setAttributeDefinitions(toAttributeDefinitionsJson(request.getAttributeDefinitions()));
        AttributeTemplate saved = templateRepository.save(entity);
        return toTplDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttributeTemplateDto> findAttributeTemplateByFqn(String fqn) {
        return templateRepository.findByFqn(fqn).map(this::toTplDto);
    }

    @Override
    public void deleteAttributeTemplate(String fqn) {
        AttributeTemplate entity = templateRepository.findByFqn(fqn)
                .orElseThrow(() -> new FqnNotFoundException(fqn));
        templateRepository.delete(entity);
    }

    // ========== DTO mapping ==========

    private EntitySchemaDto toEntityDto(EntitySchema e) {
        EntitySchemaDto d = new EntitySchemaDto();
        d.setFqn(e.getFqnValue());
        d.setPackageFqn(e.getPackageFqn());
        d.setBundleVersionFqn(e.getBundleVersionFqn());
        d.setName(e.getName());
        d.setDescription(e.getDescription());
        d.setNativeAttributes(e.getNativeAttributes());
        d.setMountedTemplateFqns(e.getMountedTemplateFqns());
        d.setJsonSchema(e.getJsonSchema());
        d.setEmbedding(e.getEmbedding());
        d.setEnabled(deriveEnabled(e.getBundleVersionFqn()));
        d.setCreatedTime(e.getCreatedTime());
        d.setUpdatedTime(e.getUpdatedTime());
        return d;
    }

    private RelationSchemaDto toRelDto(RelationSchema e) {
        RelationSchemaDto d = new RelationSchemaDto();
        d.setFqn(e.getFqnValue());
        d.setPackageFqn(e.getPackageFqn());
        d.setBundleVersionFqn(e.getBundleVersionFqn());
        d.setName(e.getName());
        d.setDescription(e.getDescription());
        d.setSourceFqn(e.getSourceFqn());
        d.setTargetFqn(e.getTargetFqn());
        d.setAssociationType(e.getAssociationType() != null ? e.getAssociationType().name() : null);
        d.setCardinalitySource(e.getCardinalitySource() != null ? e.getCardinalitySource().getNotation() : null);
        d.setCardinalityTarget(e.getCardinalityTarget() != null ? e.getCardinalityTarget().getNotation() : null);
        d.setNativeAttributes(e.getNativeAttributes());
        d.setMountedTemplateFqns(e.getMountedTemplateFqns());
        d.setJsonSchema(e.getJsonSchema());
        d.setEnabled(deriveEnabled(e.getBundleVersionFqn()));
        d.setCreatedTime(e.getCreatedTime());
        d.setUpdatedTime(e.getUpdatedTime());
        return d;
    }

    private AttributeTemplateDto toTplDto(AttributeTemplate e) {
        AttributeTemplateDto d = new AttributeTemplateDto();
        d.setFqn(e.getFqnValue());
        d.setBundleVersionFqn(e.getBundleVersionFqn());
        d.setName(e.getName());
        d.setDescription(e.getDescription());
        d.setAttributeDefinitions(e.getAttributeDefinitions());
        d.setEnabled(deriveEnabled(e.getBundleVersionFqn()));
        d.setCreatedTime(e.getCreatedTime());
        d.setUpdatedTime(e.getUpdatedTime());
        return d;
    }

    /**
     * 解析 BundleVersion FQN：请求未携带时从 packageFqn 推导（bundleCode:version）。
     */
    private String resolveBundleVersionFqn(String packageFqn, String bundleVersionFqn) {
        if (bundleVersionFqn != null && !bundleVersionFqn.isBlank()) {
            return bundleVersionFqn;
        }
        String bundleCode = fqnGenerator.toBundleCode(packageFqn);
        String version = fqnGenerator.toVersion(packageFqn);
        if (bundleCode != null && version != null) {
            return bundleCode + ":" + version;
        }
        return fqnGenerator.toParentFqn(packageFqn);
    }

    /**
     * 将原生属性 DTO 列表序列化为 JSONB 字符串。
     */
    private String toNativeAttributesJson(List<NativeAttributeDto> nativeAttributes) {
        return nativeAttributes != null ? JsonbUtils.toJsonb(nativeAttributes) : null;
    }

    /**
     * 将属性定义 DTO 列表序列化为 JSONB 字符串。
     */
    private String toAttributeDefinitionsJson(List<AttributeDefinitionDto> attributeDefinitions) {
        return attributeDefinitions != null ? JsonbUtils.toJsonb(attributeDefinitions) : null;
    }

    /**
     * 根据所属 BundleVersion 状态推导启用标记：DRAFT=false，PUBLISHED=true。
     */
    private boolean deriveEnabled(String bundleVersionFqn) {
        if (bundleVersionFqn == null || bundleVersionFqn.isBlank()) {
            return false;
        }
        return versionRepository.findByFqn(bundleVersionFqn)
                .map(BundleVersion::isPublished)
                .orElse(false);
    }

    /**
     * 写入时校验属性名冲突（FR-049）。
     * 汇总原生属性与挂载模板属性名，出现重复时抛出 30106。
     */
    private void validateAttributeConflicts(List<NativeAttributeDto> nativeAttributes,
                                            List<String> mountedTemplateFqns, String entityFqn) {
        Map<String, String> seen = new HashMap<>();
        if (nativeAttributes != null) {
            for (NativeAttributeDto attr : nativeAttributes) {
                if (attr.getName() != null && !attr.getName().isBlank()) {
                    seen.putIfAbsent(attr.getName(), "native");
                }
            }
        }
        if (mountedTemplateFqns != null) {
            for (String tplFqn : mountedTemplateFqns) {
                AttributeTemplate template = templateRepository.findByFqn(tplFqn).orElse(null);
                if (template == null || template.getAttributeDefinitions() == null) {
                    continue;
                }
                List<AttributeDefinitionDto> defs = JsonbUtils.fromJsonbList(
                        template.getAttributeDefinitions(), AttributeDefinitionDto.class);
                for (AttributeDefinitionDto def : defs) {
                    if (def.getName() == null || def.getName().isBlank()) {
                        continue;
                    }
                    if (seen.putIfAbsent(def.getName(), tplFqn) != null) {
                        throw new AttributeNameConflictException(def.getName(), entityFqn);
                    }
                }
            }
        }
    }
}

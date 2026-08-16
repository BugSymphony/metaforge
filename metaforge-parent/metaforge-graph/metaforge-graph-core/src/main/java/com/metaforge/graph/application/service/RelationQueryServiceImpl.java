package com.metaforge.graph.application.service;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.framework.web.PageHelper;
import com.metaforge.graph.api.constant.GraphConstants;
import com.metaforge.graph.api.dto.AdminQueryRequest;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.graph.api.service.RelationQueryService;
import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.model.valueobject.FQN;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关系实例查询检索服务实现。
 */
@Service
@Transactional(readOnly = true)
public class RelationQueryServiceImpl implements RelationQueryService {

    private static final Logger log = LoggerFactory.getLogger(RelationQueryServiceImpl.class);

    private final RelationInstanceRepository instanceRepository;
    private final RelationInstanceJpaRepository jpaRepository;

    public RelationQueryServiceImpl(RelationInstanceRepository instanceRepository,
                                     RelationInstanceJpaRepository jpaRepository) {
        this.instanceRepository = instanceRepository;
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RelationInstanceDto getByFqn(String fqn) {
        log.debug("FQN 精准查询: fqn={}", fqn);
        RelationInstance instance = instanceRepository.findByFqn(FQN.of(fqn))
                .orElseThrow(() -> new com.metaforge.graph.application.service.RelationDraftServiceImpl
                        .RelationNotFoundException("关系不存在: " + fqn));
        return toDto(instance);
    }

    @Override
    public List<RelationInstanceDto> getOutboundRelations(String entityFqn, String relationType, String targetEntityType) {
        log.debug("出边查询: entity={}, type={}", entityFqn, relationType);
        List<RelationInstance> list = instanceRepository.findBySourceEntityFqn(entityFqn);
        return list.stream()
                .filter(r -> relationType == null || relationType.equals(r.getRelationType()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RelationInstanceDto> getInboundRelations(String entityFqn, String relationType, String sourceEntityType) {
        log.debug("入边查询: entity={}, type={}", entityFqn, relationType);
        List<RelationInstance> list = instanceRepository.findByTargetEntityFqn(entityFqn);
        return list.stream()
                .filter(r -> relationType == null || relationType.equals(r.getRelationType()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<RelationInstanceDto> listByConditions(String fqnPrefix, String relationSchemaFqn,
                                                             PageRequest pageRequest) {
        log.debug("条件列表查询: prefix={}, schema={}", fqnPrefix, relationSchemaFqn);

        PageRequest pr = pageRequest != null ? pageRequest : new PageRequest(1, 20);
        Pageable pageable = PageHelper.toSpringPageable(pr);

        Page<RelationInstanceJpo> page = jpaRepository.findAll(
                com.metaforge.graph.infrastructure.persistence.adapter.RelationQuerySpecification.fqnPrefix(fqnPrefix),
                pageable);

        List<RelationInstanceDto> content = page.getContent().stream()
                .map(jpo -> {
                    RelationInstance instance = new RelationInstance();
                    instance.setFqn(FQN.of(jpo.getFqn()));
                    instance.setName(null);
                    instance.setRelationType(jpo.getRelationType());
                    return toDto(instance);
                })
                .collect(Collectors.toList());

        return new PageResult<>(content, page.getTotalElements(),
                page.getNumber() + 1, page.getSize());
    }

    @Override
    public PageResult<RelationInstanceDto> multiFilter(RelationQueryRequest request) {
        log.debug("多维过滤查询");

        PageRequest pr = request.getPageRequest() != null
                ? request.getPageRequest() : new PageRequest(1, 20);
        Pageable pageable = PageHelper.toSpringPageable(pr);

        Page<RelationInstanceJpo> page = jpaRepository.findAll(
                com.metaforge.graph.infrastructure.persistence.adapter.RelationQuerySpecification.multiFilter(
                        request.getRelationTypes(),
                        request.getSourceEntityFqns(),
                        request.getTargetEntityFqns(),
                        request.getRelationSchemaFqns(),
                        request.getRelationSchemaFqnPrefix(),
                        request.getNameKeyword(),
                        request.getDescriptionKeyword()),
                pageable);

        List<RelationInstanceDto> content = page.getContent().stream()
                .map(jpo -> {
                    RelationInstanceDto dto = new RelationInstanceDto();
                    dto.setId(jpo.getId());
                    dto.setFqn(jpo.getFqn());
                    dto.setName(jpo.getName());
                    dto.setDescription(jpo.getDescription());
                    dto.setSourceEntityFqn(jpo.getSourceEntityFqn());
                    dto.setTargetEntityFqn(jpo.getTargetEntityFqn());
                    dto.setRelationType(jpo.getRelationType());
                    dto.setRelationSchemaFqn(jpo.getRelationSchemaFqn());
                    dto.setCurrentVersion(jpo.getCurrentVersion());
                    dto.setCreatedTime(jpo.getCreatedTime());
                    dto.setUpdatedTime(jpo.getUpdatedTime());
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageResult<>(content, page.getTotalElements(),
                page.getNumber() + 1, page.getSize());
    }

    @Override
    public PageResult<RelationInstanceDto> adminQuery(AdminQueryRequest request) {
        log.debug("管理员全状态查询");

        PageRequest pr = request.getPageRequest() != null
                ? request.getPageRequest() : new PageRequest(1, 50);
        Pageable pageable = PageHelper.toSpringPageable(pr);

        Page<RelationInstanceJpo> page = jpaRepository.findAll(
                com.metaforge.graph.infrastructure.persistence.adapter.RelationQuerySpecification
                        .fqnPrefix(request.getFqnPrefix()), pageable);

        List<RelationInstanceDto> content = page.getContent().stream()
                .map(jpo -> {
                    RelationInstanceDto dto = new RelationInstanceDto();
                    dto.setId(jpo.getId());
                    dto.setFqn(jpo.getFqn());
                    dto.setName(jpo.getName());
                    dto.setRelationType(jpo.getRelationType());
                    dto.setCurrentVersion(jpo.getCurrentVersion());
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageResult<>(content, page.getTotalElements(),
                page.getNumber() + 1, page.getSize());
    }

    private RelationInstanceDto toDto(RelationInstance instance) {
        RelationInstanceDto dto = new RelationInstanceDto();
        dto.setId(instance.getId());
        dto.setFqn(instance.getFqnValue());
        dto.setName(instance.getNameValue());
        dto.setDescription(instance.getDescriptionValue());
        dto.setSourceEntityFqn(instance.getSourceEntityFqnValue());
        dto.setTargetEntityFqn(instance.getTargetEntityFqnValue());
        dto.setRelationType(instance.getRelationType());
        dto.setRelationSchemaFqn(instance.getRelationSchemaFqnValue());
        dto.setCurrentVersion(instance.getCurrentVersionValue());
        dto.setCreatedBy(instance.getCreatedBy());
        dto.setCreatedTime(instance.getCreatedTime());
        dto.setUpdatedBy(instance.getUpdatedBy());
        dto.setUpdatedTime(instance.getUpdatedTime());
        return dto;
    }
}

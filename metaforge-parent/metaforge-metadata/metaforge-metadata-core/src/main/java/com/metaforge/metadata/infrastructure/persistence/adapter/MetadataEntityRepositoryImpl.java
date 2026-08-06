package com.metaforge.metadata.infrastructure.persistence.adapter;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.framework.web.PageHelper;
import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import com.metaforge.metadata.infrastructure.mapper.MetadataEntityMapper;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityJpaRepository;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityJpo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MetadataEntityRepositoryImpl implements MetadataEntityRepository {

    private final MetadataEntityJpaRepository jpaRepository;
    private final MetadataEntityMapper mapper;

    public MetadataEntityRepositoryImpl(MetadataEntityJpaRepository jpaRepository, MetadataEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MetadataEntity save(MetadataEntity entity) {
        MetadataEntityJpo jpo = mapper.toJpo(entity);
        MetadataEntityJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MetadataEntity> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    public List<MetadataEntity> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<MetadataEntity> findByFqnPrefixIn(List<String> fqnPrefixes) {
        return jpaRepository.findAll(prefixSpec(fqnPrefixes)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<MetadataEntity> findByEntitySchemaFqn(String entitySchemaFqn) {
        return jpaRepository.findByEntitySchemaFqn(entitySchemaFqn).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * FQN 前缀范围分页查询（OR 并集）。
     */
    public PageResult<MetadataEntity> findByFqnPrefixIn(List<String> fqnPrefixes, PageRequest pageRequest) {
        Pageable pageable = PageHelper.toSpringPageable(pageRequest);
        Page<MetadataEntityJpo> page = jpaRepository.findAll(prefixSpec(fqnPrefixes), pageable);
        return PageHelper.fromSpringPage(page.map(mapper::toDomain));
    }

    /**
     * 按元模型类型分页查询。
     */
    public PageResult<MetadataEntity> findByEntitySchemaFqn(String entitySchemaFqn, PageRequest pageRequest) {
        Pageable pageable = PageHelper.toSpringPageable(pageRequest);
        Page<MetadataEntityJpo> page = jpaRepository.findByEntitySchemaFqn(entitySchemaFqn, pageable);
        return PageHelper.fromSpringPage(page.map(mapper::toDomain));
    }

    /**
     * JSONB content 精准匹配查询（不分页）。
     */
    public List<MetadataEntity> findByContentExactMatch(String conditionJson) {
        return jpaRepository.findByContentExactMatch(conditionJson).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * JSONB content 精准匹配分页查询。
     */
    public PageResult<MetadataEntity> findByContentExactMatch(String conditionJson, PageRequest pageRequest) {
        Pageable pageable = PageHelper.toSpringPageable(pageRequest);
        Page<MetadataEntityJpo> page = jpaRepository.findByContentExactMatch(conditionJson, pageable);
        return PageHelper.fromSpringPage(page.map(mapper::toDomain));
    }

    @Override
    public boolean existsByFqn(String fqn) {
        return jpaRepository.existsByFqn(fqn);
    }

    @Override
    public boolean existsByFqnPrefix(String fqnPrefix) {
        return jpaRepository.existsByFqnStartingWith(fqnPrefix);
    }

    @Override
    public List<String> findFqnsByParentFqnPrefix(String parentFqnPrefix) {
        if (parentFqnPrefix == null || parentFqnPrefix.isBlank()) {
            return List.of();
        }
        String childPrefix = parentFqnPrefix + ".";
        return jpaRepository.findAll((root, query, cb) ->
                        cb.like(root.get("fqn"), childPrefix + "%"))
                .stream()
                .map(MetadataEntityJpo::getFqn)
                .collect(Collectors.toList());
    }

    /**
     * 构造 FQN 前缀 OR 并集查询条件（fqn LIKE 'prefix%'）。
     */
    private org.springframework.data.jpa.domain.Specification<MetadataEntityJpo> prefixSpec(
            List<String> fqnPrefixes) {
        return (root, query, cb) -> {
            if (fqnPrefixes == null || fqnPrefixes.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(fqnPrefixes.stream()
                    .filter(p -> p != null && !p.isBlank())
                    .map(p -> cb.like(root.get("fqn"), p + "%"))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Override
    public void delete(MetadataEntity entity) {
        MetadataEntityJpo jpo = mapper.toJpo(entity);
        jpaRepository.delete(jpo);
    }

    @Override
    public void deleteByFqn(String fqn) {
        jpaRepository.deleteByFqn(fqn);
    }
}

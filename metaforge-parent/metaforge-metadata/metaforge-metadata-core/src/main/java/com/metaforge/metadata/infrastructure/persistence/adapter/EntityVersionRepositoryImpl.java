package com.metaforge.metadata.infrastructure.persistence.adapter;

import com.metaforge.metadata.domain.model.entity.EntityVersion;
import com.metaforge.metadata.domain.repository.EntityVersionRepository;
import com.metaforge.metadata.infrastructure.mapper.EntityVersionMapper;
import com.metaforge.metadata.infrastructure.persistence.jpa.EntityVersionJpo;
import com.metaforge.metadata.infrastructure.persistence.jpa.EntityVersionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EntityVersionRepositoryImpl implements EntityVersionRepository {

    private final EntityVersionJpaRepository jpaRepository;
    private final EntityVersionMapper mapper;

    public EntityVersionRepositoryImpl(EntityVersionJpaRepository jpaRepository, EntityVersionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public EntityVersion save(EntityVersion version) {
        EntityVersionJpo jpo = mapper.toJpo(version);
        EntityVersionJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public List<EntityVersion> findByFqnOrderByVersionDesc(String fqn) {
        return jpaRepository.findByFqnOrderByVersionDesc(fqn).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntityVersion> findByFqnAndVersion(String fqn, int version) {
        return jpaRepository.findByFqnAndVersion(fqn, version).map(mapper::toDomain);
    }
}

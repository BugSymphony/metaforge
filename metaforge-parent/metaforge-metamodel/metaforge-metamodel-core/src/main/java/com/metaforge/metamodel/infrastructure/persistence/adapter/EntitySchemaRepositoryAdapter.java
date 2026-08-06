package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.entity.EntitySchema;
import com.metaforge.metamodel.domain.repository.EntitySchemaRepository;
import com.metaforge.metamodel.infrastructure.mapper.EntitySchemaMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.EntitySchemaJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.EntitySchemaJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EntitySchemaRepositoryAdapter implements EntitySchemaRepository {

    private final EntitySchemaJpaRepository jpaRepository;
    private final EntitySchemaMapper mapper;

    public EntitySchemaRepositoryAdapter(EntitySchemaJpaRepository jpaRepository, EntitySchemaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public EntitySchema save(EntitySchema entity) {
        EntitySchemaJpo jpo = mapper.toJpo(entity);
        EntitySchemaJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EntitySchema> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public List<EntitySchema> findByFqnStartingWith(String fqnPrefix) {
        return jpaRepository.findByFqnStartingWith(fqnPrefix).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntitySchema> findByPackageFqn(String packageFqn) {
        return jpaRepository.findByPackageFqn(packageFqn).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntitySchema> findByBundleVersionFqn(String bundleVersionFqn) {
        return jpaRepository.findByBundleVersionFqn(bundleVersionFqn).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(EntitySchema entity) {
        jpaRepository.delete(mapper.toJpo(entity));
    }
}

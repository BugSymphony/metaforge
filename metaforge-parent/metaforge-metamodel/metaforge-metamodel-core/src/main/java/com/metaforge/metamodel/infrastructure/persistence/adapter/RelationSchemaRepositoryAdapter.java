package com.metaforge.metamodel.infrastructure.persistence.adapter;

import com.metaforge.metamodel.domain.model.entity.RelationSchema;
import com.metaforge.metamodel.domain.repository.RelationSchemaRepository;
import com.metaforge.metamodel.infrastructure.mapper.RelationSchemaMapper;
import com.metaforge.metamodel.infrastructure.persistence.jpa.RelationSchemaJpaRepository;
import com.metaforge.metamodel.infrastructure.persistence.jpa.RelationSchemaJpo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RelationSchemaRepositoryAdapter implements RelationSchemaRepository {

    private final RelationSchemaJpaRepository jpaRepository;
    private final RelationSchemaMapper mapper;

    public RelationSchemaRepositoryAdapter(RelationSchemaJpaRepository jpaRepository, RelationSchemaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RelationSchema save(RelationSchema entity) {
        RelationSchemaJpo jpo = mapper.toJpo(entity);
        RelationSchemaJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RelationSchema> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public List<RelationSchema> findByFqnStartingWith(String fqnPrefix) {
        return jpaRepository.findByFqnStartingWith(fqnPrefix).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RelationSchema> findByPackageFqn(String packageFqn) {
        return jpaRepository.findByPackageFqn(packageFqn).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RelationSchema> findByBundleVersionFqn(String bundleVersionFqn) {
        return jpaRepository.findByBundleVersionFqn(bundleVersionFqn).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(RelationSchema entity) {
        jpaRepository.delete(mapper.toJpo(entity));
    }
}

package com.metaforge.metamodel.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.metaforge.metamodel.api.enums.UpgradeLevel;
import com.metaforge.metamodel.api.enums.VersionStatus;
import com.metaforge.metamodel.domain.model.aggregate.BundleVersion;
import com.metaforge.metamodel.domain.model.valueobject.Fqn;
import com.metaforge.metamodel.infrastructure.persistence.jpa.BundleVersionJpo;

/**
 * BundleVersion MapStruct 转换器 — 领域对象 ↔ JPA 持久化对象。
 */
@Mapper(componentModel = "spring")
public interface BundleVersionMapper {

    @Mapping(target = "fqn", source = "fqnValue")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "upgradeLevel", source = "upgradeLevel", qualifiedByName = "upgradeLevelToString")
    BundleVersionJpo toJpo(BundleVersion version);

    @Mapping(target = "fqn", expression = "java(toFqn(jpo.getFqn()))")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "upgradeLevel", source = "upgradeLevel", qualifiedByName = "stringToUpgradeLevel")
    BundleVersion toDomain(BundleVersionJpo jpo);

    default Fqn toFqn(String fqn) {
        return fqn != null ? Fqn.of(fqn) : null;
    }

    @Named("statusToString")
    default String statusToString(VersionStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default VersionStatus stringToStatus(String status) {
        return status != null ? VersionStatus.valueOf(status) : null;
    }

    @Named("upgradeLevelToString")
    default String upgradeLevelToString(UpgradeLevel level) {
        return level != null ? level.name() : null;
    }

    @Named("stringToUpgradeLevel")
    default UpgradeLevel stringToUpgradeLevel(String level) {
        return level != null ? UpgradeLevel.valueOf(level) : null;
    }
}

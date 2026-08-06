package com.metaforge.sample.repository;

import com.metaforge.sample.model.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 示例 Repository，继承 JpaRepository 获得标准 CRUD 能力。
 */
@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
}

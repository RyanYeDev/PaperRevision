package com.paperrevision.domain.evaluation.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.evaluation.model.TestSuiteEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface TestSuiteRepository extends MyBatisPlusExtRepository<TestSuiteEntity> {
}

package com.paperrevision.domain.evaluation.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.paperrevision.domain.evaluation.model.TestSuiteCaseEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface TestSuiteCaseRepository extends MyBatisPlusExtRepository<TestSuiteCaseEntity> {

    @Select("SELECT * FROM agent_test_suite_cases WHERE suite_id = #{suiteId} ORDER BY sort_order ASC")
    List<TestSuiteCaseEntity> findBySuiteId(@Param("suiteId") String suiteId);
}

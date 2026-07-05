package com.paperrevision.domain.evaluation.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** 评估记录 Repository */
@Mapper
public interface EvaluationRepository extends MyBatisPlusExtRepository<EvaluationEntity> {
}

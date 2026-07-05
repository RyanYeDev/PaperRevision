package com.paperrevision.domain.evaluation.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.evaluation.model.EvaluationReport;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface EvalReportRepository extends MyBatisPlusExtRepository<EvaluationReport> {
}

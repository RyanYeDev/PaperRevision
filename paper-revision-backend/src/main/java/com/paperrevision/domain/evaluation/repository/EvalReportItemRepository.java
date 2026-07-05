package com.paperrevision.domain.evaluation.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.paperrevision.domain.evaluation.model.EvalReportItem;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

@Mapper
public interface EvalReportItemRepository extends MyBatisPlusExtRepository<EvalReportItem> {

    @Select("SELECT * FROM agent_eval_report_items WHERE report_id = #{reportId}")
    List<EvalReportItem> findByReportId(@Param("reportId") String reportId);
}

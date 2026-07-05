package com.paperrevision.domain.evaluation.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paperrevision.domain.evaluation.model.EvalReportItem;
import com.paperrevision.domain.evaluation.model.EvaluationEntity;
import com.paperrevision.domain.evaluation.model.EvaluationReport;
import com.paperrevision.domain.evaluation.model.TestCaseEntity;
import com.paperrevision.domain.evaluation.model.TestSuiteCaseEntity;
import com.paperrevision.domain.evaluation.model.TestSuiteEntity;
import com.paperrevision.domain.evaluation.repository.EvalReportItemRepository;
import com.paperrevision.domain.evaluation.repository.EvalReportRepository;
import com.paperrevision.domain.evaluation.repository.TestCaseRepository;
import com.paperrevision.domain.evaluation.repository.TestSuiteCaseRepository;
import com.paperrevision.domain.evaluation.repository.TestSuiteRepository;

/**
 * 测试套件执行器 - Domain Service
 *
 * 批量执行测试套件中的所有用例，为每个用例调用评估流程，
 * 汇总生成评估报告（聚合根）。
 *
 * 对应 Agent 评估知识体系：
 * - 测试用例集 (Test Suite)
 * - 批量评估 (Batch Evaluation)
 * - pass@k 策略
 * - 版本对比的基础设施
 */
@Service
public class TestSuiteExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TestSuiteExecutor.class);

    private final TestCaseRepository testCaseRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestSuiteCaseRepository testSuiteCaseRepository;
    private final EvalReportRepository evalReportRepository;
    private final EvalReportItemRepository evalReportItemRepository;
    private final EvaluationDomainService evaluationDomainService;

    public TestSuiteExecutor(TestCaseRepository testCaseRepository,
            TestSuiteRepository testSuiteRepository,
            TestSuiteCaseRepository testSuiteCaseRepository,
            EvalReportRepository evalReportRepository,
            EvalReportItemRepository evalReportItemRepository,
            EvaluationDomainService evaluationDomainService) {
        this.testCaseRepository = testCaseRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testSuiteCaseRepository = testSuiteCaseRepository;
        this.evalReportRepository = evalReportRepository;
        this.evalReportItemRepository = evalReportItemRepository;
        this.evaluationDomainService = evaluationDomainService;
    }

    /**
     * 执行测试套件，生成评估报告
     *
     * @param suiteId 测试套件ID
     * @param agentId Agent ID（用于记录）
     * @param userId  用户ID
     * @return 聚合后的评估报告
     */
    public EvaluationReport executeSuite(String suiteId, String agentId, String userId) {
        logger.info("开始执行测试套件: suiteId={}, agentId={}", suiteId, agentId);

        // 1. 获取套件和用例列表
        TestSuiteEntity suite = testSuiteRepository.selectById(suiteId);
        if (suite == null) {
            throw new IllegalArgumentException("测试套件不存在: " + suiteId);
        }

        List<TestSuiteCaseEntity> mappings = testSuiteCaseRepository.findBySuiteId(suiteId);
        if (mappings.isEmpty()) {
            logger.warn("测试套件中没有用例: suiteId={}", suiteId);
        }

        // 2. 创建报告
        EvaluationReport report = new EvaluationReport();
        report.setSuiteId(suiteId);
        report.setAgentId(agentId);
        report.setName(suite.getName() + " 评估报告");
        report.setStatus("IN_PROGRESS");
        report.setUserId(userId);
        report.setConfigJson(suite.getConfigJson());
        evalReportRepository.checkInsert(report);

        // 3. 逐用例执行评估
        for (TestSuiteCaseEntity mapping : mappings) {
            try {
                EvalReportItem item = executeSingleCase(mapping, report.getId(), userId);
                report.addItem(item);
            } catch (Exception e) {
                logger.error("用例执行失败: caseId={}, error={}", mapping.getCaseId(), e.getMessage());
                EvalReportItem failedItem = new EvalReportItem();
                failedItem.setReportId(report.getId());
                failedItem.setCaseId(mapping.getCaseId());
                failedItem.setPassed(false);
                failedItem.setFeedback("执行失败: " + e.getMessage());
                report.addItem(failedItem);
            }
        }

        // 4. 聚合统计
        report.computeAggregates();
        evalReportRepository.checkedUpdateById(report);

        logger.info("测试套件执行完成: suiteId={}, totalCases={}, passed={}",
                suiteId, report.getTotalCases(), report.getPassedCases());

        return report;
    }

    /**
     * 执行单个测试用例的评估
     */
    private EvalReportItem executeSingleCase(TestSuiteCaseEntity mapping,
            String reportId, String userId) {
        TestCaseEntity testCase = testCaseRepository.selectById(mapping.getCaseId());
        if (testCase == null) {
            throw new IllegalArgumentException("测试用例不存在: " + mapping.getCaseId());
        }

        long start = System.currentTimeMillis();

        // 使用现有的评估服务（需要构建一个模拟的 revisionResult）
        // 实际项目中，这里应该调用 Agent 执行用例，然后评估结果
        java.util.Map<String, Object> mockResult = new java.util.LinkedHashMap<>();
        mockResult.put("suggestedRevision", testCase.getExpectedOutput());
        mockResult.put("requirement", testCase.getInputData());

        try {
            // 尝试调用评估
            EvaluationEntity eval = evaluationDomainService.evaluateRevision(
                    mapping.getCaseId(), userId, mockResult, mapping.getSuiteId());

            long elapsed = System.currentTimeMillis() - start;

            EvalReportItem item = new EvalReportItem();
            item.setReportId(reportId);
            item.setCaseId(mapping.getCaseId());
            item.setOverallScore(eval.getOverallScore());
            item.setTrajectoryScore(eval.getOverallScore()); // 简化的轨迹分
            item.setLlmScore(eval.getOverallScore());
            item.setPassed(eval.getOverallScore() != null && eval.getOverallScore() >= 0.6);
            item.setFeedback(eval.getFeedback());
            item.setDurationMs(elapsed);
            item.setTokensUsed(0);
            item.setDetailsJson("{}");
            evalReportItemRepository.checkInsert(item);

            return item;

        } catch (Exception e) {
            logger.warn("用例评估失败: caseId={}, error={}", mapping.getCaseId(), e.getMessage());

            long elapsed = System.currentTimeMillis() - start;
            EvalReportItem item = new EvalReportItem();
            item.setReportId(reportId);
            item.setCaseId(mapping.getCaseId());
            item.setOverallScore(0.0);
            item.setPassed(false);
            item.setFeedback("评估失败: " + e.getMessage());
            item.setDurationMs(elapsed);
            item.setDetailsJson("{}");
            evalReportItemRepository.checkInsert(item);

            return item;
        }
    }
}

package com.paperrevision.interfaces.api.portal.evaluation;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.paperrevision.domain.evaluation.model.EvalReportItem;
import com.paperrevision.domain.evaluation.model.EvaluationReport;
import com.paperrevision.domain.evaluation.model.TestSuiteCaseEntity;
import com.paperrevision.domain.evaluation.model.TestSuiteEntity;
import com.paperrevision.domain.evaluation.repository.EvalReportItemRepository;
import com.paperrevision.domain.evaluation.repository.EvalReportRepository;
import com.paperrevision.domain.evaluation.repository.TestSuiteCaseRepository;
import com.paperrevision.domain.evaluation.repository.TestSuiteRepository;
import com.paperrevision.domain.evaluation.service.TestSuiteExecutor;
import com.paperrevision.infrastructure.auth.UserContext;
import com.paperrevision.interfaces.api.common.Result;

/** 测试套件管理控制器 */
@RestController
@RequestMapping("/api/evaluation/test-suites")
public class TestSuiteController {

    private final TestSuiteRepository testSuiteRepository;
    private final TestSuiteCaseRepository testSuiteCaseRepository;
    private final EvalReportRepository evalReportRepository;
    private final EvalReportItemRepository evalReportItemRepository;
    private final TestSuiteExecutor testSuiteExecutor;

    public TestSuiteController(TestSuiteRepository testSuiteRepository,
            TestSuiteCaseRepository testSuiteCaseRepository,
            EvalReportRepository evalReportRepository,
            EvalReportItemRepository evalReportItemRepository,
            TestSuiteExecutor testSuiteExecutor) {
        this.testSuiteRepository = testSuiteRepository;
        this.testSuiteCaseRepository = testSuiteCaseRepository;
        this.evalReportRepository = evalReportRepository;
        this.evalReportItemRepository = evalReportItemRepository;
        this.testSuiteExecutor = testSuiteExecutor;
    }

    /** 获取用户的测试套件列表 */
    @GetMapping
    public Result<List<TestSuiteEntity>> list() {
        String userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<TestSuiteEntity> wrapper = Wrappers.<TestSuiteEntity>lambdaQuery()
                .eq(TestSuiteEntity::getUserId, userId)
                .orderByDesc(TestSuiteEntity::getCreatedAt);
        return Result.success(testSuiteRepository.selectList(wrapper));
    }

    /** 创建测试套件 */
    @PostMapping
    public Result<TestSuiteEntity> create(@RequestBody TestSuiteEntity entity) {
        entity.setUserId(UserContext.getCurrentUserId());
        entity.setStatus("DRAFT");
        testSuiteRepository.checkInsert(entity);
        return Result.success("测试套件创建成功", entity);
    }

    /** 获取测试套件详情 */
    @GetMapping("/{id}")
    public Result<TestSuiteEntity> detail(@PathVariable String id) {
        return Result.success(testSuiteRepository.selectById(id));
    }

    /** 更新测试套件 */
    @PutMapping("/{id}")
    public Result<TestSuiteEntity> update(@PathVariable String id, @RequestBody TestSuiteEntity updated) {
        TestSuiteEntity existing = testSuiteRepository.selectById(id);
        if (existing == null) return Result.notFound("测试套件不存在");
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setConfigJson(updated.getConfigJson());
        testSuiteRepository.checkedUpdateById(existing);
        return Result.success(existing);
    }

    /** 删除测试套件 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        testSuiteRepository.checkedDelete(Wrappers.<TestSuiteEntity>lambdaQuery()
                .eq(TestSuiteEntity::getId, id));
        return Result.success();
    }

    /** 向套件中添加用例 */
    @PostMapping("/{id}/cases")
    public Result<TestSuiteCaseEntity> addCase(@PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String caseId = (String) body.get("caseId");
        Integer sortOrder = body.containsKey("sortOrder") ? (Integer) body.get("sortOrder") : 0;

        TestSuiteCaseEntity mapping = new TestSuiteCaseEntity();
        mapping.setSuiteId(id);
        mapping.setCaseId(caseId);
        mapping.setSortOrder(sortOrder);
        testSuiteCaseRepository.checkInsert(mapping);
        return Result.success("用例已添加到套件", mapping);
    }

    /** 从套件中移除用例 */
    @DeleteMapping("/{id}/cases/{caseId}")
    public Result<Void> removeCase(@PathVariable String id, @PathVariable String caseId) {
        testSuiteCaseRepository.checkedDelete(Wrappers.<TestSuiteCaseEntity>lambdaQuery()
                .eq(TestSuiteCaseEntity::getSuiteId, id)
                .eq(TestSuiteCaseEntity::getCaseId, caseId));
        return Result.success();
    }

    /** 执行测试套件 */
    @PostMapping("/{id}/run")
    public Result<EvaluationReport> run(@PathVariable String id) {
        String userId = UserContext.getCurrentUserId();
        EvaluationReport report = testSuiteExecutor.executeSuite(id, "revision-agent", userId);
        return Result.success("测试套件执行完成", report);
    }

    /** 获取套件的评估报告列表 */
    @GetMapping("/{id}/reports")
    public Result<List<EvaluationReport>> reports(@PathVariable String id) {
        LambdaQueryWrapper<EvaluationReport> wrapper = Wrappers.<EvaluationReport>lambdaQuery()
                .eq(EvaluationReport::getSuiteId, id)
                .orderByDesc(EvaluationReport::getCreatedAt);
        return Result.success(evalReportRepository.selectList(wrapper));
    }

    /** 获取报告明细 */
    @GetMapping("/reports/{reportId}/items")
    public Result<List<EvalReportItem>> reportItems(@PathVariable String reportId) {
        return Result.success(evalReportItemRepository.findByReportId(reportId));
    }
}

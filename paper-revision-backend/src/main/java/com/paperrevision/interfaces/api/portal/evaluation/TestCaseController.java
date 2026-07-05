package com.paperrevision.interfaces.api.portal.evaluation;

import java.util.List;

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
import com.paperrevision.domain.evaluation.model.TestCaseEntity;
import com.paperrevision.domain.evaluation.repository.TestCaseRepository;
import com.paperrevision.infrastructure.auth.UserContext;
import com.paperrevision.interfaces.api.common.Result;

/** 测试用例管理控制器 */
@RestController
@RequestMapping("/api/evaluation/test-cases")
public class TestCaseController {

    private final TestCaseRepository testCaseRepository;

    public TestCaseController(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    /** 获取用户的测试用例列表 */
    @GetMapping
    public Result<List<TestCaseEntity>> list() {
        String userId = UserContext.getCurrentUserId();
        LambdaQueryWrapper<TestCaseEntity> wrapper = Wrappers.<TestCaseEntity>lambdaQuery()
                .eq(TestCaseEntity::getUserId, userId)
                .orderByDesc(TestCaseEntity::getCreatedAt);
        return Result.success(testCaseRepository.selectList(wrapper));
    }

    /** 创建测试用例 */
    @PostMapping
    public Result<TestCaseEntity> create(@RequestBody TestCaseEntity entity) {
        entity.setUserId(UserContext.getCurrentUserId());
        testCaseRepository.checkInsert(entity);
        return Result.success("测试用例创建成功", entity);
    }

    /** 获取测试用例详情 */
    @GetMapping("/{id}")
    public Result<TestCaseEntity> detail(@PathVariable String id) {
        return Result.success(testCaseRepository.selectById(id));
    }

    /** 更新测试用例 */
    @PutMapping("/{id}")
    public Result<TestCaseEntity> update(@PathVariable String id, @RequestBody TestCaseEntity updated) {
        TestCaseEntity existing = testCaseRepository.selectById(id);
        if (existing == null) return Result.notFound("测试用例不存在");
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setInputData(updated.getInputData());
        existing.setExpectedOutput(updated.getExpectedOutput());
        existing.setGroundTruth(updated.getGroundTruth());
        existing.setSourceDataset(updated.getSourceDataset());
        existing.setDifficulty(updated.getDifficulty());
        testCaseRepository.checkedUpdateById(existing);
        return Result.success(existing);
    }

    /** 删除测试用例 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        testCaseRepository.checkedDelete(Wrappers.<TestCaseEntity>lambdaQuery()
                .eq(TestCaseEntity::getId, id));
        return Result.success();
    }
}

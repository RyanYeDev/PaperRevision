package com.paperrevision.domain.evaluation.service;

import com.paperrevision.domain.evaluation.model.LLMJudgeRequest;
import com.paperrevision.domain.evaluation.model.LLMJudgeResult;

/**
 * LLM 裁判服务接口 - 定义在 Domain 层（依赖倒置原则）
 *
 * 使用 LLM 作为评估裁判，根据预定义的评分标准（rubric）对 Agent 输出进行语义评估。
 * 实现类在 Infrastructure 层，依赖 LLMService 完成实际的模型调用。
 *
 * 对应 Agent 评估知识体系中的 "LLM Judge" / "语义匹配" 方法
 */
public interface LLMJudgeService {

    /**
     * 使用 LLM 作为裁判评估修订质量
     *
     * @param request 评估请求，包含原文、修改后文本、审稿意见、参考文献、评分标准
     * @return 结构化的评估结果，包含 5 个维度的分数和推理说明
     * @throws Exception 当 LLM 调用失败时抛出，调用方应做好降级处理
     */
    LLMJudgeResult evaluate(LLMJudgeRequest request) throws Exception;
}

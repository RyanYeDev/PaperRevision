package com.paperrevision.infrastructure.context;

import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import com.paperrevision.infrastructure.context.ContextCompressor.CompressedContext;
import com.paperrevision.infrastructure.utils.TokenCounter;
import com.paperrevision.infrastructure.utils.TokenCounter.ContextBudget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Agent 上下文窗口自适应适配器 — 上下文分层压缩 Step 5
 *
 * Agent 执行前估算 systemPrompt + userInput + RAG chunks 的总 token 消耗，
 * 超出预算时自动触发分层压缩降级（FULL → SUMMARY → KEYWORDS），
 * 确保不超出 LLM 上下文窗口。
 *
 * <p>复用 Step1-4 全链路：TokenCounter → ContextCompressor → LayeredContextManager → RetrievalCompressor
 */
@Service
public class AgentContextAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AgentContextAdapter.class);

    /** 默认上下文窗口上限（token），各模型不同，此值可被调用方覆盖 */
    public static final int DEFAULT_MAX_TOKENS = 4096;
    /** 预留响应 token 的比率（如 0.3 = 预留 30% 给 LLM 生成输出） */
    public static final double RESPONSE_RESERVE_RATIO = 0.3;

    private final ContextCompressor compressor;
    private final LayeredContextManager manager;

    public AgentContextAdapter(ContextCompressor compressor, LayeredContextManager manager) {
        this.compressor = compressor;
        this.manager = manager;
    }

    /**
     * 自适应调整 Agent 上下文，超预算时降级压缩。
     *
     * @param systemPrompt  Agent 系统提示词
     * @param userInput     用户输入文本
     * @param ragChunks     RAG 检索到的文档分块（可空）
     * @param summarizer    LLM 摘要函数（可 null，降级到关键词兜底）
     * @param maxTokens     模型上下文窗口上限
     * @return 适配后的上下文结果
     */
    public AdaptedContext adapt(String systemPrompt, String userInput,
                                 List<DocumentChunkEntity> ragChunks,
                                 Function<String, String> summarizer,
                                 int maxTokens) {
        // 预留部分 token 给 LLM 响应
        int budgetLimit = (int) (maxTokens * (1.0 - RESPONSE_RESERVE_RATIO));
        ContextBudget budget = new ContextBudget(budgetLimit);

        // 逐组件估算并选择层级
        List<String> parts = new ArrayList<>();

        // 1. System prompt（优先级最高，尽量全量）
        CompressedContext sysCtx = compressor.compress(systemPrompt, summarizer);
        String sysPart = selectForBudget(sysCtx, budget, "systemPrompt");
        if (!sysPart.isEmpty()) parts.add(sysPart);

        // 2. RAG 检索上下文（优先级次之，预算紧张时优先压缩这部分）
        if (ragChunks != null && !ragChunks.isEmpty()) {
            for (DocumentChunkEntity chunk : ragChunks) {
                if (budget.remaining() <= 0) break;
                CompressedContext chunkCtx = compressor.compress(chunk.getContent(), summarizer);
                String chunkPart = selectForBudget(chunkCtx, budget, "ragChunk");
                if (!chunkPart.isEmpty()) parts.add(chunkPart);
            }
        }

        // 3. 用户输入（优先级最低但必须保留——关键词兜底）
        CompressedContext userCtx = compressor.compress(userInput, summarizer);
        String userPart = selectOrDowngrade(userCtx, budget);
        parts.add(userPart);

        String assembled = String.join("\n\n", parts);
        int totalTokens = TokenCounter.estimate(assembled);
        boolean downgraded = totalTokens < TokenCounter.estimate(
                systemPrompt + "\n\n" + userInput +
                (ragChunks != null ? "\n\n" + ragChunks.stream()
                        .map(DocumentChunkEntity::getContent).reduce("", (a, b) -> a + b) : ""));

        logger.info("Agent上下文适配: {}tokens (预算{}), 降级={}",
                totalTokens, budgetLimit, downgraded);
        return new AdaptedContext(assembled, totalTokens, budgetLimit, downgraded);
    }

    /** 简化版：仅适配 systemPrompt + userInput，无 RAG */
    public AdaptedContext adaptSimple(String systemPrompt, String userInput,
                                       Function<String, String> summarizer, int maxTokens) {
        return adapt(systemPrompt, userInput, null, summarizer, maxTokens);
    }

    /** 快速检查是否超预算 */
    public boolean isOverBudget(String systemPrompt, String userInput, int maxTokens) {
        int total = TokenCounter.estimate(systemPrompt) + TokenCounter.estimate(userInput);
        return total > (int) (maxTokens * (1.0 - RESPONSE_RESERVE_RATIO));
    }

    /** 按预算选层并扣减 */
    private String selectForBudget(CompressedContext ctx, ContextBudget budget, String label) {
        LayeredContextManager.Selection sel = manager.selectLayer(ctx, budget.remaining());
        budget.tryAllocate(sel.content);
        return sel.content;
    }

    /** 选层——关键词兜底始终保留 */
    private String selectOrDowngrade(CompressedContext ctx, ContextBudget budget) {
        String content = selectForBudget(ctx, budget, "userInput");
        if (content.isEmpty()) {
            String kw = ctx.keywords.isEmpty() ? ctx.full : "关键词：" + String.join("、", ctx.keywords);
            return kw;
        }
        return content;
    }

    /** 适配结果 */
    public record AdaptedContext(String content, int actualTokens, int budgetTokens, boolean downgraded) {
        public double utilizationRate() {
            return budgetTokens > 0 ? (double) actualTokens / budgetTokens : 0;
        }
    }
}

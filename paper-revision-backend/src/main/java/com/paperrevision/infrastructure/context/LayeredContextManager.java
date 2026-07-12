package com.paperrevision.infrastructure.context;

import com.paperrevision.infrastructure.context.ContextCompressor.CompressedContext;
import com.paperrevision.infrastructure.utils.TokenCounter;
import com.paperrevision.infrastructure.utils.TokenCounter.ContextBudget;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分层上下文管理器 — 上下文分层压缩 Step 3
 *
 * 根据 token 预算({@link ContextBudget}) 为每段内容自动选择最合适的层级：
 * 预算充足用全量(FULL)，不足降级到摘要(SUMMARY)，再不足降级到关键词(KEYWORDS)。
 * 关键词层是可承受的兜底 —— 即便超预算也保留，保证核心信息不丢。
 *
 * 依赖 {@link ContextCompressor} 产出的三层结果，供 Step4(RAG 检索集成)、
 * Step5(Agent 自适应) 在组装上下文时按预算裁剪。
 */
@Service
public class LayeredContextManager {

    public enum Layer { FULL, SUMMARY, KEYWORDS }

    /** 单段选层结果 */
    public static class Selection {
        public final Layer layer;
        public final String content;
        public final int tokens;

        public Selection(Layer layer, String content, int tokens) {
            this.layer = layer;
            this.content = content;
            this.tokens = tokens;
        }
    }

    /** 为单段内容按剩余预算选层（仅计算，不扣减预算） */
    public Selection selectLayer(CompressedContext ctx, int remainingTokens) {
        if (ctx.fullTokens <= remainingTokens) {
            return new Selection(Layer.FULL, ctx.full, ctx.fullTokens);
        }
        if (!ctx.summary.isBlank() && ctx.summaryTokens <= remainingTokens) {
            return new Selection(Layer.SUMMARY, ctx.summary, ctx.summaryTokens);
        }
        String kw = keywordsAsText(ctx);
        return new Selection(Layer.KEYWORDS, kw, TokenCounter.estimate(kw));
    }

    /**
     * 按预算组装多段内容：逐段选层并从预算扣减，返回拼接文本。
     * 关键词兜底始终保留（即便扣减失败），保证核心信息不丢。
     */
    public String assemble(List<CompressedContext> segments, ContextBudget budget) {
        StringBuilder sb = new StringBuilder();
        for (CompressedContext ctx : segments) {
            Selection sel = selectLayer(ctx, budget.remaining());
            budget.tryAllocate(sel.content);
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(sel.content);
        }
        return sb.toString();
    }

    private String keywordsAsText(CompressedContext ctx) {
        return ctx.keywords.isEmpty() ? "" : "关键词：" + String.join("、", ctx.keywords);
    }
}

package com.paperrevision.infrastructure.context;

import com.paperrevision.infrastructure.context.ContextCompressor.CompressedContext;
import com.paperrevision.infrastructure.context.LayeredContextManager.Layer;
import com.paperrevision.infrastructure.context.LayeredContextManager.Selection;
import com.paperrevision.infrastructure.utils.TokenCounter.ContextBudget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** LayeredContextManager 单元测试 — 上下文分层压缩 Step 3 */
class LayeredContextManagerTest {

    private final LayeredContextManager manager = new LayeredContextManager();

    private CompressedContext sample() {
        // full 远大于 summary，便于验证降级
        String full = "这是一段需要被压缩的很长的论文正文内容用于测试分层选择".repeat(10);
        return new CompressedContext(full, "简短摘要三句话", List.of("论文", "压缩", "分层"));
    }

    @Test
    void shouldSelectFullWhenBudgetAmple() {
        CompressedContext ctx = sample();
        Selection sel = manager.selectLayer(ctx, ctx.fullTokens + 10);
        assertEquals(Layer.FULL, sel.layer);
        assertEquals(ctx.full, sel.content);
    }

    @Test
    void shouldDowngradeToSummaryWhenFullTooBig() {
        CompressedContext ctx = sample();
        // 预算 = 摘要 token，装不下全量但装得下摘要
        Selection sel = manager.selectLayer(ctx, ctx.summaryTokens);
        assertEquals(Layer.SUMMARY, sel.layer);
        assertEquals("简短摘要三句话", sel.content);
    }

    @Test
    void shouldDowngradeToKeywordsWhenBudgetTiny() {
        CompressedContext ctx = sample();
        Selection sel = manager.selectLayer(ctx, 1);
        assertEquals(Layer.KEYWORDS, sel.layer);
        assertTrue(sel.content.contains("论文"));
    }

    @Test
    void keywordsShouldBeFloorEvenIfOverBudget() {
        CompressedContext ctx = sample();
        Selection sel = manager.selectLayer(ctx, 0);
        assertEquals(Layer.KEYWORDS, sel.layer, "预算为 0 也应返回关键词兜底");
        assertFalse(sel.content.isEmpty());
    }

    @Test
    void shouldSkipBlankSummaryToKeywords() {
        CompressedContext ctx = new CompressedContext("很长很长的正文".repeat(10), "", List.of("兜底词"));
        Selection sel = manager.selectLayer(ctx, ctx.fullTokens - 1);
        assertEquals(Layer.KEYWORDS, sel.layer, "摘要为空应跳过降到关键词");
    }

    @Test
    void assembleShouldUseFullForAllWhenBudgetAmple() {
        CompressedContext a = sample();
        CompressedContext b = sample();
        ContextBudget budget = new ContextBudget(a.fullTokens + b.fullTokens + 20);
        String out = manager.assemble(List.of(a, b), budget);
        assertTrue(out.contains(a.full));
        assertEquals(2, out.split("\n\n").length, "两段应以空行分隔");
    }

    @Test
    void assembleShouldDowngradeLaterSegmentsWhenBudgetTight() {
        CompressedContext a = sample();
        CompressedContext b = sample();
        // 预算仅够第一段全量 + 一点，第二段应被降级
        ContextBudget budget = new ContextBudget(a.fullTokens + 5);
        String out = manager.assemble(List.of(a, b), budget);
        assertTrue(out.contains(a.full), "首段应为全量");
        assertFalse(out.endsWith(b.full), "次段不应为全量");
    }
}

package com.paperrevision.infrastructure.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** TokenCounter 单元测试 */
class TokenCounterTest {

    @Test
    void shouldReturnZeroForNullOrEmpty() {
        assertEquals(0, TokenCounter.estimate(null));
        assertEquals(0, TokenCounter.estimate(""));
    }

    @Test
    void shouldEstimateChineseText() {
        int tokens = TokenCounter.estimate("论文返修平台智能分析系统");
        assertTrue(tokens > 15, "中文 10 字应 > 15 token, got " + tokens);
        assertTrue(tokens < 30, "不应过高估算");
    }

    @Test
    void shouldEstimateEnglishText() {
        int tokens = TokenCounter.estimate("Paper revision with AI agent analysis");
        assertTrue(tokens > 8, "英文 7 词应 > 8 token, got " + tokens);
    }

    @Test
    void shouldEstimateMixedText() {
        int tokens = TokenCounter.estimate("使用DeepSeek进行论文返修revision");
        assertTrue(tokens > 0, "混合文本应有 token");
    }

    @Test
    void quickEstimateShouldBeReasonable() {
        String text = "这是一段很长的中文文本用于测试快速估算法".repeat(5);
        int quick = TokenCounter.quickEstimate(text);
        int precise = TokenCounter.estimate(text);
        double ratio = (double) quick / precise;
        assertTrue(ratio > 0.7 && ratio < 1.5, "快速估算应与精确估算比例合理: " + ratio);
    }

    @Test
    void shouldProvideTokenReport() {
        TokenCounter.TokenReport report = TokenCounter.report("中英文混合English test测试123");
        assertTrue(report.totalChars > 0);
        assertTrue(report.estimatedTokens > 0);
        assertTrue(report.chineseChars > 0);
        assertTrue(report.englishWords > 0);
    }

    @Test
    void shouldEstimateBatch() {
        List<String> texts = List.of("第一段文本", "second paragraph", "第三段");
        int total = TokenCounter.estimateBatch(texts);
        assertTrue(total > 5);
    }

    @Test
    void contextBudgetShouldAllocateAndTrack() {
        TokenCounter.ContextBudget budget = new TokenCounter.ContextBudget(100);
        assertTrue(budget.tryAllocate("短文本"));
        assertTrue(budget.used() > 0);
        assertTrue(budget.remaining() < 100);
        assertEquals(100, budget.max());
        assertTrue(budget.usageRate() > 0 && budget.usageRate() < 1.0);
    }

    @Test
    void contextBudgetShouldRejectWhenFull() {
        TokenCounter.ContextBudget budget = new TokenCounter.ContextBudget(10);
        boolean allocated = budget.tryAllocate("这是一段超长中文文本".repeat(10));
        assertFalse(allocated, "超出预算应拒绝分配");
    }

    @Test
    void canFitShouldNotModifyBudget() {
        TokenCounter.ContextBudget budget = new TokenCounter.ContextBudget(100);
        budget.tryAllocate("分配一些token");
        int usedBefore = budget.used();
        budget.canFit("检查是否放得下");
        assertEquals(usedBefore, budget.used(), "canFit 不应修改已使用量");
    }
}

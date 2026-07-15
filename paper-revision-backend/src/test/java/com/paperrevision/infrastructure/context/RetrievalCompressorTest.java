package com.paperrevision.infrastructure.context;

import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import com.paperrevision.infrastructure.utils.TokenCounter.ContextBudget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RetrievalCompressor 单元测试 — 上下文分层压缩 Step 4 */
class RetrievalCompressorTest {

    private final RetrievalCompressor service =
            new RetrievalCompressor(new ContextCompressor(), new LayeredContextManager());

    private DocumentChunkEntity chunk(String content, int idx) {
        DocumentChunkEntity e = new DocumentChunkEntity();
        e.setPaperId("paper-1");
        e.setChunkIndex(idx);
        e.setContent(content);
        return e;
    }

    @Test
    void shouldCompressAndAssembleWithLLMSummarizer() {
        // 使用长文本 + 紧预算，迫使 LayeredContextManager 降级到 SUMMARY 层
        String longText = "这是一段非常长的论文正文内容需要通过压缩来节省上下文窗口的token消耗量".repeat(8);
        List<DocumentChunkEntity> chunks = List.of(chunk(longText, 0));
        ContextBudget budget = new ContextBudget(60); // 紧预算：全量放不下，摘要可放入
        String result = service.compressAndAssemble(chunks, p -> "LLM摘要结果", budget);
        assertTrue(result.contains("LLM摘要结果"), "紧预算下应选中 SUMMARY 层, got: " + result);
        assertTrue(budget.used() > 0, "应消耗预算");
    }

    @Test
    void shouldCompressLocallyWithoutLLM() {
        List<DocumentChunkEntity> chunks = List.of(
                chunk("向量检索和关键词检索是两种互补的检索策略。", 0));
        ContextBudget budget = new ContextBudget(4096);
        String result = service.compressAndAssembleLocally(chunks, budget);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullOrEmptyInput() {
        assertEquals("", service.compressAndAssemble(null, p -> "x", new ContextBudget(100)));
        assertEquals("", service.compressAndAssemble(List.of(), p -> "x", new ContextBudget(100)));
        assertEquals("", service.compressAndAssembleLocally(null, new ContextBudget(100)));
        assertEquals("", service.compressAndAssembleLocally(List.of(), new ContextBudget(100)));
    }

    @Test
    void shouldFallbackWhenLLMThrows() {
        List<DocumentChunkEntity> chunks = List.of(
                chunk("一些正文内容需要被压缩", 0));
        ContextBudget budget = new ContextBudget(4096);
        String result = service.compressAndAssemble(chunks, p -> {
            throw new RuntimeException("LLM 不可用");
        }, budget);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "LLM 异常应降级到关键词兜底");
    }

    @Test
    void shouldDowngradeWhenBudgetTight() {
        // 创建多段内容，预算只够关键词层
        List<DocumentChunkEntity> chunks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            chunks.add(chunk("这是一段比较长的论文正文内容需要通过压缩来节省上下文窗口的token消耗量".repeat(3), i));
        }
        ContextBudget tight = new ContextBudget(30); // 极紧预算，所有段都降为关键词
        String result = service.compressAndAssembleLocally(chunks, tight);
        assertNotNull(result);
        // 关键词层始终保留（兜底），但总 token 应接近预算上限
        assertTrue(tight.usageRate() > 0 || result.contains("关键词"),
                "极紧预算下应降级到关键词兜底");
    }

    @Test
    void layerDistributionShouldReportCorrectly() {
        List<DocumentChunkEntity> chunks = List.of(
                chunk("短文本", 0),
                chunk("中等长度的文本内容用于测试分层分布情况".repeat(2), 1));
        ContextBudget budget = new ContextBudget(500);
        String dist = service.layerDistribution(chunks, budget);
        assertTrue(dist.contains("FULL:"), "应报告各层分布, got: " + dist);
        assertTrue(dist.contains("SUMMARY:"), "应报告摘要层");
        assertTrue(dist.contains("KEYWORDS:"), "应报告关键词层");
    }

    @Test
    void shouldHandleNullSummarizerGracefully() {
        List<DocumentChunkEntity> chunks = List.of(
                chunk("RAG检索结果压缩集成测试", 0));
        ContextBudget budget = new ContextBudget(2048);
        String result = service.compressAndAssemble(chunks, null, budget);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}

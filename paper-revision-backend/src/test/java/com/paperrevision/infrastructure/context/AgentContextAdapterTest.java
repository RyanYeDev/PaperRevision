package com.paperrevision.infrastructure.context;

import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** AgentContextAdapter 单元测试 — 上下文分层压缩 Step 5 */
class AgentContextAdapterTest {

    private final AgentContextAdapter adapter =
            new AgentContextAdapter(new ContextCompressor(), new LayeredContextManager());

    private DocumentChunkEntity chunk(String content, int idx) {
        DocumentChunkEntity e = new DocumentChunkEntity();
        e.setPaperId("p1");
        e.setChunkIndex(idx);
        e.setContent(content);
        return e;
    }

    @Test
    void shouldFitWithinBudgetWhenContextIsSmall() {
        String sys = "你是一个论文审核助手。";
        String user = "请帮我检查这篇论文的语法。";
        AgentContextAdapter.AdaptedContext result =
                adapter.adaptSimple(sys, user, p -> "摘要", 4096);
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
        assertFalse(result.downgraded(), "小上下文不应降级");
        assertTrue(result.actualTokens() <= result.budgetTokens(),
                "token 应不超预算");
    }

    @Test
    void shouldDowngradeWhenBudgetIsTight() {
        // 模拟真实 Agent 场景：带标点的中文段落（可被 ContextCompressor 正确分词压缩）
        String longText = ("论文返修平台通过AI Agent分析返修意见并给出修改方案。" +
                "平台支持PDF解析、RAG检索、工作流编排和全链路追踪。" +
                "系统采用DDD四层架构，包含领域层、应用层、基础设施层与接口层。").repeat(10);
        // 宽松预算：原始 token 远小于预算 → 全量通过
        AgentContextAdapter.AdaptedContext loose =
                adapter.adaptSimple(longText, "用户问题", null, 8192);
        assertFalse(loose.downgraded(), "宽松预算不应降级");

        // 紧预算：原始 token 远超预算 → 触发压缩降级
        AgentContextAdapter.AdaptedContext tight =
                adapter.adaptSimple(longText, longText, null, 512);
        assertTrue(tight.downgraded(), "紧预算应触发降级");
        assertFalse(tight.content().isEmpty());
    }

    @Test
    void shouldIncludeRagChunksInAdaptation() {
        String sys = "助手";
        String user = "查询";
        List<DocumentChunkEntity> chunks = List.of(
                chunk("RAG检索到的相关内容片段", 0));
        AgentContextAdapter.AdaptedContext result =
                adapter.adapt(sys, user, chunks, p -> "摘要", 4096);
        assertNotNull(result.content());
        assertTrue(result.content().contains("RAG") || result.content().contains("摘要"),
                "应包含 RAG 内容或其摘要");
    }

    @Test
    void shouldHandleNullRagChunks() {
        AgentContextAdapter.AdaptedContext result =
                adapter.adapt("sys", "user", null, null, 4096);
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty());
    }

    @Test
    void isOverBudgetShouldDetectCorrectly() {
        String shortText = "短文本";
        assertFalse(adapter.isOverBudget(shortText, shortText, 4096));

        String longText = "非常长的论文正文内容需要大量token预算来存储和处理".repeat(100);
        assertTrue(adapter.isOverBudget(longText, longText, 2048));
    }

    @Test
    void utilizationRateShouldBeBetweenZeroAndOne() {
        String text = "中等长度的测试文本用于验证利用率计算".repeat(10);
        AgentContextAdapter.AdaptedContext result =
                adapter.adaptSimple(text, text, null, 8192);
        double rate = result.utilizationRate();
        assertTrue(rate > 0, "利用率应 > 0");
        assertTrue(rate <= 1.0, "利用率应 ≤ 1.0");
    }

    @Test
    void shouldFallbackToKeywordsWhenSummarizerNull() {
        String text = "文本压缩测试数据验证降级行为".repeat(5);
        AgentContextAdapter.AdaptedContext result =
                adapter.adaptSimple(text, text, null, 128);
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty(), "null summarizer 应降级到关键词兜底");
    }
}

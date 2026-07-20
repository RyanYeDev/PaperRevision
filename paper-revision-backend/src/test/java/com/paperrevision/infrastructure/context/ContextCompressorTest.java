package com.paperrevision.infrastructure.context;

import com.paperrevision.infrastructure.context.ContextCompressor.CompressedContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** ContextCompressor 单元测试 — 上下文分层压缩 Step 2 */
class ContextCompressorTest {

    private final ContextCompressor compressor = new ContextCompressor();

    @Test
    void shouldProduceThreeLayersWithInjectedSummarizer() {
        String text = "论文返修平台通过 AI Agent 分析返修意见并给出修改方案。" +
                "平台支持 PDF 解析、RAG 检索、工作流编排和全链路追踪。";
        CompressedContext ctx = compressor.compress(text, prompt -> "这是摘要");

        assertEquals(text, ctx.full);
        assertEquals("这是摘要", ctx.summary);
        assertFalse(ctx.keywords.isEmpty(), "应提取到关键词");
    }

    @Test
    void summarizerShouldReceivePromptWithText() {
        String[] captured = new String[1];
        compressor.compress("待压缩的正文内容", prompt -> { captured[0] = prompt; return "ok"; });

        assertTrue(captured[0].startsWith(ContextCompressor.SUMMARY_PROMPT), "应带摘要 prompt 前缀");
        assertTrue(captured[0].contains("待压缩的正文内容"), "prompt 应包含原文");
    }

    @Test
    void shouldFallbackToKeywordsWhenSummarizerNull() {
        CompressedContext ctx = compressor.compressLocally("向量检索 向量检索 关键词 关键词 关键词");
        assertTrue(ctx.summary.startsWith("关键词："), "无 LLM 时应用关键词兜底, got: " + ctx.summary);
        assertTrue(ctx.keywords.contains("关键词"), "高频词应出现在关键词列表");
    }

    @Test
    void shouldFallbackWhenSummarizerThrows() {
        CompressedContext ctx = compressor.compress("一些正文内容", prompt -> { throw new RuntimeException("LLM 挂了"); });
        assertNotNull(ctx.summary, "异常时应降级不抛出");
        assertFalse(ctx.summary.startsWith(ContextCompressor.SUMMARY_PROMPT));
    }

    @Test
    void keywordsShouldRankByFrequencyAndSkipStopWords() {
        List<String> kw = compressor.extractKeywords(
                "Agent Agent Agent workflow workflow the the the the 我们 我们 我们", 5);
        assertEquals("agent", kw.get(0), "最高频应排第一（英文小写归一化）");
        assertFalse(kw.contains("the"), "英文停用词应过滤");
        assertFalse(kw.contains("我们"), "中文停用词应过滤");
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertTrue(compressor.extractKeywords(null, 8).isEmpty());
        assertTrue(compressor.extractKeywords("", 8).isEmpty());
        CompressedContext ctx = compressor.compressLocally(null);
        assertEquals("", ctx.full);
        assertEquals("", ctx.summary);
        assertEquals(0, ctx.fullTokens);
    }

    @Test
    void summaryRatioShouldReflectCompression() {
        String longText = "这是一段很长的正文内容需要被压缩成简短的摘要以节省上下文窗口的 token 消耗".repeat(5);
        CompressedContext ctx = compressor.compress(longText, prompt -> "短摘要");
        assertTrue(ctx.summaryRatio() < 1.0, "摘要应比全量短");
        assertTrue(ctx.fullTokens > ctx.summaryTokens);
    }
}

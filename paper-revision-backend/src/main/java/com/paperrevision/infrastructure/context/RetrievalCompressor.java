package com.paperrevision.infrastructure.context;

import com.paperrevision.domain.rag.model.DocumentChunkEntity;
import com.paperrevision.infrastructure.context.ContextCompressor.CompressedContext;
import com.paperrevision.infrastructure.utils.TokenCounter.ContextBudget;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检索结果压缩器 — 上下文分层压缩 Step 4
 *
 * 将 RAG 检索返回的 {@link DocumentChunkEntity} 列表自动经过
 * {@link ContextCompressor} 压缩，再通过 {@link LayeredContextManager}
 * 按 token 预算组装，输出可直接传给 LLM 的上下文文本。
 *
 * <p>使用示例：
 * <pre>{@code
 *   List<DocumentChunkEntity> chunks = retrievalService.vectorSearch(q, pid, 5);
 *   ContextBudget budget = new ContextBudget(4096);
 *   String ctx = retrievalCompressor.compressAndAssemble(chunks, llm::call, budget);
 *   // 将 ctx 作为 LLM 提示词的上下文注入
 * }</pre>
 *
 * <p>架构位置：infrastructure/context —— Infrastructure → Domain 允许，
 * 且 Application 层可直接注入本服务编排检索+压缩流程。
 */
@Service
public class RetrievalCompressor {

    private final ContextCompressor compressor;
    private final LayeredContextManager manager;

    public RetrievalCompressor(ContextCompressor compressor, LayeredContextManager manager) {
        this.compressor = compressor;
        this.manager = manager;
    }

    /**
     * 压缩检索结果并按 token 预算组装。
     *
     * @param chunks     检索返回的文档分块列表
     * @param summarizer LLM 摘要函数（可 null，降级为本地关键词兜底）
     * @param budget     token 预算，控制最终输出上限
     * @return 经过分层压缩与预算裁剪后的上下文字符串
     */
    public String compressAndAssemble(List<DocumentChunkEntity> chunks,
                                       Function<String, String> summarizer,
                                       ContextBudget budget) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        List<CompressedContext> compressed = chunks.stream()
                .map(chunk -> compressor.compress(chunk.getContent(), summarizer))
                .collect(Collectors.toList());
        return manager.assemble(compressed, budget);
    }

    /**
     * 仅本地压缩（不调用 LLM），适合快速预览、无 LLM 环境或降级场景。
     */
    public String compressAndAssembleLocally(List<DocumentChunkEntity> chunks, ContextBudget budget) {
        return compressAndAssemble(chunks, null, budget);
    }

    /** 统计压缩后的层级分布（FULL/SUMMARY/KEYWORDS 各几段），用于监控与调优 */
    public String layerDistribution(List<DocumentChunkEntity> chunks, ContextBudget budget) {
        if (chunks == null || chunks.isEmpty()) return "0 chunks";
        int[] counts = new int[3]; // FULL=0, SUMMARY=1, KEYWORDS=2
        for (DocumentChunkEntity chunk : chunks) {
            CompressedContext ctx = compressor.compressLocally(chunk.getContent());
            LayeredContextManager.Selection sel = manager.selectLayer(ctx, budget.remaining());
            counts[sel.layer.ordinal()]++;
        }
        return String.format("FULL:%d SUMMARY:%d KEYWORDS:%d (total:%d, budget:%d)",
                counts[0], counts[1], counts[2], chunks.size(), budget.max());
    }
}

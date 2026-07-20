package com.paperrevision.infrastructure.context;

import com.paperrevision.infrastructure.utils.TokenCounter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 上下文压缩服务 — 上下文分层压缩 Step 2
 *
 * 将长文本压缩为三层，供下游按 token 预算（{@link TokenCounter.ContextBudget}）选择：
 *  - full:     全量原文
 *  - summary:  段落摘要（调用 LLM 生成，prompt 见 {@link #SUMMARY_PROMPT}）
 *  - keywords: 关键词列表（本地词频提取，无需 LLM，可作降级兜底）
 *
 * LLM 调用通过 summarizer 函数注入，与具体 provider 解耦（生产传 chatModel::generate，
 * 单测传假函数），因此本服务无基础设施硬依赖，也便于测试。
 */
@Service
public class ContextCompressor {

    /** 摘要 prompt 模板 */
    public static final String SUMMARY_PROMPT =
            "将以下文本压缩为不超过3句话的关键信息，只输出摘要本身，不要解释：\n\n";

    /** 关键词候选：2-4字中文词组 或 ≥3 字母英文词（限制长度防止贪婪匹配全文为单关键词） */
    private static final Pattern TOKEN_WORD = Pattern.compile("[\\u4e00-\\u9fff]{2,4}|[a-zA-Z]{3,}");

    /** 精简中英文停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "that", "this", "with", "are", "was", "from", "have",
            "我们", "以及", "对于", "并且", "这个", "一个", "可以", "通过", "进行", "使用");

    /**
     * 压缩为三层。summarizer 为 null 或调用异常时降级：summary 用关键词拼接兜底。
     */
    public CompressedContext compress(String text, Function<String, String> summarizer) {
        String full = text == null ? "" : text;
        List<String> keywords = extractKeywords(full, 8);
        String summary;
        if (summarizer != null && !full.isBlank()) {
            try {
                summary = summarizer.apply(SUMMARY_PROMPT + full).trim();
            } catch (Exception e) {
                summary = fallbackSummary(keywords);
            }
        } else {
            summary = fallbackSummary(keywords);
        }
        return new CompressedContext(full, summary, keywords);
    }

    /** 仅本地压缩（不调用 LLM），summary 由关键词兜底 */
    public CompressedContext compressLocally(String text) {
        return compress(text, null);
    }

    /** 词频提取 top-N 关键词（英文归一化为小写，过滤停用词） */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) return List.of();
        Map<String, Integer> freq = new HashMap<>();
        Matcher m = TOKEN_WORD.matcher(text);
        while (m.find()) {
            String w = m.group();
            String key = w.matches("[a-zA-Z]+") ? w.toLowerCase() : w;
            if (STOP_WORDS.contains(key)) continue;
            freq.merge(key, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String fallbackSummary(List<String> keywords) {
        return keywords.isEmpty() ? "" : "关键词：" + String.join("、", keywords);
    }

    /** 三层压缩结果，附各层 token 估算 */
    public static class CompressedContext {
        public final String full;
        public final String summary;
        public final List<String> keywords;
        public final int fullTokens;
        public final int summaryTokens;
        public final int keywordTokens;

        public CompressedContext(String full, String summary, List<String> keywords) {
            this.full = full;
            this.summary = summary;
            this.keywords = keywords;
            this.fullTokens = TokenCounter.estimate(full);
            this.summaryTokens = TokenCounter.estimate(summary);
            this.keywordTokens = TokenCounter.estimate(String.join(" ", keywords));
        }

        /** 摘要层相对全量的 token 压缩率（越小压缩越多） */
        public double summaryRatio() {
            return fullTokens > 0 ? (double) summaryTokens / fullTokens : 0;
        }
    }
}

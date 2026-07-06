package com.paperrevision.infrastructure.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token 计数工具 — 上下文分层压缩的基础设施
 *
 * 估算规则（基于主流 LLM tokenizer 统计规律）：
 * - 中文字符 ≈ 1.5 token/字
 * - 英文单词 ≈ 1.3 token/词
 * - 数字/标点 ≈ 1 token
 *
 * 后续将扩展为 ContextBudget 管理器，支持分层压缩策略。
 */
public final class TokenCounter {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf]");
    private static final Pattern ENGLISH_WORD = Pattern.compile("[a-zA-Z]+");
    private static final Pattern DIGIT_PUNCT = Pattern.compile("[0-9\\p{P}]");

    private static final double CN_TOKEN_PER_CHAR = 1.5;
    private static final double EN_TOKEN_PER_WORD = 1.3;
    private static final double DIGIT_PUNCT_TOKEN = 1.0;

    private TokenCounter() {}

    /** 估算文本的 token 数量 */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;

        int chineseChars = countPattern(text, CHINESE_PATTERN);
        int englishWords = countPattern(text, ENGLISH_WORD);
        int digitPuncts = countPattern(text, DIGIT_PUNCT);

        // 剩余字符（空格、换行等）按每个 1 token
        int remaining = text.length() - chineseChars - englishWords - digitPuncts;

        return (int) Math.ceil(
                chineseChars * CN_TOKEN_PER_CHAR
                + englishWords * EN_TOKEN_PER_WORD
                + digitPuncts * DIGIT_PUNCT_TOKEN
                + Math.max(0, remaining)
        );
    }

    /** 批量估算 */
    public static int estimateBatch(List<String> texts) {
        return texts.stream().mapToInt(TokenCounter::estimate).sum();
    }

    /** 快速估算法：中文 ≈1.5 token/字，英文/其他 ≈4 字符/token，与 estimate 一致 */
    public static int quickEstimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        int cnChars = countPattern(text, CHINESE_PATTERN);
        int otherChars = text.length() - cnChars;
        return (int) Math.ceil(cnChars * CN_TOKEN_PER_CHAR + otherChars / 4.0);
    }

    /** 获取 Token 估算报告 */
    public static TokenReport report(String text) {
        TokenReport report = new TokenReport();
        report.totalChars = text != null ? text.length() : 0;
        report.estimatedTokens = estimate(text);
        report.chineseChars = countPattern(text, CHINESE_PATTERN);
        report.englishWords = countPattern(text, ENGLISH_WORD);
        return report;
    }

    private static int countPattern(String text, Pattern pattern) {
        if (text == null) return 0;
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) count++;
        return count;
    }

    /** Token 估算报告 */
    public static class TokenReport {
        public int totalChars;
        public int estimatedTokens;
        public int chineseChars;
        public int englishWords;

        public double compressionRatio() {
            return totalChars > 0 ? (double) estimatedTokens / totalChars : 0;
        }

        @Override
        public String toString() {
            return String.format("TokenReport{chars=%d, tokens=%d, cn=%d, en=%d, ratio=%.2f}",
                    totalChars, estimatedTokens, chineseChars, englishWords, compressionRatio());
        }
    }

    /** 上下文预算 — 控制每层上下文使用的 token 上限 */
    public static class ContextBudget {
        private final int maxTokens;
        private int usedTokens;

        public ContextBudget(int maxTokens) {
            this.maxTokens = maxTokens;
            this.usedTokens = 0;
        }

        public boolean canFit(String text) {
            return usedTokens + estimate(text) <= maxTokens;
        }

        public boolean tryAllocate(String text) {
            int tokens = estimate(text);
            if (usedTokens + tokens > maxTokens) return false;
            usedTokens += tokens;
            return true;
        }

        public int remaining() { return Math.max(0, maxTokens - usedTokens); }
        public int used() { return usedTokens; }
        public int max() { return maxTokens; }
        public double usageRate() { return maxTokens > 0 ? (double) usedTokens / maxTokens : 0; }
    }
}

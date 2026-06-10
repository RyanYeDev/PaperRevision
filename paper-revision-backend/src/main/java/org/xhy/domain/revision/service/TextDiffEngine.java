package org.xhy.domain.revision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/** 文本差异对比引擎 */
@Service
public class TextDiffEngine {

    private static final Logger logger = LoggerFactory.getLogger(TextDiffEngine.class);

    /** 生成行级差异 */
    public List<DiffLine> generateDiff(String originalText, String revisedText) {
        String[] originalLines = originalText.split("\n");
        String[] revisedLines = revisedText.split("\n");

        List<DiffLine> diff = new ArrayList<>();

        // 简单的LCS diff算法
        int[][] lcs = computeLCS(originalLines, revisedLines);

        int i = originalLines.length;
        int j = revisedLines.length;

        List<DiffLine> reversed = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && originalLines[i - 1].equals(revisedLines[j - 1])) {
                reversed.add(new DiffLine(DiffType.UNCHANGED, originalLines[i - 1]));
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                reversed.add(new DiffLine(DiffType.ADDED, revisedLines[j - 1]));
                j--;
            } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
                reversed.add(new DiffLine(DiffType.REMOVED, originalLines[i - 1]));
                i--;
            }
        }

        Collections.reverse(reversed);
        diff.addAll(reversed);

        logger.info("Diff生成完成: {}行差异", diff.size());
        return diff;
    }

    private int[][] computeLCS(String[] a, String[] b) {
        int m = a.length, n = b.length;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    public enum DiffType { UNCHANGED, ADDED, REMOVED }

    public static class DiffLine {
        private final DiffType type;
        private final String content;
        public DiffLine(DiffType type, String content) { this.type = type; this.content = content; }
        public DiffType getType() { return type; }
        public String getContent() { return content; }
    }
}

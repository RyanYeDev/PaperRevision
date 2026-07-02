package org.xhy.infrastructure.rag.parser.pdfbox.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.infrastructure.rag.parser.pdfbox.TextBlock;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 噪声过滤器
 *
 * 学术论文中的噪声:
 * 1. 页眉: 论文标题/作者名/会议名（每页顶部重复）
 * 2. 页脚: 版权声明/DOI/URL
 * 3. 页码: "1", "Page 1", "1/12"
 * 4. 水印: "CONFIDENTIAL", "DRAFT"
 * 5. 期刊/会议信息条: "IEEE Transactions on...", "© 2024 ACM"
 */
public class NoiseFilter {

    private static final Logger logger = LoggerFactory.getLogger(NoiseFilter.class);

    // 噪声模式
    private static final Pattern PAGE_NUMBER = Pattern.compile(
            "^(\\d{1,4}|Page\\s*\\d+|\\d+/\\d+)$");
    private static final Pattern COPYRIGHT = Pattern.compile(
            "(?i).*(©|copyright|all rights reserved|\\bdoi\\b:).*");
    private static final Pattern CONFERENCE_BANNER = Pattern.compile(
            "(?i).*(IEEE|ACM|Springer|Elsevier|Proceedings of|International Conference).*");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://\\S+");

    /** 过滤噪声 */
    public FilterResult filter(List<TextBlock> blocks, float pageTop, float pageBottom) {
        int noiseCount = 0;
        List<TextBlock> cleanBlocks = new ArrayList<>();

        // 统计高频出现的文本（页眉页脚通常每页都一样）
        Map<String, Integer> textFrequency = new HashMap<>();
        for (TextBlock b : blocks) {
            textFrequency.merge(b.getText().trim(), 1, Integer::sum);
        }

        int pageCount = estimatePageCount(blocks);
        float headerZone = pageTop + 50;   // 页眉区域（页面顶部50pt内）
        float footerZone = pageBottom - 50; // 页脚区域（页面底部50pt内）

        for (TextBlock block : blocks) {
            String text = block.getText().trim();
            boolean isNoise = false;

            // 1. 页眉区 + 高频重复 = 页眉
            if (block.getY() < headerZone) {
                int freq = textFrequency.getOrDefault(text, 0);
                if (freq >= pageCount * 0.5) { // 超过半数页面出现
                    block.setType(TextBlock.BlockType.HEADER);
                    isNoise = true;
                }
            }

            // 2. 页脚区 = 页脚或页码
            if (block.getY() > footerZone) {
                if (PAGE_NUMBER.matcher(text).matches()) {
                    block.setType(TextBlock.BlockType.PAGE_NUMBER);
                    isNoise = true;
                } else if (COPYRIGHT.matcher(text).matches() || URL_PATTERN.matcher(text).find()) {
                    block.setType(TextBlock.BlockType.FOOTER);
                    isNoise = true;
                }
            }

            // 3. 版权/会议信息条（通常在第一页底部或最后一页）
            if (CONFERENCE_BANNER.matcher(text).matches() && text.length() > 50) {
                block.setType(TextBlock.BlockType.FOOTER);
                isNoise = true;
            }

            // 4. 水印/空文本
            if (text.isEmpty() || text.matches("^(DRAFT|CONFIDENTIAL|SAMPLE|PREPRINT)$")) {
                block.setType(TextBlock.BlockType.NOISE);
                isNoise = true;
            }

            if (isNoise) noiseCount++;
            else cleanBlocks.add(block);
        }

        logger.info("噪声过滤完成: 移除{}个噪声块，保留{}个有效块", noiseCount, cleanBlocks.size());
        return new FilterResult(cleanBlocks, noiseCount);
    }

    /** 估算页数 */
    private int estimatePageCount(List<TextBlock> blocks) {
        return blocks.stream()
                .mapToInt(TextBlock::getPageNumber)
                .max().orElse(1);
    }

    /** 过滤结果 */
    public static class FilterResult {
        public final List<TextBlock> cleanBlocks;
        public final int noiseRemoved;

        public FilterResult(List<TextBlock> clean, int removed) {
            this.cleanBlocks = clean; this.noiseRemoved = removed;
        }
    }
}

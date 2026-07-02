package com.paperrevision.infrastructure.rag.parser.pdfbox.heading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.paperrevision.infrastructure.rag.parser.pdfbox.TextBlock;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 标题层级识别器
 *
 * 学术论文标题特征:
 * Level 1 (H1): "1. Introduction", "I. INTRODUCTION" - 编号+大字号+加粗
 * Level 2 (H2): "1.1 Background", "A. Related Work" - 子编号+中字号
 * Level 3 (H3): "1.1.1 Details" - 更深层编号
 *
 * 检测综合策略:
 * 1. 编号模式: "1.", "1.1", "I.", "A.", "一、"
 * 2. 字体特征: 加粗 + 字号大于正文平均值
 * 3. 位置特征: 左对齐，上方有空行
 */
public class HeadingDetector {

    private static final Logger logger = LoggerFactory.getLogger(HeadingDetector.class);

    // 常见论文标题模式
    private static final Pattern[] HEADING_PATTERNS = {
            Pattern.compile("^\\d+\\.\\s+[A-Z].*"),                // "1. Introduction"
            Pattern.compile("^\\d+\\.\\d+\\s+[A-Z].*"),            // "1.1 Background"
            Pattern.compile("^\\d+\\.\\d+\\.\\d+\\s+[A-Z].*"),     // "1.1.1 Detail"
            Pattern.compile("^[IVX]+\\.\\s+[A-Z].*"),              // "IV. Results"
            Pattern.compile("^[A-Z]\\..*"),                         // "A. Appendix"
            Pattern.compile("^(Abstract|Introduction|Method|Result|Discussion|Conclusion|Reference|Acknowledg)[s]?\\b.*",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(摘要|引言|方法|结果|讨论|结论|参考文献|致谢).*")
    };

    /** 检测标题层级 */
    public void detectAndLabel(List<TextBlock> blocks, float avgFontSize) {
        float headingThreshold = avgFontSize * 1.15f; // 标题至少比正文大15%

        for (TextBlock block : blocks) {
            if (isHeading(block, headingThreshold)) {
                int level = determineLevel(block);
                block.setType(TextBlock.BlockType.HEADING);
                block.setText("H" + level + ": " + block.getText());
                logger.debug("标题 H{}: {} ({}pt, bold={})", level,
                        block.getText().substring(0, Math.min(60, block.getText().length())),
                        block.getFontSize(), block.isBold());
            }
        }
    }

    /** 判断是否为标题 */
    private boolean isHeading(TextBlock block, float threshold) {
        String text = block.getText().trim();
        if (text.isEmpty() || text.length() > 200) return false; // 标题不会太长

        // 编号模式匹配
        for (Pattern p : HEADING_PATTERNS) {
            if (p.matcher(text).matches()) return true;
        }

        // 字体特征: 加粗且字号大
        if ((block.isBold() && block.getFontSize() >= threshold)) {
            return text.length() < 100; // 短文本+大字+加粗=可能是标题
        }

        return false;
    }

    /** 确定标题层级 */
    private int determineLevel(TextBlock block) {
        String text = block.getText().trim();

        // 编号深度决定层级
        if (text.matches("^\\d+\\.\\d+\\.\\d+.*")) return 3;
        if (text.matches("^\\d+\\.\\d+.*")) return 2;

        // 字号判断
        if (block.getFontSize() > 18) return 1; // 超大 = 论文标题
        if (block.getFontSize() > 14) return 2;

        return Math.min(text.split("\\.").length, 3); // 根据编号段数
    }
}

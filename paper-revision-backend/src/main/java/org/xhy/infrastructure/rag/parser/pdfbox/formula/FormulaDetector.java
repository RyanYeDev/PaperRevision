package org.xhy.infrastructure.rag.parser.pdfbox.formula;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.infrastructure.rag.parser.pdfbox.TextBlock;

import java.util.regex.Pattern;

/**
 * 公式区域检测器
 *
 * 学术论文中公式的特征:
 * 1. 居中排版 (x坐标偏离左对齐位置)
 * 2. 右侧有公式编号 "(1)", "(2a)"
 * 3. 包含数学符号: ∫∑∂√∞αβγ±≤≥∈→⇒
 * 4. 包含LaTeX风格: \frac, \sum, \int
 * 5. 使用特殊的数学字体 (如 italic 变量)
 * 6. 行高异常（分数、上下标导致行高增大）
 */
public class FormulaDetector {

    private static final Logger logger = LoggerFactory.getLogger(FormulaDetector.class);

    // 数学符号字符集 (Unicode数学运算符范围)
    private static final Pattern MATH_SYMBOLS = Pattern.compile(
            "[∫∑∏∂√∞∇∀∃∈∉⊂⊃∪∩∧∨⇒⇔→←↑↓↔⇀↼⇁↽" +
                    "αβγδεζηθικλμνξπρστυφχψω" + // 希腊小写
                    "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΠΡΣΤΥΦΧΨΩ" + // 希腊大写
                    "±×÷≤≥≠≈≡≅∝∼≪≫" +
                    "⟨⟩∥∣∤⌊⌋⌈⌉" +
                    "½⅓⅔¼¾⅕⅖⅗⅘⅙⅚]");

    // LaTeX命令
    private static final Pattern LATEX_COMMANDS = Pattern.compile(
            "\\\\frac|\\\\sum|\\\\int|\\\\prod|\\\\lim|\\\\sqrt|\\\\alpha|\\\\beta|" +
                    "\\\\gamma|\\\\delta|\\\\partial|\\\\infty|\\\\rightarrow|\\\\leftarrow|" +
                    "\\\\mathbb|\\\\mathcal|\\\\mathbf|\\\\mathrm|\\\\begin\\{|\\\\end\\{");

    /** 检测是否为公式区域 */
    public boolean isFormula(TextBlock block, float pageCenterX, double normalLeftMargin) {
        String text = block.getText().trim();
        if (text.isEmpty()) return false;

        int score = 0;

        // 1. 居中排版 (距离左页边距很远，接近页面中心)
        float blockCenter = block.getX() + block.getWidth() / 2;
        if (Math.abs(blockCenter - pageCenterX) < 50) score += 3;
        if (block.getX() > normalLeftMargin + 80) score += 2;

        // 2. 包含数学符号
        if (MATH_SYMBOLS.matcher(text).find()) score += 3;

        // 3. 包含LaTeX命令
        if (LATEX_COMMANDS.matcher(text).find()) score += 3;

        // 4. 右侧有编号 "(数字)" 或 "(数字+字母)"
        if (text.matches(".*\\(\\d+[a-z]?\\)\\s*$")) score += 2;

        // 5. 短文本 + 大量特殊字符
        if (text.length() < 200) {
            int specialCount = 0;
            for (char c : text.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) specialCount++;
            }
            float specialRatio = (float) specialCount / text.length();
            if (specialRatio > 0.15) score += 2; // 超过15%是特殊字符
        }

        // 6. 使用斜体字体（变量通常用斜体）
        if (block.isItalic() && block.getText().length() < 100) score += 1;

        return score >= 5; // 阈值: 至少5分才算公式
    }

    /** 检测并标记公式区域 */
    public void detectAndLabel(java.util.List<TextBlock> blocks, float pageWidth) {
        float pageCenterX = pageWidth / 2;

        // 估算正常左页边距（最高频的左对齐x坐标）
        double normalLeftMargin = blocks.stream()
                .filter(b -> b.getType() == TextBlock.BlockType.BODY)
                .mapToDouble(TextBlock::getX)
                .average().orElse(72.0); // 默认1英寸

        int formulaCount = 0;
        for (TextBlock block : blocks) {
            if (block.getType() == TextBlock.BlockType.BODY && isFormula(block, pageCenterX, normalLeftMargin)) {
                block.setType(TextBlock.BlockType.FORMULA);
                formulaCount++;
            }
        }
        logger.info("公式检测完成: {}个公式区域", formulaCount);
    }
}

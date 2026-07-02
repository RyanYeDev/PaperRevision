package com.paperrevision.infrastructure.rag.parser.pdfbox;

import java.util.List;

/**
 * PDF文本块模型 - 带坐标信息的文本片段
 * PDFBox底层API提供每个字符的x/y坐标和字体信息
 */
public class TextBlock {
    private String text;
    private float x, y, width, height;
    private float fontSize;
    private String fontName;
    private boolean bold;
    private boolean italic;
    private int pageNumber;

    // 分类标签
    public enum BlockType {
        HEADING,     // 章节标题
        BODY,        // 正文
        TABLE,       // 表格
        FORMULA,     // 公式
        FIGURE_CAPTION, // 图表标题
        HEADER,      // 页眉
        FOOTER,      // 页脚
        PAGE_NUMBER, // 页码
        REFERENCE,   // 参考文献条目
        NOISE        // 噪声（水印等）
    }
    private BlockType type = BlockType.BODY;

    // 工厂方法
    public static TextBlock of(String text, float x, float y, float width, float height,
            float fontSize, String fontName, int page) {
        TextBlock b = new TextBlock();
        b.text = text;
        b.x = x; b.y = y; b.width = width; b.height = height;
        b.fontSize = fontSize; b.fontName = fontName;
        b.bold = fontName != null && fontName.toLowerCase().contains("bold");
        b.italic = fontName != null && fontName.toLowerCase().contains("italic");
        b.pageNumber = page;
        return b;
    }

    /** 是否在同一行 */
    public boolean sameLineAs(TextBlock other) {
        return Math.abs(this.y - other.y) < this.height * 0.5;
    }

    /** 合并两个文本块 */
    public TextBlock merge(TextBlock other) {
        this.text += " " + other.text;
        this.width = Math.max(this.x + this.width, other.x + other.width) - Math.min(this.x, other.x);
        return this;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getFontSize() { return fontSize; }
    public String getFontName() { return fontName; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public int getPageNumber() { return pageNumber; }
    public BlockType getType() { return type; }
    public void setType(BlockType type) { this.type = type; }

    @Override
    public String toString() {
        return String.format("[%s] p%d (%.0f,%.0f) %.1fpt %s%s: %s",
                type, pageNumber, x, y, fontSize,
                bold ? "B" : "", italic ? "I" : "",
                text.length() > 80 ? text.substring(0, 80) + "..." : text);
    }
}

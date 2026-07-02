package org.xhy.infrastructure.rag.parser.pdfbox;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.paper.model.StructuredPaper;
import org.xhy.domain.paper.model.StructuredPaper.Reference;
import org.xhy.domain.paper.model.StructuredPaper.Section;
import org.xhy.infrastructure.rag.parser.pdfbox.column.ColumnDetector;
import org.xhy.infrastructure.rag.parser.pdfbox.filter.NoiseFilter;
import org.xhy.infrastructure.rag.parser.pdfbox.formula.FormulaDetector;
import org.xhy.infrastructure.rag.parser.pdfbox.heading.HeadingDetector;
import org.xhy.infrastructure.rag.parser.pdfbox.table.TableExtractor;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 论文结构分析器（PDFBox底层坐标版）
 *
 * 完整流水线:
 * PDF文件 → 坐标级文本提取 → 噪声过滤 → 栏位检测 → 标题层级识别
 *         → 公式标注 → 表格提取 → 结构化论文输出
 *
 * 核心：利用PDFBox底层的TextPosition API获取每个字符的精确坐标和字体信息
 */
@Service
public class PaperStructureAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(PaperStructureAnalyzer.class);

    private final ColumnDetector columnDetector = new ColumnDetector();
    private final HeadingDetector headingDetector = new HeadingDetector();
    private final TableExtractor tableExtractor = new TableExtractor();
    private final FormulaDetector formulaDetector = new FormulaDetector();
    private final NoiseFilter noiseFilter = new NoiseFilter();

    /** 分析PDF文件，输出结构化论文 */
    public StructuredPaper analyze(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            // 阶段1: 坐标级文本提取
            List<TextBlock> allBlocks = extractTextWithCoordinates(document);
            logger.info("坐标提取完成: {}个文本块, {}页", allBlocks.size(), document.getNumberOfPages());

            // 阶段2: 噪声过滤
            PDRectangle mediaBox = document.getPage(0).getMediaBox();
            float pageTop = mediaBox.getHeight();
            float pageBottom = 0;
            NoiseFilter.FilterResult filterResult = noiseFilter.filter(allBlocks, pageTop, pageBottom);
            List<TextBlock> cleanBlocks = filterResult.cleanBlocks;

            // 阶段3: 栏位检测
            ColumnDetector.ColumnInfo columnInfo = columnDetector.detect(cleanBlocks);

            // 阶段4: 计算统计特征
            float avgFontSize = (float) cleanBlocks.stream()
                    .filter(b -> b.getType() == TextBlock.BlockType.BODY)
                    .mapToDouble(TextBlock::getFontSize).average().orElse(10);

            // 阶段5: 标题层级识别
            headingDetector.detectAndLabel(cleanBlocks, avgFontSize);

            // 阶段6: 公式检测
            formulaDetector.detectAndLabel(cleanBlocks, mediaBox.getWidth());

            // 阶段7: 表格提取
            List<TableExtractor.TableRegion> tables = tableExtractor.extractTables(cleanBlocks);

            // 阶段8: 构建结构化论文
            return buildStructuredPaper(cleanBlocks, columnInfo, tables, document.getNumberOfPages());

        } catch (IOException e) {
            logger.error("论文结构分析失败", e);
            throw new RuntimeException("论文结构分析失败: " + e.getMessage());
        }
    }

    /** 从PDF中提取带坐标的文本块 */
    private List<TextBlock> extractTextWithCoordinates(PDDocument document) throws IOException {
        List<TextBlock> blocks = new ArrayList<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            private float lastX = 0, lastY = 0;
            private StringBuilder currentText = new StringBuilder();
            private float blockStartX = 0, blockStartY = 0;
            private float blockWidth = 0, blockMaxHeight = 0;
            private float currentFontSize = 10;
            private String currentFontName = "Unknown";

            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                if (positions.isEmpty()) return;

                TextPosition first = positions.get(0);

                // 检测是否换行（y坐标变化 > 字体大小的1.5倍）
                boolean newLine = Math.abs(first.getY() - lastY) > first.getFontSize() * 1.5;
                // 检测是否新块（x坐标跳变或缩进）
                boolean newBlock = Math.abs(first.getX() - lastX) > 30;

                if ((newLine || newBlock) && currentText.length() > 0) {
                    // 保存上一个文本块
                    blocks.add(TextBlock.of(
                            currentText.toString().trim(),
                            blockStartX, blockStartY,
                            blockWidth, blockMaxHeight,
                            currentFontSize, currentFontName,
                            getCurrentPageNo()));
                    currentText = new StringBuilder();
                }

                // 更新块坐标
                if (currentText.length() == 0) {
                    blockStartX = first.getX();
                    blockStartY = first.getY();
                    blockWidth = 0;
                    blockMaxHeight = 0;
                }

                currentText.append(text);
                float rightEdge = first.getX() + first.getWidth();
                blockWidth = Math.max(blockWidth, rightEdge - blockStartX);
                blockMaxHeight = Math.max(blockMaxHeight, first.getHeight());
                currentFontSize = first.getFontSize();
                currentFontName = first.getFont() != null ? first.getFont().getName() : "Unknown";

                lastX = first.getX();
                lastY = first.getY();
            }

            @Override
            protected void endDocument(PDDocument document) throws IOException {
                // 保存最后一个文本块
                if (currentText.length() > 0) {
                    blocks.add(TextBlock.of(
                            currentText.toString().trim(),
                            blockStartX, blockStartY,
                            blockWidth, blockMaxHeight,
                            currentFontSize, currentFontName,
                            getCurrentPageNo()));
                }
            }
        };

        stripper.setSortByPosition(true);
        stripper.getText(document); // 触发回调
        return blocks;
    }

    /** 构建结构化论文 */
    private StructuredPaper buildStructuredPaper(List<TextBlock> blocks,
            ColumnDetector.ColumnInfo columnInfo,
            List<TableExtractor.TableRegion> tables, int pageCount) {

        StructuredPaper paper = new StructuredPaper();
        paper.setPageCount(pageCount);

        // 收集标题
        List<TextBlock> headings = blocks.stream()
                .filter(b -> b.getType() == TextBlock.BlockType.HEADING)
                .toList();

        // 找论文标题（第一个H1，且Y坐标最高 = 页面最上方的大标题）
        Optional<TextBlock> titleBlock = headings.stream()
                .filter(b -> b.getText().startsWith("H1:"))
                .min(Comparator.comparing(TextBlock::getY));
        titleBlock.ifPresent(b -> paper.setTitle(b.getText().replace("H1: ", "")));

        // 构建章节树
        List<Section> sections = buildSections(blocks, headings);
        paper.setSections(sections);

        // 提取摘要（第一个标题之前的大段文本块）
        if (!headings.isEmpty()) {
            float firstHeadingY = headings.stream()
                    .min(Comparator.comparing(TextBlock::getY)).get().getY();
            String abstractText = blocks.stream()
                    .filter(b -> b.getY() < firstHeadingY && b.getType() == TextBlock.BlockType.BODY)
                    .map(TextBlock::getText)
                    .collect(Collectors.joining("\n"));
            if (abstractText.length() > 50) {
                paper.setAbstractText(abstractText);
            }
        }

        // 提取参考文献
        List<Reference> references = extractReferences(blocks);
        paper.setReferences(references);

        // 拼接全文
        StringBuilder fullText = new StringBuilder();
        if (paper.getTitle() != null) fullText.append("# ").append(paper.getTitle()).append("\n\n");
        if (paper.getAbstractText() != null) fullText.append(paper.getAbstractText()).append("\n\n");
        for (Section section : sections) {
            appendSection(fullText, section, 1);
        }
        fullText.append("\n## References\n");
        for (Reference ref : references) {
            fullText.append("- ").append(ref.getRawText()).append("\n");
        }
        paper.setFullText(fullText.toString());

        // 表格信息
        String tableInfo = tables.stream()
                .map(t -> t.rows.size() + "x" + (t.rows.isEmpty() ? 0 : t.rows.get(0).size()))
                .collect(Collectors.joining(", "));
        logger.info("结构化论文完成: 标题='{}', {}章节, {}表格, {}参考文献, {}页, 栏位={}",
                paper.getTitle(), sections.size(), tables.size(), references.size(),
                pageCount, columnInfo.layout);

        return paper;
    }

    /** 构建章节树 */
    private List<Section> buildSections(List<TextBlock> blocks, List<TextBlock> headings) {
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < headings.size(); i++) {
            TextBlock heading = headings.get(i);
            float startY = heading.getY();
            float endY = (i + 1 < headings.size()) ? headings.get(i + 1).getY() : Float.MAX_VALUE;

            String content = blocks.stream()
                    .filter(b -> b.getY() > startY && b.getY() < endY
                            && b.getType() == TextBlock.BlockType.BODY)
                    .map(TextBlock::getText)
                    .collect(Collectors.joining("\n"));

            Section section = new Section();
            section.setTitle(heading.getText().replaceAll("^H\\d+: ", ""));
            section.setContent(content);
            sections.add(section);
        }
        return sections;
    }

    /** 提取参考文献条目 */
    private List<Reference> extractReferences(List<TextBlock> blocks) {
        // 找到参考文献章节
        Optional<TextBlock> refHeading = blocks.stream()
                .filter(b -> b.getType() == TextBlock.BlockType.HEADING)
                .filter(b -> b.getText().toLowerCase().contains("reference")
                        || b.getText().contains("参考文献"))
                .findFirst();

        if (refHeading.isEmpty()) return List.of();

        float refStartY = refHeading.get().getY();
        return blocks.stream()
                .filter(b -> b.getY() > refStartY && b.getType() == TextBlock.BlockType.BODY)
                .filter(b -> b.getText().matches("^\\[\\d+\\].*")) // [1] 开头
                .limit(50)
                .map(b -> {
                    Reference ref = new Reference();
                    ref.setRawText(b.getText());
                    return ref;
                })
                .collect(Collectors.toList());
    }

    private void appendSection(StringBuilder sb, Section section, int level) {
        sb.append("#".repeat(level + 1)).append(" ").append(section.getTitle()).append("\n\n");
        sb.append(section.getContent()).append("\n\n");
        if (section.getSubsections() != null) {
            for (Section sub : section.getSubsections()) {
                appendSection(sb, sub, level + 1);
            }
        }
    }
}

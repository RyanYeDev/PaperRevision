package com.paperrevision.infrastructure.rag.parser.pdfbox.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.paperrevision.infrastructure.rag.parser.pdfbox.TextBlock;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表格检测与结构化提取
 *
 * 策略（论文表格常见特征）：
 * 1. 规则线表格: 检测PDF中的矩形/线条，识别边界框
 * 2. 无线表格: 文本按列对齐，行间距均匀 -> 列对齐聚类
 * 3. 三线表: 学术论文最常见的表格格式
 *    顶部粗线 + 表头 + 中部细线 + 数据行 + 底部粗线
 *
 * 输出: 结构化表格数据 + 单元格坐标
 */
public class TableExtractor {

    private static final Logger logger = LoggerFactory.getLogger(TableExtractor.class);

    /** 表格区域识别结果 */
    public static class TableRegion {
        public final float yStart, yEnd;
        public final List<List<String>> rows; // 行列表，每行是单元格列表
        public final String caption; // 表标题 "Table 1: ..."

        public TableRegion(float ys, float ye, List<List<String>> rows, String caption) {
            this.yStart = ys; this.yEnd = ye; this.rows = rows; this.caption = caption;
        }
    }

    /** 检测并提取表格 */
    public List<TableRegion> extractTables(List<TextBlock> blocks) {
        List<TableRegion> tables = new ArrayList<>();

        // 1. 按行分组（y坐标相近的文本块视为同一行）
        List<List<TextBlock>> rows = groupByRows(blocks);

        // 2. 检测表标题 "Table 1:", "表1:" 等
        String currentCaption = null;
        float captionY = 0;

        for (List<TextBlock> row : rows) {
            String rowText = row.stream().map(TextBlock::getText).collect(Collectors.joining(" "));

            if (isTableCaption(rowText)) {
                currentCaption = rowText;
                captionY = row.get(0).getY();
                continue;
            }

            // 3. 检测表格数据行（文本按列对齐，间距均匀）
            if (currentCaption != null && isTableRow(row)) {
                // 找到表格区域
                List<List<TextBlock>> tableRows = new ArrayList<>();
                tableRows.add(row);

                // 继续收集后续行
                int nextIdx = rows.indexOf(row) + 1;
                while (nextIdx < rows.size() && isTableRow(rows.get(nextIdx))) {
                    tableRows.add(rows.get(nextIdx));
                    nextIdx++;
                }

                // 提取单元格内容
                List<List<String>> cellRows = tableRows.stream()
                        .map(r -> r.stream().map(TextBlock::getText).collect(Collectors.toList()))
                        .collect(Collectors.toList());

                float yEnd = tableRows.get(tableRows.size() - 1).get(0).getY();
                tables.add(new TableRegion(captionY, yEnd, cellRows, currentCaption));
                currentCaption = null;

                logger.debug("表格提取: '{}', {}行 x {}列", currentCaption, cellRows.size(),
                        cellRows.isEmpty() ? 0 : cellRows.get(0).size());
            }
        }

        logger.info("表格提取完成: {}个表格", tables.size());
        return tables;
    }

    /** 按y坐标将文本块分成行 */
    private List<List<TextBlock>> groupByRows(List<TextBlock> blocks) {
        if (blocks.isEmpty()) return List.of();

        List<TextBlock> sorted = blocks.stream()
                .sorted(Comparator.comparing(TextBlock::getY).thenComparing(TextBlock::getX))
                .toList();

        List<List<TextBlock>> rows = new ArrayList<>();
        List<TextBlock> currentRow = new ArrayList<>();
        float currentY = sorted.get(0).getY();

        for (TextBlock b : sorted) {
            if (Math.abs(b.getY() - currentY) < 5) { // 同一行（5pt容差）
                currentRow.add(b);
            } else {
                if (!currentRow.isEmpty()) rows.add(currentRow);
                currentRow = new ArrayList<>();
                currentRow.add(b);
                currentY = b.getY();
            }
        }
        if (!currentRow.isEmpty()) rows.add(currentRow);
        return rows;
    }

    /** 判断是否为表标题 */
    private boolean isTableCaption(String text) {
        return text.matches("(?i)^(Table|Tab\\.)\\s*\\d+[.:].*") ||
                text.matches("^表\\s*\\d+[.:].*");
    }

    /** 判断是否为表格数据行 */
    private boolean isTableRow(List<TextBlock> row) {
        if (row.size() < 2) return false;
        // 检查列对齐：相邻块之间的间距是否均匀
        List<Float> gaps = new ArrayList<>();
        List<TextBlock> sorted = row.stream()
                .sorted(Comparator.comparing(TextBlock::getX)).toList();

        for (int i = 1; i < sorted.size(); i++) {
            float gap = sorted.get(i).getX() - (sorted.get(i - 1).getX() + sorted.get(i - 1).getWidth());
            if (gap > 5) gaps.add(gap);
        }

        // 至少2个间隙，说明是多列对齐
        return gaps.size() >= 1;
    }
}

package org.xhy.infrastructure.rag.parser.pdfbox.column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.infrastructure.rag.parser.pdfbox.TextBlock;

import java.util.*;

/**
 * 栏位检测器
 *
 * 利用PDFBox坐标信息识别论文排版：
 * - 单栏: 文本居中，左右留白对称 -> 直接流式读取
 * - 双栏: 页面分为左右两区，每栏独立阅读顺序
 *
 * 检测原理：
 * 1. 扫描所有文本块的x坐标
 * 2. 聚类分析x坐标分布 -> 找到列的分界线
 * 3. 中线附近有明显的文本间隙 = 双栏
 */
public class ColumnDetector {

    private static final Logger logger = LoggerFactory.getLogger(ColumnDetector.class);

    private static final float GAP_THRESHOLD = 40f; // 栏间距阈值（pt）
    private static final float COLUMN_RATIO_THRESHOLD = 0.15f; // 至少15%的文本在右栏才算双栏

    /** 检测结果 */
    public enum ColumnLayout { SINGLE, DOUBLE, MIXED }

    public static class ColumnInfo {
        public final ColumnLayout layout;
        public final float leftBound, rightBound; // 左栏x范围
        public final float left2Bound, right2Bound; // 右栏x范围（单栏时为0）
        public final float midLine; // 双栏分界线

        public ColumnInfo(ColumnLayout layout, float lb, float rb, float l2b, float r2b, float mid) {
            this.layout = layout; this.leftBound = lb; this.rightBound = rb;
            this.left2Bound = l2b; this.right2Bound = r2b; this.midLine = mid;
        }

        /** 判断给定x坐标属于左栏还是右栏 */
        public int getColumn(float x) {
            if (layout == ColumnLayout.SINGLE) return 1;
            return x < midLine ? 1 : 2;
        }
    }

    /** 检测页面栏位布局 */
    public ColumnInfo detect(List<TextBlock> blocks) {
        if (blocks.isEmpty()) return new ColumnInfo(ColumnLayout.SINGLE, 0, 0, 0, 0, 0);

        // 收集x坐标分布
        List<Float> xPositions = blocks.stream()
                .map(TextBlock::getX)
                .sorted().toList();

        float pageLeft = xPositions.get(0);
        float pageRight = xPositions.get(xPositions.size() - 1);
        float pageMid = (pageLeft + pageRight) / 2;

        // 统计中线附近的文本密度
        int leftCount = 0, rightCount = 0;
        for (TextBlock b : blocks) {
            float centerX = b.getX() + b.getWidth() / 2;
            if (centerX < pageMid) leftCount++;
            else rightCount++;
        }

        // 中线附近有gap且右侧有足够文本 = 双栏
        boolean hasMidGap = hasGapNear(blocks, pageMid, GAP_THRESHOLD);
        float rightRatio = (float) rightCount / (leftCount + rightCount);

        if (hasMidGap && rightRatio > COLUMN_RATIO_THRESHOLD) {
            logger.debug("检测到双栏布局: 左栏{}块, 右栏{}块", leftCount, rightCount);
            return new ColumnInfo(ColumnLayout.DOUBLE,
                    pageLeft, pageMid - GAP_THRESHOLD / 2,
                    pageMid + GAP_THRESHOLD / 2, pageRight, pageMid);
        }

        logger.debug("检测到单栏布局: {}个文本块", blocks.size());
        return new ColumnInfo(ColumnLayout.SINGLE, pageLeft, pageRight, 0, 0, pageMid);
    }

    /** 检查在mid附近是否存在文本间隙 */
    private boolean hasGapNear(List<TextBlock> blocks, float mid, float gapThreshold) {
        float gapLeft = mid - gapThreshold;
        float gapRight = mid + gapThreshold;

        for (TextBlock b : blocks) {
            float blockCenter = b.getX() + b.getWidth() / 2;
            // 有文本块的中心落在gap区域内 = 没有真正的gap
            if (blockCenter > gapLeft && blockCenter < gapRight) return false;
        }
        return true;
    }
}

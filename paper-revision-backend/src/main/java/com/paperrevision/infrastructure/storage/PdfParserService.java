package com.paperrevision.infrastructure.storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.paperrevision.domain.paper.model.StructuredPaper;
import com.paperrevision.infrastructure.rag.parser.GrobidEngine;
import com.paperrevision.infrastructure.rag.parser.GrobidEngine.GrobidNotAvailableException;
import com.paperrevision.infrastructure.rag.parser.GrobidEngine.GrobidParsingException;
import com.paperrevision.infrastructure.rag.parser.GrobidEngine.GrobidResult;
import com.paperrevision.infrastructure.rag.parser.GrobidTEIParser;
import com.paperrevision.infrastructure.rag.parser.pdfbox.PaperStructureAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * PDF 解析服务 — 四层降级链
 *
 * 主引擎:   GROBID (grobid-core 0.9.0, 布局感知, 结构化程度最高)
 *   ↓ 失败/超时
 * 一级降级: PDFBox 坐标分析 (单/双栏 + 标题层级 + 表格 + 公式 + 去噪)
 *   ↓ 失败
 * 二级降级: PDFBox 纯文本 (牺牲结构，保证可读文本)
 *   ↓ 失败
 * 三级降级: PDF 修复 → 重试解析 (PDFBox修复 + Ghostscript)
 *   ↓ 失败
 * 完全失败: 引导用户检查文件
 */
@Service
public class PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);

    /** GROBID解析超时时间 */
    private static final long GROBID_TIMEOUT_SECONDS = 120;

    @Autowired
    private GrobidEngine grobidEngine;

    @Autowired
    private GrobidTEIParser teiParser;

    @Autowired
    private PaperStructureAnalyzer structureAnalyzer;

    @Autowired
    private PdfRepairService repairService;

    /** 解析PDF文件 */
    public PdfParseResult parsePdf(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            throw new RuntimeException("文件不存在或不可读: " + filePath);
        }

        String strategy = "UNKNOWN";
        Exception lastError = null;

        // ═══════════════════════════════════════════
        // 主引擎: GROBID 布局感知解析
        // ═══════════════════════════════════════════
        if (grobidEngine.isAvailable()) {
            try {
                logger.info("[主引擎] GROBID解析开始: {}", filePath);
                GrobidResult result = executeWithTimeout(() -> grobidEngine.processFulltext(file),
                        GROBID_TIMEOUT_SECONDS);

                StructuredPaper paper = result.getPaper();
                int pageCount = getPageCount(file);
                paper.setPageCount(pageCount);
                strategy = "GROBID";

                logger.info("[主引擎] GROBID解析完成: {}ms, 章节数={}, 引用数={}",
                        result.getParseTimeMs(),
                        paper.getSections() != null ? paper.getSections().size() : 0,
                        paper.getReferences() != null ? paper.getReferences().size() : 0);
                return new PdfParseResult(paper.getFullText(), pageCount, paper, strategy);

            } catch (TimeoutException e) {
                logger.warn("[主引擎] GROBID超时({}s)，降级到一级降级", GROBID_TIMEOUT_SECONDS);
                lastError = e;
            } catch (GrobidNotAvailableException | GrobidParsingException e) {
                logger.warn("[主引擎] GROBID失败: {}，降级到一级降级", e.getMessage());
                lastError = e;
            } catch (Exception e) {
                logger.warn("[主引擎] GROBID异常: {}，降级到一级降级", e.getMessage());
                lastError = e;
            }
        } else {
            logger.info("[主引擎] GROBID未就绪({})，跳过", grobidEngine.getStatus());
        }

        // ═══════════════════════════════════════════
        // 一级降级: PDFBox 坐标分析（结构化）
        // ═══════════════════════════════════════════
        try {
            logger.info("[一级降级] PDFBox坐标分析: {}", filePath);
            StructuredPaper paper = structureAnalyzer.analyze(file);
            strategy = "PDFBox-Coordinate";
            logger.info("[一级降级] 完成: {}章节, {}页",
                    paper.getSections() != null ? paper.getSections().size() : 0,
                    paper.getPageCount());
            return new PdfParseResult(paper.getFullText(), paper.getPageCount(), paper, strategy);
        } catch (Exception e) {
            logger.warn("[一级降级] PDFBox坐标分析失败: {}，降级到二级降级", e.getMessage());
            lastError = e;
        }

        // ═══════════════════════════════════════════
        // 二级降级: PDFBox 纯文本提取
        // ═══════════════════════════════════════════
        try {
            logger.info("[二级降级] PDFBox纯文本提取: {}", filePath);
            PdfParseResult result = extractPlainText(file);
            if (result != null && result.getText() != null && result.getText().length() > 50) {
                strategy = "PDFBox-Plain";
                result.setStrategy(strategy);
                logger.info("[二级降级] 完成: {}页, {}字符", result.getPageCount(), result.getText().length());
                return result;
            }
            logger.warn("[二级降级] 提取文本过短，降级到三级降级");
        } catch (Exception e) {
            logger.warn("[二级降级] PDFBox纯文本失败: {}，降级到三级降级", e.getMessage());
            lastError = e;
        }

        // ═══════════════════════════════════════════
        // 三级降级: PDF 修复 → 重试
        // ═══════════════════════════════════════════
        try {
            logger.info("[三级降级] 尝试修复PDF: {}", filePath);
            File repairedFile = repairService.repair(file);
            if (repairedFile != null) {
                logger.info("[三级降级] PDF修复成功，重新尝试解析...");
                // 修复后用纯文本重试
                PdfParseResult result = extractPlainText(repairedFile);
                if (result != null && result.getText() != null && result.getText().length() > 50) {
                    strategy = "Repaired";
                    result.setStrategy(strategy);
                    logger.info("[三级降级] 修复后解析成功!");
                    return result;
                }
            }
        } catch (Exception e) {
            logger.warn("[三级降级] PDF修复也失败了: {}", e.getMessage());
            lastError = e;
        }

        // ═══════════════════════════════════════════
        // 完全失败
        // ═══════════════════════════════════════════
        String errorMsg = lastError != null ? lastError.getMessage() : "未知原因";
        logger.error("[完全失败] PDF解析所有策略均失败: {} → {}", filePath, errorMsg);
        throw new PdfParseFailureException(
                "PDF解析完全失败。请检查文件是否损坏、加密或不是有效的PDF。\n" +
                        "建议：1) 尝试用其他PDF阅读器打开确认文件正常\n" +
                        "      2) 使用 PDF 修复工具（Adobe Acrobat / Ghostscript）修复后重试\n" +
                        "      3) 确认文件未设置密码保护\n" +
                        "错误详情: " + errorMsg);
    }

    /** 纯文本提取（二级降级） */
    private PdfParseResult extractPlainText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);
            String text = stripper.getText(document);
            int pageCount = document.getNumberOfPages();
            return new PdfParseResult(text, pageCount, null, "PDFBox-Plain");
        }
    }

    /** 带超时的执行 */
    private <T> T executeWithTimeout(Callable<T> task, long timeoutSeconds) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = null;
        try {
            future = executor.submit(task);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            if (future != null) future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    private int getPageCount(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages();
        } catch (IOException e) { return 0; }
    }

    // -- 结果类 --

    public static class PdfParseResult {
        private final String text;
        private final int pageCount;
        private final StructuredPaper structuredPaper;
        private String strategy; // 使用的解析策略

        public PdfParseResult(String text, int pageCount, StructuredPaper paper, String strategy) {
            this.text = text; this.pageCount = pageCount;
            this.structuredPaper = paper; this.strategy = strategy;
        }
        public String getText() { return text; }
        public int getPageCount() { return pageCount; }
        public StructuredPaper getStructuredPaper() { return structuredPaper; }
        public boolean isStructured() { return structuredPaper != null; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String s) { this.strategy = s; }
    }

    public static class PdfParseFailureException extends RuntimeException {
        public PdfParseFailureException(String msg) { super(msg); }
    }
}

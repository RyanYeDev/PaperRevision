package org.xhy.infrastructure.storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xhy.domain.paper.model.StructuredPaper;
import org.xhy.infrastructure.rag.parser.GrobidClient;
import org.xhy.infrastructure.rag.parser.GrobidTEIParser;
import org.xhy.infrastructure.rag.parser.pdfbox.PaperStructureAnalyzer;

import java.io.File;
import java.io.IOException;

/**
 * PDF解析服务 - 三级策略
 *
 * 策略1: GROBID (需要本地 java -jar grobid.jar) → 最精确
 * 策略2: PDFBox坐标分析 (内置，零依赖) → 单/双栏 + 标题 + 表格 + 公式 + 噪声过滤
 * 策略3: PDFBox纯文本 (最终降级) → 只管把字提取出来
 */
@Service
public class PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);

    @Autowired
    private GrobidClient grobidClient;

    @Autowired
    private GrobidTEIParser teiParser;

    @Autowired
    private PaperStructureAnalyzer structureAnalyzer;

    /** 解析PDF文件 */
    public PdfParseResult parsePdf(String filePath) {
        File file = new File(filePath);

        // 策略1: GROBID → TEI XML → StructuredPaper
        if (grobidClient.isAvailable()) {
            try {
                logger.info("[GROBID] 解析论文: {}", filePath);
                String teiXml = grobidClient.processFulltext(file);
                StructuredPaper paper = teiParser.parseFulltext(teiXml);
                paper.setPageCount(getPageCount(file));
                return new PdfParseResult(paper.getFullText(), paper.getPageCount(), paper);
            } catch (Exception e) {
                logger.warn("[GROBID] 失败, 降级到PDFBox坐标分析: {}", e.getMessage());
            }
        }

        // 策略2: PDFBox坐标级分析 → 单/双栏 + 标题层级 + 表格 + 公式 + 去噪
        try {
            logger.info("[PDFBox坐标] 分析论文: {}", filePath);
            StructuredPaper paper = structureAnalyzer.analyze(file);
            return new PdfParseResult(paper.getFullText(), paper.getPageCount(), paper);
        } catch (Exception e) {
            logger.warn("[PDFBox坐标] 失败, 降级到纯文本: {}", e.getMessage());
        }

        // 策略3: PDFBox纯文本（最终降级）
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            int pageCount = document.getNumberOfPages();
            logger.info("[PDFBox纯文本] {}页, {}字符", pageCount, text.length());
            return new PdfParseResult(text, pageCount, null);
        } catch (IOException e) {
            logger.error("PDF解析完全失败: {}", filePath, e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }

    private int getPageCount(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages();
        } catch (IOException e) { return 0; }
    }

    public static class PdfParseResult {
        private final String text;
        private final int pageCount;
        private final StructuredPaper structuredPaper;

        public PdfParseResult(String text, int pageCount, StructuredPaper paper) {
            this.text = text; this.pageCount = pageCount; this.structuredPaper = paper;
        }
        public String getText() { return text; }
        public int getPageCount() { return pageCount; }
        public StructuredPaper getStructuredPaper() { return structuredPaper; }
        public boolean isStructured() { return structuredPaper != null; }
    }
}

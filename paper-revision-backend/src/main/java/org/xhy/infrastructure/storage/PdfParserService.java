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

import java.io.File;
import java.io.IOException;

/**
 * PDF解析服务 - 策略模式
 *
 * 优先使用 GROBID (学术论文专用，提取章节/引用/图表结构)
 * GROBID不可用时降级到 PDFBox (通用文本提取)
 */
@Service
public class PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);

    @Autowired(required = false)
    private GrobidClient grobidClient;

    @Autowired(required = false)
    private GrobidTEIParser teiParser;

    /** 解析PDF文件 */
    public PdfParseResult parsePdf(String filePath) {
        File file = new File(filePath);

        // 策略1: GROBID (学术论文最佳)
        if (grobidClient != null && grobidClient.isAvailable()) {
            try {
                logger.info("使用GROBID解析论文: {}", filePath);
                String teiXml = grobidClient.processFulltext(file);
                StructuredPaper paper = teiParser.parseFulltext(teiXml);
                paper.setPageCount(getPageCount(file));
                return new PdfParseResult(paper.getFullText(), paper.getPageCount(), paper);
            } catch (Exception e) {
                logger.warn("GROBID解析失败，降级到PDFBox: {}", e.getMessage());
            }
        }

        // 策略2: PDFBox (通用降级)
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            int pageCount = document.getNumberOfPages();
            logger.info("PDFBox解析完成: {}页, 文本长度: {}", pageCount, text.length());
            return new PdfParseResult(text, pageCount, null);
        } catch (IOException e) {
            logger.error("PDF解析失败: {}", filePath, e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }

    private int getPageCount(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            return 0;
        }
    }

    /** PDF解析结果 */
    public static class PdfParseResult {
        private final String text;
        private final int pageCount;
        private final StructuredPaper structuredPaper;

        public PdfParseResult(String text, int pageCount, StructuredPaper structuredPaper) {
            this.text = text;
            this.pageCount = pageCount;
            this.structuredPaper = structuredPaper;
        }

        public String getText() { return text; }
        public int getPageCount() { return pageCount; }
        public StructuredPaper getStructuredPaper() { return structuredPaper; }

        /** 是否是结构化解析 */
        public boolean isStructured() { return structuredPaper != null; }
    }
}

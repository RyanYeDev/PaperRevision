package org.xhy.infrastructure.storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/** PDF解析服务 */
@Service
public class PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);

    /** 解析PDF文件，提取文本内容和页数 */
    public PdfParseResult parsePdf(String filePath) {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            int pageCount = document.getNumberOfPages();

            logger.info("PDF解析完成: {}页, 文本长度: {}", pageCount, text.length());
            return new PdfParseResult(text, pageCount);
        } catch (IOException e) {
            logger.error("PDF解析失败: {}", filePath, e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage());
        }
    }

    /** PDF解析结果 */
    public static class PdfParseResult {
        private final String text;
        private final int pageCount;

        public PdfParseResult(String text, int pageCount) {
            this.text = text;
            this.pageCount = pageCount;
        }

        public String getText() { return text; }
        public int getPageCount() { return pageCount; }
    }
}

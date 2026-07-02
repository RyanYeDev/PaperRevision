package com.paperrevision.domain.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 文档分块服务 - 支持多种分块策略 */
@Service
public class DocumentChunkingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentChunkingService.class);

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP = 100;

    /** 固定大小分块 */
    public List<String> chunkByFixedSize(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // 尝试在换行处断开
            if (end < text.length()) {
                int newlineIndex = text.lastIndexOf('\n', end);
                if (newlineIndex > start + chunkSize / 2) {
                    end = newlineIndex;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - overlap;
            if (start >= text.length()) break;
        }
        logger.info("文档分块完成: {}个块, chunk_size={}, overlap={}", chunks.size(), chunkSize, overlap);
        return chunks;
    }

    /** 按段落分块 */
    public List<String> chunkByParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            if (currentChunk.length() > 0) currentChunk.append("\n\n");
            currentChunk.append(paragraph);
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        logger.info("段落分块完成: {}个块", chunks.size());
        return chunks;
    }

    /** 自适应分块 (默认策略) */
    public List<String> chunk(String text) {
        if (text == null || text.isEmpty()) return new ArrayList<>();
        // 长文档使用段落分块，短文档使用固定大小
        if (text.length() > 50000) {
            return chunkByParagraph(text);
        }
        return chunkByFixedSize(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }
}

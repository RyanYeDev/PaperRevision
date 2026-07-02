package org.xhy.application.paper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xhy.domain.paper.model.PaperEntity;
import org.xhy.domain.paper.model.PaperStatus;
import org.xhy.domain.paper.service.PaperDomainService;
import org.xhy.domain.rag.service.RagDomainService;
import org.xhy.infrastructure.storage.PdfParserService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 论文异步处理服务
 * 负责PDF解析和RAG索引的异步执行，支持断点续传和失败重试
 */
@Service
public class PaperProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PaperProcessingService.class);

    /** 每篇论文的处理进度 (0-100) */
    private final Map<String, Integer> progressMap = new ConcurrentHashMap<>();

    /** 每篇论文的处理状态消息 */
    private final Map<String, String> messageMap = new ConcurrentHashMap<>();

    private final PaperDomainService paperDomainService;
    private final PdfParserService pdfParserService;
    private final RagDomainService ragDomainService;

    public PaperProcessingService(PaperDomainService paperDomainService,
            PdfParserService pdfParserService, RagDomainService ragDomainService) {
        this.paperDomainService = paperDomainService;
        this.pdfParserService = pdfParserService;
        this.ragDomainService = ragDomainService;
    }

    /** 异步处理论文：PDF解析 -> 分块 -> 向量化 -> 索引 */
    @Async
    public void processPaperAsync(String paperId, String filePath) {
        logger.info("开始异步处理论文: {}", paperId);
        try {
            // 阶段1: 状态改为解析中 (0-30%)
            updateProgress(paperId, 5, "开始解析PDF...");
            paperDomainService.updateStatus(paperId, PaperStatus.PARSING);

            // 检查是否已经解析过（断点续传）
            PaperEntity paper = paperDomainService.getPaperById(paperId);
            String parsedText = paper.getParsedText();
            Integer pageCount = paper.getPageCount();

            if (parsedText == null || parsedText.isEmpty()) {
                // 大文件检查: 超过100MB拒绝处理
                if (paper.getFileSize() != null && paper.getFileSize() > 100 * 1024 * 1024) {
                    updateProgress(paperId, 0, "文件过大(>100MB)，请压缩后重试");
                    paperDomainService.updateStatus(paperId, PaperStatus.FAILED);
                    return;
                }

                updateProgress(paperId, 10, "正在提取PDF文本...");

                // 解析PDF
                PdfParserService.PdfParseResult result = pdfParserService.parsePdf(filePath);
                parsedText = result.getText();
                pageCount = result.getPageCount();

                // 保存解析结果（checkpoint 1）
                paperDomainService.updateParsingResult(paperId, parsedText, pageCount);
                updateProgress(paperId, 30, "PDF解析完成，" + pageCount + "页");
            } else {
                // 断点续传：跳过已完成的解析
                updateProgress(paperId, 30, "检测到已有解析结果，跳过PDF解析");
                logger.info("论文{}已有解析结果，从断点续传", paperId);
            }

            // 阶段2: RAG索引 (30-90%)
            updateProgress(paperId, 35, "正在对文档分块...");

            // 检查是否已经索引过
            var existingChunks = ragDomainService.getPaperChunks(paperId);
            if (existingChunks.isEmpty()) {
                // 分块并建立索引
                int totalChunks = estimateChunkCount(parsedText);
                updateProgress(paperId, 40, "文档已分块，共" + totalChunks + "个块，开始建立索引...");

                int chunkCount = ragDomainService.indexDocument(paperId, parsedText).size();
                updateProgress(paperId, 85, "索引完成，共" + chunkCount + "个块");
            } else {
                // 断点续传：跳过已完成的索引
                updateProgress(paperId, 85, "检测到已有索引数据，跳过索引步骤");
                logger.info("论文{}已有索引数据({}个块)，从断点续传", paperId, existingChunks.size());
            }

            // 阶段3: 完成 (90-100%)
            updateProgress(paperId, 95, "处理完成，更新状态...");
            paperDomainService.updateStatus(paperId, PaperStatus.PARSED);
            updateProgress(paperId, 100, "处理完成！论文已就绪，可以开始返修");

            logger.info("论文异步处理完成: {}, {}页, 文本长度:{}", paperId, pageCount, parsedText.length());

        } catch (Exception e) {
            logger.error("论文处理失败: {}", paperId, e);
            updateProgress(paperId, 0, "处理失败: " + e.getMessage());
            paperDomainService.updateStatus(paperId, PaperStatus.FAILED);
        }
    }

    /** 重试处理（用户手动触发） */
    public void retryProcessing(String paperId) {
        PaperEntity paper = paperDomainService.getPaperById(paperId);
        if (paper.getStatus().equals(PaperStatus.FAILED.name())
                || paper.getStatus().equals(PaperStatus.UPLOADED.name())) {
            logger.info("用户请求重新处理论文: {}", paperId);
            // 清除旧的解析结果，重新开始
            paper.setParsedText(null);
            paper.setPageCount(null);
            paperDomainService.updateStatus(paperId, PaperStatus.UPLOADED);
            processPaperAsync(paperId, paper.getFilePath());
        } else {
            throw new RuntimeException("论文当前状态不允许重试: " + paper.getStatus());
        }
    }

    /** 获取处理进度 */
    public Map<String, Object> getProgress(String paperId) {
        return Map.of(
                "paperId", paperId,
                "progress", progressMap.getOrDefault(paperId, 0),
                "message", messageMap.getOrDefault(paperId, "待处理"),
                "status", paperDomainService.getPaperById(paperId).getStatus()
        );
    }

    private void updateProgress(String paperId, int progress, String message) {
        progressMap.put(paperId, progress);
        messageMap.put(paperId, message);
        logger.debug("进度: {} - {}% - {}", paperId, progress, message);
    }

    /** 估算分块数量 */
    private int estimateChunkCount(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 1000.0);
    }

    /** 清理进度缓存 */
    public void clearProgress(String paperId) {
        progressMap.remove(paperId);
        messageMap.remove(paperId);
    }
}

package com.paperrevision.application.paper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.paperrevision.application.paper.assembler.PaperAssembler;
import com.paperrevision.application.paper.dto.PaperDTO;
import com.paperrevision.domain.paper.model.PaperEntity;
import com.paperrevision.domain.paper.model.PaperStatus;
import com.paperrevision.domain.paper.service.PaperDomainService;
import com.paperrevision.infrastructure.storage.FileStorageService;
import com.paperrevision.infrastructure.storage.PdfParserService;

import java.util.List;
import java.util.Map;

/** 论文应用服务 */
@Service
public class PaperAppService {

    private static final Logger logger = LoggerFactory.getLogger(PaperAppService.class);

    private final PaperDomainService paperDomainService;
    private final FileStorageService fileStorageService;
    private final PaperProcessingService processingService;

    public PaperAppService(PaperDomainService paperDomainService,
            FileStorageService fileStorageService, PaperProcessingService processingService) {
        this.paperDomainService = paperDomainService;
        this.fileStorageService = fileStorageService;
        this.processingService = processingService;
    }

    /** 上传论文PDF（存储后异步处理） */
    public PaperDTO uploadPaper(MultipartFile file, String userId) {
        // 文件大小限制
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new RuntimeException("文件过大，请上传小于100MB的PDF");
        }

        // 存储文件
        String filePath = fileStorageService.storeFile(file, userId);

        // 创建论文实体
        PaperEntity paper = new PaperEntity();
        paper.setTitle(file.getOriginalFilename());
        paper.setFileName(file.getOriginalFilename());
        paper.setFilePath(filePath);
        paper.setFileSize(file.getSize());
        paper.setFileType("application/pdf");
        paper.setUserId(userId);

        PaperEntity created = paperDomainService.createPaper(paper);

        // 启动异步处理（PDF解析 + RAG索引）
        // 如果中途崩溃，重启后可通过 retry 端点续传
        processingService.processPaperAsync(created.getId(), filePath);

        logger.info("论文上传成功，后台处理中: {}", created.getId());
        return PaperAssembler.toDTO(created);
    }

    /** 重试处理失败的论文 */
    public void retryProcessing(String paperId) {
        processingService.retryProcessing(paperId);
    }

    /** 获取论文处理进度 */
    public Map<String, Object> getProgress(String paperId) {
        return processingService.getProgress(paperId);
    }

    /** 获取用户论文列表 */
    public Page<PaperDTO> getUserPapers(String userId, int page, int pageSize) {
        Page<PaperEntity> entityPage = paperDomainService.getUserPapers(userId, page, pageSize);
        List<PaperDTO> dtos = PaperAssembler.toDTOs(entityPage.getRecords());

        Page<PaperDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(dtos);
        return dtoPage;
    }

    /** 获取论文详情 */
    public PaperDTO getPaperDetail(String paperId) {
        PaperEntity paper = paperDomainService.getPaperById(paperId);
        return PaperAssembler.toDTO(paper);
    }

    /** 删除论文 */
    public void deletePaper(String paperId, String userId) {
        PaperEntity paper = paperDomainService.getPaperById(paperId);
        fileStorageService.deleteFile(paper.getFilePath());
        paperDomainService.deletePaper(paperId, userId);
    }
}

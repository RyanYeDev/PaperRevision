package org.xhy.application.paper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xhy.application.paper.assembler.PaperAssembler;
import org.xhy.application.paper.dto.PaperDTO;
import org.xhy.domain.paper.model.PaperEntity;
import org.xhy.domain.paper.model.PaperStatus;
import org.xhy.domain.paper.service.PaperDomainService;
import org.xhy.infrastructure.storage.FileStorageService;
import org.xhy.infrastructure.storage.PdfParserService;

import java.util.List;

/** 论文应用服务 */
@Service
public class PaperAppService {

    private static final Logger logger = LoggerFactory.getLogger(PaperAppService.class);

    private final PaperDomainService paperDomainService;
    private final FileStorageService fileStorageService;
    private final PdfParserService pdfParserService;

    public PaperAppService(PaperDomainService paperDomainService,
            FileStorageService fileStorageService, PdfParserService pdfParserService) {
        this.paperDomainService = paperDomainService;
        this.fileStorageService = fileStorageService;
        this.pdfParserService = pdfParserService;
    }

    /** 上传论文PDF */
    public PaperDTO uploadPaper(MultipartFile file, String userId) {
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

        // 异步解析PDF
        try {
            PdfParserService.PdfParseResult result = pdfParserService.parsePdf(filePath);
            paperDomainService.updateParsingResult(created.getId(), result.getText(), result.getPageCount());
            created.setParsedText(result.getText());
            created.setPageCount(result.getPageCount());
            created.setStatus(PaperStatus.PARSED.name());
        } catch (Exception e) {
            logger.error("PDF解析失败: {}", created.getId(), e);
            paperDomainService.updateStatus(created.getId(), PaperStatus.FAILED);
        }

        return PaperAssembler.toDTO(created);
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

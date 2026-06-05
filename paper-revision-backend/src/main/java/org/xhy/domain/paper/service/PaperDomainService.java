package org.xhy.domain.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.paper.model.PaperEntity;
import org.xhy.domain.paper.model.PaperStatus;
import org.xhy.domain.paper.repository.PaperRepository;
import org.xhy.infrastructure.exception.EntityNotFoundException;

import java.util.List;

/** 论文领域服务 */
@Service
public class PaperDomainService {

    private static final Logger logger = LoggerFactory.getLogger(PaperDomainService.class);

    private final PaperRepository paperRepository;

    public PaperDomainService(PaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    /** 创建论文记录 */
    public PaperEntity createPaper(PaperEntity paper) {
        paper.setStatus(PaperStatus.UPLOADED.name());
        paperRepository.checkInsert(paper);
        logger.info("论文记录创建成功: {}", paper.getTitle());
        return paper;
    }

    /** 获取用户的论文列表 */
    public Page<PaperEntity> getUserPapers(String userId, int page, int pageSize) {
        LambdaQueryWrapper<PaperEntity> wrapper = Wrappers.<PaperEntity>lambdaQuery()
                .eq(PaperEntity::getUserId, userId)
                .orderByDesc(PaperEntity::getCreatedAt);
        return paperRepository.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /** 根据ID获取论文 */
    public PaperEntity getPaperById(String paperId) {
        PaperEntity paper = paperRepository.selectById(paperId);
        if (paper == null) {
            throw new EntityNotFoundException("论文", paperId);
        }
        return paper;
    }

    /** 更新论文解析状态 */
    public void updateParsingResult(String paperId, String parsedText, Integer pageCount) {
        PaperEntity paper = getPaperById(paperId);
        paper.setParsedText(parsedText);
        paper.setPageCount(pageCount);
        paper.setStatus(PaperStatus.PARSED.name());
        paperRepository.checkedUpdateById(paper);
    }

    /** 更新论文状态 */
    public void updateStatus(String paperId, PaperStatus status) {
        PaperEntity paper = getPaperById(paperId);
        paper.setStatus(status.name());
        paperRepository.checkedUpdateById(paper);
    }

    /** 删除论文 */
    public void deletePaper(String paperId, String userId) {
        LambdaQueryWrapper<PaperEntity> wrapper = Wrappers.<PaperEntity>lambdaQuery()
                .eq(PaperEntity::getId, paperId)
                .eq(PaperEntity::getUserId, userId);
        paperRepository.checkedDelete(wrapper);
    }
}

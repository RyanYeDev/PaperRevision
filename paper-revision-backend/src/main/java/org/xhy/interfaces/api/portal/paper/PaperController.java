package org.xhy.interfaces.api.portal.paper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xhy.application.paper.dto.PaperDTO;
import org.xhy.application.paper.service.PaperAppService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

/** 论文管理控制器 */
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperAppService paperAppService;

    public PaperController(PaperAppService paperAppService) {
        this.paperAppService = paperAppService;
    }

    /** 上传论文PDF */
    @PostMapping("/upload")
    public Result<PaperDTO> uploadPaper(@RequestParam("file") MultipartFile file) {
        String userId = UserContext.getCurrentUserId();
        PaperDTO paper = paperAppService.uploadPaper(file, userId);
        return Result.success("论文上传成功", paper);
    }

    /** 获取论文列表 */
    @GetMapping
    public Result<Page<PaperDTO>> getPapers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        String userId = UserContext.getCurrentUserId();
        return Result.success(paperAppService.getUserPapers(userId, page, pageSize));
    }

    /** 获取论文详情 */
    @GetMapping("/{paperId}")
    public Result<PaperDTO> getPaperDetail(@PathVariable String paperId) {
        return Result.success(paperAppService.getPaperDetail(paperId));
    }

    /** 删除论文 */
    @DeleteMapping("/{paperId}")
    public Result<Void> deletePaper(@PathVariable String paperId) {
        String userId = UserContext.getCurrentUserId();
        paperAppService.deletePaper(paperId, userId);
        return Result.success("删除成功");
    }
}

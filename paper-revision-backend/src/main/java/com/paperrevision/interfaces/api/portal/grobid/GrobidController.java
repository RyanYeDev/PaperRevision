package com.paperrevision.interfaces.api.portal.grobid;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.paperrevision.infrastructure.rag.parser.GrobidEngine;
import com.paperrevision.interfaces.api.common.Result;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * GROBID模型管理控制器
 * 用户可通过前端上传模型权重文件，无需命令行操作
 */
@RestController
@RequestMapping("/api/grobid")
public class GrobidController {

    private final GrobidEngine grobidEngine;

    public GrobidController(GrobidEngine grobidEngine) {
        this.grobidEngine = grobidEngine;
    }

    /** 获取GROBID状态 */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(Map.of(
                "installed", grobidEngine.isAvailable(),
                "message", grobidEngine.getStatus(),
                "modelPath", "./data/grobid-home/models/",
                "downloadUrl", "https://grobid.s3.amazonaws.com/grobid-0.9.0-models.zip",
                "downloadSize", "约 1.2 GB"
        ));
    }

    /** 上传模型文件并自动解压安装 */
    @PostMapping("/models/upload")
    public Result<Map<String, Object>> uploadModel(@RequestParam("file") MultipartFile file) {
        if (!file.getOriginalFilename().endsWith(".zip")) {
            return Result.badRequest("请上传 .zip 格式的模型压缩包");
        }

        try {
            Path grobidHome = Paths.get("./data/grobid-home");
            Path modelsDir = grobidHome.resolve("models");
            Files.createDirectories(modelsDir);

            // 解压上传的zip
            int count = 0;
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                var entry = zis.getNextEntry();
                while (entry != null) {
                    Path target = modelsDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                        count++;
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            }

            // 清除缓存，下次请求时重新初始化引擎
            grobidEngine.reinit();

            return Result.success("模型安装成功", Map.of(
                    "filesInstalled", count,
                    "installed", grobidEngine.isAvailable(),
                    "message", grobidEngine.getStatus()
            ));
        } catch (IOException e) {
            return Result.serverError("模型安装失败: " + e.getMessage());
        }
    }

    /** 卸载模型 */
    @DeleteMapping("/models")
    public Result<Void> deleteModels() {
        try {
            Path modelsDir = Paths.get("./data/grobid-home/models");
            if (Files.exists(modelsDir)) {
                deleteRecursively(modelsDir);
            }
            grobidEngine.reinit();
            return Result.success("模型已卸载");
        } catch (IOException e) {
            return Result.serverError("卸载失败: " + e.getMessage());
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        try (var files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }
}

package com.paperrevision.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** 文件存储服务（本地存储实现，可扩展S3） */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file-storage.local-path:./data/files}")
    private String basePath;

    /** 存储上传文件 */
    public String storeFile(MultipartFile file, String userId) {
        try {
            String dir = basePath + "/" + userId;
            Path dirPath = Paths.get(dir);
            Files.createDirectories(dirPath);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + extension;
            Path filePath = dirPath.resolve(storedName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件存储成功: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            logger.error("文件存储失败", e);
            throw new RuntimeException("文件存储失败: " + e.getMessage());
        }
    }

    /** 读取文件 */
    public byte[] readFile(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            logger.error("文件读取失败: {}", filePath, e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    /** 删除文件 */
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            logger.info("文件删除成功: {}", filePath);
        } catch (IOException e) {
            logger.warn("文件删除失败: {}", filePath, e);
        }
    }
}

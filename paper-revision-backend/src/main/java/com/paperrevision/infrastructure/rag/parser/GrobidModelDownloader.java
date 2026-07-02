package com.paperrevision.infrastructure.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipInputStream;

/**
 * GROBID 模型下载器
 *
 * GROBID 使用 20+ 个 CRF (条件随机场) 模型来解析论文的不同部分，
 * 模型文件约 1.2GB，只需下载一次。
 *
 * 模型下载源（按优先级）：
 * 1. GitHub Releases: https://github.com/kermitt2/grobid/releases
 * 2. AWS S3: https://grobid.s3.amazonaws.com/
 * 3. 国内镜像: 暂无，建议挂代理下载
 */
public class GrobidModelDownloader {

    private static final Logger logger = LoggerFactory.getLogger(GrobidModelDownloader.class);

    // GROBID 0.9.0 模型包
    private static final String MODEL_URL =
            "https://github.com/kermitt2/grobid/raw/master/grobid-home/models.zip";

    // 备选: AWS S3
    private static final String MODEL_URL_FALLBACK =
            "https://grobid.s3.amazonaws.com/grobid-0.9.0-models.zip";

    /** 下载模型到指定目录 */
    public static void download(Path grobidHome) throws IOException {
        Path modelsDir = grobidHome.resolve("models");
        if (Files.exists(modelsDir) && hasModelFiles(modelsDir)) {
            logger.info("GROBID模型已存在: {}", modelsDir);
            return;
        }

        Files.createDirectories(modelsDir);
        Path zipPath = grobidHome.resolve("models.zip");

        logger.info("开始下载GROBID模型 (约1.2GB，仅需一次)...");
        logger.info("下载地址: {}", MODEL_URL);
        logger.info("如果下载慢，可以手动下载并解压到: {}", modelsDir.toAbsolutePath());

        // 尝试下载
        boolean success = false;
        for (String url : new String[]{MODEL_URL, MODEL_URL_FALLBACK}) {
            try {
                logger.info("尝试: {}", url);
                try (InputStream in = new URL(url).openStream()) {
                    Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
                }
                success = true;
                break;
            } catch (IOException e) {
                logger.warn("下载失败: {} ({})", url, e.getMessage());
            }
        }

        if (!success) {
            throw new IOException(
                    "GROBID模型下载失败。请手动下载模型包并解压到: " + modelsDir.toAbsolutePath() + "\n" +
                    "下载方式:\n" +
                    "  1. 浏览器打开: https://github.com/kermitt2/grobid/releases\n" +
                    "  2. 下载对应版本模型包: grobid-0.9.0-models.zip\n" +
                    "  3. 解压到: " + modelsDir.toAbsolutePath());
        }

        // 解压
        logger.info("解压模型文件...");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            var entry = zis.getNextEntry();
            while (entry != null) {
                Path entryPath = modelsDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
        Files.deleteIfExists(zipPath);
        logger.info("GROBID模型下载+解压完成: {} ({}MB)",
                modelsDir, Files.size(modelsDir) / 1024 / 1024);
    }

    private static boolean hasModelFiles(Path modelsDir) {
        try {
            // 检查是否有关键模型文件
            return Files.list(modelsDir).anyMatch(p ->
                    p.getFileName().toString().startsWith("header") ||
                    p.getFileName().toString().startsWith("fulltext") ||
                    p.getFileName().toString().startsWith("segment"));
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        // 独立运行下载
        Path home = Paths.get(args.length > 0 ? args[0] : "./data/grobid-home");
        download(home);
    }
}

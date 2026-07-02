package org.xhy.infrastructure.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/**
 * GROBID REST API 客户端
 *
 * 本地使用方式（无需Docker）：
 * 1. 下载 grobid-0.8.1-onejar.jar: https://github.com/kermitt2/grobid/releases
 * 2. java -jar grobid-0.8.1-onejar.jar server grobid.yaml
 * 3. GROBID 启动在 localhost:8070，App自动检测并使用
 *
 * 不启动GROBID也能正常使用，自动降级到PDFBox
 */
@Service
public class GrobidClient {

    private static final Logger logger = LoggerFactory.getLogger(GrobidClient.class);

    private final HttpClient httpClient;
    private final String grobidUrl;
    private Boolean available;

    public GrobidClient(@Value("${grobid.url:http://localhost:8070}") String grobidUrl) {
        this.grobidUrl = grobidUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 检查GROBID是否可用（带缓存，避免每次请求都检测） */
    public boolean isAvailable() {
        if (available != null) return available;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(grobidUrl + "/api/isalive"))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            available = response.statusCode() == 200;
            if (available) {
                logger.info("GROBID服务已检测到: {} (启动方式: java -jar grobid-0.8.1-onejar.jar)", grobidUrl);
            }
        } catch (Exception e) {
            available = false;
            logger.info("GROBID未启动，使用PDFBox解析。启动GROBID获得更好效果: java -jar grobid-onejar.jar");
        }
        return available;
    }

    /** 解析论文全文，返回TEI XML */
    public String processFulltext(File pdfFile) throws IOException, InterruptedException {
        return callGrobid("/api/processFulltextDocument", pdfFile);
    }

    /** 解析论文头部（标题/作者/摘要） */
    public String processHeader(File pdfFile) throws IOException, InterruptedException {
        return callGrobid("/api/processHeaderDocument", pdfFile);
    }

    /** 解析参考文献 */
    public String processReferences(File pdfFile) throws IOException, InterruptedException {
        return callGrobid("/api/processReferences", pdfFile);
    }

    private String callGrobid(String endpoint, File pdfFile) throws IOException, InterruptedException {
        byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(grobidUrl + endpoint))
                .timeout(Duration.ofMinutes(5))
                .header("Accept", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        logger.info("GROBID解析: {}, 文件: {}KB", endpoint, fileBytes.length / 1024);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.info("GROBID解析成功, 响应长度: {}", response.body().length());
            return response.body();
        }
        throw new IOException("GROBID返回错误: HTTP " + response.statusCode());
    }
}

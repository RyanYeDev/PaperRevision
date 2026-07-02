package org.xhy.infrastructure.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * GROBID是一个专门针对学术论文的PDF解析工具，可以提取:
 * - 标题/作者/摘要
 * - 章节结构 (Introduction, Methods, Results, Discussion...)
 * - 参考文献列表及各自的元数据
 * - 图表及其标题
 * - 文中引用位置
 *
 * 官网: https://github.com/kermitt2/grobid
 * Docker: docker run -p 8070:8070 lfoppiano/grobid:0.8.1
 */
@Service
@ConditionalOnProperty(name = "grobid.enabled", havingValue = "true", matchIfMissing = false)
public class GrobidClient {

    private static final Logger logger = LoggerFactory.getLogger(GrobidClient.class);

    private final HttpClient httpClient;
    private final String grobidUrl;

    public GrobidClient(@Value("${grobid.url:http://localhost:8070}") String grobidUrl) {
        this.grobidUrl = grobidUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        logger.info("GROBID客户端初始化: {}", grobidUrl);
    }

    /** 解析论文全文，返回TEI XML */
    public String processFulltext(File pdfFile) throws IOException, InterruptedException {
        return callGrobid("/api/processFulltextDocument", pdfFile);
    }

    /** 解析论文头部（标题/作者/摘要），返回TEI XML */
    public String processHeader(File pdfFile) throws IOException, InterruptedException {
        return callGrobid("/api/processHeaderDocument", pdfFile);
    }

    /** 解析参考文献，返回TEI XML */
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

        logger.info("调用GROBID: {} (文件: {}KB)", endpoint, fileBytes.length / 1024);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.info("GROBID解析成功: {}, 响应长度: {}", endpoint, response.body().length());
            return response.body();
        } else {
            throw new IOException("GROBID返回错误: HTTP " + response.statusCode());
        }
    }

    /** 检查GROBID服务是否可用 */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(grobidUrl + "/api/isalive"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}

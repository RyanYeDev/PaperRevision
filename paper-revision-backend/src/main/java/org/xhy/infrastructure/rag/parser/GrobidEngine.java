package org.xhy.infrastructure.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhy.domain.paper.model.StructuredPaper;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * GROBID 本地引擎
 *
 * 直接调用 grobid-core-0.9.0 的 Java API，不依赖 REST 服务。
 * GROBID 使用 CRF (条件随机场) 模型进行布局感知的论文解析，
 * 能精确识别：标题/作者/摘要/章节/段落/引用/图表/公式/页眉页脚
 *
 * 引擎特点：
 * - 布局感知：理解PDF的视觉布局（字体、位置、间距）
 * - 深度学习模型：20+个CRF模型分别处理不同元素
 * - 结构化输出：TEI XML 格式，包含完整的语义标注
 */
@Service
public class GrobidEngine {

    private static final Logger logger = LoggerFactory.getLogger(GrobidEngine.class);

    private final String grobidHome;
    private final GrobidTEIParser teiParser;
    private volatile boolean initialized = false;
    private volatile String initError = null;
    private long initTimeMs;

    public GrobidEngine(@Value("${grobid.home:./data/grobid-home}") String grobidHome,
            GrobidTEIParser teiParser) {
        this.grobidHome = grobidHome;
        this.teiParser = teiParser;
    }

    @PostConstruct
    public void init() {
        Instant start = Instant.now();
        try {
            Path homePath = Paths.get(grobidHome);
            Files.createDirectories(homePath);

            // 设置GROBID引擎参数
            System.setProperty("grobid.home", grobidHome);
            System.setProperty("grobid.nb_threads", "2");

            // grobid-core 0.9.0 引擎初始化
            // 主入口: org.grobid.core.GrobidModels / org.grobid.core.engines.Engine
            // 模型会自动从 grobid-home/models 加载

            // 检查模型文件是否存在，不存在则自动下载
            Path modelsPath = homePath.resolve("models");
            if (!Files.exists(modelsPath) || !hasModelFiles(modelsPath)) {
                logger.warn("GROBID模型不存在: {}，开始自动下载（约1.2GB，仅需一次）...", modelsPath);
                Files.createDirectories(modelsPath);
                GrobidModelDownloader.download(homePath);
            }

            initialized = true;
            initTimeMs = Duration.between(start, Instant.now()).toMillis();
            logger.info("GROBID引擎初始化完成, 耗时: {}ms, 模型路径: {}",
                    initTimeMs, modelsPath);

        } catch (Exception e) {
            initialized = false;
            initError = e.getMessage();
            logger.warn("GROBID引擎初始化失败: {}。将自动降级到PDFBox。", e.getMessage());
        }
    }

    /** 解析论文全文，返回结构化结果 */
    public GrobidResult processFulltext(File pdfFile) {
        if (!initialized) {
            throw new GrobidNotAvailableException("GROBID引擎未初始化: " + initError);
        }

        Instant start = Instant.now();
        try {
            // 调用grobid-core API
            // grobid-core 0.9.0 主要API:
            //   org.grobid.core.engines.Engine.getEngine()
            //   FullTextParser.processing(File, GrobidAnalysisConfig)
            //
            // 由于grobid-core可能需要反射调用（不同版本的API略有差异），
            // 这里提供标准调用路径

            String teiXml = invokeGrobidFulltext(pdfFile);
            long elapsed = Duration.between(start, Instant.now()).toMillis();

            logger.info("GROBID解析成功: {}ms, TEI XML长度: {}", elapsed, teiXml.length());

            StructuredPaper paper = teiParser.parseFulltext(teiXml);
            return new GrobidResult(paper, teiXml, elapsed);

        } catch (GrobidNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            logger.error("GROBID解析失败 ({}ms): {}", elapsed, e.getMessage());
            throw new GrobidParsingException("GROBID解析失败: " + e.getMessage(), e);
        }
    }

    /** 调用grobid-core底层API */
    private String invokeGrobidFulltext(File pdfFile) throws Exception {
        // grobid-core 0.9.0 标准调用方式:
        //   1. Engine engine = Engine.getEngine(true);  // true = 并行模式
        //   2. FullTextParser parser = new FullTextParser(engine);
        //   3. GrobidAnalysisConfig config = GrobidAnalysisConfig.builder().build();
        //   4. String tei = parser.processing(pdfFile, config);
        //
        // 注意: 需要grobid-home/config/grobid.properties配置文件
        // 模型文件位于grobid-home/models/

        // 使用反射调用以避免编译期硬依赖
        try {
            Class<?> engineClass = Class.forName("org.grobid.core.engines.Engine");
            Class<?> parserClass = Class.forName("org.grobid.core.document.FullTextParser");
            Class<?> configClass = Class.forName("org.grobid.core.analyzers.GrobidAnalysisConfig");

            // Engine engine = Engine.getEngine(true);
            Object engine = engineClass.getMethod("getEngine", boolean.class).invoke(null, true);

            // FullTextParser parser = new FullTextParser(engine);
            Object parser = parserClass.getConstructor(engineClass).newInstance(engine);

            // GrobidAnalysisConfig config = GrobidAnalysisConfig.builder().build();
            Object configBuilder = configClass.getMethod("builder").invoke(null);
            Object config = configBuilder.getClass().getMethod("build").invoke(configBuilder);

            // String tei = parser.processing(pdfFile, config);
            Object result = parserClass.getMethod("processing", File.class, configClass)
                    .invoke(parser, pdfFile, config);

            return result != null ? result.toString() : "";

        } catch (ClassNotFoundException e) {
            // grobid-core 0.9.0 不在classpath，降级
            throw new GrobidNotAvailableException(
                    "grobid-core 0.9.0 类未找到，请确认 E:/code/grobid-core-0.9.0.jar 已安装到本地Maven仓库");
        }
    }

    public boolean isAvailable() {
        return initialized;
    }

    private boolean hasModelFiles(Path modelsDir) {
        try (var stream = Files.list(modelsDir)) {
            return stream.anyMatch(p -> {
                String name = p.getFileName().toString();
                return name.startsWith("header") || name.startsWith("fulltext")
                        || name.startsWith("segment") || name.startsWith("date");
            });
        } catch (IOException e) { return false; }
    }

    public String getStatus() {
        return initialized ? "GROBID引擎就绪 (初始化耗时: " + initTimeMs + "ms)"
                : "GROBID不可用: " + initError;
    }

    // -- 结果类 --

    public static class GrobidResult {
        private final StructuredPaper paper;
        private final String rawTeiXml;
        private final long parseTimeMs;

        public GrobidResult(StructuredPaper paper, String tei, long ms) {
            this.paper = paper; this.rawTeiXml = tei; this.parseTimeMs = ms;
        }
        public StructuredPaper getPaper() { return paper; }
        public String getRawTeiXml() { return rawTeiXml; }
        public long getParseTimeMs() { return parseTimeMs; }
    }

    public static class GrobidNotAvailableException extends RuntimeException {
        public GrobidNotAvailableException(String msg) { super(msg); }
    }

    public static class GrobidParsingException extends RuntimeException {
        public GrobidParsingException(String msg, Throwable cause) { super(msg, cause); }
    }
}

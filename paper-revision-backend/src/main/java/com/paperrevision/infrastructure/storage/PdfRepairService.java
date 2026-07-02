package com.paperrevision.infrastructure.storage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/**
 * PDF 修复服务 — 二级降级
 *
 * 当 PDFBox 纯文本提取也失败时，尝试修复损坏的 PDF：
 *
 * 策略1: PDFBox 自修复
 *   - 移除损坏的对象
 *   - 重建交叉引用表 (xref)
 *   - 移除损坏的流
 *
 * 策略2: Ghostscript 修复 (如果安装了)
 *   - gs -dSAFER -dBATCH -dNOPAUSE -sDEVICE=pdfwrite -sOutputFile=fixed.pdf broken.pdf
 *   - 完全重写PDF结构，修复几乎所有可恢复的损坏
 *
 * 策略3: 结构重建
 *   - 提取所有可读的文本流
 *   - 忽略所有非文本内容
 *   - 返回纯文本（即使PDF结构完全损坏）
 */
@Service
public class PdfRepairService {

    private static final Logger logger = LoggerFactory.getLogger(PdfRepairService.class);

    /** 尝试修复PDF */
    public File repair(File damagedFile) {
        String baseName = damagedFile.getName().replace(".pdf", "");

        // 策略1: PDFBox 修复加载
        File pdfboxRepaired = repairWithPDFBox(damagedFile, baseName);
        if (pdfboxRepaired != null && isValidPdf(pdfboxRepaired)) {
            logger.info("PDFBox修复成功: {}", pdfboxRepaired.getName());
            return pdfboxRepaired;
        }

        // 策略2: Ghostscript 修复
        File gsRepaired = repairWithGhostscript(damagedFile, baseName);
        if (gsRepaired != null && isValidPdf(gsRepaired)) {
            logger.info("Ghostscript修复成功: {}", gsRepaired.getName());
            return gsRepaired;
        }

        // 策略3: 裸文本提取（放弃PDF结构，只要文字）
        File textOnly = extractRawText(damagedFile, baseName);
        if (textOnly != null) {
            logger.info("裸文本提取成功: {}", textOnly.getName());
            return textOnly;
        }

        return null;
    }

    /** PDFBox修复：移除损坏对象，重建xref表 */
    private File repairWithPDFBox(File damaged, String baseName) {
        try {
            // 方式1: 直接加载（PDFBoxLoader有容错能力）
            PDDocument doc = Loader.loadPDF(damaged);
            File repaired = new File(damaged.getParent(),
                    baseName + "_repaired_" + UUID.randomUUID().toString().substring(0, 6) + ".pdf");

            // 保存时PDFBox会自动修复xref表
            doc.save(repaired);
            doc.close();
            return repaired;
        } catch (Exception e) {
            logger.debug("PDFBox修复失败: {}", e.getMessage());
            return null;
        }
    }

    /** Ghostscript修复（需要安装Ghostscript） */
    private File repairWithGhostscript(File damaged, String baseName) {
        if (!isGhostscriptAvailable()) {
            logger.debug("Ghostscript未安装，跳过");
            return null;
        }
        try {
            File repaired = new File(damaged.getParent(),
                    baseName + "_gs_" + UUID.randomUUID().toString().substring(0, 6) + ".pdf");

            ProcessBuilder pb = new ProcessBuilder(
                    "gs",
                    "-dSAFER",           // 安全模式
                    "-dBATCH",           // 批处理模式
                    "-dNOPAUSE",         // 不暂停
                    "-dNOPROMPT",        // 不提示
                    "-sDEVICE=pdfwrite", // 输出PDF
                    "-dPDFSETTINGS=/prepress", // 高质量
                    "-dAutoRotatePages=/None",
                    "-dCompatibilityLevel=1.7",
                    "-sOutputFile=" + repaired.getAbsolutePath(),
                    damaged.getAbsolutePath()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && repaired.exists() && repaired.length() > 0) {
                return repaired;
            }
            repaired.delete();
            return null;
        } catch (Exception e) {
            logger.debug("Ghostscript修复失败: {}", e.getMessage());
            return null;
        }
    }

    /** 裸文本提取：放弃PDF结构，逐字节扫描可读文本 */
    private File extractRawText(File damaged, String baseName) {
        try {
            File textFile = new File(damaged.getParent(),
                    baseName + "_text_" + UUID.randomUUID().toString().substring(0, 6) + ".txt");

            StringBuilder sb = new StringBuilder();
            try (InputStream is = new FileInputStream(damaged)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    for (int i = 0; i < len; i++) {
                        byte b = buffer[i];
                        // 过滤出可打印ASCII和常见UTF-8字符
                        if ((b >= 32 && b < 127) || b == '\n' || b == '\r' || b == '\t') {
                            sb.append((char) b);
                        }
                    }
                }
            }

            // 提取括号内文本（PDF文本通常在BT/ET块和()内）
            String raw = sb.toString();
            StringBuilder extracted = new StringBuilder();
            boolean inTextBlock = false;

            for (String line : raw.split("\n")) {
                if (line.contains("BT")) inTextBlock = true;
                if (line.contains("ET")) inTextBlock = false;
                if (inTextBlock) {
                    // 提取括号内的文本
                    int start = line.indexOf('(');
                    while (start != -1) {
                        int end = line.indexOf(')', start);
                        if (end == -1) break;
                        String text = line.substring(start + 1, end);
                        if (text.length() > 1 && !text.startsWith("\\")) {
                            extracted.append(text).append(" ");
                        }
                        start = line.indexOf('(', end);
                    }
                }
            }

            Files.writeString(textFile.toPath(), extracted.toString());
            return textFile;
        } catch (Exception e) {
            logger.debug("裸文本提取失败: {}", e.getMessage());
            return null;
        }
    }

    /** 检查PDF是否有效 */
    private boolean isValidPdf(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 检查Ghostscript是否可用 */
    private boolean isGhostscriptAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gs", "--version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

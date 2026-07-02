package org.xhy.infrastructure.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xhy.domain.paper.model.StructuredPaper;
import org.xhy.domain.paper.model.StructuredPaper.Reference;
import org.xhy.domain.paper.model.StructuredPaper.Section;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * GROBID TEI XML 解析器
 * 将GROBID返回的TEI XML转换为结构化的论文模型
 *
 * TEI (Text Encoding Initiative) 是人文科学文本编码标准
 * GROBID用它来表示论文的完整结构信息
 */
@Service
public class GrobidTEIParser {

    private static final Logger logger = LoggerFactory.getLogger(GrobidTEIParser.class);

    /** 解析GROBID返回的TEI XML，提取结构化论文信息 */
    public StructuredPaper parseFulltext(String teiXml) {
        StructuredPaper paper = new StructuredPaper();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(teiXml.getBytes(StandardCharsets.UTF_8)));

            // 提取标题
            paper.setTitle(extractText(doc, "//titleStmt/title"));

            // 提取作者
            paper.setAuthors(extractList(doc, "//author/persName/surname"));

            // 提取摘要
            paper.setAbstractText(extractText(doc, "//abstract"));

            // 提取关键词
            paper.setKeywords(extractText(doc, "//keywords"));

            // 提取章节结构
            paper.setSections(extractSections(doc));

            // 提取参考文献
            paper.setReferences(extractReferences(doc));

            // 拼接全文
            StringBuilder fullText = new StringBuilder();
            fullText.append(paper.getTitle()).append("\n");
            if (paper.getAbstractText() != null) fullText.append(paper.getAbstractText()).append("\n");
            for (Section section : paper.getSections()) {
                appendSection(fullText, section, 0);
            }
            paper.setFullText(fullText.toString());

            logger.info("TEI XML解析完成: 标题={}, 作者数={}, 章节数={}, 参考文献数={}",
                    paper.getTitle(),
                    paper.getAuthors() != null ? paper.getAuthors().size() : 0,
                    paper.getSections().size(),
                    paper.getReferences().size());

            return paper;
        } catch (Exception e) {
            logger.error("TEI XML解析失败", e);
            throw new RuntimeException("论文结构解析失败: " + e.getMessage());
        }
    }

    private List<Section> extractSections(Document doc) {
        List<Section> sections = new ArrayList<>();
        NodeList divNodes = doc.getElementsByTagName("div");
        for (int i = 0; i < divNodes.getLength(); i++) {
            Element div = (Element) divNodes.item(i);
            Section section = parseSection(div);
            if (section != null) sections.add(section);
        }
        return sections;
    }

    private Section parseSection(Element div) {
        Section section = new Section();
        // 提取章节标题
        NodeList heads = div.getElementsByTagName("head");
        if (heads.getLength() > 0) {
            section.setTitle(heads.item(0).getTextContent().trim());
        }
        // 提取章节内容
        StringBuilder content = new StringBuilder();
        NodeList children = div.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!"head".equals(child.getNodeName())) {
                content.append(child.getTextContent()).append("\n");
            }
        }
        section.setContent(content.toString().trim());
        return section;
    }

    private List<Reference> extractReferences(Document doc) {
        List<Reference> refs = new ArrayList<>();
        try {
            NodeList biblStructs = doc.getElementsByTagName("biblStruct");
            for (int i = 0; i < biblStructs.getLength(); i++) {
                Element bibl = (Element) biblStructs.item(i);
                Reference ref = new Reference();
                ref.setTitle(extractTextFromElement(bibl, "title"));
                ref.setAuthors(extractAuthorsFromRef(bibl));
                ref.setYear(extractTextFromElement(bibl, "date"));
                ref.setJournal(extractTextFromElement(bibl, "title[@level='j']"));
                ref.setDoi(extractTextFromElement(bibl, "idno[@type='DOI']"));
                ref.setRawText(bibl.getTextContent().trim());
                refs.add(ref);
            }
        } catch (Exception e) {
            logger.warn("参考文献解析部分失败: {}", e.getMessage());
        }
        return refs;
    }

    private List<String> extractAuthorsFromRef(Element bibl) {
        List<String> authors = new ArrayList<>();
        NodeList surnames = bibl.getElementsByTagName("surname");
        for (int i = 0; i < surnames.getLength(); i++) {
            authors.add(surnames.item(i).getTextContent().trim());
        }
        return authors;
    }

    private String extractText(Document doc, String xpath) {
        // 简化的XPath-like文本提取
        try {
            String[] parts = xpath.replace("//", "").split("/");
            NodeList nodes = doc.getElementsByTagName(parts[parts.length - 1]);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<String> extractList(Document doc, String xpath) {
        List<String> result = new ArrayList<>();
        try {
            String[] parts = xpath.replace("//", "").split("/");
            NodeList nodes = doc.getElementsByTagName(parts[parts.length - 1]);
            for (int i = 0; i < nodes.getLength(); i++) {
                result.add(nodes.item(i).getTextContent().trim());
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String extractTextFromElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) return nodes.item(0).getTextContent().trim();
        return null;
    }

    private void appendSection(StringBuilder sb, Section section, int depth) {
        sb.append("  ".repeat(depth)).append("# ").append(section.getTitle()).append("\n");
        sb.append(section.getContent()).append("\n");
        if (section.getSubsections() != null) {
            for (Section sub : section.getSubsections()) {
                appendSection(sb, sub, depth + 1);
            }
        }
    }
}

package com.paperrevision.domain.paper.model;

import java.util.List;
import java.util.Map;

/**
 * 结构化论文模型
 * GROBID解析后得到的论文结构，包含标题/作者/摘要/章节/参考文献
 */
public class StructuredPaper {

    private String title;
    private List<String> authors;
    private String abstractText;
    private String keywords;
    private List<Section> sections;
    private List<Reference> references;
    private String fullText;
    private int pageCount;

    /** 论文章节 */
    public static class Section {
        private String title;
        private String content;
        private int sectionLevel; // 1=h1, 2=h2, etc.
        private List<Section> subsections;
        private List<Figure> figures;
        private List<Table> tables;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getSectionLevel() { return sectionLevel; }
        public void setSectionLevel(int sectionLevel) { this.sectionLevel = sectionLevel; }
        public List<Section> getSubsections() { return subsections; }
        public void setSubsections(List<Section> subsections) { this.subsections = subsections; }
        public List<Figure> getFigures() { return figures; }
        public void setFigures(List<Figure> figures) { this.figures = figures; }
        public List<Table> getTables() { return tables; }
        public void setTables(List<Table> tables) { this.tables = tables; }
    }

    /** 图表 */
    public static class Figure {
        private String caption;
        private String label;
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class Table {
        private String caption;
        private String label;
        private List<List<String>> rows;
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> rows) { this.rows = rows; }
    }

    /** 参考文献 */
    public static class Reference {
        private String title;
        private List<String> authors;
        private String year;
        private String journal;
        private String doi;
        private String rawText;
        private int citationCount; // 在文中被引用次数

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getAuthors() { return authors; }
        public void setAuthors(List<String> authors) { this.authors = authors; }
        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }
        public String getJournal() { return journal; }
        public void setJournal(String journal) { this.journal = journal; }
        public String getDoi() { return doi; }
        public void setDoi(String doi) { this.doi = doi; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
        public int getCitationCount() { return citationCount; }
        public void setCitationCount(int citationCount) { this.citationCount = citationCount; }
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }
    public List<Reference> getReferences() { return references; }
    public void setReferences(List<Reference> references) { this.references = references; }
    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
}

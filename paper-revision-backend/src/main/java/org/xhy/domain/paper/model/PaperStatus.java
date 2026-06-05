package org.xhy.domain.paper.model;

/** 论文处理状态 */
public enum PaperStatus {
    UPLOADED("已上传"),
    PARSING("解析中"),
    PARSED("已解析"),
    REVISING("返修中"),
    COMPLETED("已完成"),
    FAILED("处理失败");

    private final String description;

    PaperStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}

package com.paperrevision.domain.paper.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paperrevision.infrastructure.entity.BaseEntity;

/** 论文实体 */
@TableName("papers")
public class PaperEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String title;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String parsedText;
    private Integer pageCount;
    private String status; // UPLOADED, PARSING, PARSED, REVISING, COMPLETED, FAILED
    private String userId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

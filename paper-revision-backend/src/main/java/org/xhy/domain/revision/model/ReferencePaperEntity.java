package org.xhy.domain.revision.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.entity.BaseEntity;

/** 参考文献实体 */
@TableName("reference_papers")
public class ReferencePaperEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String title;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String parsedText;
    private String paperId;
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
    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }
    public String getPaperId() { return paperId; }
    public void setPaperId(String paperId) { this.paperId = paperId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

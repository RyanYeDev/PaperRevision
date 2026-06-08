package org.xhy.domain.agent.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.entity.BaseEntity;

/** Agent实体 */
@TableName("agents")
public class AgentEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String description;
    private String systemPrompt;
    private String modelProvider;
    private String modelName;
    private String configJson;
    private Boolean enabled;
    private String userId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

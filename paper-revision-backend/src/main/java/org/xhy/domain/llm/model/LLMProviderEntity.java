package org.xhy.domain.llm.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.infrastructure.entity.BaseEntity;

/** LLM提供商实体 */
@TableName("llm_providers")
public class LLMProviderEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String providerType;
    private String baseUrl;
    private String apiKey;
    private String defaultModel;
    private Boolean enabled;
    private String userId;

    /** 提供商配置JSON (模型列表、参数等) */
    @TableField("config_json")
    private String configJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}

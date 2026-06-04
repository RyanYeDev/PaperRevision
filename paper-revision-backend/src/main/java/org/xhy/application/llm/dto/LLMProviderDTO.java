package org.xhy.application.llm.dto;

import java.time.LocalDateTime;

/** LLM提供商DTO */
public class LLMProviderDTO {

    private String id;
    private String name;
    private String providerType;
    private String baseUrl;
    private String apiKey;
    private String defaultModel;
    private Boolean enabled;
    private String configJson;
    private LocalDateTime createdAt;

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
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

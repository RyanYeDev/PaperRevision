package org.xhy.interfaces.dto.llm.request;

import jakarta.validation.constraints.NotBlank;

/** LLM提供商创建/更新请求 */
public class LLMProviderRequest {

    @NotBlank(message = "提供商名称不能为空")
    private String name;

    private String providerType;

    @NotBlank(message = "API地址不能为空")
    private String baseUrl;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    private String defaultModel;
    private Boolean enabled;
    private String configJson;

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
}

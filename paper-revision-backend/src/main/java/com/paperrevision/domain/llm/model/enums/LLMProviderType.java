package com.paperrevision.domain.llm.model.enums;

/** LLM提供商类型 */
public enum LLMProviderType {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
    DOUBAO("豆包(Doubao)", "https://ark.cn-beijing.volces.com/api/v3"),
    OPENAI("OpenAI", "https://api.openai.com"),
    CUSTOM("自定义", "");

    private final String displayName;
    private final String defaultBaseUrl;

    LLMProviderType(String displayName, String defaultBaseUrl) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public String getDisplayName() { return displayName; }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }
}

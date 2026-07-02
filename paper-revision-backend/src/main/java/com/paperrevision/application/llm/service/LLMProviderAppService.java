package com.paperrevision.application.llm.service;

import org.springframework.stereotype.Service;
import com.paperrevision.application.llm.assembler.LLMProviderAssembler;
import com.paperrevision.application.llm.dto.LLMProviderDTO;
import com.paperrevision.domain.llm.model.LLMProviderEntity;
import com.paperrevision.domain.llm.model.enums.LLMProviderType;
import com.paperrevision.domain.llm.service.LLMProviderDomainService;
import com.paperrevision.infrastructure.exception.BusinessException;

import java.util.List;

/** LLM提供商应用服务 */
@Service
public class LLMProviderAppService {

    private final LLMProviderDomainService domainService;

    public LLMProviderAppService(LLMProviderDomainService domainService) {
        this.domainService = domainService;
    }

    /** 创建提供商 */
    public LLMProviderDTO createProvider(String name, String providerType, String baseUrl,
            String apiKey, String defaultModel, String userId) {
        LLMProviderEntity entity = new LLMProviderEntity();
        entity.setName(name);
        entity.setProviderType(providerType);
        entity.setBaseUrl(baseUrl);
        entity.setApiKey(apiKey);
        entity.setDefaultModel(defaultModel);
        entity.setEnabled(true);
        entity.setUserId(userId);

        LLMProviderEntity created = domainService.createProvider(entity);
        return LLMProviderAssembler.toDTO(created);
    }

    /** 获取用户的所有提供商 */
    public List<LLMProviderDTO> getUserProviders(String userId) {
        List<LLMProviderEntity> providers = domainService.getUserProviders(userId);
        return LLMProviderAssembler.toDTOs(providers);
    }

    /** 更新提供商 */
    public LLMProviderDTO updateProvider(String providerId, String name, String baseUrl,
            String apiKey, String defaultModel, Boolean enabled, String configJson) {
        LLMProviderEntity updated = new LLMProviderEntity();
        updated.setName(name);
        updated.setBaseUrl(baseUrl);
        updated.setApiKey(apiKey);
        updated.setDefaultModel(defaultModel);
        updated.setEnabled(enabled);
        updated.setConfigJson(configJson);
        return LLMProviderAssembler.toDTO(domainService.updateProvider(providerId, updated));
    }

    /** 创建默认提供商（DeepSeek + Doubao） */
    public void createDefaultProviders(String userId) {
        // 创建默认DeepSeek配置
        if (domainService.getUserProviders(userId).isEmpty()) {
            LLMProviderEntity deepseek = new LLMProviderEntity();
            deepseek.setName("DeepSeek");
            deepseek.setProviderType(LLMProviderType.DEEPSEEK.name());
            deepseek.setBaseUrl(LLMProviderType.DEEPSEEK.getDefaultBaseUrl());
            deepseek.setDefaultModel("deepseek-chat");
            deepseek.setEnabled(true);
            deepseek.setUserId(userId);
            domainService.createProvider(deepseek);

            LLMProviderEntity doubao = new LLMProviderEntity();
            doubao.setName("豆包(Doubao)");
            doubao.setProviderType(LLMProviderType.DOUBAO.name());
            doubao.setBaseUrl(LLMProviderType.DOUBAO.getDefaultBaseUrl());
            doubao.setDefaultModel("doubao-pro-32k");
            doubao.setEnabled(false);
            doubao.setUserId(userId);
            domainService.createProvider(doubao);
        }
    }
}

package org.xhy.domain.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.llm.model.LLMProviderEntity;
import org.xhy.domain.llm.repository.LLMProviderRepository;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.exception.EntityNotFoundException;

import java.util.List;

/** LLM提供商领域服务 */
@Service
public class LLMProviderDomainService {

    private static final Logger logger = LoggerFactory.getLogger(LLMProviderDomainService.class);

    private final LLMProviderRepository providerRepository;

    public LLMProviderDomainService(LLMProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    /** 创建提供商配置 */
    public LLMProviderEntity createProvider(LLMProviderEntity provider) {
        // 检查同名提供商
        LambdaQueryWrapper<LLMProviderEntity> wrapper = Wrappers.<LLMProviderEntity>lambdaQuery()
                .eq(LLMProviderEntity::getUserId, provider.getUserId())
                .eq(LLMProviderEntity::getName, provider.getName());
        if (providerRepository.selectOne(wrapper) != null) {
            throw new BusinessException("同名提供商已存在");
        }
        providerRepository.checkInsert(provider);
        logger.info("LLM提供商创建成功: {}", provider.getName());
        return provider;
    }

    /** 获取用户的所有提供商 */
    public List<LLMProviderEntity> getUserProviders(String userId) {
        LambdaQueryWrapper<LLMProviderEntity> wrapper = Wrappers.<LLMProviderEntity>lambdaQuery()
                .eq(LLMProviderEntity::getUserId, userId)
                .orderByDesc(LLMProviderEntity::getCreatedAt);
        return providerRepository.selectList(wrapper);
    }

    /** 更新提供商配置 */
    public LLMProviderEntity updateProvider(String providerId, LLMProviderEntity updated) {
        LLMProviderEntity existing = providerRepository.selectById(providerId);
        if (existing == null) {
            throw new EntityNotFoundException("LLM提供商", providerId);
        }
        existing.setName(updated.getName());
        existing.setBaseUrl(updated.getBaseUrl());
        existing.setApiKey(updated.getApiKey());
        existing.setDefaultModel(updated.getDefaultModel());
        existing.setEnabled(updated.getEnabled());
        existing.setConfigJson(updated.getConfigJson());
        providerRepository.checkedUpdateById(existing);
        return existing;
    }

    /** 获取默认启用的提供商 */
    public LLMProviderEntity getDefaultProvider(String userId) {
        LambdaQueryWrapper<LLMProviderEntity> wrapper = Wrappers.<LLMProviderEntity>lambdaQuery()
                .eq(LLMProviderEntity::getUserId, userId)
                .eq(LLMProviderEntity::getEnabled, true)
                .last("LIMIT 1");
        LLMProviderEntity provider = providerRepository.selectOne(wrapper);
        if (provider == null) {
            throw new BusinessException("请先配置LLM提供商 (DeepSeek/豆包)");
        }
        return provider;
    }
}

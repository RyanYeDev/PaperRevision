package com.paperrevision.application.llm.assembler;

import org.springframework.beans.BeanUtils;
import com.paperrevision.application.llm.dto.LLMProviderDTO;
import com.paperrevision.domain.llm.model.LLMProviderEntity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** LLM提供商装配器 */
public class LLMProviderAssembler {

    public static LLMProviderDTO toDTO(LLMProviderEntity entity) {
        if (entity == null) return null;
        LLMProviderDTO dto = new LLMProviderDTO();
        BeanUtils.copyProperties(entity, dto);
        // 脱敏：只显示API Key后4位
        if (dto.getApiKey() != null && dto.getApiKey().length() > 4) {
            dto.setApiKey("****" + dto.getApiKey().substring(dto.getApiKey().length() - 4));
        }
        return dto;
    }

    public static List<LLMProviderDTO> toDTOs(List<LLMProviderEntity> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        return entities.stream().map(LLMProviderAssembler::toDTO).collect(Collectors.toList());
    }
}

package com.paperrevision.domain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.paperrevision.domain.agent.model.AgentEntity;
import com.paperrevision.domain.agent.repository.AgentRepository;
import com.paperrevision.infrastructure.exception.BusinessException;
import com.paperrevision.infrastructure.exception.EntityNotFoundException;

import java.util.List;

/** Agent领域服务 */
@Service
public class AgentDomainService {

    private static final Logger logger = LoggerFactory.getLogger(AgentDomainService.class);

    private final AgentRepository agentRepository;

    public AgentDomainService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    /** 创建Agent */
    public AgentEntity createAgent(AgentEntity agent) {
        agentRepository.checkInsert(agent);
        logger.info("Agent创建成功: {}", agent.getName());
        return agent;
    }

    /** 获取用户的所有Agent */
    public List<AgentEntity> getUserAgents(String userId) {
        LambdaQueryWrapper<AgentEntity> wrapper = Wrappers.<AgentEntity>lambdaQuery()
                .eq(AgentEntity::getUserId, userId)
                .orderByDesc(AgentEntity::getCreatedAt);
        return agentRepository.selectList(wrapper);
    }

    /** 获取Agent详情 */
    public AgentEntity getAgentById(String agentId) {
        AgentEntity agent = agentRepository.selectById(agentId);
        if (agent == null) throw new EntityNotFoundException("Agent", agentId);
        return agent;
    }

    /** 更新Agent */
    public AgentEntity updateAgent(String agentId, AgentEntity updated) {
        AgentEntity existing = getAgentById(agentId);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setSystemPrompt(updated.getSystemPrompt());
        existing.setModelProvider(updated.getModelProvider());
        existing.setModelName(updated.getModelName());
        existing.setConfigJson(updated.getConfigJson());
        existing.setEnabled(updated.getEnabled());
        agentRepository.checkedUpdateById(existing);
        return existing;
    }

    /** 删除Agent */
    public void deleteAgent(String agentId) {
        agentRepository.checkedDelete(Wrappers.<AgentEntity>lambdaQuery().eq(AgentEntity::getId, agentId));
    }
}

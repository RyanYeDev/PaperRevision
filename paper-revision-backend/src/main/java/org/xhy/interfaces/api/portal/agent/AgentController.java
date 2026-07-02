package org.xhy.interfaces.api.portal.agent;

import org.springframework.web.bind.annotation.*;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.infrastructure.auth.UserContext;
import org.xhy.interfaces.api.common.Result;

import java.util.List;

/** Agent管理控制器 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentDomainService agentDomainService;

    public AgentController(AgentDomainService agentDomainService) {
        this.agentDomainService = agentDomainService;
    }

    @GetMapping
    public Result<List<AgentEntity>> getUserAgents() {
        String userId = UserContext.getCurrentUserId();
        return Result.success(agentDomainService.getUserAgents(userId));
    }

    @PostMapping
    public Result<AgentEntity> createAgent(@RequestBody AgentEntity agent) {
        String userId = UserContext.getCurrentUserId();
        agent.setUserId(userId);
        return Result.success("Agent创建成功", agentDomainService.createAgent(agent));
    }

    @GetMapping("/{agentId}")
    public Result<AgentEntity> getAgent(@PathVariable String agentId) {
        return Result.success(agentDomainService.getAgentById(agentId));
    }

    @PutMapping("/{agentId}")
    public Result<AgentEntity> updateAgent(@PathVariable String agentId, @RequestBody AgentEntity updated) {
        return Result.success(agentDomainService.updateAgent(agentId, updated));
    }

    @DeleteMapping("/{agentId}")
    public Result<Void> deleteAgent(@PathVariable String agentId) {
        agentDomainService.deleteAgent(agentId);
        return Result.success();
    }
}

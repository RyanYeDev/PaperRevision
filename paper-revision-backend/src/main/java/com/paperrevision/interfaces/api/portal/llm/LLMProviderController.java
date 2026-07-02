package com.paperrevision.interfaces.api.portal.llm;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.paperrevision.application.llm.dto.LLMProviderDTO;
import com.paperrevision.application.llm.service.LLMProviderAppService;
import com.paperrevision.infrastructure.auth.UserContext;
import com.paperrevision.interfaces.api.common.Result;
import com.paperrevision.interfaces.dto.llm.request.LLMProviderRequest;

import java.util.List;

/** LLM提供商管理控制器 */
@RestController
@RequestMapping("/api/llm-providers")
public class LLMProviderController {

    private final LLMProviderAppService appService;

    public LLMProviderController(LLMProviderAppService appService) {
        this.appService = appService;
    }

    /** 获取当前用户的所有LLM提供商 */
    @GetMapping
    public Result<List<LLMProviderDTO>> getUserProviders() {
        String userId = UserContext.getCurrentUserId();
        return Result.success(appService.getUserProviders(userId));
    }

    /** 创建LLM提供商 */
    @PostMapping
    public Result<LLMProviderDTO> createProvider(@RequestBody @Validated LLMProviderRequest request) {
        String userId = UserContext.getCurrentUserId();
        LLMProviderDTO provider = appService.createProvider(
                request.getName(), request.getProviderType(),
                request.getBaseUrl(), request.getApiKey(),
                request.getDefaultModel(), userId);
        return Result.success("提供商创建成功", provider);
    }

    /** 更新LLM提供商 */
    @PutMapping("/{providerId}")
    public Result<LLMProviderDTO> updateProvider(@PathVariable String providerId,
            @RequestBody @Validated LLMProviderRequest request) {
        LLMProviderDTO provider = appService.updateProvider(
                providerId, request.getName(), request.getBaseUrl(),
                request.getApiKey(), request.getDefaultModel(),
                request.getEnabled(), request.getConfigJson());
        return Result.success("提供商更新成功", provider);
    }

    /** 初始化默认提供商 */
    @PostMapping("/init-defaults")
    public Result<Void> initDefaultProviders() {
        String userId = UserContext.getCurrentUserId();
        appService.createDefaultProviders(userId);
        return Result.success();
    }
}

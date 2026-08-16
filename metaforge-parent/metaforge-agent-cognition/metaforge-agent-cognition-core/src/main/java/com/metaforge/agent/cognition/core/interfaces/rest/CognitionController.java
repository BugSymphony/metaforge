package com.metaforge.agent.cognition.core.interfaces.rest;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.CognitionResponse;
import com.metaforge.agent.cognition.api.service.CognitionQueryService;
import com.metaforge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cognition")
@Tag(name = "agent-cognition")
public class CognitionController {

    private final CognitionQueryService cognitionQueryService;

    public CognitionController(CognitionQueryService cognitionQueryService) {
        this.cognitionQueryService = cognitionQueryService;
    }

    @PostMapping("/{templateId}")
    public ApiResponse<CognitionResponse> execute(
            @PathVariable String templateId,
            @RequestBody CognitionRequest request) {
        CognitionResponse response = cognitionQueryService.execute(templateId, request);
        return ApiResponse.success(response);
    }
}

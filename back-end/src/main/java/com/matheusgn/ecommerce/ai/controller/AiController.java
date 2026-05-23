package com.matheusgn.ecommerce.ai.controller;

import com.matheusgn.ecommerce.ai.dto.ChatRequest;
import com.matheusgn.ecommerce.ai.dto.ChatResponse;
import com.matheusgn.ecommerce.ai.dto.RecommendationResponse;
import com.matheusgn.ecommerce.ai.service.AiChatService;
import com.matheusgn.ecommerce.ai.service.AiRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "IA generativa")
public class AiController {

    private final AiRecommendationService aiRecommendationService;
    private final AiChatService aiChatService;

    @PostMapping("/recommendations/{customerId}")
    @Operation(summary = "Recomendações personalizadas")
    public ResponseEntity<RecommendationResponse> recommendations(@PathVariable UUID customerId) {
        return ResponseEntity.ok(aiRecommendationService.recommend(customerId));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat com assistente")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }
}

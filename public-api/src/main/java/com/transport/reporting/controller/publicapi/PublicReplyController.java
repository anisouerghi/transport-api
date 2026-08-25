package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.dto.PublicHomepageReplyResponse;
import com.transport.reporting.service.PublicTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur public : réponses visibles sur l'accueil voyageur.
 */
@RestController
@RequestMapping("/api/public/reponses")
@Tag(name = "Public - Replies")
public class PublicReplyController {

    private final PublicTrackingService publicTrackingService;

    public PublicReplyController(PublicTrackingService publicTrackingService) {
        this.publicTrackingService = publicTrackingService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les réponses visibles à l'accueil",
            description = "Accès anonyme. Filtre : signalement.publish = true. "
                    + "15 dernières réponses max, pagination 5 par page. Aucune donnée personnelle."
    )
    public ResponseEntity<ApiResponse<PageResponse<PublicHomepageReplyResponse>>> homepage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.listHomepageReplies(page, size)));
    }
}

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
                    + "Au plus 12 réponses (3 pages × 4). Les paramètres page/size sont bornés côté serveur "
                    + "(page 0..2, size ≤ 4). Retourne le message du signalement, "
                    + "le message de la réponse et le nom du voyageur."
    )
    public ResponseEntity<ApiResponse<PageResponse<PublicHomepageReplyResponse>>> homepage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.listHomepageReplies(page, size)));
    }
}

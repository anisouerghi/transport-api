package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PassengerNotificationInboxResponse;
import com.transport.reporting.dto.PassengerNotificationReadRequest;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.service.PassengerNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Boîte de notifications plateforme (cloche voyageur).
 * Le destinataire est toujours tiré du JWT — jamais d'un id fourni par le client.
 */
@RestController
@RequestMapping("/api/public/notifications")
@Tag(name = "Public - Notifications")
public class PublicNotificationController {

    private final PassengerNotificationService notificationService;

    public PublicNotificationController(PassengerNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/inbox")
    @Operation(summary = "Inbox cloche : non lus + 20 dernières notifications")
    public ResponseEntity<ApiResponse<PassengerNotificationInboxResponse>> inbox(
            @AuthenticationPrincipal PassengerPrincipal principal) {
        Long passengerId = principal != null ? principal.getPassengerId() : null;
        return ResponseEntity.ok(ApiResponse.ok(notificationService.inbox(passengerId)));
    }

    @PostMapping("/read")
    @Operation(summary = "Marquer des notifications comme lues (scoped au voyageur courant)")
    public ResponseEntity<ApiResponse<Integer>> markRead(
            @AuthenticationPrincipal PassengerPrincipal principal,
            @RequestBody(required = false) PassengerNotificationReadRequest request) {
        Long passengerId = principal != null ? principal.getPassengerId() : null;
        int updated = notificationService.markRead(passengerId, request);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}

package com.transport.reporting.controller.publicapi;

import com.transport.reporting.common.response.ApiResponse;
import com.transport.reporting.dto.PassengerReplyRequest;
import com.transport.reporting.dto.PublicReportListItemResponse;
import com.transport.reporting.dto.PublicReportTrackingResponse;
import com.transport.reporting.dto.ReplyResponse;
import com.transport.reporting.dto.ReportRequest;
import com.transport.reporting.dto.ReportResponse;
import com.transport.reporting.security.PassengerPrincipal;
import com.transport.reporting.service.PublicTrackingService;
import com.transport.reporting.service.ReplyService;
import com.transport.reporting.service.ReportService;
import com.transport.reporting.service.TurnstileValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Contrôleur public : création et suivi des signalements voyageur.
 */
@RestController
@Tag(name = "Public - Reports")
public class PublicReportController {

    /**
     * Format des références métier produites par
     * {@code ReportService#generateReference()} : {@code SIG-yyyyMMdd-xxxxxx}.
     * Validé en amont pour rejeter les requêtes mal formées sans toucher la base.
     */
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^SIG-\\d{8}-\\d{6}$");

    private final ReportService reportService;
    private final PublicTrackingService publicTrackingService;
    private final ReplyService replyService;
    private final TurnstileValidationService turnstileValidationService;

    public PublicReportController(
            ReportService reportService,
            PublicTrackingService publicTrackingService,
            ReplyService replyService,
            TurnstileValidationService turnstileValidationService) {
        this.reportService = reportService;
        this.publicTrackingService = publicTrackingService;
        this.replyService = replyService;
        this.turnstileValidationService = turnstileValidationService;
    }


    @PostMapping(value = "/api/public/signalements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Créer un signalement (type et pièces jointes optionnels)",
            description = "La priorité n'est pas acceptée côté voyageur : elle est initialisée automatiquement (MEDIUM) et gérée ensuite par les agents."
                    + " Si Cloudflare Turnstile est activé, le champ turnstileToken est obligatoire et vérifié côté serveur.",
            requestBody = @RequestBody(content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = {
                            @Encoding(name = "report", contentType = MediaType.APPLICATION_JSON_VALUE),
                            @Encoding(name = "files", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    }
            ))
    )
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @Valid @RequestPart("report")
            @Schema(implementation = ReportRequest.class) ReportRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        turnstileValidationService.verifyOrThrow(request.getTurnstileToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Report created", reportService.create(request, files)));
    }

    @GetMapping("/api/public/signalements/mine")
    @Operation(
            summary = "Lister mes 15 derniers signalements",
            description = "Réservé au voyageur authentifié. L'identité vient uniquement du JWT : "
                    + "aucun identifiant voyageur n'est accepté en paramètre. "
                    + "Filtres optionnels combinables entre eux : "
                    + "reference (fragment partiel, insensible à la casse), "
                    + "statusCode (code exact, ex. NEW / IN_PROGRESS / RESOLVED / CLOSED) et "
                    + "creationDate (journee au format yyyy-MM-dd, fuseau UTC). "
                    + "La limite de 15 s'applique après filtrage."
    )
    public ResponseEntity<ApiResponse<List<PublicReportListItemResponse>>> mine(
            @AuthenticationPrincipal PassengerPrincipal principal,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String creationDate) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.of(false, "Authentification requise.", "AUTH_REQUIRED", null));
        }

        LocalDate creationDay;
        if (StringUtils.hasText(creationDate)) {
            try {
                creationDay = LocalDate.parse(creationDate.trim());
            } catch (DateTimeParseException ex) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.of(false,
                                "Date de création invalide (attendu yyyy-MM-dd).",
                                "INVALID_DATE", null));
            }
        } else {
            creationDay = null;
        }

        return ResponseEntity.ok(ApiResponse.ok(
                publicTrackingService.listMine(principal.getPassengerId(), reference, statusCode, creationDay)));
    }

    /**
     * Réponse du voyageur authentifié à une demande de complément.
     * L'identité est celle du jeton, jamais un identifiant envoyé par le client.
     */
    @PostMapping("/api/public/signalements/{uuid}/reponses")
    @Operation(
            summary = "Répondre à une demande de complément",
            description = "Réservé au voyageur propriétaire du signalement. "
                    + "Refusé si le signalement est clôturé ou si aucune demande de complément n'est ouverte."
    )
    public ResponseEntity<ApiResponse<ReplyResponse>> replyToComplement(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal PassengerPrincipal principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody PassengerReplyRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.of(false, "Authentification requise.", "AUTH_REQUIRED", null));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reply created", replyService.addPassengerComplement(uuid, request.getMessage())));
    }

    /**
     * Suivi sécurisé par UUID (lien e-mail). N'expose que les réponses publiques.
     */
    @GetMapping("/api/public/signalements/{uuid}/follow-up")
    @Operation(
            summary = "Suivi sécurisé d'un signalement (UUID)",
            description = "Accès public via lien e-mail (UUID non prévisible). "
                    + "Retourne uniquement les informations destinées au voyageur et les réponses visibles."
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> followUpByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.findByUuid(uuid)));
    }

    /**
     * Consultation publique par référence métier ({@code SIG-yyyyMMdd-xxxxxx}).
     * Aucune authentification : la référence seule suffit, comme l'UUID du lien e-mail.
     * Retourne exactement la même projection restreinte que le suivi par UUID.
     */
    @GetMapping("/api/public/signalements/reference/{reference}")
    @Operation(
            summary = "Consulter un signalement par référence",
            description = "Accès public sans authentification. Référence au format SIG-yyyyMMdd-xxxxxx. "
                    + "Retourne uniquement les informations destinées au voyageur et les réponses visibles, "
                    + "à l'identique de GET /api/public/signalements/{uuid}/follow-up. "
                    + "Sujet à rate limiting par IP."
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> byReference(
            @PathVariable String reference) {
        if (!REFERENCE_PATTERN.matcher(reference.trim()).matches()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of(false,
                            "Référence invalide (attendu SIG-yyyyMMdd-xxxxxx).",
                            "INVALID_REFERENCE", null));
        }
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.findByReference(reference)));
    }

    /**
     * @deprecated Préférer {@link #followUpByUuid(UUID)} — conservé pour compatibilité.
     */
    @Deprecated
    @GetMapping("/api/public/suivi/{uuid}")
    @Operation(
            summary = "[Déprécié] Consulter le suivi d'un signalement par UUID",
            description = "Alias de GET /api/public/signalements/{uuid}/follow-up"
    )
    public ResponseEntity<ApiResponse<PublicReportTrackingResponse>> suiviByUuid(@PathVariable UUID uuid) {
        return followUpByUuid(uuid);
    }
}

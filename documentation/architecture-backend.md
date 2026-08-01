# Architecture Backend Spring Boot — Transport Reporting API

## Objectif

Architecture Backend simple, claire et commune pour toute l'équipe.

## Organisation par couches

```
com.transport.reporting
├── config           # Propriétés (QR, upload, CORS…)
├── controller
│   ├── publicapi    # API voyageurs (/api/public)
│   └── adminapi     # API agents (/api/admin)
├── service          # Logique métier + stockage fichiers
├── repository       # Spring Data JPA
├── entity           # Entités JPA / tables MySQL
├── dto              # Échange Angular
├── mapper           # Entity <-> DTO
├── specification    # Critères de recherche JPA
├── exception        # Gestion globale des erreurs
└── common           # Enums, réponses API communes
```

## Règles

- **Controller** : requêtes HTTP uniquement, aucun traitement métier
- **Service** : règles métier, appelle les repositories / stockage
- **Repository** : accès base de données uniquement
- **Entity** : mapping tables MySQL
- **DTO** : communication Backend ↔ Frontend
- **Mapper** : conversion Entity ↔ DTO

## Module exemple : User

Référence complète pour l'équipe :

- `entity/AppUser`
- `repository/UserRepository`
- `service/UserService`
- `controller/adminapi/UserController`
- `dto/UserRequest` + `UserResponse`
- `mapper/UserMapper`

CRUD : `GET/POST/PUT/DELETE /api/admin/users`

## Module pièces jointes (Attachment)

Réutilise l'entité existante `Attachment` (table `attachment`).

| Élément | Rôle |
|---------|------|
| `config/UploadProperties` | `app.upload.path` |
| `service/FileStorageService` | Validation + écriture disque (UUID) |
| `service/AttachmentService` | Persistence + lecture contenu |
| `mapper/AttachmentMapper` | Entity → `AttachmentResponse` |
| `controller/publicapi/PublicReportController` | Création multipart |
| `controller/adminapi/AdminAttachmentController` | Liste / view / download |

Détails fonctionnels et contraintes : [`attachments.md`](attachments.md).

## Module Journal d'audit (AuditLog)

| Élément | Rôle |
|---------|------|
| `entity/AuditLog` | Table `audit_log` |
| `common/enums/AuditAction`, `AuditModule`, `AuditResult` | Types extensibles |
| `service/AuditLogService` | Recherche + `record()` (REQUIRES_NEW) |
| `controller/adminapi/AdminAuditLogController` | `POST /search`, `GET /{id}` |
| `dto/AuditLogCriteria`, `AuditLogResponse`, `AuditLogEvent` | Recherche / réponse / événement |
| `specification/AuditLogSpecification` | Filtres JPA |

Les services métier appellent `AuditLogService.record` (pas les contrôleurs).  
Détails : [`audit-logs.md`](audit-logs.md).

## Sécurité (JWT + RBAC)

Voir [`security.md`](security.md).

Package `com.transport.reporting.security` : JWT, filter, UserDetails, `@PreAuthorize`.

## Module Signalements — priorité interne

- Création publique : pas de champ priorité ; défaut métier `MEDIUM`
- Admin : `PATCH /api/admin/signalements/{id}/priority` (`REPORT_UPDATE_PRIORITY`)
- Historique : entrée `report_history` (commentaire) + audit `PRIORITY_CHANGE`
- API publique (création / suivi) : priorité non exposée dans la réponse

## Module Réponses agents (Reply)

| Élément | Rôle |
|---------|------|
| `entity/Reply` | Table `reply` (`email_sent`, `public_response`) |
| `dto/ReplyRequest`, `ReplyResponse` | Création / lecture |
| `service/ReplyService` | Réponse + statut + e-mail + visibilité suivi |
| `AdminReplyController` | `GET/POST /api/admin/reports/{id}/replies` |

- `publicResponse` (défaut `true`) : visible dans le suivi voyageur
- `sendEmail` : notification si le voyageur a une adresse e-mail
- `PassengerResponse.anonymous` : sans nom / e-mail / téléphone

## Module Voyageurs (Passenger) — admin

| Élément | Rôle |
|---------|------|
| `entity/Passenger` | Table `passenger` (+ `active`) |
| `dto/PassengerCriteria`, `PassengerResponse` | Recherche / réponse |
| `specification/PassengerSpecification` | Filtres (dont état actif) |
| `service/PassengerService` | `search`, `findById`, `setActive`, `findOrCreate` |
| `controller/adminapi/AdminPassengerController` | `POST /search`, `GET /{id}`, `PATCH …/activate\|deactivate` |

Permissions : `PASSENGER_VIEW`, `PASSENGER_SEARCH`, `PASSENGER_ACTIVATE`, `PASSENGER_DEACTIVATE`.

## Module Rapports & Statistiques (squelette)

| Élément | Rôle |
|---------|------|
| `dto/StatisticsOverviewResponse` | Indicateurs de base |
| `service/StatisticsService` | Agrégats (extensible) |
| `controller/adminapi/AdminStatisticsController` | `GET /api/admin/statistics/overview` |

Permission : `REPORT_STATISTICS_VIEW`.

## Entités / tables

`SUPPORT_TYPE`, `TRANSPORT_SUPPORT`, `REPORT_TYPE`, `PASSENGER`, `STATUS`, `APP_USER`, `ROLE`, `PERMISSION`, `USER_ROLE`, `ROLE_PERMISSION`, `APP_MENU`, `REPORT`, `ATTACHMENT`, `REPORT_HISTORY`, `REPLY`, `AUDIT_LOG`

## Swagger

http://localhost:8080/swagger-ui/index.html

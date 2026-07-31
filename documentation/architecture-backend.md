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

## Entités / tables

`SUPPORT_TYPE`, `TRANSPORT_SUPPORT`, `REPORT_TYPE`, `PASSENGER`, `STATUS`, `APP_USER`, `REPORT`, `ATTACHMENT`, `REPORT_HISTORY`, `REPLY`, `AUDIT_LOG`

## Swagger

http://localhost:8080/swagger-ui/index.html

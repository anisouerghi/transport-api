# Journal d'audit (Audit Logs)

## Objectif

Tracer automatiquement les actions importantes réalisées dans l'application (administration et opérations associées) afin d'assurer la traçabilité.

Les écritures d'audit sont déclenchées **depuis les services métier**, jamais depuis les contrôleurs.

## Modèle

Table `audit_log` / entité `AuditLog`.

| Champ | Description |
|-------|-------------|
| `auditLogId` | Identifiant technique |
| `actionDate` | Date + heure (Instant) |
| `userId` / `username` / `userFullName` | Acteur |
| `ipAddress` | Adresse IP (X-Forwarded-For ou remote) |
| `actionType` | Enum `AuditAction` (CREATE, UPDATE, DELETE, REPLY, …) |
| `module` | Enum `AuditModule` (USERS, REPORTS, …) |
| `entityName` / `entityId` | Entité concernée |
| `oldValue` / `newValue` | Instantané textuel avant / après |
| `description` | Libellé lisible |
| `userAgent` / `browser` / `operatingSystem` | Contexte client |
| `result` | SUCCESS / FAILURE |

Les enums sont stockés en `VARCHAR` : de nouvelles valeurs peuvent être ajoutées sans migration de schéma.

## API Admin

Base : `/api/admin/audit-logs`

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/search` | Recherche paginée multicritère |
| `GET` | `/{id}` | Détail d'une entrée |

### Critères (`AuditLogCriteria`)

Tous optionnels et combinables : `user`, `userId`, `module`, `actionType`, `actionDateFrom`, `actionDateTo`, `result`, `ipAddress`, `entityName`, `entityId`.

### Exemple recherche

```http
POST /api/admin/audit-logs/search
Content-Type: application/json

{
  "filters": {
    "module": "USERS",
    "actionType": "UPDATE",
    "result": "SUCCESS",
    "actionDateFrom": "2026-07-01T00:00:00Z",
    "actionDateTo": "2026-07-31T23:59:59Z"
  },
  "pageable": {
    "page": 0,
    "size": 10,
    "sortBy": "actionDate",
    "sortDirection": "DESC"
  }
}
```

## Enregistrement automatique

`AuditLogService.record(AuditLogEvent)` :

- transaction `REQUIRES_NEW` (n'impacte pas la transaction métier) ;
- enrichit IP / User-Agent via `RequestMetadata` ;
- parse navigateur / OS via `UserAgentParser` ;
- charge username / nom depuis `userId` si besoin ;
- avale les erreurs d'écriture (log ERROR) pour ne pas casser le métier.

Tant qu'il n'y a pas de JWT, les opérations admin sont attribuées à l'utilisateur seed (`AuditActors.DEFAULT_ADMIN_USER_ID = 1`).

### Services branchés

| Service | Actions tracées |
|---------|-----------------|
| `UserService` | CREATE, UPDATE, DELETE, activation |
| `SupportTypeService` | CREATE, UPDATE, DELETE |
| `ReportTypeService` | CREATE, UPDATE, DELETE, activation |
| `ReportNatureService` | CREATE, UPDATE, DELETE, activation |
| `ReportService.updateNature` | `NATURE_CHANGE` |
| `TransportSupportService` | CREATE, UPDATE, DELETE, régénération QR |
| `ReportService` | CREATE public (acteur PUBLIC) |
| `ReplyService` | REPLY + STATUS_CHANGE |

## Frontend admin

Route : `/audit-logs` — menu **Journal d'audit**.

- liste paginée Bootstrap / CoreUI ;
- filtres : utilisateur, module, action, dates, résultat, IP ;
- tri colonnes ;
- modal de détail (anciennes / nouvelles valeurs) ;
- bouton Actualiser.

## Extension

1. Ajouter une valeur à `AuditAction` ou `AuditModule`.
2. Appeler `auditLogService.record(...)` dans le service métier concerné.
3. Aucune migration SQL nécessaire (enums en VARCHAR).

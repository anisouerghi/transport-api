# Module `common`

Bibliothèque partagée (JAR non exécutable).

## Contenu (après étapes 3–5)

- Entities, repositories, dto, mappers, specifications
- Exceptions métier + `GlobalExceptionHandler`
- Sécurité partagée : JWT, principals, filtre, `PermissionChecker`, `AuditActors`
- Config partagée : upload, QR, frontend URL, password encoder, mail sanitizer
- Services partagés : Report, ReportType, TransportSupport, Passenger(+auth), Attachment, Email, QR, AuditLog, Status, Role, Dashboard, Statistics, PublicTracking…

## Non inclus

- Controllers HTTP (dans `public-api` / `transport-api`)
- `SecurityConfig` admin, initializers schéma / seed (`transport-api`)
- `PublicSecurityConfig` (`public-api`)

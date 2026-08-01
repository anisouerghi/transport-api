# transport-api

Architecture Backend Spring Boot — signalement transport public.

## Organisation (par couches)

```
com.transport.reporting
├── config
├── controller
│   ├── publicapi     # /api/public/**
│   └── adminapi      # /api/admin/**
├── service
├── repository
├── entity
├── dto
├── mapper
├── exception
├── specification
└── common
```

## Règles

| Couche | Responsabilité |
|--------|----------------|
| Controller | HTTP uniquement |
| Service | Logique métier |
| Repository | Accès JPA |
| Entity | Tables MySQL |
| DTO | Échange Angular |
| Mapper | Entity ↔ DTO |

## Base MySQL

```sql
CREATE DATABASE IF NOT EXISTS transport_reporting;
```

## Démarrage

```bash
mvn spring-boot:run
```

- Swagger : http://localhost:8080/swagger-ui/index.html
- Admin démo : `admin` / `admin123`

## Configuration utile

| Propriété | Rôle |
|-----------|------|
| `app.qr.base-url` | Base URL des QR (`{base}/report/{uuid}`) |
| `app.qr.storage-path` | Images QR générées |
| `app.upload.path` | Répertoire des pièces jointes |
| `spring.servlet.multipart.max-file-size` | 10MB |
| `spring.servlet.multipart.max-request-size` | 30MB |

## API

### Public `/api/public`

| Méthode | Endpoint | Notes |
|---------|----------|-------|
| GET | `/api/public/supports/{uuid}` | Support actif via QR |
| GET | `/api/public/report-types` | Types actifs |
| POST | `/api/public/signalements` | **multipart** : part `report` (JSON) + `files` (optionnel) |
| GET | `/api/public/suivi/{reference}` | Suivi voyageur |

### Admin `/api/admin`

| Méthode | Endpoint |
|---------|----------|
| CRUD | `/api/admin/users` |
| search / CRUD | `/api/admin/support-types`, `/api/admin/report-types`, `/api/admin/transport-supports` |
| GET / POST search | `/api/admin/signalements` |
| GET | `/api/admin/signalements/{id}` |
| GET | `/api/admin/signalements/{id}/attachments` |
| GET | `/api/admin/attachments/{id}/view` |
| GET | `/api/admin/attachments/{id}/download` |
| GET/POST | `/api/admin/reports/{id}/replies` |
| GET | `/api/admin/dashboard` |
| GET | `/api/admin/statuses` ou `/api/admin/status` |
| POST search / GET | `/api/admin/audit-logs` |
| CRUD | `/api/admin/roles` |
| GET | `/api/admin/permissions` |
| POST | `/api/auth/login` |
| GET | `/api/auth/me` |

## Sécurité

Voir : [`documentation/security.md`](documentation/security.md).

Compte seed : `admin` / `admin123`

## Journal d'audit

Voir : [`documentation/audit-logs.md`](documentation/audit-logs.md).

## Pièces jointes

Voir la documentation détaillée : [`documentation/attachments.md`](documentation/attachments.md).

Documentation d'architecture : [`documentation/architecture-backend.md`](documentation/architecture-backend.md).

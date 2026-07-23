# transport-api

Squelette Backend Spring Boot — signalement transport public.

## Stack

- Java 17 / Spring Boot 3.3
- Spring Web + Data JPA + Validation
- MySQL (`transport_reporting`)
- Lombok / springdoc OpenAPI

## Structure

```
com.transport.reporting
├── config          # SwaggerConfig, WebConfig, DataInitializer
├── common          # response, exception, enums, dto
├── modules
│   ├── user        # CRUD complet (modèle)
│   ├── support
│   ├── report
│   ├── passenger
│   ├── status
│   └── dashboard
└── TransportApplication.java
```

Chaque module : `controller` / `service` / `repository` / `entity` / `dto`

## Base MySQL

```sql
CREATE DATABASE IF NOT EXISTS transport_reporting
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tables : `SUPPORT_TYPE`, `TRANSPORT_SUPPORT`, `REPORT_TYPE`, `PASSENGER`, `STATUS`, `APP_USER`, `REPORT`, `ATTACHMENT`, `REPORT_HISTORY`, `REPLY`

## Démarrage

```bash
mvn spring-boot:run
```

- Swagger : http://localhost:8080/swagger-ui/index.html
- Admin démo : `admin` / `admin123`

## API

### Public `/api/public`

| Méthode | Endpoint |
|---------|----------|
| GET | `/api/public/supports/{uuid}` |
| POST | `/api/public/signalements` |
| GET | `/api/public/suivi/{reference}` |

### Admin `/api/admin`

| Méthode | Endpoint |
|---------|----------|
| CRUD | `/api/admin/users` |
| GET/POST | `/api/admin/supports` |
| GET | `/api/admin/signalements` |
| GET | `/api/admin/dashboard` |
| GET | `/api/admin/statuses` |

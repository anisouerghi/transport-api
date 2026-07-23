# Architecture Backend Spring Boot — Transport Reporting API

## Objectif

Architecture Backend simple, claire et commune pour toute l'équipe.

## Organisation par couches

```
com.transport.reporting
├── config
├── controller
│   ├── publicapi    # API voyageurs (/api/public)
│   └── adminapi     # API agents (/api/admin)
├── service          # Logique métier
├── repository       # Spring Data JPA
├── entity           # Entités JPA / tables MySQL
├── dto              # Échange Angular
├── mapper           # Entity <-> DTO
├── exception        # Gestion globale des erreurs
└── common           # Enums, réponses API communes
```

## Règles

- **Controller** : requêtes HTTP uniquement, aucun traitement métier
- **Service** : règles métier, appelle les repositories
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

## Entités / tables

`SUPPORT_TYPE`, `TRANSPORT_SUPPORT`, `REPORT_TYPE`, `PASSENGER`, `STATUS`, `APP_USER`, `REPORT`, `ATTACHMENT`, `REPORT_HISTORY`, `REPLY`

## Swagger

http://localhost:8080/swagger-ui/index.html

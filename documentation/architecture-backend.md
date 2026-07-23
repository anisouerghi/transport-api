# Architecture Backend – Transport Reporting API

## Objectif

Squelette Spring Boot commun pour l'équipe.

## Modules

| Module | Rôle |
|--------|------|
| `user` | Utilisateurs internes — **CRUD complet (modèle)** |
| `support` | Types et supports QR |
| `report` | Signalements, pièces jointes, historique, réponses |
| `passenger` | Voyageurs |
| `status` | Statuts workflow |
| `dashboard` | Indicateurs |

## Convention module

```
module/
├── controller
├── service
├── repository
├── entity
└── dto
```

Controller → Service → Repository. Pas de logique métier dans le controller. Pas d'Entity exposée en API.

## Tables MySQL (`transport_reporting`)

`SUPPORT_TYPE`, `TRANSPORT_SUPPORT`, `REPORT_TYPE`, `PASSENGER`, `STATUS`, `APP_USER`, `REPORT`, `ATTACHMENT`, `REPORT_HISTORY`, `REPLY`

## Swagger

http://localhost:8080/swagger-ui/index.html
